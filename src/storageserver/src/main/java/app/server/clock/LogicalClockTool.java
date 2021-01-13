/*
 * Tool to verify causal delivery of messages given two clocks
 * and verify concurrency.
 * 
 * @author Grupo10-FSD
 * 
*/

package app.server.clock;

import app.Config;
import app.server.data.SMRequest;
import app.server.data.SMResponse;

public class LogicalClockTool {

    /**
     * Implements causal rule for SMRequest
     * 
     * @param msg           server request message
     * @param LOGICAL_CLOCK server clock
     * @return true if the message should be accepted
     */
    public synchronized static boolean conditionVerifier(SMRequest msg, int[] LOGICAL_CLOCK) {

        int fromID = msg.getFromID();
        int[] clock = msg.getClock();

        boolean check = verify(clock, LOGICAL_CLOCK, fromID);

        return check;
    }

    /**
     * Implements causal rule for SMResponse
     * 
     * @param msg           server response message
     * @param LOGICAL_CLOCK server clock
     * @return true if the message should be accepted
     */
    public synchronized static boolean conditionVerifier(SMResponse msg, int[] LOGICAL_CLOCK) {

        int fromID = msg.getFromID();
        int[] clock = msg.getClock();

        boolean check = verify(clock, LOGICAL_CLOCK, fromID);

        return check;
    }

    /**
     * Implements causal rule for two clocks.
     * 
     * @param clock         Receiver clock
     * @param LOGICAL_CLOCK Local clock
     * @param fromID        Receiver id
     * @return
     */
    private synchronized static boolean verify(int[] clock, int[] LOGICAL_CLOCK, int fromID) {

        boolean res = ((LOGICAL_CLOCK[fromID] + 1) == clock[fromID]);

        for (int i = 0; i < Config.nr_servers && res; i++)
            if (i != fromID)
                res = res && (clock[i] <= LOGICAL_CLOCK[i]);

        // se a mensagem foi aceite

        if (res) {
            for (int i = 0; i < Config.nr_servers; i++)
                LOGICAL_CLOCK[i] = Integer.max(LOGICAL_CLOCK[i], clock[i]);
        }

        return res;
    }

    /**
     * Converts any array to a string.
     * 
     * @param arr array
     * @return string representation of the array
     */
    public static String printArray(int[] arr) {
        String res = "[";
        for (int i = 0; i < arr.length; i++) {
            if (i == arr.length - 1)
                res += (arr[i]);
            else
                res += (arr[i] + ",");
        }
        res += ("]");
        return res;
    }

    /**
     * Tests if two clocks are concurrent, by vector clocks rules.
     * 
     * @param c1 clock 1
     * @param c2 clock 2
     * @return true if they are concurrent
     */
    public synchronized static boolean areConcurrent(int[] c1, int[] c2) {

        int bigger = 0, smaller = 0;
        boolean concurrent = false;

        for (int i = 0; i < Config.nr_servers && !concurrent; i++) {
            if (c1[i] <= c2[i])
                smaller++;
            else if (c1[i] >= c2[i])
                bigger++;
            if (smaller > 0 && bigger > 0)
                concurrent = true;
        }

        return concurrent;
    }
}
