package com.marioplus12.ideareg.controller;

import com.marioplus12.ideareg.util.archiver.UnzipUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author marioplus12
 */
@Controller
@RequestMapping("/idea/code")
@Slf4j
public class RegController {

    @Value("${activation-code-url}")
    private String activationCodeUrl;

    @Value("${base-save-path}")
    private String baseSavePath;

    @Resource
    private WebClient webClient;

    @GetMapping("/refresh")
    public String refresh(Model model) throws IOException {
        FileUtils.deleteDirectory(this.getSaveDir());
        return getCode(model);
    }

    @GetMapping
    public String getCode(Model model) throws IOException {
        if (this.noCacheCode()) {
            this.downloadCode();
        } else {
            log.debug("从缓存读取code");
        }
        Map<String, String> codeMap = this.loadCodeCache();
        model.addAttribute("codeMap", codeMap);
        return "index";
    }

    public void downloadCode() {
        Path path = Paths.get(baseSavePath + "/temp/jihuoma.zip");
        this.downloadFile(activationCodeUrl, path, true);

        try (FileInputStream fis = new FileInputStream(path.toFile())) {
            UnzipUtil.decompression(fis, this.getSavePath());
        } catch (IOException | NullPointerException e) {
            log.error(e.getMessage(), e);
            throw new RuntimeException("从远程获取激活码失败", e);
        } finally {
            this.deleteIfExists(path);
        }
    }

    private Path getSavePath() {
        return Paths.get(baseSavePath + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
    }

    private File getSaveDir() {
        return getSavePath().toFile();
    }

    private HashMap<String, String> loadCodeCache() throws IOException {
        if (noCacheCode()) {
            throw new RuntimeException("获取本地code缓存失败");
        }

        HashMap<String, String> map = new HashMap<>(16);
        File[] files = this.getSaveDir().listFiles((dir, name) -> StringUtils.endsWithIgnoreCase(name, ".txt"));
        for (File child : Objects.requireNonNull(files)) {
            String code = String.join("\n", Files.readAllLines(child.toPath()));
            map.put(child.getName(), code);
        }
        return map;
    }

    private void deleteIfExists(Path path) {
        File file = path.toFile();
        if (file.exists()) {
            boolean delete = file.delete();
        }
    }

    private boolean noCacheCode() {
        File file = this.getSaveDir();
        if (!file.exists()) {
            return true;
        }
        File[] files = file.listFiles();
        return files == null || files.length == 0;
    }

    private void downloadFile(String url, Path path, boolean replace) {
        if (replace) {
            this.deleteIfExists(path);
        }
        log.debug("开始从下载 {}", path.getFileName());
        Flux<DataBuffer> dataBufferFlux = webClient.get()
                .uri(url)
                .accept(MediaType.APPLICATION_OCTET_STREAM)
                .retrieve()
                .bodyToFlux(DataBuffer.class);

        path.toFile().getParentFile().mkdirs();

        DataBufferUtils.write(dataBufferFlux, path, StandardOpenOption.CREATE_NEW)
                .share()
                .block(Duration.ofMinutes(5));
        log.debug("{} 已下载", path.getFileName());
    }
}
