package com.madwind.cdnserver.handler;

import com.madwind.cdnserver.proxy.ProxyData;
import com.madwind.cdnserver.proxy.TS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ZeroCopyHttpOutputMessage;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
public class ProxyHandler {
    Logger logger = LoggerFactory.getLogger(ProxyHandler.class);

    public Mono<ServerResponse> getFile(ServerRequest serverRequest) {
        String urlParam = serverRequest.queryParam("url")
                                       .orElseThrow();
        logger.info("proxy: {}", urlParam);
        String fileName = urlParam.substring(urlParam.lastIndexOf('/') + 1);
        String extendName = fileName.substring(fileName.lastIndexOf('.') + 1);

        ProxyData proxyData;
        try {
            Class<?> proxyDataClass = Class.forName("com.madwind.cdnserver.proxy." + extendName.toUpperCase());
            proxyData = (ProxyData) proxyDataClass
                    .getConstructor(String.class)
                    .newInstance(urlParam);
        } catch (ReflectiveOperationException e) {
            proxyData = new TS(urlParam);
        }

        ProxyData finalProxyData = proxyData;
        return ServerResponse.ok()
                             .contentType(finalProxyData.getMediaType())
                             .body((p, a) -> {
                                         ZeroCopyHttpOutputMessage resp = (ZeroCopyHttpOutputMessage) p;
                                         return resp.writeWith(finalProxyData.getDataBufferFlux());
                                     }
                             );

    }
}
