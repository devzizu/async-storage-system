/*
 * Generic Client Response GET Message.
 * 
 * @author Grupo10-FSD
 * 
*/

package app.data;

import java.io.Serializable;
import java.util.Map;

public class CMResponseGet implements Serializable {

    private static final long serialVersionUID = 1L;

    private int MESSAGE_ID;
    private Map<Long, byte[]> resultGet;

    public CMResponseGet(int MESSAGE_ID, Map<Long, byte[]> resultGet) {
        this.MESSAGE_ID = MESSAGE_ID;
        this.resultGet = resultGet;
    }

    public int getMESSAGE_ID() {
        return this.MESSAGE_ID;
    }

    public Map<Long, byte[]> getResultGet() {
        return this.resultGet;
    }
}
