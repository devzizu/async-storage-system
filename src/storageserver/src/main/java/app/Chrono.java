/*
 * This class implements methods for performance measurement
 * 
 * @author Grupo10-FSD
 * 
*/

package app;

public class Chrono {

    /**
     * Start time
     */
    private static long startTimeNano;
    /**
     * End time
     */
    private static long stopTimeNano;

    /**
     * Set start value as the current system nano time
     */
    public static void start() {

        startTimeNano = System.nanoTime();
    }

    /**
     * Set stop value as the current system nano time
     */
    public static void stop() {

        stopTimeNano = System.nanoTime();
    }

    /**
     * Returns statistics about the current start/stop sequence
     */
    public static String stats() {

        long elapseNano = stopTimeNano - startTimeNano;
        long mili = elapseNano / 1000000;
        long seconds = mili / 1000;

        return "[nanoseconds]: " + elapseNano + " | [milliseconds]: " + mili + " | [seconds]: " + seconds;
    }
}
