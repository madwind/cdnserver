package com.madwind.dlproxy.handler;

import com.madwind.dlproxy.service.TsStartTimeService;
import com.madwind.dlproxy.proxy.Common;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class ProxyHandler {
    Logger logger = LoggerFactory.getLogger(ProxyHandler.class);
    WebClient webClient;
    final List<String> IGNORE_HEADERS = Arrays.asList("host", "origin", "referer", "cdn-loop", "cf-", "x-");
    final TsStartTimeService tsStartTimeService;

    public ProxyHandler(WebClient.Builder webClientBuilder, TsStartTimeService tsStartTimeService) throws SSLException {
        this.tsStartTimeService = tsStartTimeService;
        SslContext sslContext = SslContextBuilder
                .forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        HttpClient httpClient = HttpClient.create()
                .followRedirect(true)
                .responseTimeout(Duration.ofSeconds(60))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(10))
                        .addHandlerLast(new WriteTimeoutHandler(10)))
                .secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));
        this.webClient = webClientBuilder.clientConnector(new ReactorClientHttpConnector(httpClient)).build();
    }

    public Mono<ServerResponse> getFile(ServerRequest serverRequest) {
        String urlParam = serverRequest.queryParam("url")
                .orElseThrow(() -> new IllegalArgumentException("Missing url"));

        boolean lengthOnly = serverRequest.queryParam("startTime")
                .map("true"::equalsIgnoreCase)
                .orElse(false);

        Mono<ServerResponse> serverResponseMono;
        HttpHeaders httpHeaders = buildRequestHeader(serverRequest, urlParam);
        Common common = new Common(webClient, httpHeaders);
        if (lengthOnly) {
            return common.getDataBuffer(urlParam).flatMap(fluxResponseEntity -> {
                Flux<DataBuffer> body = fluxResponseEntity.getBody();
                if (body == null) {
                    return ServerResponse.status(502).bodyValue(urlParam + " Empty TS stream");
                }

                return tsStartTimeService.getStartTimeFromFlux(body)
                        .flatMap(duration -> {
                            Map<String, Object> data = Map.of("url", urlParam, "startTime", duration);
                            logger.info(data.toString());
                            return ServerResponse.ok().bodyValue(data);
                        });
            });
        }
        serverResponseMono = common.handle(urlParam);

        return serverResponseMono.onErrorResume(throwable -> {
            logger.warn(throwable.getMessage());
            if (throwable instanceof WebClientResponseException e) {
                return ServerResponse.status(e.getStatusCode()).build();
            }
            return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .bodyValue(throwable.getMessage());
        }).doOnNext(serverResponse -> logger.info("proxy: {}, size: {}", urlParam, serverResponse.headers()
                .get(HttpHeaders.CONTENT_LENGTH)));
    }

    private HttpHeaders buildRequestHeader(ServerRequest serverRequest, String urlParam) {
        try {
            String host = new URI(urlParam).getHost();
            HttpHeaders httpHeaders = new HttpHeaders();
            HttpHeaders requestHeaders = serverRequest.headers().asHttpHeaders();
            requestHeaders.forEach((s, strings) -> {
                String s1 = s.toLowerCase();
                if (IGNORE_HEADERS.stream().noneMatch(s1::startsWith)) {
                    httpHeaders.addAll(s, strings);
                }
            });
            httpHeaders.add("Host", host);
            return httpHeaders;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
