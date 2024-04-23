package com.madwind.cdnserver.proxy;

import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

public interface ProxyResponse {
    Mono<ServerResponse> handle(String urlParam);
}
