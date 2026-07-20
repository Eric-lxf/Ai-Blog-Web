package com.ruoyi.mall.product.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.mall.product.constant.MallProductConstants;
import com.ruoyi.mall.product.domain.MallAttr;
import com.ruoyi.mall.product.domain.MallAttrOption;
import com.ruoyi.mall.product.domain.MallCategory;
import com.ruoyi.mall.product.domain.MallCategoryAttr;
import com.ruoyi.mall.product.domain.MallSpuAttrValue;
import com.ruoyi.mall.product.dto.MallAttrOptionSaveRequest;
import com.ruoyi.mall.product.dto.MallAttrPageQuery;
import com.ruoyi.mall.product.dto.MallAttrSaveRequest;
import com.ruoyi.mall.product.dto.MallCategoryAttrBindRequest;
import com.ruoyi.mall.product.mapper.MallAttrMapper;
import com.ruoyi.mall.product.mapper.MallAttrOptionMapper;
import com.ruoyi.mall.product.mapper.MallCategoryAttrMapper;
import com.ruoyi.mall.product.mapper.MallCategoryMapper;
import com.ruoyi.mall.product.mapper.MallSpuAttrValueMapper;
import com.ruoyi.mall.product.service.MallAttrService;
import com.ruoyi.mall.product.vo.MallAttrTemplateVO;
import com.ruoyi.mall.product.vo.MallAttrVO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MallAttrServiceImpl implements MallAttrService
{
    private final MallAttrMapper mallAttrMapper;
    private final MallAttrOptionMapper mallAttrOptionMapper;
    private final MallCategoryAttrMapper mallCategoryAttrMapper;
    private final MallCategoryMapper mallCategoryMapper;
    private final MallSpuAttrValueMapper mallSpuAttrValueMapper;

    @Override
    public Page<MallAttr> page(MallAttrPageQuery query)
    {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null ? 10 : Math.min(query.getPageSize(), 100);
        return mallAttrMapper.selectPage(new Page<>(pageNum, pageSize), queryWrapper(query));
    }

    @Override
    public List<MallAttr> list(MallAttrPageQuery query)
    {
        return mallAttrMapper.selectList(queryWrapper(query));
    }

    @Override
    public MallAttr get(Long id)
    {
        return requireAttr(id);
    }

    @Override
    public Long create(MallAttrSaveRequest request)
    {
        MallAttr attr = new MallAttr();
        copyRequest(request, attr);
        attr.setCreateBy(SecurityUtils.getUsername());
        mallAttrMapper.insert(attr);
        return attr.getId();
    }

    @Override
    public void update(MallAttrSaveRequest request)
    {
        if (request.getId() == null)
        {
            throw new ServiceException("属性ID不能为空", HttpStatus.BAD_REQUEST);
        }
        requireAttr(request.getId());
        MallAttr attr = new MallAttr();
        attr.setId(request.getId());
        copyRequest(request, attr);
        attr.setUpdateBy(SecurityUtils.getUsername());
        mallAttrMapper.updateById(attr);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id)
    {
        requireAttr(id);
        Long refs = mallCategoryAttrMapper.selectCount(new LambdaQueryWrapper<MallCategoryAttr>()
                .eq(MallCategoryAttr::getAttrId, id));
        if (refs != null && refs > 0)
        {
            throw new ServiceException("属性已绑定类目，不能删除", HttpStatus.BAD_REQUEST);
        }
        Long spuRefs = mallSpuAttrValueMapper.selectCount(new LambdaQueryWrapper<MallSpuAttrValue>()
                .eq(MallSpuAttrValue::getAttrId, id));
        if (spuRefs != null && spuRefs > 0)
        {
            throw new ServiceException("属性已被商品引用，不能删除", HttpStatus.BAD_REQUEST);
        }
        mallAttrOptionMapper.delete(new LambdaQueryWrapper<MallAttrOption>()
                .eq(MallAttrOption::getAttrId, id));
        mallAttrMapper.deleteById(id);
    }

    @Override
    public List<MallAttrOption> listOptions(Long attrId)
    {
        requireAttr(attrId);
        return mallAttrOptionMapper.selectList(new LambdaQueryWrapper<MallAttrOption>()
                .eq(MallAttrOption::getAttrId, attrId)
                .orderByAsc(MallAttrOption::getSort)
                .orderByAsc(MallAttrOption::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceOptions(Long attrId, List<MallAttrOptionSaveRequest> options)
    {
        requireAttr(attrId);
        mallAttrOptionMapper.delete(new LambdaQueryWrapper<MallAttrOption>()
                .eq(MallAttrOption::getAttrId, attrId));
        if (CollectionUtils.isEmpty(options))
        {
            return;
        }
        for (MallAttrOptionSaveRequest item : options)
        {
            MallAttrOption option = new MallAttrOption();
            option.setAttrId(attrId);
            option.setValue(item.getValue());
            option.setSort(item.getSort() == null ? 0 : item.getSort());
            option.setStatus(StringUtils.hasText(item.getStatus()) ? item.getStatus() : MallProductConstants.STATUS_NORMAL);
            mallAttrOptionMapper.insert(option);
        }
    }

    @Override
    public List<MallCategoryAttr> listCategoryAttrs(Long categoryId)
    {
        requireCategory(categoryId);
        return mallCategoryAttrMapper.selectList(new LambdaQueryWrapper<MallCategoryAttr>()
                .eq(MallCategoryAttr::getCategoryId, categoryId)
                .orderByAsc(MallCategoryAttr::getSort)
                .orderByAsc(MallCategoryAttr::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceCategoryAttrs(Long categoryId, List<MallCategoryAttrBindRequest.Item> items)
    {
        requireCategory(categoryId);
        requireLeafCategory(categoryId);
        List<MallCategoryAttrBindRequest.Item> bindItems = items == null ? List.of() : items;
        Set<Long> attrIds = new HashSet<>();
        for (MallCategoryAttrBindRequest.Item item : bindItems)
        {
            String attrType = item.getAttrType() == null ? "" : item.getAttrType().trim();
            if (!MallProductConstants.ATTR_TYPE_SALE.equals(attrType)
                    && !MallProductConstants.ATTR_TYPE_DESC.equals(attrType))
            {
                throw new ServiceException("属性类型仅支持 SALE 或 DESC", HttpStatus.BAD_REQUEST);
            }
            if (!attrIds.add(item.getAttrId()))
            {
                throw new ServiceException("同一类目不能重复绑定同一属性", HttpStatus.BAD_REQUEST);
            }
            requireAttr(item.getAttrId());
        }
        mallCategoryAttrMapper.delete(new LambdaQueryWrapper<MallCategoryAttr>()
                .eq(MallCategoryAttr::getCategoryId, categoryId));
        for (MallCategoryAttrBindRequest.Item item : bindItems)
        {
            MallCategoryAttr bind = new MallCategoryAttr();
            bind.setCategoryId(categoryId);
            bind.setAttrId(item.getAttrId());
            bind.setAttrType(item.getAttrType().trim());
            bind.setRequired(StringUtils.hasText(item.getRequired()) ? item.getRequired() : "0");
            bind.setSort(item.getSort() == null ? 0 : item.getSort());
            mallCategoryAttrMapper.insert(bind);
        }
    }

    @Override
    public MallAttrTemplateVO getAttrTemplate(Long categoryId)
    {
        requireCategory(categoryId);
        List<MallCategoryAttr> binds = listCategoryAttrs(categoryId);
        MallAttrTemplateVO template = new MallAttrTemplateVO();
        if (binds.isEmpty())
        {
            return template;
        }
        Set<Long> attrIds = binds.stream().map(MallCategoryAttr::getAttrId).collect(Collectors.toSet());
        Map<Long, MallAttr> attrMap = mallAttrMapper.selectList(new LambdaQueryWrapper<MallAttr>()
                .in(MallAttr::getId, attrIds)).stream()
                .collect(Collectors.toMap(MallAttr::getId, Function.identity()));
        Map<Long, List<MallAttrOption>> optionsMap = mallAttrOptionMapper.selectList(new LambdaQueryWrapper<MallAttrOption>()
                .in(MallAttrOption::getAttrId, attrIds)
                .eq(MallAttrOption::getStatus, MallProductConstants.STATUS_NORMAL)
                .orderByAsc(MallAttrOption::getSort)
                .orderByAsc(MallAttrOption::getId)).stream()
                .collect(Collectors.groupingBy(MallAttrOption::getAttrId));

        List<MallAttrVO> saleAttrs = new ArrayList<>();
        List<MallAttrVO> descAttrs = new ArrayList<>();
        for (MallCategoryAttr bind : binds)
        {
            MallAttr attr = attrMap.get(bind.getAttrId());
            if (attr == null || !Objects.equals(attr.getStatus(), MallProductConstants.STATUS_NORMAL))
            {
                continue;
            }
            MallAttrVO vo = toAttrVO(attr, bind, optionsMap.getOrDefault(attr.getId(), List.of()));
            if (MallProductConstants.ATTR_TYPE_SALE.equals(bind.getAttrType()))
            {
                saleAttrs.add(vo);
            }
            else if (MallProductConstants.ATTR_TYPE_DESC.equals(bind.getAttrType()))
            {
                descAttrs.add(vo);
            }
        }
        saleAttrs.sort(Comparator.comparing(MallAttrVO::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(MallAttrVO::getId, Comparator.nullsLast(Long::compareTo)));
        descAttrs.sort(Comparator.comparing(MallAttrVO::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(MallAttrVO::getId, Comparator.nullsLast(Long::compareTo)));
        template.setSaleAttrs(saleAttrs);
        template.setDescAttrs(descAttrs);
        return template;
    }

    private LambdaQueryWrapper<MallAttr> queryWrapper(MallAttrPageQuery query)
    {
        LambdaQueryWrapper<MallAttr> wrapper = new LambdaQueryWrapper<>();
        if (query != null)
        {
            if (StringUtils.hasText(query.getName()))
            {
                wrapper.like(MallAttr::getName, query.getName().trim());
            }
            if (StringUtils.hasText(query.getStatus()))
            {
                wrapper.eq(MallAttr::getStatus, query.getStatus().trim());
            }
            if (StringUtils.hasText(query.getInputType()))
            {
                wrapper.eq(MallAttr::getInputType, query.getInputType().trim());
            }
        }
        wrapper.orderByAsc(MallAttr::getSort).orderByAsc(MallAttr::getId);
        return wrapper;
    }

    private MallAttr requireAttr(Long id)
    {
        MallAttr attr = mallAttrMapper.selectById(id);
        if (attr == null)
        {
            throw new ServiceException("属性不存在", HttpStatus.NOT_FOUND);
        }
        return attr;
    }

    private MallCategory requireCategory(Long id)
    {
        MallCategory category = mallCategoryMapper.selectById(id);
        if (category == null)
        {
            throw new ServiceException("类目不存在", HttpStatus.NOT_FOUND);
        }
        return category;
    }

    private void requireLeafCategory(Long categoryId)
    {
        Long children = mallCategoryMapper.selectCount(new LambdaQueryWrapper<MallCategory>()
                .eq(MallCategory::getParentId, categoryId));
        if (children != null && children > 0)
        {
            throw new ServiceException("仅叶子类目可绑定属性", HttpStatus.BAD_REQUEST);
        }
    }

    private void copyRequest(MallAttrSaveRequest request, MallAttr attr)
    {
        attr.setName(request.getName());
        String inputType = StringUtils.hasText(request.getInputType())
                ? request.getInputType().trim()
                : MallProductConstants.INPUT_TYPE_TEXT;
        if (!MallProductConstants.INPUT_TYPE_TEXT.equals(inputType)
                && !MallProductConstants.INPUT_TYPE_SELECT.equals(inputType)
                && !MallProductConstants.INPUT_TYPE_MULTI.equals(inputType))
        {
            throw new ServiceException("录入类型仅支持 text、select 或 multi", HttpStatus.BAD_REQUEST);
        }
        attr.setInputType(inputType);
        attr.setSort(request.getSort() == null ? 0 : request.getSort());
        attr.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : MallProductConstants.STATUS_NORMAL);
        attr.setRemark(request.getRemark());
    }

    private MallAttrVO toAttrVO(MallAttr attr, MallCategoryAttr bind, List<MallAttrOption> options)
    {
        MallAttrVO vo = new MallAttrVO();
        vo.setId(attr.getId());
        vo.setName(attr.getName());
        vo.setInputType(attr.getInputType());
        vo.setStatus(attr.getStatus());
        vo.setSort(bind.getSort() != null ? bind.getSort() : attr.getSort());
        vo.setRemark(attr.getRemark());
        vo.setAttrType(bind.getAttrType());
        vo.setRequired(bind.getRequired());
        vo.setOptions(options);
        return vo;
    }
}
