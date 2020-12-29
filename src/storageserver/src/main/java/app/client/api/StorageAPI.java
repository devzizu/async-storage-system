
package app.client.api;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import app.Config;
import app.Serialization;
import app.client.FutureResponses;
import app.client.handler.ClientService;
import app.data.CMRequestGet;
import app.data.CMRequestPut;
import io.atomix.utils.net.Address;

public class StorageAPI {

    private ClientService clientService;

    public StorageAPI(ClientService clientService) {

        this.clientService = clientService;
    }

    public CompletableFuture<Void> put(Map<Long, byte[]> map, int store) {

        FutureResponses futureR = this.clientService.getFutureResponses();
        int lastId = futureR.getId();

        CMRequestPut clMessage = new CMRequestPut(map, lastId, clientService.getClientPort());

        byte[] messageBytes = null;

        try {

            messageBytes = Serialization.serialize(clMessage);

        } catch (Exception e) {

            System.out.println("error serializing in put method...");
        }

        this.clientService.gNettyMessagingService()
                .sendAsync(Address.from("localhost", Config.init_server_port + store), "client_put", messageBytes)
                .thenRun(() -> {

                    System.out.println("(api) enviei pedido PUT para a transacao: " + lastId);

                }).exceptionally(t -> {
                    t.printStackTrace();
                    return null;
                });

        CompletableFuture<Void> resultPut = new CompletableFuture<>();

        futureR.addPendingPutRequest(resultPut);

        return resultPut;
    }

    public CompletableFuture<Map<Long, byte[]>> get(Collection<Long> keys, int store) {

        FutureResponses futureR = this.clientService.getFutureResponses();
        int lastId = futureR.getId();

        CMRequestGet requestGet = new CMRequestGet(keys, lastId, clientService.getClientPort());
        byte[] messageBytes = null;

        try {

            messageBytes = Serialization.serialize(requestGet);

        } catch (Exception e) {

            System.out.println("error serializing in get method...");
        }

        this.clientService.gNettyMessagingService()
                .sendAsync(Address.from("localhost", Config.init_server_port + store), "client_get", messageBytes)
                .thenRun(() -> {

                    System.out.println("(api) enviei pedido GET para a transacao: " + lastId);

                }).exceptionally(t -> {
                    t.printStackTrace();
                    return null;
                });

        CompletableFuture<Map<Long, byte[]>> resultGet = new CompletableFuture<>();

        futureR.addPendingGetRequest(resultGet);

        return resultGet;
    }
}
