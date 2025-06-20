package com.madwind.downloadproxy.proxy;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ZeroCopyHttpOutputMessage;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public class Common implements ProxyResponse {

    private final int maxInMemorySize = 20 * 1024 * 1024;

    private final WebClient webClient;
    private final HttpHeaders httpHeaders;

    public Common(WebClient webClient, HttpHeaders httpHeaders) {
        this.webClient = webClient;
        this.httpHeaders = httpHeaders;
    }

    @Override
    public Mono<ServerResponse> handle(String urlParam) {
        return webClient.mutate()
                .baseUrl(urlParam)
                .defaultHeaders(httpHeaders -> {
                    httpHeaders.clear();
                    httpHeaders.addAll(this.httpHeaders);
                })
                .codecs(configurer ->
                        configurer.defaultCodecs()
                                .maxInMemorySize(maxInMemorySize)
                )
                .build()
                .get()
                .retrieve()
                .onStatus(HttpStatusCode::isError, ClientResponse::createException)
                .toEntityFlux(DataBuffer.class)
                .flatMap(fluxResponseEntity -> ServerResponse.status(fluxResponseEntity.getStatusCode())
                        .headers(httpHeaders -> {
                            httpHeaders.addAll(fluxResponseEntity.getHeaders());
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
