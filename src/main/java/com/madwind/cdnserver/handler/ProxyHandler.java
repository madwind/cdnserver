package com.madwind.cdnserver.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ZeroCopyHttpOutputMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URL;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ProxyHandler {
    private static final MediaType M3U8 = MediaType.parseMediaType("application/vnd.apple.mpegurl");
    private static final MediaType JPG = MediaType.parseMediaType("image/jpeg");
    private static final MediaType KEY = MediaType.parseMediaType("application/octet-stream");
    private static final MediaType TS = MediaType.parseMediaType("video/mp2t");
    private static final Pattern KEYURI = Pattern.compile("(?<=URI=\").*?(?=\")");
    Logger logger = LoggerFactory.getLogger(ProxyHandler.class);

    public Mono<ServerResponse> getFile(ServerRequest serverRequest) {
        String urlParam = serverRequest.queryParam("url")
                                       .orElseThrow();
        String testParam = serverRequest.queryParam("test")
                                        .orElse(null);
        long start = System.currentTimeMillis();
        logger.info("proxy: {}", urlParam);
        String fileName = urlParam.substring(urlParam.lastIndexOf('/') + 1);

        MediaType mediaType;
        WebClient.ResponseSpec retrieve = WebClient.builder()
                                                   .baseUrl(urlParam)
                                                   .exchangeStrategies(ExchangeStrategies.builder()
                                                                                         .codecs(configurer ->
                                                                                                 configurer.defaultCodecs()
                                                                                                           .maxInMemorySize(20 * 1024 * 1024)
                                                                                         )
                                                                                         .build())
                                                   .build()
                                                   .get()
                                                   .retrieve();
        Flux<DataBuffer> dataBufferFlux;
        if (fileName.toLowerCase().endsWith("m3u8")) {
            mediaType = M3U8;

            dataBufferFlux = retrieve.bodyToMono(byte[].class)
                                     .flatMapMany(bytes -> {
                                         try {
                                             URL url = new URL(urlParam);
                                             String s = new String(bytes);
                                             String[] originM3u8List = s.split("\n");
                                             StringBuilder m3u8ListStingBuilder = new StringBuilder();
                                             for (String line : originM3u8List) {
                                                 if (line.toLowerCase().startsWith("#ext-x-key")) {
                                                     Matcher matcher = KEYURI.matcher(line);
                                                     if (matcher.find()) {
                                                         String uri = matcher.group();
                                                         line = line.replace(uri, new URL(url, uri).toString());
                                                     }
                                                 } else if (!line.startsWith("#")) {
                                                     line = new URL(url, line).toString();
                                                 }
                                                 m3u8ListStingBuilder.append(line)
                                                                     .append("\n");
                                             }
                                             return Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(m3u8ListStingBuilder.toString()
                                                                                                                               .getBytes()));
                                         } catch (IOException e) {
                                             e.printStackTrace();
                                         }
                                         return Flux.empty();
                                     });
        } else if (fileName.toLowerCase().endsWith("jpg")) {
            mediaType = JPG;
            dataBufferFlux = retrieve.bodyToFlux(DataBuffer.class);
        } else if (fileName.toLowerCase().endsWith("key")) {
            mediaType = KEY;
            dataBufferFlux = retrieve.bodyToFlux(DataBuffer.class);
        } else {
            mediaType = TS;

            if (testParam != null) {
                byte[] bytes = new byte[500];
                Random r = new Random();
                r.nextBytes(bytes);
                dataBufferFlux = Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes));
            } else {
                dataBufferFlux = retrieve.bodyToMono(byte[].class).flatMapMany(bytes -> {
                    if (bytes.length > 10 * 1024) {
                        return Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(bytes));
                    }
                    return Flux.empty();
                });
            }
        }

        return dataBufferFlux.hasElements()
                             .flatMap(t -> {
                                 if (t) {
                                     return ServerResponse.ok()
                                                          .contentType(mediaType)
                                                          .body((p, a) -> {
                                                                      ZeroCopyHttpOutputMessage resp = (ZeroCopyHttpOutputMessage) p;
                                                                      p.getHeaders()
                                                                       .add(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "*");
                                                                      p.getHeaders()
                                                                       .add(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Cache-Time");
                                                                      p.getHeaders()
                                                                       .add("Cache-Time", String.valueOf(System.currentTimeMillis() - start));
                                                                      return resp.writeWith(dataBufferFlux);
                                                                  }
                                                          );
                                 }
                                 return ServerResponse.badRequest().build();
                             });
    }
}
