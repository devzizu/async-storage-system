import io.atomix.cluster.messaging.MessagingConfig;
import io.atomix.cluster.messaging.impl.NettyMessagingService;
import io.atomix.utils.net.Address;
import io.netty.channel.unix.Socket;

import org.apache.commons.lang3.SerializationUtils;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ChatAtomix {

    public static int nProcessos = 3;

    public static int id;

    public static int[] localArray = { 0, 0, 0 };

    public static List<Message> l = new ArrayList<>();

    public static void correQueue(Address a) {

        for (int i = 0; i < l.size(); i++) {
            Message m = l.remove(0);
            if (conditionVerifier(m)) {
                System.out.println("[" + a + "]: " + m.toString());
            }
        }
    }

    public static boolean conditionVerifier(Message msg) {
        boolean res = ((localArray[msg.id] + 1) == msg.array[msg.id]);
        for (int i = 0; i < nProcessos && res; i++) {
            if (i != msg.id) {
                res = res && (msg.array[i] <= localArray[i]);
            }
        }
        if (res) {
            for (int i = 0; i < nProcessos; i++) {
                localArray[i] = Integer.max(localArray[i], msg.array[i]);
            }

        } else {
            l.add(msg);
        }
        return res;
    }

    public static void main(String[] args) throws Exception {

        l.add(new Message(new int[] { 1, 1, 1 }, 1, "ola do futuro"));

        int port = Integer.parseInt(args[1]);
        ;

        id = Integer.parseInt(args[0]);

        ScheduledExecutorService es = Executors.newScheduledThreadPool(2);
        NettyMessagingService ms = new NettyMessagingService("nome", Address.from(port), new MessagingConfig());

        int porta = 12345;

        ms.registerHandler("hello", (a, m) -> {
            Message message = SerializationUtils.deserialize(m);

            boolean b = conditionVerifier(message);
            if (b) {
                System.out.println("[" + a + "]: " + message.toString());
                correQueue(a);
            }
            System.out.println(l.toString());

        }, es);

        ms.start();

        Scanner in = new Scanner(System.in);
        String str;

        while ((str = in.nextLine()) != null) {

            localArray[id]++;
            Message msg = new Message(localArray, id, str);
            byte[] data = SerializationUtils.serialize(msg);

            for (int i = 0; i < nProcessos; i++) {
                if (i != id) {
                    ms.sendAsync(Address.from("localhost", porta + i), "hello", data).thenRun(() -> {
                        // System.out.println("Mensagem enviada!");
                    }).exceptionally(t -> {
                        t.printStackTrace();
                        return null;
                    });
                }
            }
        }
    }
}