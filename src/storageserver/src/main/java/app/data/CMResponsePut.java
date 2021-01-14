/*
 * Generic Client Response PUT Message.
 * 
 * @author Grupo10-FSD
 * 
*/

package app.data;

import java.io.Serializable;

public class CMResponsePut implements Serializable {

    private static final long serialVersionUID = 1L;

    private int MESSAGE_ID;

    public CMResponsePut(int MESSAGE_ID) {
        this.MESSAGE_ID = MESSAGE_ID;
    }

    public int getMESSAGE_ID() {
        return this.MESSAGE_ID;
    }
}
