package com.resume.common.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * 图片文件头魔数检测工具，供头像/缩略图上传等场景复用，
 * 防止伪造 Content-Type 或文件名后缀上传任意内容。
 */
public final class ImageMagicUtil {

    private ImageMagicUtil() {
    }

    /**
     * 识别图片真实格式，返回小写扩展名（不含点）：jpg / png / webp / gif；无法识别返回 null。
     */
    public static String detectFormat(byte[] header) {
        if (header == null || header.length < 2) {
            return null;
        }
        if ((header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8) {
            return "jpg";
        }
        if (header.length < 4) {
            return null;
        }
        if ((header[0] & 0xFF) == 0x89 && (header[1] & 0xFF) == 0x50 && (header[2] & 0xFF) == 0x4E
                && (header[3] & 0xFF) == 0x47) {
            return "png";
        }
        if ((header[0] & 0xFF) == 0x52 && (header[1] & 0xFF) == 0x49 && (header[2] & 0xFF) == 0x46
                && (header[3] & 0xFF) == 0x46 && header.length >= 12
                && (header[8] & 0xFF) == 0x57 && (header[9] & 0xFF) == 0x45
                && (header[10] & 0xFF) == 0x42 && (header[11] & 0xFF) == 0x50) {
            return "webp";
        }
        if ((header[0] & 0xFF) == 0x47 && (header[1] & 0xFF) == 0x49 && (header[2] & 0xFF) == 0x46
                && (header[3] & 0xFF) == 0x38) {
            return "gif";
        }
        return null;
    }

    /**
     * 从流的前若干字节识别图片格式（读取 12 字节，不关闭流；若流支持 mark 则重置）。
     * <p>
     * 注意：对于不支持 mark 的流，此方法会消耗前 12 字节且无法回推；调用方若需复用
     * 同一 InputStream 实例，应先使用 {@link #detectFormat(byte[])}（如 {@code file.getBytes()}）
     * 或自行包装为 PushbackInputStream。当前头像/缩略图校验均使用 {@code file.getBytes()}
     * 或每次 {@code file.getInputStream()} 新流，不受此限制。
     * </p>
     */
    public static String detectFormat(InputStream in) throws IOException {
        if (in == null) {
            return null;
        }
        if (in.markSupported()) {
            in.mark(12);
            byte[] header = in.readNBytes(12);
            in.reset();
            return detectFormat(header);
        }
        // 不支持 mark：直接读取（调用方应使用 byte[] 重载以避免消耗）
        return detectFormat(in.readNBytes(12));
    }

    /**
     * 根据识别出的格式返回对应 MIME 类型；无法识别返回 null。
     */
    public static String toContentType(String format) {
        if (format == null) {
            return null;
        }
        return switch (format) {
            case "jpg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            default -> null;
        };
    }
}
