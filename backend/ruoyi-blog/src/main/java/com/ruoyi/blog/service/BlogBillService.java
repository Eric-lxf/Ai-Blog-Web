package com.ruoyi.blog.service;

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

    /** 调用 AI 视觉模型识别账单图片，返回解析结果（不写库）。 */
    BillVO recognize(BillRecognizeRequest request);

    /** 查询消费分析数据，months 为近几个月（3/6/12）。 */
    BillAnalysisVO analysis(int months);
}