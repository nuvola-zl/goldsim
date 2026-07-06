package com.gold.file.controller;

import com.gold.common.result.Result;
import com.gold.file.utils.AliOssUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/file")
@RequiredArgsConstructor
public class FileUploadController {

    private final AliOssUtil aliOssUtil;

    /** 允许上传的文件后缀白名单 */
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp",
            "mp4", "mov", "avi", "webm",
            "mp3", "wav", "aac"
    );

    /** 单个文件最大 50MB */
    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024;

    /**
     * 文件类型 Magic Bytes 映射表
     *  key: 文件后缀
     *  value: 该文件类型的特征字节（魔术字节）
     * 原理：每种文件格式在文件开头有固定的字节签名
     * 例如 JPEG 以 FF D8 FF 开头，PNG 以 89 50 4E 47 开头
     * 改后缀无法伪造这些字节
     */
    private static final Map<String, List<byte[]>> MAGIC_BYTES_MAP = new HashMap<>();

    static {
        // JPEG: FF D8 FF
        MAGIC_BYTES_MAP.put("jpg", List.of(
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
        ));
        MAGIC_BYTES_MAP.put("jpeg", List.of(
                new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
        ));

        // PNG: 89 50 4E 47 0D 0A 1A 0A
        MAGIC_BYTES_MAP.put("png", List.of(
                new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}
        ));

        // GIF: 47 49 46 38 (GIF87a / GIF89a)
        MAGIC_BYTES_MAP.put("gif", Arrays.asList(
                new byte[]{0x47, 0x49, 0x46, 0x38, 0x37, 0x61},  // GIF87a
                new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61}   // GIF89a
        ));

        // WebP: 52 49 46 46 ... 57 45 42 50
        MAGIC_BYTES_MAP.put("webp", List.of(
                new byte[]{0x52, 0x49, 0x46, 0x46}  // RIFF 头，后面跟 WEBP
        ));

        // MP4 / MOV / AVI: 都有多种格式头，简化校验
        // MP4: 通常以 ftyp 标识，文件偏移4字节处
        MAGIC_BYTES_MAP.put("mp4", Arrays.asList(
                new byte[]{0x00, 0x00, 0x00},  // 多种box size格式
                new byte[]{0x66, 0x74, 0x79, 0x70}  // ftyp (偏移4)
        ));

        // MP3: ID3 标签或帧同步字
        MAGIC_BYTES_MAP.put("mp3", Arrays.asList(
                new byte[]{(byte) 0xFF, (byte) 0xFB},  // MPEG 音频帧
                new byte[]{(byte) 0xFF, (byte) 0xF3},
                new byte[]{(byte) 0xFF, (byte) 0xF2},
                new byte[]{0x49, 0x44, 0x33}  // ID3 标签
        ));

        // WAV: 52 49 46 46 (RIFF)
        MAGIC_BYTES_MAP.put("wav", List.of(
                new byte[]{0x52, 0x49, 0x46, 0x46}
        ));

        // AAC: ADTS 帧头 FF F1 或 FF F9
        MAGIC_BYTES_MAP.put("aac", Arrays.asList(
                new byte[]{(byte) 0xFF, (byte) 0xF1},
                new byte[]{(byte) 0xFF, (byte) 0xF9}
        ));
    }


    /**
     * 通用文件上传
     */
    @PostMapping("/upload")
    public Result<String> upload(@RequestPart("file") MultipartFile file) {
        return doUpload(file, null);
    }

    /**
     * 指定文件夹上传（供其他服务调用）
     */
    @PostMapping("/upload-to")
    public Result<String> uploadTo(String folder, MultipartFile file) {
        // 【安全加固】folder 参数防路径遍历
        if (folder != null && (folder.contains("..") || folder.contains("/") || folder.contains("\\"))) {
            return Result.error("A00001", "非法文件夹路径");
        }
        return doUpload(file, folder);
    }

    /**
     * 统一上传逻辑
     */
    private Result<String> doUpload(MultipartFile file, String customFolder) {
        // 1. 空文件校验
        if (file == null || file.isEmpty()) {
            return Result.error("A00001", "上传文件不能为空");
        }

        // 2. 文件大小校验
        if (file.getSize() > MAX_FILE_SIZE) {
            return Result.error("A00001", "文件大小不能超过50MB");
        }

        // 3. 提取后缀
        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename).toLowerCase();

        // 4. 白名单校验
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            return Result.error("A00001", "不支持的文件类型: " + extension);
        }

        // 5. 【关键安全加固】Magic Bytes 校验
        // 防止改后缀上传恶意文件（如把 .exe 改成 .jpg）
        if (!validateMagicBytes(file, extension)) {
            log.warn("文件 Magic Bytes 校验失败，可能伪造后缀，文件名: {}", originalFilename);
            return Result.error("A00001", "文件内容与实际类型不符");
        }

        // 6. 按类型分文件夹
        String folder = customFolder != null ? customFolder : getFolderByExtension(extension);
        String objectName = folder + "/" + UUID.randomUUID().toString().replace("-", "") + "." + extension;

        try {
            String url = aliOssUtil.upload(file.getBytes(), objectName);
            return Result.success(url);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error("A00001", "文件上传失败，请重试");
        }
    }

    /**
     * 校验文件 Magic Bytes
     *
     * @param file 上传的文件
     * @param extension 声明的文件后缀
     * @return true-校验通过 false-校验失败
     */
    private boolean validateMagicBytes(MultipartFile file, String extension) {
        List<byte[]> expectedMagicList = MAGIC_BYTES_MAP.get(extension);

        // 如果没有配置该类型的 Magic Bytes，跳过校验（向后兼容）
        if (expectedMagicList == null || expectedMagicList.isEmpty()) {
            return true;
        }

        try (InputStream is = file.getInputStream()) {
            // 读取文件前8个字节（足够覆盖所有常见格式）
            byte[] fileHeader = new byte[8];
            int read = is.read(fileHeader);

            if (read < 3) {
                return false; // 文件太小，不可能是正常图片/视频
            }

            // 遍历该类型的所有可能 Magic Bytes（一种格式可能有多种头）
            for (byte[] expectedMagic : expectedMagicList) {
                if (startsWith(fileHeader, expectedMagic)) {
                    return true;
                }
            }

            // 特殊处理：MP4/MOV 的 ftyp 在偏移4处
            if (("mp4".equals(extension) || "mov".equals(extension) || "avi".equals(extension))
                    && read >= 8) {
                // 检查偏移4-7是否为 ftyp 或 mdat 等
                byte[] ftyp = new byte[]{0x66, 0x74, 0x79, 0x70}; // "ftyp"
                byte[] mdat = new byte[]{0x6D, 0x64, 0x61, 0x74}; // "mdat"
                byte[] moov = new byte[]{0x6D, 0x6F, 0x6F, 0x76}; // "moov"

                byte[] offset4 = new byte[]{fileHeader[4], fileHeader[5], fileHeader[6], fileHeader[7]};

                return Arrays.equals(offset4, ftyp) || Arrays.equals(offset4, mdat) || Arrays.equals(offset4, moov);
            }

            return false;

        } catch (IOException e) {
            log.error("读取文件 Magic Bytes 失败", e);
            return false;
        }
    }

    /**
     * 检查 fileHeader 是否以 magic 开头
     */
    private boolean startsWith(byte[] fileHeader, byte[] magic) {
        if (fileHeader.length < magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (fileHeader[i] != magic[i]) {
                return false;
            }
        }
        return true;
    }

    private String extractExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int lastDot = filename.lastIndexOf(".");
        if (lastDot == -1 || lastDot == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }

    private String getFolderByExtension(String ext) {
        if (Arrays.asList("jpg", "jpeg", "png", "gif", "webp").contains(ext)) {
            return "images";
        }
        if (Arrays.asList("mp4", "mov", "avi", "webm").contains(ext)) {
            return "videos";
        }
        if (Arrays.asList("mp3", "wav", "aac").contains(ext)) {
            return "audios";
        }
        return "files";
    }
}