package app.server.data;

import java.io.Serializable;
import java.util.List;

public class SMResponsePut implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<Long> keysToAbort;
    private int TRANSACTION_ID;

    public SMResponsePut(List<Long> keys, int id) {

        this.keysToAbort = keys;
        this.TRANSACTION_ID = id;
    }

    public List<Long> getKeysToAbort() {
        return this.keysToAbort;
    }

    public int getTRANSACTION_ID() {
        return this.TRANSACTION_ID;
    }

}
