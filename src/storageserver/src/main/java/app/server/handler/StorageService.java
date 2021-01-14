/*
 * Runs all services for storage services messages.
 * 
 * @author Grupo10-FSD
 * 
*/

package app.server.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import app.Config;
import app.Serialization;
import app.data.CMRequestGet;
import app.data.CMRequestPut;
import app.data.CMResponseGet;
import app.data.CMResponsePut;
import app.server.clock.LogicalClockTool;
import app.server.data.GetTransaction;
import app.server.data.PutTransaction;
import app.server.data.SMRequest;
import app.server.data.SMResponse;
import app.server.data.StorageValue;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;

public class StorageService {

    /**
     * Logs all events to console.
     */
    private static Logger LOGGER = Logger.getLogger(StorageService.class);

    /**
     * Executor service
     */
    private ScheduledExecutorService executorService;
    /**
     * Messaging service
     */
    private NettyMessagingService messagingService;
    /**
     * Database set of this server id
     */
    private ConcurrentHashMap<Long, StorageValue> DATABASE_SET;
    /**
     * Local logical clock
     */
    private int[] LOGICAL_CLOCK;
    /**
     * Server id
     */
    private int SERVER_ID;

    /**
     * Locks for concurrency control in clock access and transaction access
     */
    static ReentrantLock lockDispach = new ReentrantLock();
    static ReentrantLock lockCheckFinished = new ReentrantLock();
    static ReentrantLock lockClock = new ReentrantLock();

    // static boolean canEnter = true;

    /**
     * Get transactions
     */
    private ConcurrentHashMap<Integer, HashMap<Integer, PutTransaction>> WAITING_PUTS;
    /**
     * Put transactions
     */
    private ConcurrentHashMap<Integer, HashMap<Integer, GetTransaction>> WAITING_GETS;
    /**
     * Queue of unresolved requests
     */
    private ConcurrentLinkedQueue<SMRequest> QUEUE_REQUESTS;
    /**
     * Queue of unresolved responses
     */
    private ConcurrentLinkedQueue<SMResponse> QUEUE_RESPONSES;

    /**
     * Constructor for StorageService.
     * 
     * @param sid   server id
     * @param sport server port
     * @param clock server clock
     */
    public StorageService(int sid, int sport, int[] clock) {

        this.executorService = Executors.newScheduledThreadPool(Config.server_thread_pool_size);
        this.messagingService = new NettyMessagingService("serverms_" + sid, Address.from(sport),
                new MessagingConfig());

        this.SERVER_ID = sid;
        this.LOGICAL_CLOCK = clock;

        this.DATABASE_SET = new ConcurrentHashMap<>();

        if (sid == 0)
            this.DATABASE_SET.put((long) 0, new StorageValue("old".getBytes(), new int[] { 1, -1 }, 0));

        this.WAITING_PUTS = new ConcurrentHashMap<>();
        this.WAITING_GETS = new ConcurrentHashMap<>();

        this.QUEUE_REQUESTS = new ConcurrentLinkedQueue<>();
        this.QUEUE_RESPONSES = new ConcurrentLinkedQueue<>();
    }

    public void start() {

        LOGGER.warn("registering handlers...");

        this.register_handlers();

        LOGGER.warn("starting messaging service...");

        this.messagingService.start();
    }

    /**
     * Registers all handlers for the service
     */
    public void register_handlers() {

        this.register_client_put();
        this.register_client_get();
        this.register_server_request_put();
        this.register_server_request_get();
        this.register_server_response_put();
        this.register_server_response_get();
        this.register_server_update_clock();
    }

    /**
     * Gets destination server using circular based array
     * 
     * @param key key to query
     * @return destination id
     */
    public int find_storage_service(Long key) {
        return (int) (key % Config.nr_servers);
    }

    /**
     * Generic function to send an assynchronous message
     * 
     * @param port        destination port
     * @param typeHandler type of message
     * @param data        message byte array
     * @param print       log message
     */
    public void sendAsync(int port, String typeHandler, byte[] data, String print) {

        this.messagingService.sendAsync(Address.from("localhost", port), typeHandler, data).thenRun(() -> {
            LOGGER.warn("[" + typeHandler + "] " + print);
        }).exceptionally(t -> {
            t.printStackTrace();
            return null;
        });
    }

    /**
     * Registers client put message handler
     */
    private void register_client_put() {

        this.messagingService.registerHandler("client_put", (address, messageBytes) -> {

            CMRequestPut cmreqputMessage = null;

            try {
                cmreqputMessage = (CMRequestPut) Serialization.deserialize(messageBytes);
            } catch (ClassNotFoundException | IOException e) {
            }

            // create client put transaction

            int[] copyTimestamp = new int[Config.nr_servers];
            System.arraycopy(LOGICAL_CLOCK, 0, copyTimestamp, 0, Config.nr_servers);
            int requestID = cmreqputMessage.getMESSAGE_ID();

            List<Long> keysToUpdate = cmreqputMessage.getRequestPut().keySet().stream().collect(Collectors.toList());
            PutTransaction transaction = new PutTransaction(keysToUpdate, copyTimestamp, cmreqputMessage.getCLI_PORT(),
                    requestID);

            LOGGER.info("received client " + address + " PUT request (transaction: " + requestID + ") | timestamp = "
                    + LogicalClockTool.printArray(copyTimestamp));
            // LOGGER.info("PUT/[" + requestID + "] PUT transaction: " +
            // transaction.toString());

            int clientPort = cmreqputMessage.getCLI_PORT();
            if (this.WAITING_PUTS.containsKey(clientPort)) {

                HashMap<Integer, PutTransaction> beforeMap = this.WAITING_PUTS.get(clientPort);
                beforeMap.put(requestID, transaction);

            } else {

                HashMap<Integer, PutTransaction> newMap = new HashMap<>();
                newMap.put(requestID, transaction);
                this.WAITING_PUTS.put(clientPort, newMap);
            }

            boolean imAlwaysTheDestination = true;

            for (Map.Entry<Long, byte[]> pedidoEntry : cmreqputMessage.getRequestPut().entrySet()) {

                Long keyToProcess = pedidoEntry.getKey();
                byte[] keyData = pedidoEntry.getValue();
                StorageValue svData = new StorageValue(keyData, copyTimestamp, this.SERVER_ID);

                int keyDestinationServerID = this.find_storage_service(keyToProcess);

                if (keyDestinationServerID == this.SERVER_ID) {

                    LOGGER.info("PUT/[" + requestID + "] processing key " + keyToProcess + " [my key]");

                    transaction.setDone(keyToProcess);

                    if (this.DATABASE_SET.containsKey(keyToProcess)) {

                        StorageValue lValue = this.DATABASE_SET.get(keyToProcess);

                        if (LogicalClockTool.areConcurrent(lValue.getTimeStamp(), svData.getTimeStamp())) {

                            // conflito detetado

                            if (lValue.getServerWhichUpdate() < this.SERVER_ID) {

                                // destination server wins
                                LOGGER.info("PUT/[" + requestID + "] time conflict on key " + keyToProcess
                                        + " [my key], [WIN] NOT updating");

                            } else {

                                // source server wins

                                LOGGER.info("PUT/[" + requestID + "] time conflict on key " + keyToProcess
                                        + " [my key], [LOOSE] updating...");

                                lValue.setServerWhichUpdate(svData.getServerWhichUpdate());
                                this.DATABASE_SET.replace(keyToProcess, svData);
                            }

                        } else {

                            int countBefore = 0;
                            for (int i = 0; i < Config.nr_servers; i++) {
                                if (lValue.getTimeStamp()[i] < svData.getTimeStamp()[i]) {
                                    countBefore++;
                                }
                            }

                            if (countBefore == Config.nr_servers) {

                                LOGGER.info("PUT/[" + requestID + "] no time conflict, key " + keyToProcess
                                        + " [my key], replacing...");

                                this.DATABASE_SET.replace(keyToProcess, svData);
                            }
                        }

                    } else {

                        LOGGER.info("PUT/[" + requestID + "] new key " + keyToProcess + " [my key], inserted...");

                        svData.setServerWhichUpdate(this.SERVER_ID);
                        this.DATABASE_SET.put(keyToProcess, svData);
                    }

                    // LOGGER.info("PUT/[" + requestID + "] my database = " +
                    // this.DATABASE_SET.toString());

                } else {

                    lockClock.lock();
                    // incrementar relogio
                    this.LOGICAL_CLOCK[this.SERVER_ID]++;

                    int[] copyClock = new int[Config.nr_servers];
                    System.arraycopy(LOGICAL_CLOCK, 0, copyClock, 0, Config.nr_servers);
                    lockClock.unlock();

                    // enviar pedido para o servidor certo

                    LOGGER.info("PUT/[" + requestID + "] processing key " + keyToProcess + " [NOT my key]");

                    imAlwaysTheDestination = false;

                    SMRequest sendRequest = new SMRequest(keyToProcess, svData, this.SERVER_ID, requestID, "put",
                            copyClock, copyTimestamp);
                    sendRequest.setClientPort(clientPort);

                    byte[] sendBytes = null;

                    try {
                        sendBytes = Serialization.serialize(sendRequest);
                    } catch (IOException e) {
                    }

                    int toServer = Config.init_server_port + keyDestinationServerID;
                    String toPrint = "PUT/ requesting server " + toServer + " to put key " + keyToProcess;
                    this.sendAsync(toServer, "server_request_put", sendBytes, toPrint);

                    // enviar pedido para todos de update clock

                    SMRequest sendUpdateClock = new SMRequest(copyClock, this.SERVER_ID);

                    this.send_update_clock_all_servers(sendUpdateClock, keyDestinationServerID);
                }
            }

            if (imAlwaysTheDestination) {

                LOGGER.info("PUT/ [" + requestID + "] i was always the destination, responding back to client");

                CMResponsePut responsePut = new CMResponsePut(requestID);

                byte[] sendBytes = null;

                try {
                    sendBytes = Serialization.serialize(responsePut);
                } catch (IOException e) {
                }

                int cPort = transaction.getClientPort();
                String toPrint = "PUT/ client " + cPort + " PUT transaction finished, vou avisar o cliente";

                this.sendAsync(cPort, "client_response_put", sendBytes, toPrint);

                this.WAITING_PUTS.get(clientPort).remove(requestID);

                if (this.WAITING_PUTS.get(clientPort).isEmpty())
                    this.WAITING_PUTS.remove(clientPort);
            }

        }, this.executorService);
    }

    /**
     * Registers client get message handler
     */
    private void register_client_get() {

        this.messagingService.registerHandler("client_get", (address, messageBytes) -> {

            CMRequestGet cmreqgetMessage = null;

            try {
                cmreqgetMessage = (CMRequestGet) Serialization.deserialize(messageBytes);
            } catch (ClassNotFoundException | IOException e) {
            }

            Collection<Long> keys = cmreqgetMessage.getKeysToRequest();
            int clientPort = cmreqgetMessage.getCLI_PORT();
            int transactionID = cmreqgetMessage.getMESSAGE_ID();
            GetTransaction transaction = new GetTransaction(clientPort, transactionID, keys);

            LOGGER.info("received client " + address + " GET request (transaction: " + transactionID + ")");
            LOGGER.info("[" + transactionID + "] GET transaction: " + keys.toString());

            if (this.WAITING_GETS.containsKey(clientPort)) {
                HashMap<Integer, GetTransaction> beforeMap = this.WAITING_GETS.get(clientPort);
                beforeMap.put(transactionID, transaction);
            } else {
                HashMap<Integer, GetTransaction> newMap = new HashMap<>();
                newMap.put(transactionID, transaction);
                this.WAITING_GETS.put(clientPort, newMap);
            }

            boolean isAlwaysForMe = true;

            for (Long keyToGet : keys) {

                int keyDestinationServerID = this.find_storage_service(keyToGet);

                if (keyDestinationServerID == this.SERVER_ID) {

                    if (this.DATABASE_SET.containsKey(keyToGet)) {

                        LOGGER.info("GET/[" + transactionID + "] processing key " + keyToGet + " [my key] FOUND");

                        StorageValue value = this.DATABASE_SET.get(keyToGet);

                        transaction.setDone(keyToGet, value.getData());
                        transaction.incrementDone();

                    } else {

                        LOGGER.info("GET/[" + transactionID + "] processing key " + keyToGet
                                + " [my key] NOT FOUND, removing from transaction");

                        transaction.removeUnexisting(keyToGet);
                    }

                } else {

                    isAlwaysForMe = false;

                    lockClock.lock();

                    // incrementar relogio
                    this.LOGICAL_CLOCK[this.SERVER_ID]++;

                    int[] copyClock = new int[Config.nr_servers];
                    System.arraycopy(LOGICAL_CLOCK, 0, copyClock, 0, Config.nr_servers);

                    lockClock.unlock();

                    LOGGER.info("GET/[" + transactionID + "] processing key " + keyToGet + " [NOT my key], asking "
                            + (keyDestinationServerID + Config.init_server_port));

                    SMRequest smreqMessage = new SMRequest(keyToGet, this.SERVER_ID, transactionID, "get", copyClock);
                    smreqMessage.setClientPort(clientPort);

                    byte[] sendBytes = null;

                    try {
                        sendBytes = Serialization.serialize(smreqMessage);
                    } catch (IOException e) {
                    }

                    int toServer = Config.init_server_port + keyDestinationServerID;
                    String toPrint = "GET/ requesting server " + toServer + " to get key " + keyToGet;
                    this.sendAsync(toServer, "server_request_get", sendBytes, toPrint);

                    SMRequest sendUpdateClock = new SMRequest(copyClock, this.SERVER_ID);

                    this.send_update_clock_all_servers(sendUpdateClock, keyDestinationServerID);
                }
            }

            if (isAlwaysForMe && transaction.isFinished()) {

                LOGGER.info("GET/ [" + transactionID + "] transaction finished, responding back to client");

                Map<Long, byte[]> resultGet = transaction.getPreparedGets();
                CMResponseGet responseGet = new CMResponseGet(transactionID, resultGet);

                byte[] sendBytes = null;

                try {
                    sendBytes = Serialization.serialize(responseGet);
                } catch (IOException e) {
                }

                String toPrint = "GET/ responding to client " + clientPort + " GET transaction " + transactionID;
                this.sendAsync(clientPort, "client_response_get", sendBytes, toPrint);
            }

        }, this.executorService);

    }

    /**
     * Registers server request put message handler
     */
    private void register_server_request_put() {

        this.messagingService.registerHandler("server_request_put", (address, messageBytes) -> {

            SMRequest smreqputMessage = null;

            try {
                smreqputMessage = (SMRequest) Serialization.deserialize(messageBytes);
            } catch (ClassNotFoundException | IOException e) {
            }

            boolean canBeExecuted = LogicalClockTool.conditionVerifier(smreqputMessage, this.LOGICAL_CLOCK);

            LOGGER.info("PUT/ [from: " + address + "] received server request put | can execute = " + canBeExecuted);

            if (canBeExecuted) {
                process_server_request_put(smreqputMessage);
            } else {
                LOGGER.info("REQ_PUT/ [from: " + address + "] added request put key " + smreqputMessage.getKeyToVerify()
                        + "to requests queue...");
                this.QUEUE_REQUESTS.add(smreqputMessage);
            }

            LOGGER.info("[from: " + address + "] dispaching events...");
            dispach_queued_events();

        }, this.executorService);
    }

    /**
     * Registers server request get message handler
     */
    private void register_server_request_get() {

        this.messagingService.registerHandler("server_request_get", (address, messageBytes) -> {

            SMRequest smreqgetMessage = null;

            try {
                smreqgetMessage = (SMRequest) Serialization.deserialize(messageBytes);
            } catch (ClassNotFoundException | IOException e) {
            }

            boolean canBeExecuted = LogicalClockTool.conditionVerifier(smreqgetMessage, this.LOGICAL_CLOCK);

            LOGGER.info(
                    "REQ_GET/ [from: " + address + "] received server request get | can execute = " + canBeExecuted);

            if (canBeExecuted) {
                process_server_request_get(smreqgetMessage);
            } else {
                LOGGER.info("REQ_GET/ [from: " + address + "] added request get key " + smreqgetMessage.getKeyToVerify()
                        + "to requests queue...");

                this.QUEUE_REQUESTS.add(smreqgetMessage);
            }

            LOGGER.info("[from: " + address + "] dispaching events...");
            dispach_queued_events();

        }, this.executorService);
    }

    /**
     * Registers server response put message handler
     */
    private void register_server_response_put() {

        this.messagingService.registerHandler("server_response_put", (address, messageBytes) -> {

            SMResponse smresputMessage = null;

            try {
                smresputMessage = (SMResponse) Serialization.deserialize(messageBytes);
            } catch (ClassNotFoundException | IOException e) {
            }

            boolean canBeExecuted = LogicalClockTool.conditionVerifier(smresputMessage, LOGICAL_CLOCK);

            LOGGER.info("RES_PUT/ [from: " + address + "] received server response put " + smresputMessage.getKey()
                    + " | can execute = " + canBeExecuted);

            if (canBeExecuted) {
                this.process_server_response_put(smresputMessage);
            } else {
                LOGGER.info("RES_PUT/ [from: " + address + "] added response put key " + smresputMessage.getKey()
                        + "to responses queue...");

                this.QUEUE_RESPONSES.add(smresputMessage);
            }

            LOGGER.info("[from: " + address + "] dispaching events...");
            dispach_queued_events();

        }, this.executorService);
    }

    /**
     * Registers server response get message handler
     */
    private void register_server_response_get() {

        this.messagingService.registerHandler("server_response_get", (address, messageBytes) -> {

            SMResponse smresgetMessage = null;

            try {
                smresgetMessage = (SMResponse) Serialization.deserialize(messageBytes);
            } catch (ClassNotFoundException | IOException e) {
            }

            boolean canBeExecuted = LogicalClockTool.conditionVerifier(smresgetMessage, LOGICAL_CLOCK);

            LOGGER.info("RES_GET/ [from: " + address + "] received server response get  " + smresgetMessage.getKey()
                    + " | can execute = " + canBeExecuted);

            if (canBeExecuted) {
                this.process_server_response_get(smresgetMessage);
            } else {
                LOGGER.info("RES_GET/ [from: " + address + "] added response get key " + smresgetMessage.getKey()
                        + "to responses queue...");

                this.QUEUE_RESPONSES.add(smresgetMessage);
            }

            LOGGER.info("[from: " + address + "] dispaching events...");
            dispach_queued_events();

        }, this.executorService);

    }

    /**
     * Sends update clock messages to all servers (except current and destination)
     */
    public void send_update_clock_all_servers(SMRequest sendUpdateClock, int destination) {

        byte[] sendBytesUpdateClock = null;

        try {
            sendBytesUpdateClock = Serialization.serialize(sendUpdateClock);
        } catch (IOException e) {
        }

        for (int i = 0; i < Config.nr_servers; i++) {

            if (i != this.SERVER_ID && i != destination) {
                int toPort = Config.init_server_port + i;
                String print = "CLOCK_UPDATE/ sending update clock to server " + toPort;
                this.sendAsync(toPort, "server_update_clock", sendBytesUpdateClock, print);
            }
        }
    }

    /**
     * Registers server update clock message handler
     */
    private void register_server_update_clock() {

        this.messagingService.registerHandler("server_update_clock", (address, messageBytes) -> {

            SMRequest update_clock_message = null;

            try {
                update_clock_message = (SMRequest) Serialization.deserialize(messageBytes);
            } catch (ClassNotFoundException | IOException e) {
            }

            boolean canBeExecuted = LogicalClockTool.conditionVerifier(update_clock_message, this.LOGICAL_CLOCK);

            LOGGER.info("CLOCK_UPDATE/ [from: " + address + "] received server clock update | can execute = "
                    + canBeExecuted);

            if (!canBeExecuted) {
                this.QUEUE_REQUESTS.add(update_clock_message);
                LOGGER.info("CLOCK_UPDATE/ added clock update message "
                        + LogicalClockTool.printArray(update_clock_message.getClock()) + " | my clock = "
                        + LogicalClockTool.printArray(this.LOGICAL_CLOCK) + "to requests queue...");
            }

            LOGGER.info("[from: " + address + "] dispaching events...");
            dispach_queued_events();

        }, this.executorService);
    }

    /**
     * Processes server request put message handler
     */
    private void process_server_request_put(SMRequest smreqputMessage) {

        int fromPort = smreqputMessage.getFromID() + Config.init_server_port;

        Long keyRequest = smreqputMessage.getKeyToVerify();

        StorageValue requestedValue = smreqputMessage.getKeyValue();
        StorageValue lastValue = this.DATABASE_SET.get(keyRequest);

        LOGGER.info("[from: " + fromPort + "] received server request put key " + keyRequest + " | "
                + LogicalClockTool.printArray(requestedValue.getTimeStamp()));

        boolean wasUpdated = true;

        if (this.DATABASE_SET.containsKey(keyRequest)) {

            if (LogicalClockTool.areConcurrent(lastValue.getTimeStamp(), requestedValue.getTimeStamp())) {

                // conflito detetado

                if (lastValue.getServerWhichUpdate() < smreqputMessage.getFromID()) {

                    LOGGER.info("[key: " + keyRequest + "] time conflict, [WIN] NOT updating...");

                    wasUpdated = false;

                } else {

                    LOGGER.info("[key: " + keyRequest + "] time conflict, [LOOSE] updating...");

                    wasUpdated = true;

                    requestedValue.setServerWhichUpdate(smreqputMessage.getFromID());
                    this.DATABASE_SET.replace(keyRequest, requestedValue);
                }

            } else {

                int countBefore = 0;
                for (int i = 0; i < Config.nr_servers; i++) {
                    if (lastValue.getTimeStamp()[i] < requestedValue.getTimeStamp()[i]) {
                        countBefore++;
                    }
                }

                if (countBefore == Config.nr_servers) {

                    LOGGER.info("[key: " + keyRequest + "] NO conflict, replacing...");

                    this.DATABASE_SET.replace(keyRequest, requestedValue);
                }
            }

        } else {

            LOGGER.info("[key: " + keyRequest + "] NO exists, inserting...");

            requestedValue.setServerWhichUpdate(smreqputMessage.getFromID());

            this.DATABASE_SET.put(keyRequest, requestedValue);
        }

        // LOGGER.info("my database = " + this.DATABASE_SET.toString());

        lockClock.lock();

        // incrementar relogio
        this.LOGICAL_CLOCK[this.SERVER_ID]++;

        int[] copyClock = new int[Config.nr_servers];
        System.arraycopy(LOGICAL_CLOCK, 0, copyClock, 0, Config.nr_servers);

        lockClock.unlock();

        int reqID = smreqputMessage.getRequestID();
        SMResponse smresputMessage = new SMResponse(reqID, wasUpdated, keyRequest, "put", copyClock, this.SERVER_ID);
        smresputMessage.setClientPort(smreqputMessage.getClientPort());

        byte[] sendBytes = null;

        try {
            sendBytes = Serialization.serialize(smresputMessage);
        } catch (IOException e) {
        }

        String print = "key " + smreqputMessage.getKeyToVerify() + " responding with request for id " + reqID
                + " to server " + fromPort;
        this.sendAsync(fromPort, "server_response_put", sendBytes, print);

        SMRequest sendUpdateClock = new SMRequest(copyClock, this.SERVER_ID);
        this.send_update_clock_all_servers(sendUpdateClock, smreqputMessage.getFromID());

        LOGGER.info("dispaching events...");
        dispach_queued_events();

        LOGGER.warn(">>>>>>> [DATABASE]: " + this.DATABASE_SET.size());
    }

    /**
     * Processes server request get message handler
     */
    public void process_server_request_get(SMRequest smreqgetMessage) {

        Long keyToCheck = smreqgetMessage.getKeyToVerify();
        StorageValue resultValue = null;

        int transactionID = smreqgetMessage.getRequestID();

        lockClock.lock();

        // incrementar relogio
        this.LOGICAL_CLOCK[this.SERVER_ID]++;

        int[] copyClock = new int[Config.nr_servers];
        System.arraycopy(LOGICAL_CLOCK, 0, copyClock, 0, Config.nr_servers);

        lockClock.unlock();

        LOGGER.info("[transaction: " + transactionID + "] received server request get key " + keyToCheck);

        SMResponse smresgetMessage = new SMResponse(transactionID, keyToCheck, "get", copyClock, this.SERVER_ID);
        smresgetMessage.setClientPort(smreqgetMessage.getClientPort());

        if (this.DATABASE_SET.containsKey(keyToCheck)) {

            LOGGER.info(
                    "[transaction: " + transactionID + "] processing key " + keyToCheck + " [FOUND] saving value...");

            resultValue = this.DATABASE_SET.get(keyToCheck);

            smresgetMessage.setHasValue(true);
            smresgetMessage.setValue(resultValue);

        } else {

            LOGGER.info("[transaction: " + transactionID + "] processing key " + keyToCheck
                    + " [NOT FOUND] replying null...");

            smresgetMessage.setHasValue(false);
        }

        byte[] sendBytes = null;

        try {
            sendBytes = Serialization.serialize(smresgetMessage);
        } catch (IOException e) {
        }

        int toServer = Config.init_server_port + smreqgetMessage.getFromID();
        String print = "responding with request for id " + transactionID + " to server " + toServer;
        this.sendAsync(toServer, "server_response_get", sendBytes, print);

        SMRequest sendUpdateClock = new SMRequest(copyClock, this.SERVER_ID);
        this.send_update_clock_all_servers(sendUpdateClock, smreqgetMessage.getFromID());

        LOGGER.info("dispaching events...");
        dispach_queued_events();

        LOGGER.warn(">>>>>>> [DATABASE]: " + this.DATABASE_SET.size());
    }

    /**
     * Processes server response get message handler
     */
    public void process_server_response_get(SMResponse smresgetMessage) {

        int transactionID = smresgetMessage.getRequestID();

        GetTransaction transaction = null;
        int clientPort = smresgetMessage.getClientPort();
        if (this.WAITING_GETS.containsKey(clientPort))
            transaction = this.WAITING_GETS.get(clientPort).get(transactionID);

        Long key = smresgetMessage.getKey();

        LOGGER.info("[transaction: " + transactionID + "] received response to get key " + key);

        if (smresgetMessage.hasValue()) {

            StorageValue value = smresgetMessage.getValue();
            transaction.setDone(key, value.getData());
            transaction.incrementDone();

        } else {

            transaction.removeUnexisting(key);

            LOGGER.info("[transaction: " + transactionID + "] that key was NOT FOUND, removing from GET/ transaction");
        }

        if (!(transaction == null)) {

            if (transaction.isFinished()) {

                CMResponseGet responseGet = new CMResponseGet(transactionID, transaction.getPreparedGets());

                byte[] sendBytes = null;

                try {
                    sendBytes = Serialization.serialize(responseGet);
                } catch (IOException e) {
                }

                LOGGER.info("[transaction: " + transactionID + "] GET/ transaction fininshed, replying to client");

                int cPort = transaction.getClientPort();
                String toPrint = "client " + cPort + " GET transaction finished, vou avisar o cliente";

                this.sendAsync(cPort, "client_response_get", sendBytes, toPrint);

                this.WAITING_GETS.get(clientPort).remove(transactionID);
            }
        }

        LOGGER.info("[transaction: " + transactionID + "] dispaching events...");

        dispach_queued_events();

    }

    /**
     * Processes server response put message handler
     */
    public void process_server_response_put(SMResponse smresputMessage) {

        int transactionID = smresputMessage.getRequestID();

        Long requestedKey = smresputMessage.getKey();

        LOGGER.info("[transaction: " + transactionID + "] received response to put key " + requestedKey);

        lockCheckFinished.lock();

        int clientPort = smresputMessage.getClientPort();

        PutTransaction transaction = null;

        if (this.WAITING_PUTS.containsKey(clientPort))
            transaction = this.WAITING_PUTS.get(clientPort).get(transactionID);

        if (!(transaction == null)) {

            transaction.setDone(requestedKey);

            if (transaction.isFinished()) {

                CMResponsePut responsePut = new CMResponsePut(transactionID);

                byte[] sendBytes = null;

                try {
                    sendBytes = Serialization.serialize(responsePut);
                } catch (IOException e) {
                }

                LOGGER.info("[transaction:  on finished, responding to client");

                int cPort = transaction.getClientPort();
                String toPrint = "client " + cPort + " PUT transaction finished, vou avisar o cliente";

                this.sendAsync(cPort, "client_response_put", sendBytes, toPrint);

                this.WAITING_PUTS.get(transaction.getClientPort()).remove(transactionID);

                if (this.WAITING_PUTS.get(transaction.getClientPort()).isEmpty())
                    this.WAITING_PUTS.remove(transaction.getClientPort());

                LOGGER.warn(">>>>>>> [DATABASE]: " + this.DATABASE_SET.size());
            }
        }

        lockCheckFinished.unlock();

        LOGGER.info("[transaction: " + transactionID + "] dispaching events...");

        dispach_queued_events();

    }

    /**
     * Dispach all queued events
     */
    public synchronized void dispach_queued_events() {

        ArrayList<SMResponse> toRemoveRes = new ArrayList<>();
        for (SMResponse res : this.QUEUE_RESPONSES) {

            boolean canBeExecuted = LogicalClockTool.conditionVerifier(res, LOGICAL_CLOCK);

            if (canBeExecuted) {
                if (res.isResponsePut())
                    process_server_response_put(res);
                else
                    process_server_response_get(res);

                toRemoveRes.add(res);
            }
        }

        this.QUEUE_RESPONSES.removeAll(toRemoveRes);

        ArrayList<SMRequest> toRemoveRep = new ArrayList<>();

        for (SMRequest req : this.QUEUE_REQUESTS) {

            boolean canBeExecuted = LogicalClockTool.conditionVerifier(req, LOGICAL_CLOCK);

            if (canBeExecuted) {

                if (req.isPutRequest())
                    process_server_request_put(req);
                else if (!req.isUpdateClock())
                    process_server_request_get(req);

                toRemoveRep.add(req);
            }
        }

        this.QUEUE_REQUESTS.removeAll(toRemoveRep);

        LOGGER.warn("[****] AFTER DISPATCHING:\n\t\t      REQ_QUEUE_SIZE = " + this.QUEUE_REQUESTS.size()
                + " | RES_QUEUE_SIZE = " + this.QUEUE_RESPONSES.size());
    }
}