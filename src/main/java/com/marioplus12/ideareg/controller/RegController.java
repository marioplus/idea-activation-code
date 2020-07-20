package com.marioplus12.ideareg.controller;

import com.marioplus12.ideareg.util.archiver.UnzipUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;

/**
 * @author marioplus12
 */
@RestController
@RequestMapping
@Slf4j
public class RegController {

    @Value("${activation-code-url}")
    private String activationCodeUrl;

    @Value("${base-save-path}")
    private String baseSavePath;

    private final ThreadLocal<WebClient> webClientThreadLocal = ThreadLocal.withInitial(() -> WebClient.builder().build());

    @GetMapping()
    public HashMap<String, String> getActivationCode() throws IOException {
        Path savePath = this.getSavePath();
        if (!savePath.toFile().exists()) {
            log.info("从远程获取激活码：{}", LocalDateTime.now().toString());
            this.downloadActivationCode();
        }
        return this.getActivationCodeFromLocal(savePath);
    }

    public void downloadActivationCode() {
        Mono<ClientResponse> mono = webClientThreadLocal.get()
                .get()
                .uri(activationCodeUrl)
                .exchange();
        ClientResponse response = mono.block(Duration.of(5, ChronoUnit.MINUTES));
        try {
            Resource resource = response.bodyToMono(Resource.class).block();
            UnzipUtil.decompression(resource.getInputStream(), this.getSavePath());
        } catch (IOException | NullPointerException e) {
            e.printStackTrace();
        }
    }

    private Path getSavePath() {
        return Paths.get(baseSavePath + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
    }

    private HashMap<String, String> getActivationCodeFromLocal(Path path) throws IOException {
        if (path == null) {
            return null;
        }

        HashMap<String, String> map = new HashMap<>();
        File file = path.toFile();
        if (!file.isDirectory() && StringUtils.endsWithIgnoreCase(file.getName(), ".txt")) {
            String code = String.join("\n", Files.readAllLines(file.toPath()));
            map.put(file.getName(), code);
        }
        if (file.isDirectory() && file.listFiles().length > 0) {
            for (File child : file.listFiles()) {
                map.putAll(this.getActivationCodeFromLocal(child.toPath()));
            }
        }
        return map;
    }
}
