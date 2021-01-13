/*
 * Generic Client Response PUT Message.
 * 
 * @author Grupo10-FSD
 * 
*/

package app.data;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;

import app.server.data.StorageValue;

public class CMResponsePut implements Serializable {

    private static final long serialVersionUID = 1L;

    private int MESSAGE_ID;
    private ConcurrentHashMap<Long, StorageValue> keysUpdatedBy;

    public CMResponsePut(int MESSAGE_ID, ConcurrentHashMap<Long, StorageValue> updatedBy) {
        this.MESSAGE_ID = MESSAGE_ID;
        this.keysUpdatedBy = updatedBy;
    }

    public int getMESSAGE_ID() {
        return this.MESSAGE_ID;
    }

    public ConcurrentHashMap<Long, StorageValue> getUpdatedByResult() {
        return this.keysUpdatedBy;
    }
}
