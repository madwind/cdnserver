package com.madwind.cdnserver.proxy;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ZeroCopyHttpOutputMessage;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;
import java.util.Random;

public class TEST implements ProxyResponse {
    private final MediaType mediaType = MediaType.parseMediaType("application/octet-stream");

    public MediaType getMediaType() {
        return mediaType;
    }

    @Override
    public Mono<ServerResponse> handle() {
        byte[] bytes = new byte[500 * 1024];
        Random r = new Random();
        r.nextBytes(bytes);
        Flux<DataBuffer> dataBufferFlux = Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes));
        return ServerResponse.ok()
                             .contentType(getMediaType())
                             .body((p, a) -> {
                                         ZeroCopyHttpOutputMessage resp = (ZeroCopyHttpOutputMessage) p;
                                         HttpHeaders headers = resp.getHeaders();
                                         headers.setLastModified(ZonedDateTime.now());
                                         return resp.writeWith(dataBufferFlux);
                                     }
                             );
    }
}
