
package app.client.api;

import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

import org.apache.log4j.Logger;

import app.Config;
import app.Serialization;
import app.client.FutureResponses;
import app.client.handler.ClientService;
import app.data.CMRequestGet;
import app.data.CMRequestPut;
import io.atomix.utils.net.Address;

public class StorageAPI {

    private static Logger LOGGER = Logger.getLogger(StorageAPI.class);

    private ClientService clientService;
    private int serverID;

    public StorageAPI(ClientService clientService) {

        this.clientService = clientService;
    }

    public void setDestinationID(int serverID) {

        this.serverID = serverID;
    }

    public int getRandomDestinationID() {
        return (new Random()).nextInt(Config.nr_servers);
    }

    public CompletableFuture<Void> put(Map<Long, byte[]> map) {

        FutureResponses futureR = this.clientService.getFutureResponses();
        int lastId = futureR.getId();

        CMRequestPut clMessage = new CMRequestPut(map, lastId, clientService.getClientPort());

        byte[] messageBytes = null;

        try {

            messageBytes = Serialization.serialize(clMessage);

        } catch (Exception e) {

            LOGGER.error("error serializing in put method...");
        }

        // int serverPort = Config.init_server_port + getRandomDestinationID();
        int serverPort = Config.init_server_port + this.serverID;

        this.clientService.gNettyMessagingService()
                .sendAsync(Address.from("localhost", serverPort), "client_put", messageBytes).thenRun(() -> {

                    LOGGER.info("[destID:" + serverPort + "] (api) enviei pedido PUT para a transacao: " + lastId);

                }).exceptionally(t -> {
                    t.printStackTrace();
                    return null;
                });

        CompletableFuture<Void> resultPut = new CompletableFuture<>();

        futureR.addPendingPutRequest(resultPut);

        return resultPut;
    }

    public CompletableFuture<Map<Long, byte[]>> get(Collection<Long> keys) {

        FutureResponses futureR = this.clientService.getFutureResponses();
        int lastId = futureR.getId();

        CMRequestGet requestGet = new CMRequestGet(keys, lastId, clientService.getClientPort());
        byte[] messageBytes = null;

        try {

            messageBytes = Serialization.serialize(requestGet);

        } catch (Exception e) {

            LOGGER.error("error serializing in put method...");
        }

        // int serverPort = Config.init_server_port + getRandomDestinationID();
        int serverPort = Config.init_server_port + this.serverID;

        this.clientService.gNettyMessagingService()
                .sendAsync(Address.from("localhost", serverPort), "client_get", messageBytes).thenRun(() -> {

                    LOGGER.info("[destID:" + serverPort + "] (api) enviei pedido GET para a transacao: " + lastId);

                }).exceptionally(t -> {
                    t.printStackTrace();
                    return null;
                });

        CompletableFuture<Map<Long, byte[]>> resultGet = new CompletableFuture<>();

        futureR.addPendingGetRequest(resultGet);

        return resultGet;
    }
}
