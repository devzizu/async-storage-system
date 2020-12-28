package app.server.handler;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

import app.Config;
import app.Serialization;
import app.data.ClientMessage;
import app.server.data.PutTransaction;
import app.server.data.ServerMessage;
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

        this.register_client_handlers();

        this.messagingService.start();
    }

    public void register_client_handlers() {

        this.messagingService.registerHandler("client_put", (address, messageBytes) -> {

            ClientMessage cMessage = null;

            try {
                cMessage = Serialization.deserializeCM(messageBytes);
            } catch (ClassNotFoundException | IOException e) {
            }

            // create client put transaction

            List<Long> keys = cMessage.getRequestPut().keySet().stream().collect(Collectors.toList());
            int[] copyTimestamp = new int[Config.nr_servers];
            System.arraycopy(LOGICAL_CLOCK, 0, copyTimestamp, 0, Config.nr_servers);

            PutTransaction clientTransaction = new PutTransaction(keys, copyTimestamp);

            // add new transaction

            this.WAITING_PUTS.put(cMessage.getMESSAGE_ID(), clientTransaction);

            // atualiza relogio proprio

            for (Map.Entry<Long, byte[]> entries : cMessage.getRequestPut().entrySet()) {

                Long keyATM = entries.getKey();
                byte[] valueATM = entries.getValue();

                // calculate destination server id

                int DESTINATIONID = this.find_storage_service(keyATM);

                if (DESTINATIONID == this.SERVER_ID) {

                    this.DATABASE_SET.put(keyATM, valueATM);

                    clientTransaction.setDone(keyATM);

                } else {

                    // calculate destination server port

                    int DestServerPort = Config.init_server_port + DESTINATIONID;

                    // creating request message

                    ServerMessage sendRequest = new ServerMessage(clientTransaction, keyATM, valueATM, this.SERVER_ID);

                    byte[] sendBytes = null;

                    try {
                        sendBytes = Serialization.serialize(sendRequest);
                    } catch (IOException e) {
                    }

                    // send request to dest server

                    this.messagingService.sendAsync(Address.from("localhost", DestServerPort), "server_put", sendBytes)
                            .thenRun(() -> {
                                System.out.println("Sent transaction request for key " + keyATM + " to server port "
                                        + DestServerPort);
                            }).exceptionally(t -> {
                                t.printStackTrace();
                                return null;
                            });

                }
            }

            this.messagingService.sendAsync(address, "client_response", messageBytes).thenRun(() -> {

                System.out.println("Mensagem Response PUT enviada!");

            }).exceptionally(t -> {
                t.printStackTrace();
                return null;
            });

        }, this.executorService);

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

        this.messagingService.registerHandler("server_put", (address, messageBytes) -> {

            // FAZER O CONDITION_VERIFIER

            ServerMessage sMessage = null;

            try {
                sMessage = Serialization.deserializeSM(messageBytes);
            } catch (ClassNotFoundException | IOException e) {
            }

            PutTransaction transactionReceived = sMessage.getTransaction();

            boolean hasEncontredCommon = false;

            for (Map.Entry<Integer, PutTransaction> entries : this.WAITING_PUTS.entrySet()) {

                PutTransaction currenTransaction = entries.getValue();

                HashSet<Long> emComum = transactionReceived.getInCommon(currenTransaction);

                // confito -> comparar relogios
                if (emComum.contains(sMessage.getKeyToVerify())) {

                    // encontrou cenas em comum
                    hasEncontredCommon = true;

                    // conflit of timestamps, wins the least id
                    if (currenTransaction.getTimestamp()[this.SERVER_ID] == transactionReceived
                            .getTimestamp()[this.SERVER_ID]) {

                        if (this.SERVER_ID < sMessage.getFromID()) {

                            // destination server wins

                            // enviar lista em comum para cancelar

                        } else {

                            // source server wins

                            // cancelar a minha lista, fazer puts e enviar confirmacao
                            // this.DATABASE_SET.put(sMessage.getKeyToVerify(), sMessage.getKeyValue());
                            // currenTransaction.removeAll(emComum);
                        }

                    }
                }
            }

            if (!hasEncontredCommon) {

                // se nao tem cenas em comum fazer put, confirmacao de volta, etc....

            }

        }, this.executorService);

        this.messagingService.registerHandler("server_get", (address, messageBytes) ->

        {

        }, this.executorService);

    }

    public int find_storage_service(Long key) {
        return (int) (key % Config.nr_servers);
    }
}
