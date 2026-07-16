package com.ruoyi.blog.mapper;

import java.time.LocalDate;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.blog.domain.BlogBill;
import com.ruoyi.blog.vo.BillCategoryAmountVO;
import com.ruoyi.blog.vo.BillMonthlyRow;

@Mapper
public interface BlogBillMapper extends BaseMapper<BlogBill>
{

    @Select("""
            SELECT category AS name, SUM(amount) AS value
            FROM blog_bill
            WHERE user_id = #{userId} AND is_deleted = 0
              AND bill_date >= #{startDate} AND bill_date <= #{endDate}
              AND (direction IS NULL OR direction = '' OR direction = '支出')
            GROUP BY category
            ORDER BY value DESC
            """)
    List<BillCategoryAmountVO> selectCategoryTotals(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Select("""
            SELECT DATE_FORMAT(bill_date, '%Y-%m') AS month,
                   category,
                   SUM(amount) AS amount
            FROM blog_bill
            WHERE user_id = #{userId} AND is_deleted = 0
              AND bill_date >= #{startDate} AND bill_date <= #{endDate}
              AND (direction IS NULL OR direction = '' OR direction = '支出')
            GROUP BY DATE_FORMAT(bill_date, '%Y-%m'), category
            ORDER BY month
            """)
    List<BillMonthlyRow> selectMonthlyTotals(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
