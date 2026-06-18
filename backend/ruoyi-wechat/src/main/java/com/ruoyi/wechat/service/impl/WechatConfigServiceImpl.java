package com.ruoyi.wechat.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.ruoyi.system.domain.SysConfig;
import com.ruoyi.system.service.ISysConfigService;
import com.ruoyi.wechat.config.WechatProperties;
import com.ruoyi.wechat.dto.WechatModuleConfigUpdateRequest;
import com.ruoyi.wechat.service.WechatConfigService;
import com.ruoyi.wechat.vo.WechatModuleConfigVO;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class WechatConfigServiceImpl implements WechatConfigService
{
    private static final String KEY_ENABLED = "wechat.enabled";
    private static final String KEY_DEFAULT_ACCOUNT = "wechat.defaultAccountId";
    private static final String KEY_CALLBACK_ENCRYPT = "wechat.callback.encrypt";

    private final ISysConfigService sysConfigService;
    private final WechatProperties wechatProperties;

    @Override
    public boolean isWechatEnabled()
    {
        String configured = sysConfigService.selectConfigByKey(KEY_ENABLED);
        if (configured == null || configured.isBlank())
        {
            return wechatProperties.isEnabled();
        }
        return Boolean.parseBoolean(configured);
    }

    @Override
    public WechatModuleConfigVO getModuleConfig()
    {
        WechatModuleConfigVO vo = new WechatModuleConfigVO();
        vo.setEnabled(isWechatEnabled());
        vo.setDefaultAccountId(getString(KEY_DEFAULT_ACCOUNT, ""));
        vo.setCallbackEncrypt(getBoolean(KEY_CALLBACK_ENCRYPT, false));
        return vo;
    }

    @Override
    public void updateModuleConfig(WechatModuleConfigUpdateRequest request)
    {
        upsertConfig(KEY_ENABLED, String.valueOf(request.getEnabled()), "微信公众号功能开关", "是否启用微信公众号模块");
        upsertConfig(KEY_DEFAULT_ACCOUNT, request.getDefaultAccountId() == null ? "" : request.getDefaultAccountId(),
                "微信公众号默认账号ID", "为空时需前端显式传 accountId");
        upsertConfig(KEY_CALLBACK_ENCRYPT, String.valueOf(request.getCallbackEncrypt()), "公众号回调密文模式",
                "true=兼容/安全模式，false=明文模式");
    }

    @Override
    public boolean getBoolean(String key, boolean defaultValue)
    {
        String value = sysConfigService.selectConfigByKey(key);
        if (value == null || value.isBlank())
        {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    @Override
    public String getString(String key, String defaultValue)
    {
        String value = sysConfigService.selectConfigByKey(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    private void upsertConfig(String key, String value, String name, String remark)
    {
        SysConfig query = new SysConfig();
        query.setConfigKey(key);
        List<SysConfig> configs = sysConfigService.selectConfigList(query);
        if (configs.isEmpty())
        {
            SysConfig config = new SysConfig();
            config.setConfigName(name);
            config.setConfigKey(key);
            config.setConfigValue(value);
            config.setConfigType("Y");
            config.setRemark(remark);
            sysConfigService.insertConfig(config);
            return;
        }
        SysConfig config = configs.get(0);
        config.setConfigValue(value);
        sysConfigService.updateConfig(config);
    }
}
