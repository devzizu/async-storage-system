package app.client;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import app.data.ClientMessage;

public class FutureResponses {

    private ConcurrentHashMap<Integer, CompletableFuture<Void>> CFUTURE_PUTS;
    private ConcurrentHashMap<Integer, CompletableFuture<Map<Long, byte[]>>> CFUTURE_GETS;

    private int requestId;

    public FutureResponses() {

        this.CFUTURE_GETS = new ConcurrentHashMap<>();
        this.CFUTURE_PUTS = new ConcurrentHashMap<>();
        this.requestId = 0;
    }

    public void addPendingRequest(String type, CompletableFuture<?> request) {

        switch (type) {
            case "PUT":
                this.CFUTURE_PUTS.put(this.requestId++, (CompletableFuture<Void>) request);
                break;
            case "GET":
                this.CFUTURE_GETS.put(this.requestId++, (CompletableFuture<Map<Long, byte[]>>) request);
                break;
        }
    }

    public int getId() {
        return this.requestId;
    }

    public void complete(ClientMessage cMessage) {

        int id = cMessage.getMESSAGE_ID();

        if (cMessage.isPut())
            this.CFUTURE_PUTS.get(id).complete(null);
        else
            this.CFUTURE_GETS.get(id).complete(cMessage.getRequestPut());
    }
}
