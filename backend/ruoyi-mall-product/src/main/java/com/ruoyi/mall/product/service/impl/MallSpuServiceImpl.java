package com.ruoyi.mall.product.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.mall.product.constant.MallProductConstants;
import com.ruoyi.mall.product.domain.MallAttrOption;
import com.ruoyi.mall.product.domain.MallBrand;
import com.ruoyi.mall.product.domain.MallCategory;
import com.ruoyi.mall.product.domain.MallSku;
import com.ruoyi.mall.product.domain.MallSpu;
import com.ruoyi.mall.product.domain.MallSpuAttrValue;
import com.ruoyi.mall.product.domain.MallSpuImage;
import com.ruoyi.mall.product.dto.MallSkuSaveRequest;
import com.ruoyi.mall.product.dto.MallSpuAttrValueRequest;
import com.ruoyi.mall.product.dto.MallSpuImageSaveRequest;
import com.ruoyi.mall.product.dto.MallSpuPageQuery;
import com.ruoyi.mall.product.dto.MallSpuSaveRequest;
import com.ruoyi.mall.product.mapper.MallBrandMapper;
import com.ruoyi.mall.product.mapper.MallCategoryMapper;
import com.ruoyi.mall.product.mapper.MallSkuMapper;
import com.ruoyi.mall.product.mapper.MallSpuAttrValueMapper;
import com.ruoyi.mall.product.mapper.MallSpuImageMapper;
import com.ruoyi.mall.product.mapper.MallSpuMapper;
import com.ruoyi.mall.product.service.MallAttrService;
import com.ruoyi.mall.product.service.MallFrontCategoryService;
import com.ruoyi.mall.product.service.MallSpuService;
import com.ruoyi.mall.product.vo.MallAttrTemplateVO;
import com.ruoyi.mall.product.vo.MallAttrVO;
import com.ruoyi.mall.product.vo.MallSpuAttrValueVO;
import com.ruoyi.mall.product.vo.MallSpuDetailVO;
import com.ruoyi.mall.product.vo.MallSpuVO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MallSpuServiceImpl implements MallSpuService
{
    private final MallSpuMapper mallSpuMapper;
    private final MallSkuMapper mallSkuMapper;
    private final MallSpuImageMapper mallSpuImageMapper;
    private final MallSpuAttrValueMapper mallSpuAttrValueMapper;
    private final MallCategoryMapper mallCategoryMapper;
    private final MallBrandMapper mallBrandMapper;
    private final MallAttrService mallAttrService;
    private final MallFrontCategoryService mallFrontCategoryService;

    @Override
    public Page<MallSpuVO> page(MallSpuPageQuery query)
    {
        return querySpus(query, false);
    }

    @Override
    public Page<MallSpuVO> publicPage(MallSpuPageQuery query)
    {
        return querySpus(query, true);
    }

    @Override
    public MallSpuDetailVO detail(Long id)
    {
        MallSpu spu = requireSpu(id);
        return toDetailVO(spu, false);
    }

    @Override
    public MallSpuDetailVO publicDetail(Long id)
    {
        MallSpu spu = mallSpuMapper.selectOne(new LambdaQueryWrapper<MallSpu>()
                .eq(MallSpu::getId, id)
                .eq(MallSpu::getStatus, MallProductConstants.SPU_STATUS_ON)
                .eq(MallSpu::getDelFlag, MallProductConstants.DEL_FLAG_NORMAL));
        if (spu == null)
        {
            throw new ServiceException("商品不存在", HttpStatus.NOT_FOUND);
        }
        return toDetailVO(spu, true);
    }

    @Override
    @Transactional
    public Long save(MallSpuSaveRequest request)
    {
        validateStatus(request.getStatus(), true);
        validateCategoryAndBrand(request.getCategoryId(), request.getBrandId());
        validateLeafCategory(request.getCategoryId());

        MallAttrTemplateVO template = mallAttrService.getAttrTemplate(request.getCategoryId());
        validateRequiredDescAttrs(template, request.getAttrValues());
        validateSaleSpecsForRequests(template, request.getSkus());

        String username = SecurityUtils.getUsername();
        MallSpu spu = new MallSpu();
        if (request.getId() != null)
        {
            requireSpu(request.getId());
            spu.setId(request.getId());
            spu.setUpdateBy(username);
        }
        else
        {
            spu.setCreateBy(username);
            spu.setDelFlag(MallProductConstants.DEL_FLAG_NORMAL);
        }
        copyRequest(request, spu);

        if (spu.getId() == null)
        {
            mallSpuMapper.insert(spu);
        }
        else
        {
            mallSpuMapper.updateById(spu);
        }

        saveSkus(spu.getId(), request.getSkus(), username);
        saveImages(spu.getId(), request.getImages());
        saveAttrValues(spu.getId(), template, request.getAttrValues());
        if (MallProductConstants.SPU_STATUS_ON.equals(spu.getStatus()))
        {
            validatePublishable(spu.getId());
        }
        return spu.getId();
    }

    @Override
    @Transactional
    public void delete(Long id)
    {
        requireSpu(id);
        String username = SecurityUtils.getUsername();
        mallSpuMapper.update(null, new LambdaUpdateWrapper<MallSpu>()
                .eq(MallSpu::getId, id)
                .eq(MallSpu::getDelFlag, MallProductConstants.DEL_FLAG_NORMAL)
                .set(MallSpu::getDelFlag, MallProductConstants.DEL_FLAG_DELETED)
                .set(MallSpu::getUpdateBy, username));
        mallSkuMapper.update(null, new LambdaUpdateWrapper<MallSku>()
                .eq(MallSku::getSpuId, id)
                .eq(MallSku::getDelFlag, MallProductConstants.DEL_FLAG_NORMAL)
                .set(MallSku::getDelFlag, MallProductConstants.DEL_FLAG_DELETED)
                .set(MallSku::getUpdateBy, username));
        mallSpuAttrValueMapper.delete(new LambdaQueryWrapper<MallSpuAttrValue>().eq(MallSpuAttrValue::getSpuId, id));
    }

    @Override
    @Transactional
    public void publish(Long id, String status)
    {
        validateStatus(status, false);
        MallSpu spu = requireSpu(id);
        if (MallProductConstants.SPU_STATUS_ON.equals(status))
        {
            validateLeafCategory(spu.getCategoryId());
            MallAttrTemplateVO template = mallAttrService.getAttrTemplate(spu.getCategoryId());
            List<MallSpuAttrValue> storedValues = mallSpuAttrValueMapper.selectList(
                    new LambdaQueryWrapper<MallSpuAttrValue>().eq(MallSpuAttrValue::getSpuId, id));
            List<MallSpuAttrValueRequest> valueRequests = storedValues.stream().map(v -> {
                MallSpuAttrValueRequest req = new MallSpuAttrValueRequest();
                req.setAttrId(v.getAttrId());
                req.setValue(v.getValue());
                return req;
            }).toList();
            validateRequiredDescAttrs(template, valueRequests);

            List<MallSku> enabledSkus = mallSkuMapper.selectList(new LambdaQueryWrapper<MallSku>()
                    .eq(MallSku::getSpuId, id)
                    .eq(MallSku::getStatus, MallProductConstants.STATUS_NORMAL)
                    .eq(MallSku::getDelFlag, MallProductConstants.DEL_FLAG_NORMAL));
            validateSaleSpecsForSkus(template, enabledSkus);
            validatePublishable(id);
        }
        int rows = mallSpuMapper.update(null, new LambdaUpdateWrapper<MallSpu>()
                .eq(MallSpu::getId, id)
                .eq(MallSpu::getDelFlag, MallProductConstants.DEL_FLAG_NORMAL)
                .set(MallSpu::getStatus, status)
                .set(MallSpu::getUpdateBy, SecurityUtils.getUsername()));
        if (rows == 0)
        {
            throw new ServiceException("商品状态更新失败", HttpStatus.ERROR);
        }
    }

    private Page<MallSpuVO> querySpus(MallSpuPageQuery query, boolean publicOnly)
    {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null ? 10 : Math.min(query.getPageSize(), 100);
        LambdaQueryWrapper<MallSpu> wrapper = new LambdaQueryWrapper<MallSpu>()
                .eq(MallSpu::getDelFlag, MallProductConstants.DEL_FLAG_NORMAL);
        String nameKeyword = StringUtils.hasText(query.getKeyword()) ? query.getKeyword().trim()
                : (StringUtils.hasText(query.getName()) ? query.getName().trim() : null);
        if (nameKeyword != null)
        {
            wrapper.like(MallSpu::getName, nameKeyword);
        }
        if (query.getCategoryId() != null)
        {
            if (publicOnly)
            {
                Set<Long> backIds = mallFrontCategoryService.resolveBackCategoryIds(query.getCategoryId());
                if (backIds.isEmpty())
                {
                    return emptyPage(pageNum, pageSize);
                }
                wrapper.in(MallSpu::getCategoryId, backIds);
            }
            else
            {
                wrapper.eq(MallSpu::getCategoryId, query.getCategoryId());
            }
        }
        if (query.getBrandId() != null)
        {
            wrapper.eq(MallSpu::getBrandId, query.getBrandId());
        }
        if (publicOnly)
        {
            wrapper.eq(MallSpu::getStatus, MallProductConstants.SPU_STATUS_ON);
        }
        else if (StringUtils.hasText(query.getStatus()))
        {
            wrapper.eq(MallSpu::getStatus, query.getStatus().trim());
        }
        applySpuSort(wrapper, query.getSort(), publicOnly);
        Page<MallSpu> result = mallSpuMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        Map<Long, String> categoryMap = loadCategoryMap(result.getRecords());
        Map<Long, String> brandMap = loadBrandMap(result.getRecords());
        Page<MallSpuVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(spu -> toVO(spu, categoryMap, brandMap)).toList());
        return voPage;
    }

    private Page<MallSpuVO> emptyPage(int pageNum, int pageSize)
    {
        Page<MallSpuVO> page = new Page<>(pageNum, pageSize, 0);
        page.setRecords(List.of());
        return page;
    }

    /**
     * sort 白名单：price 按启用 SKU 最低价升序；latest 或其它值走默认。
     * 公开列表默认按更新时间倒序；运营列表默认按运营 sort 升序。
     */
    private void applySpuSort(LambdaQueryWrapper<MallSpu> wrapper, String sort, boolean publicOnly)
    {
        if ("price".equalsIgnoreCase(sort))
        {
            wrapper.last("ORDER BY (SELECT MIN(s.price) FROM mall_sku s WHERE s.spu_id = mall_spu.id"
                    + " AND s.del_flag = '" + MallProductConstants.DEL_FLAG_NORMAL + "'"
                    + " AND s.status = '0') ASC, mall_spu.id DESC");
            return;
        }
        if (publicOnly || "latest".equalsIgnoreCase(sort))
        {
            wrapper.orderByDesc(MallSpu::getUpdateTime).orderByDesc(MallSpu::getId);
            return;
        }
        wrapper.orderByAsc(MallSpu::getSort).orderByDesc(MallSpu::getUpdateTime).orderByDesc(MallSpu::getId);
    }

    private MallSpu requireSpu(Long id)
    {
        MallSpu spu = mallSpuMapper.selectOne(new LambdaQueryWrapper<MallSpu>()
                .eq(MallSpu::getId, id)
                .eq(MallSpu::getDelFlag, MallProductConstants.DEL_FLAG_NORMAL));
        if (spu == null)
        {
            throw new ServiceException("商品不存在", HttpStatus.NOT_FOUND);
        }
        return spu;
    }

    private void copyRequest(MallSpuSaveRequest request, MallSpu spu)
    {
        spu.setCategoryId(request.getCategoryId());
        spu.setBrandId(request.getBrandId());
        spu.setName(request.getName());
        spu.setSubtitle(request.getSubtitle());
        spu.setMainImage(request.getMainImage());
        spu.setDetailHtml(request.getDetailHtml());
        spu.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : MallProductConstants.SPU_STATUS_DRAFT);
        spu.setSort(request.getSort() == null ? 0 : request.getSort());
        spu.setRemark(request.getRemark());
    }

    private void saveSkus(Long spuId, List<MallSkuSaveRequest> requests, String username)
    {
        List<MallSku> existing = mallSkuMapper.selectList(new LambdaQueryWrapper<MallSku>()
                .eq(MallSku::getSpuId, spuId)
                .eq(MallSku::getDelFlag, MallProductConstants.DEL_FLAG_NORMAL));
        Map<Long, MallSku> existingById = existing.stream()
                .filter(sku -> sku.getId() != null)
                .collect(Collectors.toMap(MallSku::getId, Function.identity()));
        Set<Long> submittedIds = CollectionUtils.isEmpty(requests) ? Set.of()
                : requests.stream().map(MallSkuSaveRequest::getId).filter(Objects::nonNull).collect(Collectors.toSet());

        if (!CollectionUtils.isEmpty(requests))
        {
            for (MallSkuSaveRequest request : requests)
            {
                validateSku(request);
                MallSku sku = new MallSku();
                if (request.getId() != null)
                {
                    if (!existingById.containsKey(request.getId()))
                    {
                        throw new ServiceException("SKU不存在或不属于当前商品", HttpStatus.BAD_REQUEST);
                    }
                    sku.setId(request.getId());
                    sku.setUpdateBy(username);
                }
                else
                {
                    sku.setSpuId(spuId);
                    sku.setCreateBy(username);
                    sku.setDelFlag(MallProductConstants.DEL_FLAG_NORMAL);
                }
                copySkuRequest(request, sku);
                if (sku.getId() == null)
                {
                    mallSkuMapper.insert(sku);
                }
                else
                {
                    mallSkuMapper.updateById(sku);
                }
            }
        }

        List<Long> removedIds = existing.stream()
                .map(MallSku::getId)
                .filter(id -> !submittedIds.contains(id))
                .toList();
        if (!removedIds.isEmpty())
        {
            mallSkuMapper.update(null, new LambdaUpdateWrapper<MallSku>()
                    .in(MallSku::getId, removedIds)
                    .set(MallSku::getDelFlag, MallProductConstants.DEL_FLAG_DELETED)
                    .set(MallSku::getUpdateBy, username));
        }
    }

    private void saveImages(Long spuId, List<MallSpuImageSaveRequest> images)
    {
        mallSpuImageMapper.delete(new LambdaQueryWrapper<MallSpuImage>().eq(MallSpuImage::getSpuId, spuId));
        if (CollectionUtils.isEmpty(images))
        {
            return;
        }
        for (MallSpuImageSaveRequest request : images)
        {
            MallSpuImage image = new MallSpuImage();
            image.setSpuId(spuId);
            image.setUrl(request.getUrl());
            image.setSort(request.getSort() == null ? 0 : request.getSort());
            mallSpuImageMapper.insert(image);
        }
    }

    private void saveAttrValues(Long spuId, MallAttrTemplateVO template, List<MallSpuAttrValueRequest> attrValues)
    {
        mallSpuAttrValueMapper.delete(new LambdaQueryWrapper<MallSpuAttrValue>().eq(MallSpuAttrValue::getSpuId, spuId));
        if (CollectionUtils.isEmpty(attrValues))
        {
            return;
        }
        List<MallAttrVO> descAttrs = template == null || template.getDescAttrs() == null
                ? List.of() : template.getDescAttrs();
        Set<Long> descAttrIds = descAttrs.stream()
                .map(MallAttrVO::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<Long> seen = new HashSet<>();
        for (MallSpuAttrValueRequest request : attrValues)
        {
            if (request.getAttrId() == null)
            {
                continue;
            }
            if (!descAttrIds.contains(request.getAttrId()))
            {
                throw new ServiceException("描述属性不在当前类目模板中", HttpStatus.BAD_REQUEST);
            }
            if (!seen.add(request.getAttrId()))
            {
                throw new ServiceException("描述属性不能重复", HttpStatus.BAD_REQUEST);
            }
            String value = request.getValue() == null ? "" : request.getValue().trim();
            if (!StringUtils.hasText(value))
            {
                continue;
            }
            MallSpuAttrValue row = new MallSpuAttrValue();
            row.setSpuId(spuId);
            row.setAttrId(request.getAttrId());
            row.setValue(value);
            mallSpuAttrValueMapper.insert(row);
        }
    }

    private void copySkuRequest(MallSkuSaveRequest request, MallSku sku)
    {
        sku.setSkuCode(request.getSkuCode());
        sku.setSpecsJson(request.getSpecsJson());
        sku.setPrice(request.getPrice());
        sku.setStock(request.getStock());
        sku.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : MallProductConstants.STATUS_NORMAL);
        sku.setRemark(request.getRemark());
    }

    private void validateSku(MallSkuSaveRequest request)
    {
        if (request.getPrice() == null || request.getPrice().compareTo(BigDecimal.ZERO) < 0)
        {
            throw new ServiceException("SKU价格不能小于0", HttpStatus.BAD_REQUEST);
        }
        if (request.getStock() == null || request.getStock() < 0)
        {
            throw new ServiceException("SKU库存不能小于0", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateLeafCategory(Long categoryId)
    {
        Long childCount = mallCategoryMapper.selectCount(new LambdaQueryWrapper<MallCategory>()
                .eq(MallCategory::getParentId, categoryId));
        if (childCount != null && childCount > 0)
        {
            throw new ServiceException("请选择后台叶子类目", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateRequiredDescAttrs(MallAttrTemplateVO template, List<MallSpuAttrValueRequest> attrValues)
    {
        List<MallAttrVO> descAttrs = template.getDescAttrs() == null ? List.of() : template.getDescAttrs();
        if (descAttrs.isEmpty())
        {
            return;
        }
        Map<Long, String> valueMap = new HashMap<>();
        if (!CollectionUtils.isEmpty(attrValues))
        {
            for (MallSpuAttrValueRequest item : attrValues)
            {
                if (item.getAttrId() != null)
                {
                    valueMap.put(item.getAttrId(), item.getValue());
                }
            }
        }
        for (MallAttrVO attr : descAttrs)
        {
            if (!"1".equals(attr.getRequired()))
            {
                continue;
            }
            String value = valueMap.get(attr.getId());
            if (!StringUtils.hasText(value))
            {
                throw new ServiceException("描述属性「" + attr.getName() + "」不能为空", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private void validateSaleSpecsForRequests(MallAttrTemplateVO template, List<MallSkuSaveRequest> skus)
    {
        List<MallAttrVO> saleAttrs = template.getSaleAttrs() == null ? List.of() : template.getSaleAttrs();
        if (saleAttrs.isEmpty() || CollectionUtils.isEmpty(skus))
        {
            return;
        }
        for (MallSkuSaveRequest sku : skus)
        {
            String status = StringUtils.hasText(sku.getStatus()) ? sku.getStatus() : MallProductConstants.STATUS_NORMAL;
            if (!MallProductConstants.STATUS_NORMAL.equals(status))
            {
                continue;
            }
            validateSaleSpecsJson(saleAttrs, sku.getSpecsJson(), sku.getSkuCode());
        }
    }

    private void validateSaleSpecsForSkus(MallAttrTemplateVO template, List<MallSku> skus)
    {
        List<MallAttrVO> saleAttrs = template.getSaleAttrs() == null ? List.of() : template.getSaleAttrs();
        if (saleAttrs.isEmpty())
        {
            return;
        }
        for (MallSku sku : skus)
        {
            validateSaleSpecsJson(saleAttrs, sku.getSpecsJson(), sku.getSkuCode());
        }
    }

    private void validateSaleSpecsJson(List<MallAttrVO> saleAttrs, String specsJson, String skuCode)
    {
        String label = StringUtils.hasText(skuCode) ? skuCode : "SKU";
        Map<String, Object> specs = parseSpecsJson(specsJson, label);
        for (MallAttrVO attr : saleAttrs)
        {
            if (!specs.containsKey(attr.getName()))
            {
                throw new ServiceException(label + " 缺少销售属性「" + attr.getName() + "」", HttpStatus.BAD_REQUEST);
            }
            Object raw = specs.get(attr.getName());
            if (MallProductConstants.INPUT_TYPE_SELECT.equals(attr.getInputType())
                    || MallProductConstants.INPUT_TYPE_MULTI.equals(attr.getInputType()))
            {
                Set<String> options = optionValues(attr);
                List<String> values = extractSpecValues(raw, attr.getInputType());
                if (values.isEmpty())
                {
                    throw new ServiceException(label + " 销售属性「" + attr.getName() + "」不能为空", HttpStatus.BAD_REQUEST);
                }
                for (String value : values)
                {
                    if (!options.contains(value))
                    {
                        throw new ServiceException(label + " 销售属性「" + attr.getName() + "」值不合法", HttpStatus.BAD_REQUEST);
                    }
                }
            }
        }
    }

    private Map<String, Object> parseSpecsJson(String specsJson, String label)
    {
        if (!StringUtils.hasText(specsJson))
        {
            throw new ServiceException(label + " 规格JSON不能为空", HttpStatus.BAD_REQUEST);
        }
        try
        {
            Map<String, Object> specs = JSON.parseObject(specsJson.trim(), new TypeReference<Map<String, Object>>() {});
            if (specs == null)
            {
                throw new ServiceException(label + " 规格JSON不合法", HttpStatus.BAD_REQUEST);
            }
            return specs;
        }
        catch (JSONException ex)
        {
            throw new ServiceException(label + " 规格JSON不合法", HttpStatus.BAD_REQUEST);
        }
    }

    private Set<String> optionValues(MallAttrVO attr)
    {
        if (CollectionUtils.isEmpty(attr.getOptions()))
        {
            return Set.of();
        }
        return attr.getOptions().stream()
                .map(MallAttrOption::getValue)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    private List<String> extractSpecValues(Object raw, String inputType)
    {
        if (raw == null)
        {
            return List.of();
        }
        if (raw instanceof Collection<?> collection)
        {
            List<String> values = new ArrayList<>();
            for (Object item : collection)
            {
                if (item != null && StringUtils.hasText(String.valueOf(item)))
                {
                    values.add(String.valueOf(item).trim());
                }
            }
            return values;
        }
        String text = String.valueOf(raw).trim();
        if (!StringUtils.hasText(text))
        {
            return List.of();
        }
        if (MallProductConstants.INPUT_TYPE_MULTI.equals(inputType) && text.contains(","))
        {
            List<String> values = new ArrayList<>();
            for (String part : text.split(","))
            {
                if (StringUtils.hasText(part))
                {
                    values.add(part.trim());
                }
            }
            return values;
        }
        return List.of(text);
    }

    private void validatePublishable(Long spuId)
    {
        List<MallSku> enabledSkus = mallSkuMapper.selectList(new LambdaQueryWrapper<MallSku>()
                .eq(MallSku::getSpuId, spuId)
                .eq(MallSku::getStatus, MallProductConstants.STATUS_NORMAL)
                .eq(MallSku::getDelFlag, MallProductConstants.DEL_FLAG_NORMAL));
        if (enabledSkus.isEmpty())
        {
            throw new ServiceException("上架商品至少需要一个启用SKU", HttpStatus.BAD_REQUEST);
        }
        for (MallSku sku : enabledSkus)
        {
            if (sku.getPrice() == null || sku.getPrice().compareTo(BigDecimal.ZERO) < 0)
            {
                throw new ServiceException("启用SKU价格不能小于0", HttpStatus.BAD_REQUEST);
            }
            if (sku.getStock() == null || sku.getStock() < 0)
            {
                throw new ServiceException("启用SKU库存不能小于0", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private void validateStatus(String status, boolean allowDraft)
    {
        if (!StringUtils.hasText(status))
        {
            return;
        }
        String value = status.trim();
        boolean valid = MallProductConstants.SPU_STATUS_ON.equals(value)
                || MallProductConstants.SPU_STATUS_OFF.equals(value)
                || (allowDraft && MallProductConstants.SPU_STATUS_DRAFT.equals(value));
        if (!valid)
        {
            throw new ServiceException("商品状态不合法", HttpStatus.BAD_REQUEST);
        }
    }

    private void validateCategoryAndBrand(Long categoryId, Long brandId)
    {
        MallCategory category = mallCategoryMapper.selectById(categoryId);
        if (category == null)
        {
            throw new ServiceException("商品类目不存在", HttpStatus.BAD_REQUEST);
        }
        if (brandId != null)
        {
            MallBrand brand = mallBrandMapper.selectById(brandId);
            if (brand == null)
            {
                throw new ServiceException("商品品牌不存在", HttpStatus.BAD_REQUEST);
            }
        }
    }

    private MallSpuDetailVO toDetailVO(MallSpu spu, boolean publicOnly)
    {
        Map<Long, String> categoryMap = loadCategoryMap(List.of(spu));
        Map<Long, String> brandMap = loadBrandMap(List.of(spu));
        MallSpuDetailVO vo = new MallSpuDetailVO();
        BeanUtils.copyProperties(toVO(spu, categoryMap, brandMap), vo);
        LambdaQueryWrapper<MallSku> skuWrapper = new LambdaQueryWrapper<MallSku>()
                .eq(MallSku::getSpuId, spu.getId())
                .eq(MallSku::getDelFlag, MallProductConstants.DEL_FLAG_NORMAL);
        if (publicOnly)
        {
            skuWrapper.eq(MallSku::getStatus, MallProductConstants.STATUS_NORMAL);
        }
        skuWrapper.orderByAsc(MallSku::getId);
        vo.setSkus(mallSkuMapper.selectList(skuWrapper));
        vo.setImages(mallSpuImageMapper.selectList(new LambdaQueryWrapper<MallSpuImage>()
                .eq(MallSpuImage::getSpuId, spu.getId())
                .orderByAsc(MallSpuImage::getSort)
                .orderByAsc(MallSpuImage::getId)));
        vo.setAttrValues(loadAttrValueVOs(spu));
        return vo;
    }

    private List<MallSpuAttrValueVO> loadAttrValueVOs(MallSpu spu)
    {
        List<MallSpuAttrValue> rows = mallSpuAttrValueMapper.selectList(new LambdaQueryWrapper<MallSpuAttrValue>()
                .eq(MallSpuAttrValue::getSpuId, spu.getId())
                .orderByAsc(MallSpuAttrValue::getId));
        if (rows.isEmpty())
        {
            return List.of();
        }
        MallAttrTemplateVO template = mallAttrService.getAttrTemplate(spu.getCategoryId());
        Map<Long, MallAttrVO> descMap = (template.getDescAttrs() == null ? List.<MallAttrVO>of() : template.getDescAttrs())
                .stream().collect(Collectors.toMap(MallAttrVO::getId, Function.identity(), (a, b) -> a));
        Map<Long, MallAttrVO> saleMap = (template.getSaleAttrs() == null ? List.<MallAttrVO>of() : template.getSaleAttrs())
                .stream().collect(Collectors.toMap(MallAttrVO::getId, Function.identity(), (a, b) -> a));

        List<MallSpuAttrValueVO> result = new ArrayList<>();
        for (MallSpuAttrValue row : rows)
        {
            MallSpuAttrValueVO item = new MallSpuAttrValueVO();
            item.setAttrId(row.getAttrId());
            item.setValue(row.getValue());
            MallAttrVO desc = descMap.get(row.getAttrId());
            if (desc != null)
            {
                item.setAttrName(desc.getName());
                item.setAttrType(MallProductConstants.ATTR_TYPE_DESC);
            }
            else
            {
                MallAttrVO sale = saleMap.get(row.getAttrId());
                if (sale != null)
                {
                    item.setAttrName(sale.getName());
                    item.setAttrType(MallProductConstants.ATTR_TYPE_SALE);
                }
                else
                {
                    item.setAttrType(MallProductConstants.ATTR_TYPE_DESC);
                }
            }
            result.add(item);
        }
        return result;
    }

    private MallSpuVO toVO(MallSpu spu, Map<Long, String> categoryMap, Map<Long, String> brandMap)
    {
        MallSpuVO vo = new MallSpuVO();
        BeanUtils.copyProperties(spu, vo);
        vo.setCategoryName(categoryMap.get(spu.getCategoryId()));
        vo.setBrandName(brandMap.get(spu.getBrandId()));
        return vo;
    }

    private Map<Long, String> loadCategoryMap(List<MallSpu> spus)
    {
        List<Long> ids = spus.stream().map(MallSpu::getCategoryId).filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty())
        {
            return Map.of();
        }
        return mallCategoryMapper.selectList(new LambdaQueryWrapper<MallCategory>().in(MallCategory::getId, ids)).stream()
                .collect(Collectors.toMap(MallCategory::getId, MallCategory::getName));
    }

    private Map<Long, String> loadBrandMap(List<MallSpu> spus)
    {
        List<Long> ids = spus.stream().map(MallSpu::getBrandId).filter(Objects::nonNull).distinct().toList();
        if (ids.isEmpty())
        {
            return Map.of();
        }
        return mallBrandMapper.selectList(new LambdaQueryWrapper<MallBrand>().in(MallBrand::getId, ids)).stream()
                .collect(Collectors.toMap(MallBrand::getId, MallBrand::getName));
    }
}
