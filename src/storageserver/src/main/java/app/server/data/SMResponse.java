/*
 * Generic Server Response Message.
 * 
 * @author Grupo10-FSD
 * 
*/

package app.server.data;

import java.io.Serializable;

import app.server.clock.LogicalClockTool;

public class SMResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private int requestID;
    private boolean updated;
    private Long key;
    private StorageValue keyValue;
    private boolean isPut;
    private boolean hasValue;

    private int[] clock;
    private boolean isUpdateClock;
    private int fromID;
    private int clientPort;

    public SMResponse(int tid, boolean updated, Long key, String type, int[] clock, int fromId) {

        this.fromID = fromId;
        this.requestID = tid;
        this.updated = updated;
        this.key = key;
        this.isPut = type.equals("put") ? true : false;
        this.hasValue = false;
        this.isUpdateClock = false;
        this.clock = clock;
    }

    public SMResponse(int tid, Long key, String type, int[] clock, int fromID) {

        this.requestID = tid;
        this.key = key;
        this.isPut = type.equals("put") ? true : false;
        this.hasValue = false;
        this.isUpdateClock = false;
        this.clock = clock;
        this.fromID = fromID;
    }

    public SMResponse(int[] clock, int fromId) {

        this.fromID = fromId;
        this.isUpdateClock = true;
        this.clock = clock;
    }

    public void setClientPort(int port) {
        this.clientPort = port;
    }

    public int getClientPort() {
        return this.clientPort;
    }

    public int getRequestID() {
        return this.requestID;
    }

    public boolean wasUpdated() {
        return this.updated;
    }

    public StorageValue getValue() {

        return this.keyValue;
    }

    public Long getKey() {
        return this.key;
    }

    public boolean isResponsePut() {
        return this.isPut;
    }

    public void setHasValue(boolean s) {
        this.hasValue = s;
    }

    public void setValue(StorageValue val) {
        this.keyValue = val;
    }

    public boolean hasValue() {
        return this.hasValue;
    }

    public int[] getClock() {

        return this.clock;
    }

    public int getFromID() {

        return this.fromID;
    }

    public boolean isUpdatedClock() {

        return this.isUpdateClock;
    }

    @Override
    public String toString() {
        return "{" + ", key='" + getKey() + "'" + ", clock='" + LogicalClockTool.printArray(getClock()) + "'" + "}";
    }
}
