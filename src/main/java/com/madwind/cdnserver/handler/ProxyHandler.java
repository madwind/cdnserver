package com.madwind.cdnserver.handler;

import com.madwind.cdnserver.proxy.Common;
import com.madwind.cdnserver.proxy.M3u8;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;

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
        SslContext sslContext;
        try {
            sslContext = SslContextBuilder
                    .forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE)
                    .build();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
        HttpClient httpClient = HttpClient.create().followRedirect(true)
                                          .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));

        WebClient.Builder webClientBuilder = WebClient.builder()
                                                      .clientConnector(new ReactorClientHttpConnector(httpClient));

        String fileName = urlParam.substring(urlParam.lastIndexOf('/') + 1);
        String extendName = fileName.substring(fileName.lastIndexOf('.') + 1);
        Mono<ServerResponse> serverResponseMono;
        if ("m3u8".equalsIgnoreCase(extendName)) {
            serverResponseMono = new M3u8(webClientBuilder, urlParam).handle(webClientBuilder);
        } else {
            serverResponseMono = new Common(urlParam).handle(webClientBuilder);
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
