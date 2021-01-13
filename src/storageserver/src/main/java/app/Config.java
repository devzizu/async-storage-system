/*
 * Reads and stores all the configuration atributes using an
 * structured config TOML file (as defined for this project).
 * 
 * @author Grupo10-FSD
 * 
*/

package app;

import java.io.File;
import java.util.Map;

import com.moandjiezana.toml.Toml;

public class Config {

    /**
     * Stores the defaults table
     */
    private static Map<String, Object> defaults;
    /**
     * Stores the servers table
     */
    private static Map<String, Object> servers;
    /**
     * Stores the number of servers
     */
    public static int nr_servers;
    /**
     * Stores the server thread pool size for the storage server
     */
    public static int server_thread_pool_size;
    /**
     * Stores the client thread pool size
     */
    public static int client_thread_pool_size;
    /**
     * Stores the start server port
     */
    public static int init_server_port;
    /**
     * Stores the start client port
     */
    public static int init_client_port;

    /**
     * Read TOML configuration file and store all its values.
     * 
     * @param configFileName configuration file name
     * @throws Exception
     */
    public static void read(String configFileName) throws Exception {

        File configFile = new File("../config.toml");
        Toml tomlObj = new Toml().read(configFile);

        defaults = tomlObj.getTable("defaults").toMap();
        servers = tomlObj.getTable("servers").toMap();

        nr_servers = Integer.parseInt(Config.get_value("defaults", "nr_servers"));
        init_client_port = Integer.parseInt(Config.get_value("servers", "init_client_port"));
        client_thread_pool_size = Integer.parseInt(Config.get_value("defaults", "client_thread_pool_size"));

        init_server_port = Integer.parseInt(Config.get_value("servers", "init_server_port"));
        server_thread_pool_size = Integer.parseInt(Config.get_value("defaults", "server_thread_pool_size"));
    }

    /**
     * Get value from a given table.
     * 
     * @param table table name
     * @param key   atribute to search
     * @return value from the table
     */
    private static String get_value(String table, String key) {

        String result;

        switch (table) {

            case "defaults":
                result = defaults.get(key).toString();
                break;

            case "servers":
                result = servers.get(key).toString();
                break;

            default:
                result = null;
                break;
        }

        return result;
    }
}
