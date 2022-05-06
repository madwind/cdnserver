package com.madwind.cdnserver.handler;

import com.madwind.cdnserver.proxy.ProxyResponse;
import com.madwind.cdnserver.proxy.TS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        try {
            Class<?> proxyResponseClass = Class.forName("com.madwind.cdnserver.proxy." + extendName.toUpperCase());
            ProxyResponse proxyResponse = (ProxyResponse) proxyResponseClass
                    .getConstructor(String.class)
                    .newInstance(urlParam);
            return proxyResponse.handle();
        } catch (ReflectiveOperationException e) {
            return new TS(urlParam).handle();
        }
    }
}
