package app.server.handler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import app.Config;
import app.Serialization;
import app.data.CMRequestPut;
import app.data.CMResponsePut;
import app.server.data.PutTransaction;
import app.server.data.SMRequestPut;
import app.server.data.SMResponsePut;
import app.server.data.SMUpdateClock;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;

public class StorageService {

    private ScheduledExecutorService executorService;
    private NettyMessagingService messagingService;
    private Map<Long, byte[]> DATABASE_SET;
    private int[] LOGICAL_CLOCK;
    private int SERVER_ID;

    // Queue of put transactions
    private Map<Integer, PutTransaction> WAITING_PUTS;

    public StorageService(int sid, int sport, int[] clock) {

        this.executorService = Executors.newScheduledThreadPool(Config.server_thread_pool_size);
        this.messagingService = new NettyMessagingService("serverms_" + sid, Address.from(sport),
                new MessagingConfig());
        this.LOGICAL_CLOCK = clock;
        this.DATABASE_SET = new HashMap<>();
        this.SERVER_ID = sid;
        this.WAITING_PUTS = new HashMap<>();
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

            CMRequestPut cMessage = null;

            try {
                cMessage = (CMRequestPut) Serialization.deserialize(messageBytes);
            } catch (ClassNotFoundException | IOException e) {
            }

            // create client put transaction

            List<Long> keys = cMessage.getRequestPut().keySet().stream().collect(Collectors.toList());
            int[] copyTimestamp = new int[Config.nr_servers];
            System.arraycopy(LOGICAL_CLOCK, 0, copyTimestamp, 0, Config.nr_servers);

            PutTransaction clientTransaction = new PutTransaction(keys, copyTimestamp, cMessage.getCLI_PORT());

            System.out.println("[<client_put>] Received <client_put> from " + address);
            System.out.println("[<client_put>] (" + address + ") my db = " + this.DATABASE_SET.toString());

            // add new transaction

            this.WAITING_PUTS.put(cMessage.getMESSAGE_ID(), clientTransaction);

            System.out.println("[<client_put>] (" + address + ") put on waiting_puts = " + this.WAITING_PUTS);

            // atualiza relogio proprio

            for (Map.Entry<Long, byte[]> entries : cMessage.getRequestPut().entrySet()) {

                Long keyATM = entries.getKey();
                byte[] valueATM = entries.getValue();

                // calculate destination server id

                int DESTINATIONID = this.find_storage_service(keyATM);

                System.out.println("[<client_put>] (" + address + ") processing key = " + keyATM
                        + " which destination server is " + (DESTINATIONID + Config.init_server_port));

                if (DESTINATIONID == this.SERVER_ID) {

                    this.DATABASE_SET.put(keyATM, valueATM);

                    System.out.println("[<client_put>] (" + address + ") im the destination of key = " + keyATM
                            + " my db is now " + this.DATABASE_SET.toString());

                    clientTransaction.setDone(keyATM);

                } else {

                    // calculate destination server port

                    int DestServerPort = Config.init_server_port + DESTINATIONID;

                    // creating request message

                    SMRequestPut sendRequest = new SMRequestPut(clientTransaction, keyATM, valueATM, this.SERVER_ID,
                            cMessage.getMESSAGE_ID());

                    byte[] sendBytes = null;

                    try {
                        sendBytes = Serialization.serialize(sendRequest);
                    } catch (IOException e) {
                    }

                    // send request to dest server
                    String toPrint = "Sent transaction request for key " + keyATM + " to server port " + DestServerPort;
                    this.sendAsync(DestServerPort, "server_request_put", sendBytes, toPrint);

                    System.out.println("[<client_put>] (" + address + ") im NOT the destination of key = " + keyATM
                            + ", so im requesting server " + DestServerPort);

                }
            }

            if (clientTransaction.isFinished()) {

                System.out.println(
                        "[<client_put>] (" + address + ") client transaction finished, sending response to client!");

                CMResponsePut responsePut = new CMResponsePut(cMessage.getMESSAGE_ID());

                byte[] sendBytes = null;

                try {
                    sendBytes = Serialization.serialize(responsePut);
                } catch (IOException e) {
                }

                // avisar o client
                this.sendAsync(clientTransaction.getClientPort(), "client_response_put", sendBytes,
                        "client transaction finished, vou avisar o cliente");
            }

        }, this.executorService);
    }

    private void register_client_get() {

        this.messagingService.registerHandler("client_get", (address, messageBytes) -> {
            /*
             * ClientMessage cMessage = null;
             * 
             * try { cMessage = Serialization.deserialize(messageBytes); } catch
             * (ClassNotFoundException | IOException e) { }
             * 
             * System.out.println("Received client get:");
             * System.out.println(cMessage.toString());
             * 
             * this.messagingService.sendAsync(address, "client_response",
             * messageBytes).thenRun(() -> {
             * 
             * System.out.println("Mensagem Response GET enviada!");
             * 
             * }).exceptionally(t -> { t.printStackTrace(); return null; });
             */
        }, this.executorService);
    }

    private void register_server_request_put() {

        this.messagingService.registerHandler("server_request_put", (address, messageBytes) -> {

            // FAZER O CONDITION_VERIFIER

            SMRequestPut sMessage = null;
            try {
                sMessage = (SMRequestPut) Serialization.deserialize(messageBytes);
            } catch (ClassNotFoundException | IOException e) {
            }

            PutTransaction transactionReceived = sMessage.getTransaction();

            boolean hasEncontredCommon = false;
            boolean lostForID = false;
            int fromID = sMessage.getFromID();

            System.out.println("[<server_request_put>] (" + address + ") received request put from server "
                    + (Config.init_server_port + fromID));

            for (Map.Entry<Integer, PutTransaction> entries : this.WAITING_PUTS.entrySet()) {

                PutTransaction currenTransaction = entries.getValue();

                HashSet<Long> emComum = transactionReceived.getInCommon(currenTransaction);
                List<Long> emComumList = emComum.stream().collect(Collectors.toList());

                System.out.println("[<server_request_put>] (" + address + ") a processar transacao com chaves = "
                        + currenTransaction.getKeysToPut() + ", com emComum = " + emComumList);

                // confito -> comparar relogios
                if (emComum.contains(sMessage.getKeyToVerify())) {

                    // encontrou cenas em comum
                    hasEncontredCommon = true;

                    System.out.println("[<server_request_put>] (" + address + ") chave em comum key = "
                            + sMessage.getKeyToVerify());

                    // conflit of timestamps, wins the least id
                    if (currenTransaction.getTimestamp()[this.SERVER_ID] == transactionReceived
                            .getTimestamp()[this.SERVER_ID]) {

                        System.out.println("[<server_request_put>] (" + address + ") conflito detetado");

                        if (this.SERVER_ID < sMessage.getFromID()) {

                            // destination server wins
                            // enviar lista em comum para cancelar
                            int destPort = Config.init_server_port + fromID;

                            System.out.println("[<server_request_put>] (" + address
                                    + ") ganhei conflito, a mandar a lista em comum " + emComumList + " para o server "
                                    + destPort);

                            SMResponsePut responsePut = new SMResponsePut(emComumList, sMessage.getTransactionID());

                            byte[] sendBytes = null;

                            try {
                                sendBytes = Serialization.serialize(responsePut);
                            } catch (IOException e) {
                            }

                            String toPrint = "Response to transaction id " + responsePut.getTRANSACTION_ID()
                                    + " from server id " + fromID;
                            this.sendAsync(destPort, "server_response_put", sendBytes, toPrint);

                        } else {

                            // source server wins
                            lostForID = true;

                            currenTransaction.removeAll(emComumList);

                            System.out.println("[<server_request_put>] (" + address + ") perdi conflito");

                            if (currenTransaction.isFinished()) {

                                CMResponsePut responsePut = new CMResponsePut(sMessage.getTransactionID());

                                byte[] sendBytes = null;

                                try {
                                    sendBytes = Serialization.serialize(responsePut);
                                } catch (IOException e) {
                                }

                                System.out.println("[<server_request_put>] (" + address + ") transacao "
                                        + sMessage.getTransactionID() + " terminou, a avisar o cliente");

                                // avisar o client
                                this.sendAsync(currenTransaction.getClientPort(), "client_response_put", sendBytes,
                                        "client transaction finished, vou avisar o cliente");
                            }
                        }

                    }

                }
            }

            if (!hasEncontredCommon || lostForID) {

                this.DATABASE_SET.put(sMessage.getKeyToVerify(), sMessage.getKeyValue());

                if (lostForID)
                    System.out.println("[<server_request_put>] (" + address
                            + ") perdi por id em alguma, my db after update " + this.DATABASE_SET.toString()
                            + ", e enviei a lista vazia para " + (Config.init_server_port + fromID));
                else
                    System.out.println("[<server_request_put>] (" + address
                            + ") nao tem em comum nada, my db after update " + this.DATABASE_SET.toString()
                            + ", e enviei a lista vazia para " + (Config.init_server_port + fromID));

                // se nao tem cenas em comum fazer put, confirmacao de volta, etc....

                SMResponsePut responsePut = new SMResponsePut(new ArrayList<>(), sMessage.getTransactionID());

                byte[] sendBytes = null;

                try {
                    sendBytes = Serialization.serialize(responsePut);
                } catch (IOException e) {
                }

                String toPrint = "Response to transaction id " + responsePut.getTRANSACTION_ID() + " from server id "
                        + fromID;
                this.sendAsync(Config.init_server_port + fromID, "server_response_put", sendBytes, toPrint);
            }

            this.LOGICAL_CLOCK[this.SERVER_ID]++;

            this.send_update_clock_all_servers();

            System.out.println("client: " + transactionReceived.getClientPort() + " end of request put: "
                    + this.DATABASE_SET.toString());

        }, this.executorService);

    }

    private void register_server_request_get() {

        this.messagingService.registerHandler("server_request_get", (address, messageBytes) -> {

        }, this.executorService);
    }

    private void register_server_response_put() {

        this.messagingService.registerHandler("server_response_put", (address, messageBytes) -> {

            SMResponsePut responseMessage = null;

            try {
                responseMessage = (SMResponsePut) Serialization.deserialize(messageBytes);
            } catch (ClassNotFoundException | IOException e) {
            }

            int tid = responseMessage.getTRANSACTION_ID();
            PutTransaction tidTransaction = this.WAITING_PUTS.get(tid);

            System.out.println("[<server_response_put>] (" + address + ") recebi resposta ao server put, com lista "
                    + tidTransaction.getKeysToPut() + " vou remover");

            tidTransaction.removeAll(responseMessage.getKeysToAbort());

            if (tidTransaction.isFinished()) {

                System.out.println(
                        "[<server_response_put>] (" + address + ") acabou transacao " + tid + " vou avisar o cliente");

                CMResponsePut responsePut = new CMResponsePut(tid);

                byte[] sendBytes = null;

                try {
                    sendBytes = Serialization.serialize(responsePut);
                } catch (IOException e) {
                }

                // avisar o client
                this.sendAsync(tidTransaction.getClientPort(), "client_response_put", sendBytes,
                        "client transaction finished, vou avisar o cliente");
            }

        }, this.executorService);
    }

    private void register_server_response_get() {

        this.messagingService.registerHandler("server_response_get", (address, messageBytes) -> {

        }, this.executorService);
    }

    private void register_server_update_clock() {

        this.messagingService.registerHandler("server_update_clock", (address, messageBytes) -> {

            // FIXME update clock

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
}
