package com.madwind.cdnserver.proxy;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

public class KEY implements ProxyData {
    private final String urlParam;
    private final MediaType mediaType = MediaType.parseMediaType("application/octet-stream");
    private final int maxInMemorySize = 1024 * 1024;

    public KEY(String urlParam) {
        this.urlParam = urlParam;
    }

    @Override
    public MediaType getMediaType() {
        return mediaType;
    }

    @Override
    public Flux<DataBuffer> getDataBufferFlux() {
        return WebClient.builder()
                        .baseUrl(urlParam)
                        .exchangeStrategies(ExchangeStrategies.builder()
                                                              .codecs(configurer ->
                                                                      configurer.defaultCodecs()
                                                                                .maxInMemorySize(maxInMemorySize)
                                                              )
                                                              .build())
                        .build()
                        .get()
                        .retrieve().bodyToFlux(DataBuffer.class);
    }
}
