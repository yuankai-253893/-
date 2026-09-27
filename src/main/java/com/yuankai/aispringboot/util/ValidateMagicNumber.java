package com.yuankai.aispringboot.util;

import com.yuankai.aispringboot.common.ResultCode;
import com.yuankai.aispringboot.exception.BusinessException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ValidateMagicNumber {
    public static void validateMagicNumber(MultipartFile file, String extLower) throws IOException {
        if ("txt".equals(extLower)) {
            return;
        }

        byte[] header = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.read(header);
        }
        if (read <= 0) {
            throw new BusinessException(ResultCode.FILE_CONTENT_INVALID.getMsg());
        }

        boolean ok;
        switch (extLower) {
            case "jpg":
            case "jpeg":
                // JPEG: FF D8 FF
                ok = startsWith(header, 0, new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF});
                break;
            case "png":
                // PNG: 89 50 4E 47 0D 0A 1A 0A
                ok = startsWith(header, 0, new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
                break;
            case "gif":
                // GIF: "GIF87a" 或 "GIF89a"
                ok = startsWith(header, 0, "GIF87a".getBytes(StandardCharsets.US_ASCII))
                        || startsWith(header, 0, "GIF89a".getBytes(StandardCharsets.US_ASCII));
                break;
            case "bmp":
                // BMP: "BM"
                ok = startsWith(header, 0, new byte[]{0x42, 0x4D});
                break;
            case "webp":
                // WEBP: "RIFF"...."WEBP"（RIFF 容器，第 8 字节起是 "WEBP"）
                ok = startsWith(header, 0, "RIFF".getBytes(StandardCharsets.US_ASCII))
                        && startsWith(header, 8, "WEBP".getBytes(StandardCharsets.US_ASCII));
                break;
            case "pdf":
                // PDF: "%PDF"
                ok = startsWith(header, 0, "%PDF".getBytes(StandardCharsets.US_ASCII));
                break;
            case "doc":
                // doc 是 OLE2 复合文档: D0 CF 11 E0 A1 B1 1A E1
                ok = startsWith(header, 0, new byte[]{(byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0, (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1});
                break;
            case "docx":
                // docx 是 ZIP 容器: "PK\x03\x04"
                ok = startsWith(header, 0, new byte[]{0x50, 0x4B, 0x03, 0x04});
                break;
            default:
                ok = false;
        }

        if (!ok) {
            throw new BusinessException(ResultCode.FILE_CONTENT_INVALID.getMsg() + ": 文件内容与扩展名不匹配");
        }
    }

    private static boolean startsWith(byte[] data, int offset, byte[] magic) {
        if (data.length < offset + magic.length) {
            return false;
        }
        for (int i = 0; i < magic.length; i++) {
            if (data[offset + i] != magic[i]) {
                return false;
            }
        }
        return true;
    }

}
