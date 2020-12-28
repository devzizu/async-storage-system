
package app.server.data;

import java.io.Serializable;

public class SMRequestPut implements Serializable {

    private static final long serialVersionUID = 1L;

    private PutTransaction transaction;
    private Long keyToVerify;
    private byte[] keyValue;
    private int FROM_ID;
    private int TRANSACTION_ID;

    public SMRequestPut(PutTransaction transaction, Long keyToVerify, byte[] keyValue, int fromID, int tid) {
        this.transaction = transaction;
        this.keyToVerify = keyToVerify;
        this.keyValue = keyValue;
        this.FROM_ID = fromID;
        this.TRANSACTION_ID = tid;
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

    public int getTransactionID() {
        return this.TRANSACTION_ID;
    }
}
