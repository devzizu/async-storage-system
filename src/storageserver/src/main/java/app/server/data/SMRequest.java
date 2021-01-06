
package app.server.data;

import java.io.Serializable;

import app.server.clock.LogicalClockTool;

public class SMRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long keyToVerify;
    private StorageValue keyValue;
    private int FROM_ID;
    private int requestID;
    private boolean isPut;
    private boolean isUpdateClock;
    private int[] clock;

    public SMRequest(Long keyToVerify, int fromID, int tid, String type, int[] clock) {

        this.keyToVerify = keyToVerify;
        this.FROM_ID = fromID;
        this.requestID = tid;
        this.isPut = type.equals("put") ? true : false;
        this.clock = clock;
        this.isUpdateClock = false;
    }

    public SMRequest(Long keyToVerify, StorageValue keyValue, int fromID, int tid, String type, int[] clock) {

        this.keyToVerify = keyToVerify;
        this.keyValue = keyValue;
        this.FROM_ID = fromID;
        this.requestID = tid;
        this.isPut = type.equals("put") ? true : false;
        this.isUpdateClock = false;
        this.clock = clock;
    }

    public SMRequest(int[] clock, int fromID) {

        this.isUpdateClock = true;
        this.clock = clock;
        this.FROM_ID = fromID;
    }

    public boolean isUpdateClock() {

        return this.isUpdateClock;
    }

    public int[] getClock() {
        return this.clock;
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

    @Override
    public String toString() {
        return "{" + " keyToVerify='" + getKeyToVerify() + "'" + ", clock='" + LogicalClockTool.printArray(getClock())
                + "'" + "}";
    }
}
