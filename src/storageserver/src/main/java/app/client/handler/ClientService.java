
package app.client.handler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import app.Config;
import app.Serialization;
import app.client.FutureResponses;
import app.data.ClientMessage;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;

public class ClientService {

    private ScheduledExecutorService executorService;
    private NettyMessagingService messagingService;
    private FutureResponses FUTURE_RESPONSES;

    public ClientService(int cid, int cport, FutureResponses res) {

        this.executorService = Executors.newScheduledThreadPool(Config.client_thread_pool_size);
        this.messagingService = new NettyMessagingService("clientms_" + cid, Address.from(cport),
                new MessagingConfig());
        this.FUTURE_RESPONSES = res;
    }

    public void start() {

        this.register_client_handlers();

        this.messagingService.start();
    }

    public void register_client_handlers() {

        // handler for put requests
        this.messagingService.registerHandler("client_response", (address, messageBytes) -> {

            process_response(messageBytes);

        }, this.executorService);
    }

    private void process_response(byte[] messageBytes) {
        ClientMessage cMessage = null;

        try {
            cMessage = Serialization.deserialize(messageBytes);
        } catch (ClassNotFoundException | IOException e) {
        }

        System.out.println("Recebi " + (cMessage.isPut() ? "put" : "get") + " , vou processar");

        this.FUTURE_RESPONSES.complete(cMessage);
    }

    public NettyMessagingService gNettyMessagingService() {
        return this.messagingService;
    }

    public FutureResponses getFutureResponses() {
        return this.FUTURE_RESPONSES;
    }
}
