
package server.config;

import java.io.File;
import java.util.Map;

import com.moandjiezana.toml.Toml;

public class Config {

    private static File configFile = new File("../config.toml");
    private static Toml tomlObj = new Toml().read(configFile);

    private static Map<String, Object> defaults = tomlObj.getTable("defaults").toMap();
    private static Map<String, Object> servers = tomlObj.getTable("servers").toMap();

    public static String get_value(String table, String key) {

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
