/*
 * Generic Server Update Clock.
 * 
 * @author Grupo10-FSD
 * 
*/

package app.server.data;

import java.io.Serializable;

public class SMUpdateClock implements Serializable {

    private static final long serialVersionUID = 1L;

    private int[] clock;

    public SMUpdateClock(int[] clock) {
        this.clock = clock;
    }

    public int[] getClock() {
        return this.clock;
    }
}
