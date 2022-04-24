package com.madwind.cdnserver.proxy;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;

import java.util.Random;

public class TEST implements ProxyData {
    private final MediaType mediaType = MediaType.parseMediaType("application/octet-stream");

    @Override
    public MediaType getMediaType() {
        return mediaType;
    }

    @Override
    public Flux<DataBuffer> getDataBufferFlux() {
        byte[] bytes = new byte[500 * 1024];
        Random r = new Random();
        r.nextBytes(bytes);
        return Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes));
    }
}
