
package app.client;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

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

        Map<Long, byte[]> mapTestPut = new HashMap<Long, byte[]>();

        mapTestPut.put((long) 1, "val1".getBytes());
        mapTestPut.put((long) 2, "val2".getBytes());
        mapTestPut.put((long) 3, "val3".getBytes());
        mapTestPut.put((long) 4, "val4".getBytes());

        CompletableFuture<Void> resultPut = API.put(mapTestPut);
        CompletableFuture<Map<Long, byte[]>> resultGet = API
                .get(mapTestPut.keySet().stream().collect(Collectors.toList()));

        resultPut.thenAccept(action -> {
            System.out.println("Received response put!");
        });

        resultGet.thenAccept(action -> {
            System.out.println("Received response get!");
        });
    }
}
