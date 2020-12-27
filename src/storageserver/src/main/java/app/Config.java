
package app;

import java.io.File;
import java.util.Map;

import com.moandjiezana.toml.Toml;

public class Config {

    private static Map<String, Object> defaults;
    private static Map<String, Object> servers;

    public static int nr_servers;
    public static int server_thread_pool_size;
    public static int client_thread_pool_size;

    public static int init_server_port;
    public static int init_client_port;

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
