
package app.server.clock;

import app.Config;
import app.server.data.SMRequest;
import app.server.data.SMResponse;

public class LogicalClockTool {

    public synchronized static boolean conditionVerifier(SMRequest msg, int[] LOGICAL_CLOCK) {

        int fromID = msg.getFromID();
        int[] clock = msg.getClock();

        boolean check = verify(clock, LOGICAL_CLOCK, fromID);

        return check;
    }

    public synchronized static boolean conditionVerifier(SMResponse msg, int[] LOGICAL_CLOCK) {

        int fromID = msg.getFromID();
        int[] clock = msg.getClock();

        boolean check = verify(clock, LOGICAL_CLOCK, fromID);

        return check;
    }

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
