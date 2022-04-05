package com.madwind.cdnserver.route;


import com.madwind.cdnserver.handler.ProxyHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;

@Configuration
public class ProxyRoutesConfig {
    @Bean
    public RouterFunction<ServerResponse> proxyRoutes(ProxyHandler proxyHandler) {
        return RouterFunctions
                .route(GET("/api/proxy/file"), proxyHandler::getFile);
    }
}
