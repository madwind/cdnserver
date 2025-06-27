package com.madwind.dlproxy;


import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

@Service
public class TsDurationService {

    public Mono<Double> getDurationFromFlux(Flux<DataBuffer> dataFlux) {
        return DataBufferUtils.join(dataFlux)
                .map(dataBuffer -> {
                    byte[] data = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(data);
                    DataBufferUtils.release(dataBuffer);
                    return data;
                })
                .flatMap(this::getDurationFromBytes);
    }

    public Mono<Double> getDurationFromBytes(byte[] tsBytes) {
        return Mono.fromCallable(() -> {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffprobe",
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    "-i", "pipe:0"
            );

            Process process = pb.start();

            try (OutputStream stdin = process.getOutputStream()) {
                stdin.write(tsBytes);
                stdin.flush();
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line = reader.readLine();
                process.waitFor(2, TimeUnit.SECONDS);
                return Double.parseDouble(line);
            } finally {
                process.destroy();
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
