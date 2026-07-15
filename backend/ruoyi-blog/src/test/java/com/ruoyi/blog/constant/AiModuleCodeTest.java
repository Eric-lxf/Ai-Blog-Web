package com.ruoyi.blog.constant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AiModuleCodeTest
{
    @Test
    void acceptsOnlyTheSixFeatureModules()
    {
        assertTrue(AiModuleCode.isSupported(AiModuleCode.EDITOR));
        assertTrue(AiModuleCode.isSupported(AiModuleCode.BILL_ADVICE));
        assertFalse(AiModuleCode.isSupported("chat"));
        assertFalse(AiModuleCode.isSupported(null));
    }
}
