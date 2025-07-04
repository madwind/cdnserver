package com.madwind.dlproxy.service;


import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.TimeUnit;

@Service
public class TsStartTimeService {

    public Mono<Double> getStartTimeFromFlux(Flux<DataBuffer> dataFlux) {
        return DataBufferUtils.join(dataFlux)
                .map(dataBuffer -> {
                    byte[] data = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(data);
                    DataBufferUtils.release(dataBuffer);
                    return data;
                })
                .flatMap(this::getStartTimeFromFile);
    }

    public Mono<Double> getStartTimeFromFile(byte[] tsBytes) {
        return Mono.fromCallable(() -> {
            Path tempFile = Files.createTempFile("tsfile-", ".ts");
            try {
                Files.write(tempFile, tsBytes, StandardOpenOption.TRUNCATE_EXISTING);

                ProcessBuilder pb = new ProcessBuilder(
                        "ffprobe",
                        "-v", "error",
                        "-show_entries", "format=start_time",
                        "-of", "default=noprint_wrappers=1:nokey=1",
                        tempFile.toAbsolutePath().toString()
                );

                Process process = pb.start();

                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    process.waitFor(2, TimeUnit.SECONDS);
                    String line = reader.readLine();

                    if (line == null || line.isBlank() || line.equals("N/A")) {
                        throw new IllegalStateException("ffprobe failed to get start_time from file: " + line);
                    }

                    return Double.parseDouble(line);
                } finally {
                    process.destroy();
                    Files.deleteIfExists(tempFile);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to probe start_time", e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }
}
