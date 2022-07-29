package com.madwind.cdnserver.handler;

import com.madwind.cdnserver.proxy.Common;
import com.madwind.cdnserver.proxy.M3u8;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class ProxyHandler {
    Logger logger = LoggerFactory.getLogger(ProxyHandler.class);
    WebClient.Builder webClientBuilder;

    public ProxyHandler(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public Mono<ServerResponse> getFile(ServerRequest serverRequest) {
        String urlParam = serverRequest.queryParam("url")
                                       .orElseThrow();
        logger.info("proxy: {}", urlParam);
        String fileName = urlParam.substring(urlParam.lastIndexOf('/') + 1);
        String extendName = fileName.substring(fileName.lastIndexOf('.') + 1);
        Mono<ServerResponse> serverResponseMono;
        if ("m3u8".equalsIgnoreCase(extendName)) {
            serverResponseMono = new M3u8(webClientBuilder,urlParam).handle();
        } else {
            serverResponseMono = new Common(webClientBuilder,urlParam).handle();
        }
        return serverResponseMono.onErrorResume(throwable -> {
            logger.warn(throwable.getMessage());
            if (throwable instanceof WebClientResponseException e) {
                return ServerResponse.status(e.getStatusCode()).build();
            }
            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .bodyValue(throwable.getMessage());
        });
    }
}
