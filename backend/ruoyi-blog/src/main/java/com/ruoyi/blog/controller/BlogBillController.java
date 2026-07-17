package com.ruoyi.blog.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.blog.dto.BillPageQuery;
import com.ruoyi.blog.dto.BillRecognizeRequest;
import com.ruoyi.blog.dto.BillSaveRequest;
import com.ruoyi.blog.service.BlogBillService;
import com.ruoyi.blog.vo.BillAnalysisVO;
import com.ruoyi.blog.vo.BillVO;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;

import lombok.RequiredArgsConstructor;

@Validated
@RestController
@RequestMapping("/blog/bill")
@RequiredArgsConstructor
public class BlogBillController extends BlogControllerSupport
{

    private final BlogBillService blogBillService;

    @PreAuthorize("@ss.hasPermi('blog:bill:list')")
    @GetMapping("/list")
    public TableDataInfo page(@Valid BillPageQuery query)
    {
        Page<BillVO> page = blogBillService.page(query);
        return mpPageTable(page);
    }

    @PreAuthorize("@ss.hasPermi('blog:bill:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id)
    {
        return AjaxResult.success(blogBillService.getById(id));
    }

    @PreAuthorize("@ss.hasPermi('blog:bill:add') or @ss.hasPermi('blog:bill:edit')")
    @Log(title = "账单", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult save(@Valid @RequestBody BillSaveRequest request)
    {
        return AjaxResult.success(blogBillService.save(request));
    }

    @PreAuthorize("@ss.hasPermi('blog:bill:remove')")
    @Log(title = "账单", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult delete(@PathVariable Long[] ids)
    {
        blogBillService.deleteByIds(ids);
        return AjaxResult.success();
    }

    @PreAuthorize("@ss.hasPermi('blog:bill:recognize')")
    @Log(title = "AI账单识别", businessType = BusinessType.AI, isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/recognize")
    public AjaxResult recognize(@Valid @RequestBody BillRecognizeRequest request)
    {
        return AjaxResult.success(blogBillService.recognize(request));
    }

    /** 上传图片 / PDF / Excel 识别账单明细（不写库）。 */
    @PreAuthorize("@ss.hasPermi('blog:bill:recognize')")
    @Log(title = "AI账单文件识别", businessType = BusinessType.AI, isSaveRequestData = false, isSaveResponseData = false)
    @PostMapping("/recognize/file")
    public AjaxResult recognizeFile(@RequestParam("file") MultipartFile file)
    {
        return AjaxResult.success(blogBillService.recognizeFile(file));
    }

    @PreAuthorize("@ss.hasPermi('blog:bill:analysis')")
    @GetMapping("/analysis")
    public AjaxResult analysis(
            @RequestParam(defaultValue = "6") @Min(1) @Max(12) int months)
    {
        BillAnalysisVO vo = blogBillService.analysis(months);
        return AjaxResult.success(vo);
    }
}
