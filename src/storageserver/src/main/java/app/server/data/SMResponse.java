package app.server.data;

import java.io.Serializable;

public class SMResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private int requestID;
    private boolean updated;
    private Long key;
    private StorageValue keyValue;
    private boolean isPut;
    private boolean hasValue;

    public SMResponse(int tid, boolean updated, Long key, String type) {

        this.requestID = tid;
        this.updated = updated;
        this.key = key;
        this.isPut = type.equals("put") ? true : false;
        this.hasValue = false;
    }

    public SMResponse(int tid, Long key, String type) {

        this.requestID = tid;
        this.key = key;
        this.isPut = type.equals("put") ? true : false;
        this.hasValue = false;
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
}