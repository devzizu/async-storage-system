
package app.server;

import app.Config;
import app.server.handler.StorageService;

public class StorageServer {

    private static int SERVER_ID;
    private static int SERVER_PORT;
    private static int[] LOGICAL_CLOCK;

    public static void main(String[] args) {

        System.out.print("\033[H\033[2J");

        // ---------------------------------------------------------------------------------------------------
        // reading arguments, with format = "<id>"

        try {

            SERVER_ID = Integer.parseInt(args[0]);
            Config.read("../config.toml");

        } catch (Exception e) {

            System.out.println("error configuring server port, exiting... ");
            return;
        }

        SERVER_PORT = Config.init_server_port + SERVER_ID;

        System.out.println("storage server configured to run @ port = " + SERVER_PORT);
        System.out.println("------------------------------------------------------------------------------------");
        System.out.println("-----------------------------STORAGE " + SERVER_ID + "------------------------------");
        System.out.println("------------------------------------------------------------------------------------");

        // ---------------------------------------------------------------------------------------------------

        LOGICAL_CLOCK = new int[Config.nr_servers];
        for (int i = 0; i < Config.nr_servers; i++)
            LOGICAL_CLOCK[i] = 0;

        StorageService st_service = new StorageService(SERVER_ID, SERVER_PORT, LOGICAL_CLOCK);

        st_service.start();
    }
}