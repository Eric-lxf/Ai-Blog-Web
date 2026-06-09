package com.ruoyi.wechat.support;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import com.ruoyi.common.exception.ServiceException;

/**
 * ????????????????????/?????????
 */
public final class WechatCryptUtil
{
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String BASE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private final byte[] aesKey;
    private final String token;
    private final String appId;

    public WechatCryptUtil(String token, String encodingAesKey, String appId)
    {
        this.token = token;
        this.appId = appId;
        String normalizedKey = encodingAesKey == null ? "" : encodingAesKey.replace(" ", "");
        this.aesKey = Base64.getDecoder().decode(normalizedKey);
    }

    public String decryptXml(String msgSignature, String timestamp, String nonce, String encryptedXml)
    {
        String cipherText = extractEncryptPart(encryptedXml);
        return decryptContent(msgSignature, timestamp, nonce, cipherText);
    }

    public String decryptContent(String msgSignature, String timestamp, String nonce, String encryptedContent)
    {
        String signature = WechatSignatureUtils.signMessage(token, timestamp, nonce, encryptedContent);
        if (!signature.equalsIgnoreCase(msgSignature))
        {
            throw new ServiceException("msg_signature verification failed");
        }
        return decrypt(encryptedContent);
    }

    public String encrypt(String plainText)
    {
        String encrypted = encrypt(randomStr(16), plainText);
        String timeStamp = Long.toString(System.currentTimeMillis() / 1000L);
        String nonce = randomStr(16);
        String signature = WechatSignatureUtils.signMessage(token, timeStamp, nonce, encrypted);
        return "<xml>\n" + "<Encrypt><![CDATA[" + encrypted + "]]></Encrypt>\n"
                + "<MsgSignature><![CDATA[" + signature + "]]></MsgSignature>\n"
                + "<TimeStamp>" + timeStamp + "</TimeStamp>\n"
                + "<Nonce><![CDATA[" + nonce + "]]></Nonce>\n" + "</xml>";
    }

    String decrypt(String cipherText)
    {
        try
        {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
            IvParameterSpec iv = new IvParameterSpec(Arrays.copyOfRange(aesKey, 0, 16));
            cipher.init(Cipher.DECRYPT_MODE, keySpec, iv);
            byte[] original = cipher.doFinal(Base64.getDecoder().decode(cipherText));
            byte[] bytes = pkcs7Decode(original);
            if (bytes.length < 20)
            {
                throw new ServiceException("invalid decrypted payload");
            }
            int xmlLength = bytesNetworkOrder2Number(Arrays.copyOfRange(bytes, 16, 20));
            int startIndex = 20;
            int endIndex = startIndex + xmlLength;
            if (xmlLength < 0 || endIndex > bytes.length)
            {
                throw new ServiceException("invalid decrypted message length");
            }
            return new String(Arrays.copyOfRange(bytes, startIndex, endIndex), StandardCharsets.UTF_8);
        }
        catch (ServiceException e)
        {
            throw e;
        }
        catch (Exception e)
        {
            throw new ServiceException("message decrypt failed: " + e.getMessage());
        }
    }

    String encrypt(String randomStr, String plainText)
    {
        ByteGroup byteCollector = new ByteGroup();
        byte[] randomStringBytes = randomStr.getBytes(StandardCharsets.UTF_8);
        byte[] plainTextBytes = plainText.getBytes(StandardCharsets.UTF_8);
        byte[] bytesOfSizeInNetworkOrder = number2BytesInNetworkOrder(plainTextBytes.length);
        byte[] appIdBytes = appId.getBytes(StandardCharsets.UTF_8);
        byteCollector.addBytes(randomStringBytes);
        byteCollector.addBytes(bytesOfSizeInNetworkOrder);
        byteCollector.addBytes(plainTextBytes);
        byteCollector.addBytes(appIdBytes);
        byteCollector.addBytes(pkcs7Encode(byteCollector.size()));
        try
        {
            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(aesKey, "AES");
            IvParameterSpec iv = new IvParameterSpec(aesKey, 0, 16);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, iv);
            return Base64.getEncoder().encodeToString(cipher.doFinal(byteCollector.toBytes()));
        }
        catch (Exception e)
        {
            throw new ServiceException("message encrypt failed: " + e.getMessage());
        }
    }

    private static String extractEncryptPart(String xml)
    {
        try
        {
            Document document = WechatXmlUtils.parseDocument(xml);
            Element root = document.getDocumentElement();
            return root.getElementsByTagName("Encrypt").item(0).getTextContent();
        }
        catch (Exception e)
        {
            throw new ServiceException("missing Encrypt node in callback body");
        }
    }

    private static byte[] number2BytesInNetworkOrder(int number)
    {
        return new byte[] {
                (byte) (number >> 24 & 0xFF),
                (byte) (number >> 16 & 0xFF),
                (byte) (number >> 8 & 0xFF),
                (byte) (number & 0xFF)
        };
    }

    private static int bytesNetworkOrder2Number(byte[] bytesInNetworkOrder)
    {
        int sourceNumber = 0;
        for (byte value : bytesInNetworkOrder)
        {
            sourceNumber <<= 8;
            sourceNumber |= value & 0xff;
        }
        return sourceNumber;
    }

    private static byte[] pkcs7Encode(int count)
    {
        int amountToPad = 32 - (count % 32);
        byte[] padBytes = new byte[amountToPad];
        Arrays.fill(padBytes, (byte) amountToPad);
        return padBytes;
    }

    private static byte[] pkcs7Decode(byte[] decrypted)
    {
        int pad = decrypted[decrypted.length - 1];
        if (pad < 1 || pad > 32)
        {
            pad = 0;
        }
        return Arrays.copyOfRange(decrypted, 0, decrypted.length - pad);
    }

    private static String randomStr(int length)
    {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++)
        {
            sb.append(BASE_CHARS.charAt(RANDOM.nextInt(BASE_CHARS.length())));
        }
        return sb.toString();
    }

    private static final class ByteGroup
    {
        private byte[] bytes = new byte[0];

        private ByteGroup addBytes(byte[] data)
        {
            byte[] merged = Arrays.copyOf(bytes, bytes.length + data.length);
            System.arraycopy(data, 0, merged, bytes.length, data.length);
            bytes = merged;
            return this;
        }

        private int size()
        {
            return bytes.length;
        }

        private byte[] toBytes()
        {
            return bytes;
        }
    }
}
