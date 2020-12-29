package app.server.handler;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import app.Config;
import app.Serialization;
import app.data.CMRequestGet;
import app.data.CMRequestPut;
import app.data.CMResponseGet;
import app.data.CMResponsePut;
import app.server.data.GetTransaction;
import app.server.data.PutTransaction;
import app.server.data.SMRequest;
import app.server.data.SMResponse;
import app.server.data.SMUpdateClock;
import app.server.data.StorageValue;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;

public class StorageService {

    private ScheduledExecutorService executorService;
    private NettyMessagingService messagingService;
    private Map<Long, StorageValue> DATABASE_SET;
    private int[] LOGICAL_CLOCK;
    private int SERVER_ID;

    static ReentrantLock lock = new ReentrantLock();
    static boolean canEnter = true;

    private ConcurrentHashMap<Integer, PutTransaction> WAITING_PUTS;
    private ConcurrentHashMap<Integer, GetTransaction> WAITING_GETS;

    public StorageService(int sid, int sport, int[] clock) {

        this.executorService = Executors.newScheduledThreadPool(Config.server_thread_pool_size);
        this.messagingService = new NettyMessagingService("serverms_" + sid, Address.from(sport),
                new MessagingConfig());

        this.SERVER_ID = sid;
        this.LOGICAL_CLOCK = clock;

        this.DATABASE_SET = new HashMap<>();

        this.WAITING_PUTS = new ConcurrentHashMap<>();
        this.WAITING_GETS = new ConcurrentHashMap<>();
    }

    public void start() {

        this.register_handlers();

        this.messagingService.start();
    }

    public void register_handlers() {

        this.register_client_put();
        this.register_client_get();
        this.register_server_request_put();
        this.register_server_request_get();
        this.register_server_response_put();
        this.register_server_response_get();
        this.register_server_update_clock();
    }

    public int find_storage_service(Long key) {
        return (int) (key % Config.nr_servers);
    }

    public void sendAsync(int port, String typeHandler, byte[] data, String print) {

        this.messagingService.sendAsync(Address.from("localhost", port), typeHandler, data).thenRun(() -> {
            System.out.println(print);
        }).exceptionally(t -> {
            t.printStackTrace();
            return null;
        });
    }

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

            System.out.println("created PUT transaction = " + transaction.toString());

            this.WAITING_PUTS.put(requestID, transaction);

            System.out.println("recebi pedido do cliente " + address + " para as keys = " + keysToUpdate);

            boolean imAlwaysTheDestination = true;

            for (Map.Entry<Long, byte[]> pedidoEntry : cmreqputMessage.getRequestPut().entrySet()) {

                Long keyToProcess = pedidoEntry.getKey();
                byte[] keyData = pedidoEntry.getValue();
                StorageValue svData = new StorageValue(keyData, copyTimestamp, this.SERVER_ID);

                int keyDestinationServerID = this.find_storage_service(keyToProcess);

                if (keyDestinationServerID == this.SERVER_ID) {

                    System.out.println("a processar key = " + keyToProcess + " (sou o destino), timestamp = "
                            + printArray(copyTimestamp));

                    transaction.setDone(keyToProcess);

                    if (this.DATABASE_SET.containsKey(keyToProcess)) {

                        StorageValue lValue = this.DATABASE_SET.get(keyToProcess);

                        if (lValue.getTimeStamp()[this.SERVER_ID] == svData.getTimeStamp()[this.SERVER_ID]) {

                            // conflito detetado

                            if (lValue.getServerWhichUpdate() < this.SERVER_ID) {

                                // destination server wins
                                System.out.println("conflito com a chave " + keyToProcess + ", NAO VAI ATUALIZAR");

                            } else {

                                // source server wins
                                System.out.println("conflito com a chave " + keyToProcess + ", VAI ATUALIZAR");

                                lValue.setServerWhichUpdate(svData.getServerWhichUpdate());
                                this.DATABASE_SET.replace(keyToProcess, svData);
                            }

                        } else {

                            System.out.println(
                                    "NAO ha conflito com a chave " + keyToProcess + ", vou dar replace/atualiar");

                            this.DATABASE_SET.replace(keyToProcess, svData);
                        }

                    } else {

                        System.out.println("NAO existia a chave " + keyToProcess + ", vou inserir");

                        svData.setServerWhichUpdate(this.SERVER_ID);
                        this.DATABASE_SET.put(keyToProcess, svData);
                    }

                    System.out.println("MY DATABASE = " + this.DATABASE_SET.toString());

                } else {

                    System.out.println("a processar key = " + keyToProcess + " (NAO sou o destino), timestamp = "
                            + printArray(copyTimestamp));

                    imAlwaysTheDestination = false;

                    SMRequest sendRequest = new SMRequest(keyToProcess, svData, this.SERVER_ID, requestID, "put");

                    byte[] sendBytes = null;

                    try {
                        sendBytes = Serialization.serialize(sendRequest);
                    } catch (IOException e) {
                    }

                    int toServer = Config.init_server_port + keyDestinationServerID;
                    String toPrint = "requesting server " + toServer + " to put key " + keyToProcess;
                    this.sendAsync(toServer, "server_request_put", sendBytes, toPrint);
                }
            }

            if (imAlwaysTheDestination) {

                System.out.println("fui sempre o destino, tudo acabou, a responder ao cliente");

                CMResponsePut responsePut = new CMResponsePut(requestID);

                byte[] sendBytes = null;

                try {
                    sendBytes = Serialization.serialize(responsePut);
                } catch (IOException e) {
                }

                int cPort = transaction.getClientPort();
                String toPrint = "client " + cPort + " PUT transaction finished, vou avisar o cliente";

                this.sendAsync(cPort, "client_response_put", sendBytes, toPrint);

                this.WAITING_PUTS.remove(requestID);
            }

        }, this.executorService);
    }

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

            System.out.println("created GET transaction = " + transaction.toString());

            this.WAITING_GETS.put(transactionID, transaction);

            boolean isAlwaysForMe = true;

            for (Long keyToGet : keys) {

                int keyDestinationServerID = this.find_storage_service(keyToGet);

                if (keyDestinationServerID == this.SERVER_ID) {

                    if (this.DATABASE_SET.containsKey(keyToGet)) {

                        StorageValue value = this.DATABASE_SET.get(keyToGet);

                        transaction.setDone(keyToGet, value.getData());
                        transaction.incrementDone();

                    } else {

                        transaction.removeUnexisting(keyToGet);
                    }

                } else {

                    isAlwaysForMe = false;

                    SMRequest smreqMessage = new SMRequest(keyToGet, this.SERVER_ID, transactionID, "get");

                    byte[] sendBytes = null;

                    try {
                        sendBytes = Serialization.serialize(smreqMessage);
                    } catch (IOException e) {
                    }

                    int toServer = Config.init_server_port + keyDestinationServerID;
                    String toPrint = "requesting server " + toServer + " to get key " + keyToGet;
                    this.sendAsync(toServer, "server_request_get", sendBytes, toPrint);

                }
            }

            if (isAlwaysForMe && transaction.isFinished()) {

                Map<Long, byte[]> resultGet = transaction.getPreparedGets();
                CMResponseGet responseGet = new CMResponseGet(transactionID, resultGet);

                byte[] sendBytes = null;

                try {
                    sendBytes = Serialization.serialize(responseGet);
                } catch (IOException e) {
                }

                String toPrint = "responding to client " + clientPort + " GET transaction " + transactionID;
                this.sendAsync(clientPort, "client_response_get", sendBytes, toPrint);
            }

        }, this.executorService);

    }

    private void register_server_request_put() {

        this.messagingService.registerHandler("server_request_put", (address, messageBytes) -> {

            SMRequest smreqputMessage = null;
            try {
                smreqputMessage = (SMRequest) Serialization.deserialize(messageBytes);
            } catch (ClassNotFoundException | IOException e) {
            }

            Long keyRequest = smreqputMessage.getKeyToVerify();

            StorageValue requestedValue = smreqputMessage.getKeyValue();
            StorageValue lastValue = this.DATABASE_SET.get(keyRequest);

            System.out.println("recebi pedido do servidor = " + address + " para processar " + keyRequest
                    + " com timestamp = " + printArray(requestedValue.getTimeStamp()));

            boolean wasUpdated = true;

            if (this.DATABASE_SET.containsKey(keyRequest)) {

                if (lastValue.getTimeStamp()[this.SERVER_ID] == requestedValue.getTimeStamp()[this.SERVER_ID]) {

                    // conflito detetado

                    if (lastValue.getServerWhichUpdate() < smreqputMessage.getFromID()) {

                        // destination server wins
                        System.out.println("conflito com a chave " + keyRequest + ", NÃO VAI ATUALIZAR");

                        wasUpdated = false;

                    } else {

                        // source server wins
                        System.out.println("conflito com a chave " + keyRequest + ", VAI ATUALIZAR");

                        wasUpdated = true;

                        requestedValue.setServerWhichUpdate(smreqputMessage.getFromID());
                        this.DATABASE_SET.replace(keyRequest, requestedValue);
                    }

                } else {

                    System.out.println("NAO ha conflito com a chave " + keyRequest + ", vou dar replace/atualiar");

                    this.DATABASE_SET.replace(keyRequest, requestedValue);
                }

            } else {

                System.out.println("NAO existia a chave " + keyRequest + ", vou inserir");

                requestedValue.setServerWhichUpdate(smreqputMessage.getFromID());
                this.DATABASE_SET.put(keyRequest, requestedValue);
            }

            System.out.println("MY DATABASE = " + this.DATABASE_SET.toString());

            int reqID = smreqputMessage.getRequestID();
            SMResponse smresputMessage = new SMResponse(reqID, wasUpdated, keyRequest, "put");

            byte[] sendBytes = null;

            try {
                sendBytes = Serialization.serialize(smresputMessage);
            } catch (IOException e) {
            }

            int fromPort = smreqputMessage.getFromID() + Config.init_server_port;
            String print = "responding with request for id " + reqID + " to server " + fromPort;
            this.sendAsync(fromPort, "server_response_put", sendBytes, print);

        }, this.executorService);

    }

    private void register_server_request_get() {

        this.messagingService.registerHandler("server_request_get", (address, messageBytes) -> {

            SMRequest smreqgetMessage = null;

            try {
                smreqgetMessage = (SMRequest) Serialization.deserialize(messageBytes);
            } catch (ClassNotFoundException | IOException e) {
            }

            Long keyToCheck = smreqgetMessage.getKeyToVerify();
            StorageValue resultValue = null;

            int transactionID = smreqgetMessage.getRequestID();
            SMResponse smresgetMessage = new SMResponse(transactionID, keyToCheck, "get");

            if (this.DATABASE_SET.containsKey(keyToCheck)) {

                resultValue = this.DATABASE_SET.get(keyToCheck);

                smresgetMessage.setHasValue(true);
                smresgetMessage.setValue(resultValue);

            } else {

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

        }, this.executorService);
    }

    private void register_server_response_put() {

        this.messagingService.registerHandler("server_response_put", (address, messageBytes) -> {

            SMResponse smresputMessage = null;

            try {
                smresputMessage = (SMResponse) Serialization.deserialize(messageBytes);
            } catch (ClassNotFoundException | IOException e) {
            }

            int transactionID = smresputMessage.getRequestID();
            PutTransaction transaction = this.WAITING_PUTS.get(transactionID);

            Long requestedKey = smresputMessage.getKey();

            transaction.setDone(requestedKey);

            System.out.println("[finished = " + transaction.isFinished() + "] transaction = " + transaction.toString());

            lock.lock();

            if (canEnter && transaction.isFinished()) {
                canEnter = false;
                CMResponsePut responsePut = new CMResponsePut(transactionID);

                byte[] sendBytes = null;

                try {
                    sendBytes = Serialization.serialize(responsePut);
                } catch (IOException e) {
                }

                int cPort = transaction.getClientPort();
                String toPrint = "client " + cPort + " PUT transaction finished, vou avisar o cliente";

                this.sendAsync(cPort, "client_response_put", sendBytes, toPrint);

                this.WAITING_PUTS.remove(transactionID);
            }

            lock.unlock();

        }, this.executorService);
    }

    private void register_server_response_get() {

        this.messagingService.registerHandler("server_response_get", (address, messageBytes) -> {

            SMResponse smresgetMessage = null;

            try {
                smresgetMessage = (SMResponse) Serialization.deserialize(messageBytes);
            } catch (ClassNotFoundException | IOException e) {
            }

            int transactionID = smresgetMessage.getRequestID();
            GetTransaction transaction = this.WAITING_GETS.get(transactionID);

            Long key = smresgetMessage.getKey();

            if (smresgetMessage.hasValue()) {

                StorageValue value = smresgetMessage.getValue();
                transaction.setDone(key, value.getData());
                transaction.incrementDone();

            } else {

                transaction.removeUnexisting(key);
            }

            if (transaction.isFinished()) {

                CMResponseGet responseGet = new CMResponseGet(transactionID, transaction.getPreparedGets());

                byte[] sendBytes = null;

                try {
                    sendBytes = Serialization.serialize(responseGet);
                } catch (IOException e) {
                }

                int cPort = transaction.getClientPort();
                String toPrint = "client " + cPort + " GET transaction finished, vou avisar o cliente";

                this.sendAsync(cPort, "client_response_get", sendBytes, toPrint);

                this.WAITING_GETS.remove(transactionID);
            }

        }, this.executorService);
    }

    public void send_update_clock_all_servers() {

        SMUpdateClock clock_response = new SMUpdateClock(this.LOGICAL_CLOCK);
        byte[] sendBytes = null;

        try {
            sendBytes = Serialization.serialize(clock_response);
        } catch (IOException e) {
        }

        for (int i = 0; i < Config.nr_servers; i++) {
            if (i != this.SERVER_ID)
                this.sendAsync(i + Config.init_server_port, "server_update_clock", sendBytes,
                        "sending update clock to " + (i + Config.init_server_port));
        }
    }

    private void register_server_update_clock() {

        this.messagingService.registerHandler("server_update_clock", (address, messageBytes) -> {

        }, this.executorService);
    }

    public String printArray(int[] arr) {
        String res = "[";
        for (int i = 0; i < arr.length; i++) {
            if (i == arr.length - 1)
                res += (arr[i]);
            else
                res += (arr[i] + ",");
        }
        res += ("]");
        return res;
    }
}
