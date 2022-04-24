package com.madwind.cdnserver.proxy;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class M3U8 implements ProxyData {
    private final String urlParam;
    private final MediaType mediaType = MediaType.parseMediaType("application/vnd.apple.mpegurl");
    private final int maxInMemorySize = 5 * 1024 * 1024;
    private static final Pattern KEYURI = Pattern.compile("(?<=URI=\").*?(?=\")");

    public M3U8(String urlParam) {
        this.urlParam = urlParam;
    }

    @Override
    public MediaType getMediaType() {
        return mediaType;
    }

    @Override
    public Flux<DataBuffer> getDataBufferFlux() {
        return WebClient.builder()
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
                        .bodyToMono(byte[].class)
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
    }
}
