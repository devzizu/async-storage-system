
package app.server.data;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class PutTransaction implements Serializable {

    private static final long serialVersionUID = 1L;

    // keys to put
    private Map<Long, Boolean> keysToPut;
    private int[] timestamp;

    public PutTransaction(List<Long> keys, int[] timestamp) {

        this.keysToPut = new HashMap<>();
        keys.forEach(k -> this.keysToPut.put(k, false));
        this.timestamp = timestamp;
    }

    public void setDone(Long key) {

        this.keysToPut.replace(key, true);
    }

    public Map<Long, Boolean> getKeysToPut() {
        return this.keysToPut;
    }

    public int[] getTimestamp() {
        return this.timestamp;
    }

    @Override
    public String toString() {
        return "{" + " keysToPut='" + getKeysToPut() + "'" + ", timestamp='" + getTimestamp() + "'" + "}";
    }

    public HashSet<Long> getInCommon(PutTransaction received) {

        List<Long> received_keys = received.getKeysToPut().keySet().stream().collect(Collectors.toList());
        List<Long> my_keys = this.keysToPut.keySet().stream().collect(Collectors.toList());

        Set<Long> emComum = received_keys.stream().distinct().filter(my_keys::contains).collect(Collectors.toSet());

        return emComum.stream().collect(Collectors.toCollection(HashSet::new));
    }
}