package com.madwind.cdnserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;

@SpringBootTest
class CdnserverApplicationTests {

    @Test
    void contextLoads() throws IOException {
        Mono.just(Flux.empty()).map(a -> {
            return a;
        }).block();


    }


}
