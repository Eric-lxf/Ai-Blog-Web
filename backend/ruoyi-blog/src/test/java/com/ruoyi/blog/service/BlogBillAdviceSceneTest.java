package com.ruoyi.blog.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.blog.constant.AiModuleCode;
import com.ruoyi.blog.dto.AiCompletionRequest;
import com.ruoyi.blog.mapper.BlogBillMapper;
import com.ruoyi.blog.service.impl.BlogBillServiceImpl;
import com.ruoyi.blog.vo.BillCategoryAmountVO;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;

@ExtendWith(MockitoExtension.class)
class BlogBillAdviceSceneTest
{
    private static final Long USER_ID = 100L;

    @Mock
    private BlogBillMapper billMapper;

    @Mock
    private DeepSeekService deepSeekService;

    private BlogBillServiceImpl service;

    @BeforeEach
    void setUp()
    {
        service = new BlogBillServiceImpl(billMapper, deepSeekService, new ObjectMapper());

        SysUser sysUser = new SysUser();
        sysUser.setUserId(USER_ID);
        sysUser.setUserName("bill-tester");
        LoginUser loginUser = new LoginUser(USER_ID, 1L, sysUser, Set.of());
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(loginUser, null, List.of()));
    }

    @AfterEach
    void tearDown()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    void analysisUsesBillAdviceSceneAndModuleCode()
    {
        BillCategoryAmountVO category = new BillCategoryAmountVO();
        category.setName("餐饮食品");
        category.setValue(new BigDecimal("188.80"));

        when(billMapper.selectCategoryTotals(eq(USER_ID), any(), any())).thenReturn(List.of(category));
        when(billMapper.selectMonthlyTotals(eq(USER_ID), any(), any())).thenReturn(List.of());
        when(billMapper.selectCount(any())).thenReturn(1L);
        when(deepSeekService.chatCompletion(any(AiCompletionRequest.class), eq(AiModuleCode.BILL_ADVICE)))
                .thenReturn("[]");

        service.analysis(6);

        verify(deepSeekService).chatCompletion(
                argThat((AiCompletionRequest request) -> "BILL_ADVICE".equals(request.getScene())),
                eq(AiModuleCode.BILL_ADVICE));
    }
}
