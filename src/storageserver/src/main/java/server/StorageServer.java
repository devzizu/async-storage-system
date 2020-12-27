
package server;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import server.config.Config;

public class StorageServer {

    private static int INIT_PORT = Integer.parseInt(Config.get_value("servers", "init_port"));

    public static void main(String[] args) {

        // ---------------------------------------------------------------------------------------------------

        // arguments = <id>

        int SERVER_ID = -1;

        try {
            SERVER_ID = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.out.println("could not read config file...");
        }

        if (SERVER_ID == -1)
            return;

        // ---------------------------------------------------------------------------------------------------

        ScheduledExecutorService es = Executors.newScheduledThreadPool(2);
        NettyMessagingService ms = new NettyMessagingService("nome", Address.from(port), new MessagingConfig());

        ms.registerHandler("random", (a, m) -> {

            System.out.println(m);

        }, es);

        ms.start();
    }
}