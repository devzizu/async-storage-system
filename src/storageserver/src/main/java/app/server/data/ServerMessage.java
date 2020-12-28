
package app.server.data;

import java.io.Serializable;

public class ServerMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private PutTransaction transaction;
    private Long keyToVerify;
    private byte[] keyValue;
    private int FROM_ID;

    public ServerMessage(PutTransaction transaction, Long keyToVerify, byte[] keyValue, int fromID) {
        this.transaction = transaction;
        this.keyToVerify = keyToVerify;
        this.keyValue = keyValue;
        this.FROM_ID = fromID;
    }

    public PutTransaction getTransaction() {
        return this.transaction;
    }

    public Long getKeyToVerify() {
        return this.keyToVerify;
    }

    public byte[] getKeyValue() {
        return this.keyValue;
    }

    public int getFromID() {
        return this.FROM_ID;
    }
}
