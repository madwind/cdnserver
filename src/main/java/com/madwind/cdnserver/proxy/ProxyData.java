package com.madwind.cdnserver.proxy;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;

public interface ProxyData {
    MediaType getMediaType();

    Flux<DataBuffer> getDataBufferFlux();
}
