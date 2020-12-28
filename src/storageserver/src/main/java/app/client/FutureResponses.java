package app.client;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import app.data.CMResponsePut;

public class FutureResponses {

    private ConcurrentHashMap<Integer, CompletableFuture<Void>> CFUTURE_PUTS;
    private ConcurrentHashMap<Integer, CompletableFuture<Map<Long, byte[]>>> CFUTURE_GETS;

    private int requestId;

    public FutureResponses() {

        this.CFUTURE_GETS = new ConcurrentHashMap<>();
        this.CFUTURE_PUTS = new ConcurrentHashMap<>();
        this.requestId = 0;
    }

    public void addPendingPutRequest(CompletableFuture<Void> request) {

        this.CFUTURE_PUTS.put(this.requestId++, request);
    }

    public int getId() {
        return this.requestId;
    }

    public void complete(CMResponsePut resPut) {

        this.CFUTURE_PUTS.get(resPut.getMESSAGE_ID()).complete(null);
    }
}
