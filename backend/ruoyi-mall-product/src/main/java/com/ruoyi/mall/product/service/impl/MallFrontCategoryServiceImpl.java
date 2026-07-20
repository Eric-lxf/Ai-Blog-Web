package com.ruoyi.mall.product.service.impl;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.mall.product.constant.MallProductConstants;
import com.ruoyi.mall.product.domain.MallCategory;
import com.ruoyi.mall.product.domain.MallFrontCategory;
import com.ruoyi.mall.product.domain.MallFrontCategoryRel;
import com.ruoyi.mall.product.dto.MallFrontCategoryQuery;
import com.ruoyi.mall.product.dto.MallFrontCategorySaveRequest;
import com.ruoyi.mall.product.mapper.MallCategoryMapper;
import com.ruoyi.mall.product.mapper.MallFrontCategoryMapper;
import com.ruoyi.mall.product.mapper.MallFrontCategoryRelMapper;
import com.ruoyi.mall.product.service.MallFrontCategoryService;
import com.ruoyi.mall.product.vo.MallFrontCategoryTreeVO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MallFrontCategoryServiceImpl implements MallFrontCategoryService
{
    private final MallFrontCategoryMapper mallFrontCategoryMapper;
    private final MallFrontCategoryRelMapper mallFrontCategoryRelMapper;
    private final MallCategoryMapper mallCategoryMapper;

    @Override
    public List<MallFrontCategory> list(MallFrontCategoryQuery query)
    {
        return mallFrontCategoryMapper.selectList(queryWrapper(query));
    }

    @Override
    public List<MallFrontCategoryTreeVO> tree(MallFrontCategoryQuery query)
    {
        List<MallFrontCategoryTreeVO> nodes = list(query).stream().map(this::toTreeVO).toList();
        Map<Long, List<MallFrontCategoryTreeVO>> childrenMap = nodes.stream()
                .collect(Collectors.groupingBy(node -> node.getParentId() == null ? 0L : node.getParentId()));
        nodes.forEach(node -> node.setChildren(childrenMap.getOrDefault(node.getId(), List.of())));
        return nodes.stream()
                .filter(node -> node.getParentId() == null || node.getParentId() == 0L)
                .sorted(Comparator.comparing(MallFrontCategoryTreeVO::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(MallFrontCategoryTreeVO::getId, Comparator.nullsLast(Long::compareTo)))
                .toList();
    }

    @Override
    public List<MallFrontCategoryTreeVO> listActiveTree()
    {
        MallFrontCategoryQuery query = new MallFrontCategoryQuery();
        query.setStatus(MallProductConstants.STATUS_NORMAL);
        return tree(query);
    }

    @Override
    public MallFrontCategory getById(Long id)
    {
        return requireFrontCategory(id);
    }

    @Override
    public Long create(MallFrontCategorySaveRequest request)
    {
        MallFrontCategory category = new MallFrontCategory();
        copyRequest(request, category);
        category.setCreateBy(SecurityUtils.getUsername());
        mallFrontCategoryMapper.insert(category);
        return category.getId();
    }

    @Override
    public void update(MallFrontCategorySaveRequest request)
    {
        if (request.getId() == null)
        {
            throw new ServiceException("前台类目ID不能为空", HttpStatus.BAD_REQUEST);
        }
        requireFrontCategory(request.getId());
        if (Objects.equals(request.getId(), request.getParentId()))
        {
            throw new ServiceException("上级类目不能选择自己", HttpStatus.BAD_REQUEST);
        }
        MallFrontCategory category = new MallFrontCategory();
        category.setId(request.getId());
        copyRequest(request, category);
        category.setUpdateBy(SecurityUtils.getUsername());
        mallFrontCategoryMapper.updateById(category);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id)
    {
        requireFrontCategory(id);
        Long children = mallFrontCategoryMapper.selectCount(new LambdaQueryWrapper<MallFrontCategory>()
                .eq(MallFrontCategory::getParentId, id));
        if (children != null && children > 0)
        {
            throw new ServiceException("存在子类目，不能删除", HttpStatus.BAD_REQUEST);
        }
        mallFrontCategoryRelMapper.delete(new LambdaQueryWrapper<MallFrontCategoryRel>()
                .eq(MallFrontCategoryRel::getFrontId, id));
        mallFrontCategoryMapper.deleteById(id);
    }

    @Override
    public List<MallFrontCategoryRel> listRels(Long frontId)
    {
        requireFrontCategory(frontId);
        return mallFrontCategoryRelMapper.selectList(new LambdaQueryWrapper<MallFrontCategoryRel>()
                .eq(MallFrontCategoryRel::getFrontId, frontId)
                .orderByAsc(MallFrontCategoryRel::getId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void replaceRels(Long frontId, List<Long> backCategoryIds)
    {
        requireFrontCategory(frontId);
        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        if (!CollectionUtils.isEmpty(backCategoryIds))
        {
            for (Long backId : backCategoryIds)
            {
                if (backId != null)
                {
                    uniqueIds.add(backId);
                }
            }
        }
        for (Long backId : uniqueIds)
        {
            requireLeafBackCategory(backId);
        }
        mallFrontCategoryRelMapper.delete(new LambdaQueryWrapper<MallFrontCategoryRel>()
                .eq(MallFrontCategoryRel::getFrontId, frontId));
        for (Long backId : uniqueIds)
        {
            MallFrontCategoryRel rel = new MallFrontCategoryRel();
            rel.setFrontId(frontId);
            rel.setBackCategoryId(backId);
            mallFrontCategoryRelMapper.insert(rel);
        }
    }

    @Override
    public Set<Long> resolveBackCategoryIds(Long frontCategoryId)
    {
        requireFrontCategory(frontCategoryId);
        Set<Long> frontIds = collectSubtreeFrontIds(frontCategoryId);
        if (frontIds.isEmpty())
        {
            return Set.of();
        }
        List<MallFrontCategoryRel> rels = mallFrontCategoryRelMapper.selectList(
                new LambdaQueryWrapper<MallFrontCategoryRel>().in(MallFrontCategoryRel::getFrontId, frontIds));
        return rels.stream()
                .map(MallFrontCategoryRel::getBackCategoryId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<Long> collectSubtreeFrontIds(Long rootId)
    {
        List<MallFrontCategory> all = mallFrontCategoryMapper.selectList(new LambdaQueryWrapper<>());
        Map<Long, List<MallFrontCategory>> childrenMap = all.stream()
                .collect(Collectors.groupingBy(c -> c.getParentId() == null ? 0L : c.getParentId()));
        Set<Long> result = new HashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        queue.add(rootId);
        while (!queue.isEmpty())
        {
            Long id = queue.poll();
            if (!result.add(id))
            {
                continue;
            }
            List<MallFrontCategory> children = childrenMap.getOrDefault(id, List.of());
            for (MallFrontCategory child : children)
            {
                queue.add(child.getId());
            }
        }
        return result;
    }

    private void requireLeafBackCategory(Long categoryId)
    {
        MallCategory category = mallCategoryMapper.selectById(categoryId);
        if (category == null)
        {
            throw new ServiceException("后台类目不存在: " + categoryId, HttpStatus.BAD_REQUEST);
        }
        Long children = mallCategoryMapper.selectCount(new LambdaQueryWrapper<MallCategory>()
                .eq(MallCategory::getParentId, categoryId));
        if (children != null && children > 0)
        {
            throw new ServiceException("仅允许映射后台叶子类目", HttpStatus.BAD_REQUEST);
        }
    }

    private LambdaQueryWrapper<MallFrontCategory> queryWrapper(MallFrontCategoryQuery query)
    {
        LambdaQueryWrapper<MallFrontCategory> wrapper = new LambdaQueryWrapper<>();
        if (query != null)
        {
            if (query.getParentId() != null)
            {
                wrapper.eq(MallFrontCategory::getParentId, query.getParentId());
            }
            if (StringUtils.hasText(query.getName()))
            {
                wrapper.like(MallFrontCategory::getName, query.getName().trim());
            }
            if (StringUtils.hasText(query.getStatus()))
            {
                wrapper.eq(MallFrontCategory::getStatus, query.getStatus().trim());
            }
        }
        wrapper.orderByAsc(MallFrontCategory::getSort).orderByAsc(MallFrontCategory::getId);
        return wrapper;
    }

    private MallFrontCategory requireFrontCategory(Long id)
    {
        MallFrontCategory category = mallFrontCategoryMapper.selectById(id);
        if (category == null)
        {
            throw new ServiceException("前台类目不存在", HttpStatus.NOT_FOUND);
        }
        return category;
    }

    private void copyRequest(MallFrontCategorySaveRequest request, MallFrontCategory category)
    {
        category.setParentId(request.getParentId() == null ? 0L : request.getParentId());
        category.setName(request.getName());
        category.setSort(request.getSort() == null ? 0 : request.getSort());
        category.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : MallProductConstants.STATUS_NORMAL);
        category.setIcon(request.getIcon());
        category.setRemark(request.getRemark());
    }

    private MallFrontCategoryTreeVO toTreeVO(MallFrontCategory category)
    {
        MallFrontCategoryTreeVO vo = new MallFrontCategoryTreeVO();
        BeanUtils.copyProperties(category, vo);
        return vo;
    }
}
