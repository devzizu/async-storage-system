/*
 * Generic GET transaction.
 * 
 * @author Grupo10-FSD
 * 
*/

package app.server.data;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class GetTransaction implements Serializable {

    private static final long serialVersionUID = 1L;

    private int clientPort;
    private int TRANSACTION_ID;
    private int getsDone;

    private Map<Long, byte[]> preparedGets;

    public GetTransaction(int clientPort, int TRANSACTION_ID, Collection<Long> keys) {
        this.clientPort = clientPort;
        this.TRANSACTION_ID = TRANSACTION_ID;
        this.getsDone = 0;
        this.preparedGets = new HashMap<>();
        keys.forEach(k -> this.preparedGets.put(k, null));
    }

    public int getClientPort() {
        return this.clientPort;
    }

    public int getTRANSACTION_ID() {
        return this.TRANSACTION_ID;
    }

    public int getGetsDone() {
        return this.getsDone;
    }

    public synchronized void incrementDone() {
        this.getsDone++;
    }

    public Map<Long, byte[]> getPreparedGets() {
        return this.preparedGets;
    }

    public void setDone(Long key, byte[] value) {
        this.preparedGets.replace(key, value);
    }

    public boolean isFinished() {
        return this.preparedGets.size() == this.getsDone;
    }

    public void removeUnexisting(Long key) {
        this.preparedGets.remove(key);
    }

    @Override
    public String toString() {
        return "{ getsDone='" + getGetsDone() + "'" + ", preparedGets='" + getPreparedGets() + "'" + "}";
    }

}
