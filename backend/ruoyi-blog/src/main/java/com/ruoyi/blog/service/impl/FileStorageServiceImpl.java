package com.ruoyi.blog.service.impl;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.ruoyi.blog.config.BlogFileProperties;
import com.ruoyi.blog.service.FileStorageService;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService
{

    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "gif", "webp");

    private final BlogFileProperties properties;

    @Override
    public String storeImage(MultipartFile file)
    {
        if (file == null || file.isEmpty())
        {
            throw new ServiceException("上传文件不能为空", HttpStatus.BAD_REQUEST);
        }
        String ext = StringUtils.getFilenameExtension(file.getOriginalFilename());
        if (ext == null || !ALLOWED_EXT.contains(ext.toLowerCase()))
        {
            throw new ServiceException("仅支持 jpg/png/gif/webp 图片", HttpStatus.BAD_REQUEST);
        }
        byte[] header = readHeader(file, 12);
        if (!isValidImageHeader(header, ext.toLowerCase()))
        {
            throw new ServiceException("文件内容与扩展名不匹配", HttpStatus.BAD_REQUEST);
        }

        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String filename = UUID.randomUUID().toString().replace("-", "") + "." + ext.toLowerCase();
        Path dir = Paths.get(properties.getUploadDir(), dateDir);
        try
        {
            Files.createDirectories(dir);
            Path target = dir.resolve(filename);
            file.transferTo(target.toFile());
            String prefix = properties.getUrlPrefix();
            if (!prefix.startsWith("/"))
            {
                prefix = "/" + prefix;
            }
            return prefix + "/" + dateDir + "/" + filename;
        }
        catch (IOException e)
        {
            throw new ServiceException("文件保存失败", HttpStatus.ERROR);
        }
    }

    private byte[] readHeader(MultipartFile file, int len)
    {
        try (InputStream in = file.getInputStream())
        {
            byte[] buf = new byte[len];
            int read = in.read(buf);
            if (read <= 0)
            {
                return new byte[0];
            }
            if (read < len)
            {
                byte[] actual = new byte[read];
                System.arraycopy(buf, 0, actual, 0, read);
                return actual;
            }
            return buf;
        }
        catch (IOException e)
        {
            throw new ServiceException("无法读取上传文件", HttpStatus.BAD_REQUEST);
        }
    }

    private boolean isValidImageHeader(byte[] header, String ext)
    {
        if (header.length < 3)
        {
            return false;
        }
        return switch (ext)
        {
            case "jpg", "jpeg" -> (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8;
            case "png" ->
                header.length >= 8 && header[0] == (byte) 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47;
            case "gif" -> header.length >= 6 && header[0] == 'G' && header[1] == 'I' && header[2] == 'F';
            case "webp" -> header.length >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
            default -> false;
        };
    }
}
