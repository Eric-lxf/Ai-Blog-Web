package com.ruoyi.blog.constant;

public final class BlogCommentConstants
{
    private BlogCommentConstants()
    {
    }

    public static final int STATUS_PENDING = 0;
    public static final int STATUS_APPROVED = 1;
    public static final int STATUS_REJECTED = 2;
    public static final int STATUS_HIDDEN = 3;
    public static final int STATUS_SPAM = 4;

    public static final int AI_NOT_CHECKED = 0;
    public static final int AI_CHECKING = 1;
    public static final int AI_PASS = 2;
    public static final int AI_SUSPICIOUS = 3;
    public static final int AI_HIGH_RISK = 4;

    public static final int REPORT_PENDING = 0;
    public static final int REPORT_HANDLED = 1;

    public static final String ACTION_BLOCK = "block";
    public static final String ACTION_REPLACE = "replace";
    public static final String ACTION_REVIEW = "review";

    public static final String SORT_NEW = "new";
    public static final String SORT_HOT = "hot";
}
