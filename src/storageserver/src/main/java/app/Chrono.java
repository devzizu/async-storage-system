package app;

public class Chrono {

    private static long startTimeNano;
    private static long stopTimeNano;

    public static void start() {

        startTimeNano = System.nanoTime();
    }

    public static void stop() {

        stopTimeNano = System.nanoTime();
    }

    public static String stats() {

        long elapseNano = stopTimeNano - startTimeNano;
        long mili = elapseNano / 1000000;
        long seconds = mili / 1000;

        return "[nanoseconds]: " + elapseNano + " | [milliseconds]: " + mili + " | [seconds]: " + seconds;
    }
}
