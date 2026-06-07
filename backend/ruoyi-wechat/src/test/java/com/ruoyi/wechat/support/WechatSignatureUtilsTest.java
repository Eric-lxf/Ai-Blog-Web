package com.ruoyi.wechat.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WechatSignatureUtilsTest
{
    @Test
    void sign_should_be_stable()
    {
        String signature = WechatSignatureUtils.sign("token", "1711111111", "12345");
        assertEquals("236c67b5c129c0e1da415bc8ccc0b1d6b5ec188e", signature);
    }

    @Test
    void verifySignature_should_check_signature()
    {
        String signature = WechatSignatureUtils.sign("token", "1711111111", "12345");
        assertTrue(WechatSignatureUtils.verifySignature("token", "1711111111", "12345", signature));
        assertFalse(WechatSignatureUtils.verifySignature("token", "1711111111", "wrong", signature));
    }
}
