
package app.server.data;

import java.io.Serializable;

import app.Config;
import app.server.clock.LogicalClockTool;

public class StorageValue implements Serializable {

    private static final long serialVersionUID = 1L;

    private byte[] data;
    private int[] timestamp;
    private int ServerWhichUpdate;

    public StorageValue(byte[] d, int[] time, int server) {
        this.data = d;
        this.timestamp = time;
        this.ServerWhichUpdate = server;
    }

    public int[] getTimeStamp() {

        int[] copyTimestamp = new int[Config.nr_servers];
        System.arraycopy(this.timestamp, 0, copyTimestamp, 0, Config.nr_servers);

        return copyTimestamp;
    }

    public int getServerWhichUpdate() {
        return ServerWhichUpdate;
    }

    public void setServerWhichUpdate(int serverWhichUpdate) {
        ServerWhichUpdate = serverWhichUpdate;
    }

    @Override
    public String toString() {
        return "{" + "dTime='" + LogicalClockTool.printArray(timestamp) + "'" + " updated by server "
                + ServerWhichUpdate + "}";
    }

    public byte[] getData() {

        return this.data;
    }
}
