package com.madwind.cdnserver.route;


import com.madwind.cdnserver.handler.ProxyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

@Configuration
public class ProxyRoutesConfig {
    Logger log = LoggerFactory.getLogger(ProxyRoutesConfig.class);
    @Value("${PROXY_PATH:/}")
    String proxyPath;

    @Bean
    public RouterFunction<ServerResponse> proxyRoutes(ProxyHandler proxyHandler) {
        log.info("proxyPath: {}", proxyPath);
        return RouterFunctions
                .route(GET(proxyPath), proxyHandler::getFile);
    }
}
