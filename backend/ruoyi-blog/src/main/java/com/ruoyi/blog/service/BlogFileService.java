package com.ruoyi.blog.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.blog.domain.BlogFile;
import org.springframework.web.multipart.MultipartFile;

public interface BlogFileService
{

    /** 上传文件并保存记录，返回文件记录。 */
    BlogFile upload(MultipartFile file);

    /** 分页查询当前用户的文件列表。 */
    Page<BlogFile> page(int pageNum, int pageSize, String keyword);

    /** 删除文件记录（OSS 对象需另行清理）。 */
    void delete(Long id);
}