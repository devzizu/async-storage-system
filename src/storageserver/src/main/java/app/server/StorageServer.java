
package app.server;

import org.apache.log4j.Logger;

import app.Config;
import app.server.clock.LogicalClockTool;
import app.server.handler.StorageService;

public class StorageServer {

    private static Logger LOGGER = Logger.getLogger(StorageServer.class);

    private static int SERVER_ID;
    private static int SERVER_PORT;
    private static int[] LOGICAL_CLOCK;

    public static void main(String[] args) {

        System.out.print("\033[H\033[2J");

        // ---------------------------------------------------------------------------------------------------
        // reading arguments, with format = "<id>"

        try {

            String congif = "config.toml";

            SERVER_ID = Integer.parseInt(args[0]);
            Config.read("../" + congif);

            LOGGER.info("Configuration file \"" + congif + "\"");
        } catch (Exception e) {

            LOGGER.error("Error configuring client service port, exiting...");
            return;
        }

        SERVER_PORT = Config.init_server_port + SERVER_ID;

        LOGGER.info("STORAGE ID " + SERVER_ID + " running...");

        // ---------------------------------------------------------------------------------------------------

        LOGICAL_CLOCK = new int[Config.nr_servers];
        for (int i = 0; i < Config.nr_servers; i++)
            LOGICAL_CLOCK[i] = 0;

        LOGGER.info("initializing local clock " + LogicalClockTool.printArray(LOGICAL_CLOCK));

        StorageService st_service = new StorageService(SERVER_ID, SERVER_PORT, LOGICAL_CLOCK);

        st_service.start();
    }
}