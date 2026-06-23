package com.ruoyi.blog.service.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.blog.domain.BlogFile;
import com.ruoyi.blog.mapper.BlogFileMapper;
import com.ruoyi.blog.service.BlogFileService;
import com.ruoyi.blog.service.FileStorageService;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BlogFileServiceImpl implements BlogFileService
{

    private final FileStorageService fileStorageService;
    private final BlogFileMapper fileMapper;

    @Override
    public BlogFile upload(MultipartFile file)
    {
        String url = fileStorageService.storeFile(file);

        BlogFile record = new BlogFile();
        record.setFileName(file.getOriginalFilename());
        record.setFileUrl(url);
        record.setFileType(file.getContentType());
        record.setFileSize(file.getSize());
        record.setUserId(SecurityUtils.getUserId());
        record.setCreateTime(LocalDateTime.now());
        fileMapper.insert(record);
        return record;
    }

    @Override
    public Page<BlogFile> page(int pageNum, int pageSize, String keyword)
    {
        Long userId = SecurityUtils.getUserId();
        LambdaQueryWrapper<BlogFile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BlogFile::getUserId, userId);
        if (StringUtils.hasText(keyword))
        {
            wrapper.like(BlogFile::getFileName, keyword);
        }
        wrapper.orderByDesc(BlogFile::getCreateTime);
        return fileMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public void delete(Long id)
    {
        BlogFile file = fileMapper.selectById(id);
        if (file == null)
        {
            throw new ServiceException("文件不存在", HttpStatus.NOT_FOUND);
        }
        if (!file.getUserId().equals(SecurityUtils.getUserId()))
        {
            throw new ServiceException("无权限删除该文件", HttpStatus.FORBIDDEN);
        }
        fileMapper.deleteById(id);
    }
}