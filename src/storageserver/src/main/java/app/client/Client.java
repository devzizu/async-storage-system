
package app.client;

import java.util.HashMap;
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

        Map<Long, byte[]> mapTestPut = new HashMap<Long, byte[]>();

        CompletableFuture<Void> resultPut = null;

        if (CLIENT_ID == 0) {

            mapTestPut.put((long) 0, "storage0".getBytes());
            mapTestPut.put((long) 1, "storage1".getBytes());
            mapTestPut.put((long) 2, "storage2".getBytes());

            resultPut = API.put(mapTestPut, 0);

        } else if (CLIENT_ID == 1) {

            mapTestPut.put((long) 1, "storage1".getBytes());
            mapTestPut.put((long) 0, "storage2".getBytes());

            resultPut = API.put(mapTestPut, 2);
        }

        // CompletableFuture<Map<Long, byte[]>> resultGet = API
        // .get(mapTestPut.keySet().stream().collect(Collectors.toList()));

        resultPut.thenAccept(action -> {
            System.out.println("(cliente) Recebi confirmação de que terminou, OK!");
        });

        // resultGet.thenAccept(action -> {
        // System.out.println("Received response get!");
        // });
    }
}
