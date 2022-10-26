package com.madwind.cdnserver.proxy;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ZeroCopyHttpOutputMessage;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.regex.Pattern;

public class M3u8 implements ProxyResponse {
    private final MediaType mediaType = MediaType.parseMediaType("application/vnd.apple.mpegurl");
    private final int maxInMemorySize = 5 * 1024 * 1024;
    private static final Pattern KEYURI = Pattern.compile("(?<=URI=\").*?(?=\")");
    private final WebClient webClient;
    private final HttpHeaders httpHeaders;

    public M3u8(WebClient webClient, HttpHeaders httpHeaders) {
        this.webClient = webClient;
        this.httpHeaders = httpHeaders;
    }

    public MediaType getMediaType() {
        return mediaType;
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
                        .onStatus(HttpStatus::isError, ClientResponse::createException)
                        .toEntityFlux(DataBuffer.class)
                        .flatMap(fluxResponseEntity -> ServerResponse.status(fluxResponseEntity.getStatusCode())
                                                                     .headers(httpHeaders -> {
                                                                         httpHeaders.addAll(fluxResponseEntity.getHeaders());
                                                                     }).body((p, a) -> {
                                            ZeroCopyHttpOutputMessage resp = (ZeroCopyHttpOutputMessage) p;
                                            if (fluxResponseEntity.getBody() != null) {
                                                fluxResponseEntity.getBody().map(dataBuffer -> {
                                                            byte[] bytes = new byte[dataBuffer.readableByteCount()];
                                                            dataBuffer.read(bytes);
                                                            return new String(bytes);
                                                        }
                                                ).collectList().map(strings -> String.join("", strings)).subscribe(s -> System.out.println(s));
                                                return resp.writeWith(fluxResponseEntity.getBody());
                                            }
                                            return resp.writeWith(Flux.empty());
                                        }
                                )
                        );
//                                                   .bodyToMono(byte[].class)
//                                                   .flatMapMany(bytes -> {
//                                                       try {
//                                                           URL url = new URL(urlParam);
//                                                           String s = new String(bytes);
//
//                                                           String[] originM3u8List = s.split("\n");
//                                                           StringBuilder m3u8ListStingBuilder = new StringBuilder();
//                                                           for (String line : originM3u8List) {
//                                                               if (line.toLowerCase().startsWith("#ext-x-key")) {
//                                                                   Matcher matcher = KEYURI.matcher(line);
//                                                                   if (matcher.find()) {
//                                                                       String uri = matcher.group();
//                                                                       line = line.replace(uri, new URL(url, uri).toString());
//                                                                   }
//                                                               } else if (!line.startsWith("#")) {
//                                                                   line = new URL(url, line).toString();
//                                                               }
//                                                               m3u8ListStingBuilder.append(line)
//                                                                                   .append("\n");
//                                                           }
//                                                           return Flux.just(DefaultDataBufferFactory.sharedInstance.wrap(m3u8ListStingBuilder.toString()
//                                                                                                                                             .getBytes()));
//                                                       } catch (IOException e) {
//                                                           e.printStackTrace();
//                                                       }
//                                                       return Flux.empty();
//                                                   });
//        return ServerResponse.ok()
//                             .contentType(getMediaType())
//                             .body((p, a) -> {
//                                         ZeroCopyHttpOutputMessage resp = (ZeroCopyHttpOutputMessage) p;
//                                         HttpHeaders headers = resp.getHeaders();
//                                         headers.setLastModified(ZonedDateTime.now());
//                                         return resp.writeWith(dataBufferFlux);
//                                     }
//                             );
    }
}
