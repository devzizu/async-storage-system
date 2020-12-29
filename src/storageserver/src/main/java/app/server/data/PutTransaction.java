
package app.server.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PutTransaction implements Serializable {

    private static final long serialVersionUID = 1L;

    // keys to put
    private Map<Long, Boolean> keysToPut;
    private int[] timestamp;
    private int clientPort;
    private int TRANSACTION_ID;

    public PutTransaction(List<Long> keys, int[] timestamp, int port, int id) {

        this.keysToPut = new HashMap<>();
        keys.forEach(k -> this.keysToPut.put(k, false));
        this.timestamp = timestamp;
        this.clientPort = port;
        this.TRANSACTION_ID = id;
    }

    public synchronized void setDone(Long key) {

        this.keysToPut.replace(key, true);
    }

    public Map<Long, Boolean> getKeysToPut() {
        return this.keysToPut;
    }

    public int[] getTimestamp() {
        return this.timestamp;
    }

    public int getClientPort() {
        return this.clientPort;
    }

    public int getTRANSACTION_ID() {
        return TRANSACTION_ID;
    }

    @Override
    public String toString() {
        return "{" + " keysToPut='" + getKeysToPut() + "'" + ", timestamp='" + getTimestamp() + "}";
    }

    public synchronized boolean isFinished() {

        return this.keysToPut.entrySet().stream().filter(e -> e.getValue() == false).count() == 0;
    }
}