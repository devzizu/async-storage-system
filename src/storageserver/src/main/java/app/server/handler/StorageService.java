package app.server.handler;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import app.Config;
import app.Serialization;
import app.data.ClientMessage;
import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;

public class StorageService {

    private ScheduledExecutorService executorService;
    private NettyMessagingService messagingService;
    private int[] LOGICAL_CLOCK;

    public StorageService(int sid, int sport, int[] clock) {

        this.executorService = Executors.newScheduledThreadPool(Config.server_thread_pool_size);
        this.messagingService = new NettyMessagingService("serverms_" + sid, Address.from(sport),
                new MessagingConfig());
        this.LOGICAL_CLOCK = clock;
    }

    public void start() {

        this.register_client_handlers();

        this.messagingService.start();
    }

    public void register_client_handlers() {

        this.messagingService.registerHandler("client_put", (address, messageBytes) -> {

            ClientMessage cMessage = null;

            try {
                cMessage = Serialization.deserialize(messageBytes);
            } catch (ClassNotFoundException | IOException e) {
            }

            System.out.println("Received client put:");
            System.out.println(cMessage.toString());

            this.messagingService.sendAsync(address, "client_response", messageBytes).thenRun(() -> {

                System.out.println("Mensagem Response PUT enviada!");

            }).exceptionally(t -> {
                t.printStackTrace();
                return null;
            });

        }, this.executorService);

        this.messagingService.registerHandler("client_get", (address, messageBytes) -> {

            ClientMessage cMessage = null;

            try {
                cMessage = Serialization.deserialize(messageBytes);
            } catch (ClassNotFoundException | IOException e) {
            }

            System.out.println("Received client get:");
            System.out.println(cMessage.toString());

            this.messagingService.sendAsync(address, "client_response", messageBytes).thenRun(() -> {

                System.out.println("Mensagem Response GET enviada!");

            }).exceptionally(t -> {
                t.printStackTrace();
                return null;
            });

        }, this.executorService);
    }

}
