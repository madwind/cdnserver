package com.madwind.cdnserver.proxy;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ZeroCopyHttpOutputMessage;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZonedDateTime;

public class Common implements ProxyResponse {

    private final WebClient.Builder webClientBuilder;
    private final String urlParam;
    private final int maxInMemorySize = 20 * 1024 * 1024;

    public Common(WebClient.Builder webClientBuilder, String urlParam) {
        this.webClientBuilder = webClientBuilder;
        this.urlParam = urlParam;
    }

    @Override
    public Mono<ServerResponse> handle() {
        return webClientBuilder
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
                .onStatus(HttpStatus::isError, ClientResponse::createException)
                .toEntityFlux(DataBuffer.class)
                .flatMap(fluxResponseEntity -> ServerResponse.status(fluxResponseEntity.getStatusCode())
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
                                                                         if (fluxResponseEntity.getBody() != null) {

                                                                             return resp.writeWith(fluxResponseEntity.getBody());
                                                                         }
                                                                         return resp.writeWith(Flux.empty());
                                                                     }
                                                             )
                );
    }
}
