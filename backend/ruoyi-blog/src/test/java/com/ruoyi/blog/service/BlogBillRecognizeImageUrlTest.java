package com.ruoyi.blog.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.ruoyi.blog.service.impl.BlogBillServiceImpl;
import com.ruoyi.common.exception.ServiceException;

class BlogBillRecognizeImageUrlTest
{
    @Test
    void acceptsHttpAndDataImageUrls()
    {
        assertEquals("https://example.com/a.jpg",
                BlogBillServiceImpl.validateRecognizeImageUrl("https://example.com/a.jpg"));
        assertEquals("data:image/png;base64,abc",
                BlogBillServiceImpl.validateRecognizeImageUrl(" data:image/png;base64,abc "));
    }

    @Test
    void rejectsBrowserLocalBlobAndFileUrls()
    {
        assertThrows(ServiceException.class,
                () -> BlogBillServiceImpl.validateRecognizeImageUrl("blob:http://localhost/uuid"));
        assertThrows(ServiceException.class,
                () -> BlogBillServiceImpl.validateRecognizeImageUrl("file:///tmp/a.jpg"));
        assertThrows(ServiceException.class,
                () -> BlogBillServiceImpl.validateRecognizeImageUrl("ftp://example.com/a.jpg"));
    }
}
