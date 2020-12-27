
package app.client.api;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.SerializationUtils;

import app.Config;
import app.Serialization;
import app.client.FutureResponses;
import app.client.handler.ClientService;
import app.data.ClientMessage;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;

public class StorageAPI {

    private ClientService clientService;

    public StorageAPI(ClientService clientService) {

        this.clientService = clientService;
    }

    public CompletableFuture<Void> put(Map<Long, byte[]> map) {

        FutureResponses futureR = this.clientService.getFutureResponses();
        int lastId = futureR.getId();

        ClientMessage clMessage = new ClientMessage(map, lastId);

        byte[] messageBytes = null;

        try {

            messageBytes = Serialization.serialize(clMessage);

        } catch (Exception e) {

            System.out.println("error serializing in put method...");
        }

        this.clientService.gNettyMessagingService()
                .sendAsync(Address.from("localhost", Config.init_server_port), "client_put", messageBytes)
                .thenRun(() -> {

                    System.out.println("Mensagem PUT enviada!");

                }).exceptionally(t -> {
                    t.printStackTrace();
                    return null;
                });

        CompletableFuture<Void> resultPut = new CompletableFuture<>();

        futureR.addPendingRequest("PUT", resultPut);

        return resultPut;
    }

    public CompletableFuture<Map<Long, byte[]>> get(Collection<Long> keys) {

        FutureResponses futureR = this.clientService.getFutureResponses();
        int lastId = futureR.getId();

        ClientMessage clMessage = new ClientMessage(keys, lastId);

        byte[] messageBytes = null;

        try {

            messageBytes = Serialization.serialize(clMessage);

        } catch (Exception e) {

            System.out.println("error serializing in get method...");
        }

        this.clientService.gNettyMessagingService()
                .sendAsync(Address.from("localhost", Config.init_server_port), "client_get", messageBytes)
                .thenRun(() -> {

                    System.out.println("Mensagem GET enviada!");

                }).exceptionally(t -> {
                    t.printStackTrace();
                    return null;
                });

        CompletableFuture<Map<Long, byte[]>> resultGet = new CompletableFuture<>();

        futureR.addPendingRequest("GET", resultGet);

        return resultGet;
    }
}
