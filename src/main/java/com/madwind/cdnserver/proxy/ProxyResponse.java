package com.madwind.cdnserver.proxy;

import org.springframework.http.CacheControl;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.Duration;

public interface ProxyResponse {
    CacheControl CACHE_CONTROL = CacheControl.maxAge(Duration.ofDays(365));

    Mono<ServerResponse> handle(String urlParam);
}
