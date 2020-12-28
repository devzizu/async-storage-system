package app.data;

import java.io.Serializable;
import java.util.Collection;
import java.util.Map;

public class ClientMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private Map<Long, byte[]> requestPut;
    private Collection<Long> requestGet;
    private int MESSAGE_ID;
    private boolean isPut;

    public ClientMessage(Map<Long, byte[]> requestPut, int id) {
        this.requestPut = requestPut;
        this.MESSAGE_ID = id;
        this.isPut = true;
    }

    public ClientMessage(Collection<Long> requestGet, int id) {
        this.requestGet = requestGet;
        this.MESSAGE_ID = id;
        this.isPut = false;
    }

    public Map<Long, byte[]> getRequestPut() {
        return this.requestPut;
    }

    public Collection<Long> getRequestGet() {
        return this.requestGet;
    }

    public int getMESSAGE_ID() {
        return this.MESSAGE_ID;
    }

    public boolean isPut() {
        return this.isPut;
    }

    public void setRequestPut(Map<Long, byte[]> requestPut) {
        this.requestPut = requestPut;
    }

    public void setRequestGet(Collection<Long> requestGet) {
        this.requestGet = requestGet;
    }

    public void setMESSAGE_ID(int MESSAGE_ID) {
        this.MESSAGE_ID = MESSAGE_ID;
    }

    public void setIsPut(boolean isPut) {
        this.isPut = isPut;
    }

    @Override
    public String toString() {
        return "{" + " requestPut='" + getRequestPut() + "'" + ", requestGet='" + getRequestGet() + "'"
                + ", MESSAGE_ID='" + getMESSAGE_ID() + "'" + ", isPut='" + isPut() + "'" + "}";
    }

}
