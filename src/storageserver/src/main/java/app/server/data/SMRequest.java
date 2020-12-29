
package app.server.data;

import java.io.Serializable;

public class SMRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long keyToVerify;
    private StorageValue keyValue;
    private int FROM_ID;
    private int requestID;
    private boolean isPut;

    public SMRequest(Long keyToVerify, int fromID, int tid, String type) {

        this.keyToVerify = keyToVerify;
        this.FROM_ID = fromID;
        this.requestID = tid;
        this.isPut = type.equals("put") ? true : false;
    }

    public SMRequest(Long keyToVerify, StorageValue keyValue, int fromID, int tid, String type) {

        this.keyToVerify = keyToVerify;
        this.keyValue = keyValue;
        this.FROM_ID = fromID;
        this.requestID = tid;
        this.isPut = type.equals("put") ? true : false;
    }

    public Long getKeyToVerify() {
        return this.keyToVerify;
    }

    public StorageValue getKeyValue() {
        return this.keyValue;
    }

    public int getFromID() {
        return this.FROM_ID;
    }

    public int getRequestID() {
        return this.requestID;
    }

    public boolean isPutRequest() {
        return this.isPut;
    }
}
