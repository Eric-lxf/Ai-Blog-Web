package com.ruoyi.wechat.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WechatCryptUtilTest
{
    private static final String ENCODING_AES_KEY = "abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG";
    private static final String TOKEN = "pamtest";
    private static final String APP_ID = "wxb11529c136998cb6";
    private static final String PLAIN_TEXT = "\u6211\u662f\u4e2d\u6587abcd123";
    private static final String ENCRYPTED = "jn1L23DB+6ELqJ+6bruv21Y6MD7KeIfP82D6gU39rmkgczbWwt5+3bnyg5K55bgVtVzd832WzZGMhkP72vVOfg==";

    @Test
    void decrypt_should_restore_official_sample()
    {
        WechatCryptUtil cryptUtil = new WechatCryptUtil(TOKEN, ENCODING_AES_KEY, APP_ID);
        assertEquals(PLAIN_TEXT, cryptUtil.decrypt(ENCRYPTED));
    }

    @Test
    void encrypt_should_roundtrip()
    {
        WechatCryptUtil cryptUtil = new WechatCryptUtil(TOKEN, ENCODING_AES_KEY, APP_ID);
        String encrypted = cryptUtil.encrypt("aaaabbbbccccdddd", PLAIN_TEXT);
        assertEquals(PLAIN_TEXT, cryptUtil.decrypt(encrypted));
    }

    @Test
    void encryptXml_should_contain_encrypt_node()
    {
        WechatCryptUtil cryptUtil = new WechatCryptUtil(TOKEN, ENCODING_AES_KEY, APP_ID);
        String xml = cryptUtil.encrypt(buildTextReply("openid", "gh_test", "hello"));
        assertTrue(xml.contains("<Encrypt><![CDATA["));
        assertTrue(xml.contains("<MsgSignature><![CDATA["));
    }

    private String buildTextReply(String toUser, String fromUser, String content)
    {
        return "<xml><ToUserName><![CDATA[" + toUser + "]]></ToUserName><FromUserName><![CDATA[" + fromUser
                + "]]></FromUserName><CreateTime>1407743423</CreateTime><MsgType><![CDATA[text]]></MsgType><Content><![CDATA["
                + content + "]]></Content></xml>";
    }
}
