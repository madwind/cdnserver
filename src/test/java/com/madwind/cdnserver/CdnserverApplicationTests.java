package com.madwind.cdnserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SpringBootTest
class CdnserverApplicationTests {

    @Test
    void contextLoads() throws IOException {
        URL url = new URL("https://www.123.com/1/2/3/4");
        String a = "#EXT-X-KEY:METHOD=AES-128,URI=\"20220222/3JKsfPN1/1000kb/hls/key.key\"";
        Pattern compile = Pattern.compile("(?<=URI=\").*?(?=\")");
        Matcher matcher = compile.matcher(a);
        if (matcher.find()) {
            String group = matcher.group();
            String replace = a.replace(group, new URL(url, group).toString());
            System.out.println(replace);
        }


    }


}
