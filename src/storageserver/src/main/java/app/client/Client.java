
package app.client;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import app.Config;
import app.client.api.StorageAPI;
import app.client.handler.ClientService;

public class Client {

    private static int CLIENT_ID;
    private static int CLIENT_PORT;

    public static void main(String[] args) {

        System.out.print("\033[H\033[2J");

        // ---------------------------------------------------------------------------------------------------
        // reading arguments, with format = "<id>"

        try {

            CLIENT_ID = Integer.parseInt(args[0]);
            Config.read("../config.toml");

        } catch (Exception e) {

            System.out.println("error configuring server port, exiting... ");
            return;
        }

        CLIENT_PORT = Config.init_client_port + CLIENT_ID;

        System.out.println("client configured to run @ port = " + CLIENT_PORT);

        // ---------------------------------------------------------------------------------------------------

        // saves client pending responses
        FutureResponses responses = new FutureResponses();

        // handles client handlers completes responses
        ClientService cli_service = new ClientService(CLIENT_ID, CLIENT_PORT, responses);
        cli_service.start();

        // makes requests and queues them
        StorageAPI API = new StorageAPI(cli_service);

        // ---------------------------------------------------------------------------------------------------

        if (CLIENT_ID == 0) {

            CompletableFuture<Void> resultPut = null;

            Map<Long, byte[]> mapTestPut = new HashMap<>();
            mapTestPut.put((long) 0, "chave0".getBytes());
            mapTestPut.put((long) 2, "chave2".getBytes());
            mapTestPut.put((long) 1, "chave1".getBytes());
            mapTestPut.put((long) 3, "chave3".getBytes());

            resultPut = API.put(mapTestPut, 0);

            resultPut.thenAccept(voidValue -> {
                System.out.println("(cliente) Recebi confirmação PUT de que terminou, OK!");
            });

        } else if (CLIENT_ID == 1) {

            Collection<Long> keysToGet = new HashSet<Long>();
            keysToGet.add((long) 0);
            keysToGet.add((long) 2);
            keysToGet.add((long) 1);
            keysToGet.add((long) 3);

            CompletableFuture<Map<Long, byte[]>> resultGet = API.get(keysToGet, 1);

            resultGet.thenAccept(map -> {
                System.out.println("(cliente) Recebi confirmação GET de que terminou, OK!");
                System.out.println("Result map:");
                map.entrySet().forEach(
                        e -> System.out.println("key: " + e.getKey() + ", val: " + (new String(e.getValue()))));
            });
        }
    }
}
