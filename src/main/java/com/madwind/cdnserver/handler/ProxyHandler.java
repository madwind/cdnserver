package com.madwind.cdnserver.handler;

import com.madwind.cdnserver.proxy.Common;
import io.netty.channel.ChannelOption;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.resolver.dns.DnsAddressResolverGroup;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import io.netty.resolver.dns.SequentialDnsServerAddressStreamProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
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
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Component
public class ProxyHandler {
    Logger logger = LoggerFactory.getLogger(ProxyHandler.class);
    WebClient webClient;
    final List<String> IGNORE_HEADERS = Arrays.asList("host", "origin", "referer", "cdn-loop", "cf-", "x-");

    public ProxyHandler(WebClient.Builder webClientBuilder) throws SSLException {
        SslContext sslContext = SslContextBuilder
                .forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        HttpClient httpClient = HttpClient.create()
                .followRedirect(true)
                .resolver(
                        new DnsAddressResolverGroup(
                                new DnsNameResolverBuilder()
                                        .channelType(NioDatagramChannel.class)
                                        .nameServerProvider(
                                                new SequentialDnsServerAddressStreamProvider(
                                                        new InetSocketAddress("1.1.1.1", 53),
                                                        new InetSocketAddress("8.8.8.8", 53)))))
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
                .orElseThrow();
        Mono<ServerResponse> serverResponseMono;
        HttpHeaders httpHeaders = buildRequestHeader(serverRequest, urlParam);
        serverResponseMono = new Common(webClient, httpHeaders).handle(urlParam);
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
            String host = new URL(urlParam).getHost();
            HttpHeaders httpHeaders = new HttpHeaders();
            HttpHeaders requestHeaders = serverRequest.headers().asHttpHeaders();
            requestHeaders.forEach((s, strings) -> {
                String s1 = s.toLowerCase();
                if (IGNORE_HEADERS.stream().noneMatch(s1::startsWith)) {
                    httpHeaders.addAll(s, strings);
                }
            });
            httpHeaders.add("Host", host);
            System.out.println(httpHeaders.toSingleValueMap());
            return httpHeaders;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
