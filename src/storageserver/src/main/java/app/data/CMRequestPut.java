/*
 * Generic Client Request PUT Message.
 * 
 * @author Grupo10-FSD
 * 
*/

package app.data;

import java.io.Serializable;
import java.util.Map;

public class CMRequestPut implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<Long, byte[]> requestPut;
    private int MESSAGE_ID;
    private int CLI_PORT;

    public CMRequestPut(Map<Long, byte[]> requestPut, int id, int port) {
        this.requestPut = requestPut;
        this.MESSAGE_ID = id;
        this.CLI_PORT = port;
    }

    public Map<Long, byte[]> getRequestPut() {
        return this.requestPut;
    }

    public int getMESSAGE_ID() {
        return this.MESSAGE_ID;
    }

    public int getCLI_PORT() {
        return this.CLI_PORT;
    }

    @Override
    public String toString() {
        return "{" + " requestPut='" + getRequestPut() + "'" + ", MESSAGE_ID='" + getMESSAGE_ID() + "'" + ", CLI_PORT='"
                + getCLI_PORT() + "'" + "}";
    }

}
