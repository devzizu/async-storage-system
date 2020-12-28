
package app.client.handler;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import app.Config;
import app.Serialization;
import app.client.FutureResponses;
import app.data.CMResponsePut;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;

public class ClientService {

    private ScheduledExecutorService executorService;
    private NettyMessagingService messagingService;
    private FutureResponses FUTURE_RESPONSES;
    private int clientPort;

    public ClientService(int cid, int cport, FutureResponses res) {

        this.executorService = Executors.newScheduledThreadPool(Config.client_thread_pool_size);
        this.messagingService = new NettyMessagingService("clientms_" + cid, Address.from(cport),
                new MessagingConfig());
        this.FUTURE_RESPONSES = res;
        this.clientPort = cport;
    }

    public void start() {

        this.register_client_handlers();

        this.messagingService.start();
    }

    public void register_client_handlers() {

        this.register_client_response();
    }

    private void register_client_response() {

        this.messagingService.registerHandler("client_response_put", (address, messageBytes) -> {

            CMResponsePut responsePut = null;

            try {
                responsePut = (CMResponsePut) Serialization.deserialize(messageBytes);
            } catch (ClassNotFoundException | IOException e) {
            }

            System.out.println("Recebi resposta ao put, vou processar");

            this.FUTURE_RESPONSES.complete(responsePut);

        }, this.executorService);
    }

    public NettyMessagingService gNettyMessagingService() {
        return this.messagingService;
    }

    public FutureResponses getFutureResponses() {
        return this.FUTURE_RESPONSES;
    }

    public int getClientPort() {
        return this.clientPort;
    }
}
