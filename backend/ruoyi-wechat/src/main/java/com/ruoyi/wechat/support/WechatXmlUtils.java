package com.ruoyi.wechat.support;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public final class WechatXmlUtils
{
    private WechatXmlUtils()
    {
    }

    public static Document parseDocument(String xml)
    {
        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        }
        catch (Exception e)
        {
            throw new IllegalArgumentException("invalid xml", e);
        }
    }

    public static String readTag(String xml, String tag)
    {
        if (xml == null || xml.isBlank())
        {
            return "";
        }
        try
        {
            Document document = parseDocument(xml);
            NodeList list = document.getElementsByTagName(tag);
            if (list.getLength() == 0 || list.item(0) == null)
            {
                return "";
            }
            return list.item(0).getTextContent();
        }
        catch (Exception e)
        {
            return "";
        }
    }

    public static boolean containsEncryptNode(String xml)
    {
        return xml != null && xml.contains("<Encrypt>");
    }
}
