package com.marioplus12.ideareg.util.archiver;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.utils.Lists;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 解压zip类
 *
 * @author marioplus12
 */
@Slf4j
public class UnzipUtil {

    /**
     * 解压
     *
     * @param is   压缩文件流
     * @param path 解压路径
     */
    public static List<Path> decompression(InputStream is, Path path) throws IOException {
        validSavePath(path);

        if (is == null || is.available() <= 0) {
            throw new DecompressionException("待解压的流为空");
        }

        return doDecompression(is, path);
    }

    /**
     * 校验解压路径
     *
     * @param path 解压路径
     * @throws DecompressionException 解压异常
     */
    private static void validSavePath(Path path) throws DecompressionException {
        if (path == null) {
            throw new DecompressionException("创建存放存放解压文件存放路径");
        }
        File saveDir = path.toFile();
        if (!saveDir.exists()) {
            boolean mkdirs = saveDir.mkdirs();
            if (!mkdirs) {
                throw new DecompressionException("创建存放解压文件的文件夹失败，路径：" + path);
            }
        }
        if (!saveDir.isDirectory()) {
            throw new DecompressionException("指定存放解压文件的文件夹为文件，路径：" + path);
        }
    }

    /**
     * 解压
     *
     * @param is   压缩文件流
     * @param path 解压路径
     */
    private static List<Path> doDecompression(InputStream is, Path path) throws DecompressionException {
        log.debug("开始解压...");
        ArchiveInputStream ais = new ZipArchiveInputStream(is);
        ArrayList<Path> savedPathList = Lists.newArrayList();
        try {
            ArchiveEntry entry;
            while (Objects.nonNull(entry = ais.getNextEntry())) {
                if (!ais.canReadEntryData(entry)) {
                    continue;
                }
                log.debug("解压文件：{}", entry.getName());
                String name = String.format("%s/%s", path.toString(), entry.getName());
                Path currPath = Paths.get(name);
                Files.copy(ais, currPath, StandardCopyOption.REPLACE_EXISTING);
                log.debug("解压完毕：{}", entry.getName());
                savedPathList.add(currPath);
            }
        } catch (IOException e) {
            throw new DecompressionException("解压失败", e);
        }
        log.debug("解压完毕");
        return savedPathList;
    }

    public static void main(String[] args) throws IOException {
        FileInputStream zipStream = new FileInputStream("C:\\Users\\mario\\Desktop\\jihuoma.zip");
        Path path = Paths.get("C:/Users/mario/Desktop/jihuoma");

        decompression(zipStream, path);
    }
}
