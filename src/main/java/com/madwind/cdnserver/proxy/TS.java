package com.madwind.cdnserver.proxy;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ZeroCopyHttpOutputMessage;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.ZonedDateTime;

public class TS implements ProxyResponse {
    private final String urlParam;
    private final MediaType mediaType = MediaType.parseMediaType("video/mp2t");
    private final int maxInMemorySize = 20 * 1024 * 1024;

    private long contentLength;

    public TS(String urlParam) {
        this.urlParam = urlParam;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

    @Override
    public Mono<ServerResponse> handle() {
        Flux<DataBuffer> dataBufferFlux = WebClient.builder()
                                                   .clientConnector(new ReactorClientHttpConnector(
                                                           HttpClient.create().followRedirect(true)
                                                   ))
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
        return ServerResponse.ok()
                             .contentType(getMediaType())
                             .body((p, a) -> {
                                         ZeroCopyHttpOutputMessage resp = (ZeroCopyHttpOutputMessage) p;
                                         HttpHeaders headers = resp.getHeaders();
                                         headers.setLastModified(ZonedDateTime.now());
                                         headers.setCacheControl(ProxyResponse.CACHE_CONTROL);
                                         return resp.writeWith(dataBufferFlux);
                                     }
                             );
    }
}
