package com.ruoyi.blog.service;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.blog.dto.BillPageQuery;
import com.ruoyi.blog.dto.BillRecognizeRequest;
import com.ruoyi.blog.dto.BillSaveRequest;
import com.ruoyi.blog.vo.BillAnalysisVO;
import com.ruoyi.blog.vo.BillVO;

public interface BlogBillService
{

    Page<BillVO> page(BillPageQuery query);

    BillVO getById(Long id);

    Long save(BillSaveRequest request);

    void delete(Long id);

    /** 批量删除账单（仅当前用户数据）。 */
    void deleteByIds(Long[] ids);

    /** 调用 AI 视觉模型识别账单图片，返回解析结果列表（不写库；支持多行明细）。 */
    List<BillVO> recognize(BillRecognizeRequest request);

    /**
     * 识别本地上传文件：图片/PDF 走视觉 OCR，Excel 本地解析表头映射。
     */
    List<BillVO> recognizeFile(MultipartFile file);

    /** 查询消费分析数据，months 为近几个月（3/6/12）。 */
    BillAnalysisVO analysis(int months);
}