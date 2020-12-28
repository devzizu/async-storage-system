
package app.server.data;

import java.io.Serializable;

public class ServerMessage implements Serializable {

    private PutTransaction transaction;
    private Long keyToVerify;
    private byte[] keyValue;

    public ServerMessage(PutTransaction transaction, Long keyToVerify, byte[] keyValue) {
        this.transaction = transaction;
        this.keyToVerify = keyToVerify;
        this.keyValue = keyValue;
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

}
