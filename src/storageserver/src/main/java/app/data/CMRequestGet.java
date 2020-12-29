package app.data;

import java.io.Serializable;
import java.util.Collection;

public class CMRequestGet implements Serializable {

    private static final long serialVersionUID = 1L;

    private Collection<Long> keysToRequest;
    private int MESSAGE_ID;
    private int CLI_PORT;

    public CMRequestGet(Collection<Long> keysToRequest, int MESSAGE_ID, int CLI_PORT) {
        this.keysToRequest = keysToRequest;
        this.MESSAGE_ID = MESSAGE_ID;
        this.CLI_PORT = CLI_PORT;
    }

    public Collection<Long> getKeysToRequest() {
        return this.keysToRequest;
    }

    public int getMESSAGE_ID() {
        return this.MESSAGE_ID;
    }

    public int getCLI_PORT() {
        return this.CLI_PORT;
    }
}
