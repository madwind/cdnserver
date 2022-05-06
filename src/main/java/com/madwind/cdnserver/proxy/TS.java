package com.madwind.cdnserver.proxy;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.MediaType;
import org.springframework.http.ZeroCopyHttpOutputMessage;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.ZonedDateTime;
import java.util.Objects;

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
        return WebClient.builder()
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
                        .retrieve()
                        .toEntityFlux(DataBuffer.class)
                        .flatMap(fluxResponseEntity -> ServerResponse.ok()
                                                                     .contentType(getMediaType())
                                                                     .headers(httpHeaders -> {
                                                                         httpHeaders.setLastModified(ZonedDateTime.now());
                                                                         httpHeaders.setCacheControl(ProxyResponse.CACHE_CONTROL);
                                                                         httpHeaders.setContentType(fluxResponseEntity.getHeaders()
                                                                                                                      .getContentType());
                                                                         httpHeaders.setContentLength(fluxResponseEntity.getHeaders()
                                                                                                                        .getContentLength());
                                                                     })
                                                                     .body((p, a) -> {
                                                                                 ZeroCopyHttpOutputMessage resp = (ZeroCopyHttpOutputMessage) p;
                                                                                 return resp.writeWith(Objects.requireNonNull(fluxResponseEntity.getBody()));
                                                                             }
                                                                     )
                        );
    }
}
