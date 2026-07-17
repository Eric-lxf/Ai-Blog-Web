package com.ruoyi.mall.product.service.impl;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.mall.product.constant.MallProductConstants;
import com.ruoyi.mall.product.domain.MallCategory;
import com.ruoyi.mall.product.dto.MallCategoryQuery;
import com.ruoyi.mall.product.dto.MallCategorySaveRequest;
import com.ruoyi.mall.product.mapper.MallCategoryMapper;
import com.ruoyi.mall.product.service.MallCategoryService;
import com.ruoyi.mall.product.vo.MallCategoryTreeVO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MallCategoryServiceImpl implements MallCategoryService
{
    private final MallCategoryMapper mallCategoryMapper;

    @Override
    public List<MallCategory> list(MallCategoryQuery query)
    {
        return mallCategoryMapper.selectList(queryWrapper(query));
    }

    @Override
    public List<MallCategoryTreeVO> tree(MallCategoryQuery query)
    {
        List<MallCategoryTreeVO> nodes = list(query).stream().map(this::toTreeVO).toList();
        Map<Long, List<MallCategoryTreeVO>> childrenMap = nodes.stream()
                .collect(Collectors.groupingBy(node -> node.getParentId() == null ? 0L : node.getParentId()));
        nodes.forEach(node -> node.setChildren(childrenMap.getOrDefault(node.getId(), List.of())));
        return nodes.stream()
                .filter(node -> node.getParentId() == null || node.getParentId() == 0L)
                .sorted(Comparator.comparing(MallCategoryTreeVO::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(MallCategoryTreeVO::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    @Override
    public List<MallCategory> listActive()
    {
        MallCategoryQuery query = new MallCategoryQuery();
        query.setStatus(MallProductConstants.STATUS_NORMAL);
        return list(query);
    }

    @Override
    public MallCategory getById(Long id)
    {
        return requireCategory(id);
    }

    @Override
    public Long create(MallCategorySaveRequest request)
    {
        MallCategory category = new MallCategory();
        copyRequest(request, category);
        category.setCreateBy(SecurityUtils.getUsername());
        mallCategoryMapper.insert(category);
        return category.getId();
    }

    @Override
    public void update(MallCategorySaveRequest request)
    {
        if (request.getId() == null)
        {
            throw new ServiceException("类目ID不能为空", HttpStatus.BAD_REQUEST);
        }
        requireCategory(request.getId());
        if (Objects.equals(request.getId(), request.getParentId()))
        {
            throw new ServiceException("上级类目不能选择自己", HttpStatus.BAD_REQUEST);
        }
        MallCategory category = new MallCategory();
        category.setId(request.getId());
        copyRequest(request, category);
        category.setUpdateBy(SecurityUtils.getUsername());
        mallCategoryMapper.updateById(category);
    }

    @Override
    public void delete(Long id)
    {
        requireCategory(id);
        Long children = mallCategoryMapper.selectCount(new LambdaQueryWrapper<MallCategory>()
                .eq(MallCategory::getParentId, id));
        if (children != null && children > 0)
        {
            throw new ServiceException("存在子类目，不能删除", HttpStatus.BAD_REQUEST);
        }
        mallCategoryMapper.deleteById(id);
    }

    private LambdaQueryWrapper<MallCategory> queryWrapper(MallCategoryQuery query)
    {
        LambdaQueryWrapper<MallCategory> wrapper = new LambdaQueryWrapper<>();
        if (query != null)
        {
            if (query.getParentId() != null)
            {
                wrapper.eq(MallCategory::getParentId, query.getParentId());
            }
            if (StringUtils.hasText(query.getName()))
            {
                wrapper.like(MallCategory::getName, query.getName().trim());
            }
            if (StringUtils.hasText(query.getStatus()))
            {
                wrapper.eq(MallCategory::getStatus, query.getStatus().trim());
            }
        }
        wrapper.orderByAsc(MallCategory::getSort).orderByAsc(MallCategory::getId);
        return wrapper;
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

    private void copyRequest(MallCategorySaveRequest request, MallCategory category)
    {
        category.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        category.setName(request.getName());
        category.setSort(request.getSort() == null ? 0 : request.getSort());
        category.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : MallProductConstants.STATUS_NORMAL);
        category.setIcon(request.getIcon());
        category.setRemark(request.getRemark());
    }

    private MallCategoryTreeVO toTreeVO(MallCategory category)
    {
        MallCategoryTreeVO vo = new MallCategoryTreeVO();
        BeanUtils.copyProperties(category, vo);
        return vo;
    }
}
