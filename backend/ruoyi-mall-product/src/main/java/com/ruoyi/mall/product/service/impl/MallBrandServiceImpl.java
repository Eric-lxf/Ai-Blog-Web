package com.ruoyi.mall.product.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.mall.product.constant.MallProductConstants;
import com.ruoyi.mall.product.domain.MallBrand;
import com.ruoyi.mall.product.dto.MallBrandPageQuery;
import com.ruoyi.mall.product.dto.MallBrandSaveRequest;
import com.ruoyi.mall.product.mapper.MallBrandMapper;
import com.ruoyi.mall.product.service.MallBrandService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MallBrandServiceImpl implements MallBrandService
{
    private final MallBrandMapper mallBrandMapper;

    @Override
    public Page<MallBrand> page(MallBrandPageQuery query)
    {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null ? 10 : Math.min(query.getPageSize(), 100);
        LambdaQueryWrapper<MallBrand> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getName()))
        {
            wrapper.like(MallBrand::getName, query.getName().trim());
        }
        if (StringUtils.hasText(query.getStatus()))
        {
            wrapper.eq(MallBrand::getStatus, query.getStatus().trim());
        }
        wrapper.orderByAsc(MallBrand::getSort).orderByAsc(MallBrand::getId);
        return mallBrandMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
    }

    @Override
    public List<MallBrand> listActive()
    {
        return mallBrandMapper.selectList(new LambdaQueryWrapper<MallBrand>()
                .eq(MallBrand::getStatus, MallProductConstants.STATUS_NORMAL)
                .orderByAsc(MallBrand::getSort)
                .orderByAsc(MallBrand::getId));
    }

    @Override
    public MallBrand getById(Long id)
    {
        return requireBrand(id);
    }

    @Override
    public Long create(MallBrandSaveRequest request)
    {
        MallBrand brand = new MallBrand();
        copyRequest(request, brand);
        brand.setCreateBy(SecurityUtils.getUsername());
        mallBrandMapper.insert(brand);
        return brand.getId();
    }

    @Override
    public void update(MallBrandSaveRequest request)
    {
        if (request.getId() == null)
        {
            throw new ServiceException("品牌ID不能为空", HttpStatus.BAD_REQUEST);
        }
        requireBrand(request.getId());
        MallBrand brand = new MallBrand();
        brand.setId(request.getId());
        copyRequest(request, brand);
        brand.setUpdateBy(SecurityUtils.getUsername());
        mallBrandMapper.updateById(brand);
    }

    @Override
    public void delete(Long id)
    {
        requireBrand(id);
        mallBrandMapper.deleteById(id);
    }

    private MallBrand requireBrand(Long id)
    {
        MallBrand brand = mallBrandMapper.selectById(id);
        if (brand == null)
        {
            throw new ServiceException("品牌不存在", HttpStatus.NOT_FOUND);
        }
        return brand;
    }

    private void copyRequest(MallBrandSaveRequest request, MallBrand brand)
    {
        brand.setName(request.getName());
        brand.setLogo(request.getLogo());
        brand.setSort(request.getSort() == null ? 0 : request.getSort());
        brand.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus() : MallProductConstants.STATUS_NORMAL);
        brand.setRemark(request.getRemark());
    }
}
