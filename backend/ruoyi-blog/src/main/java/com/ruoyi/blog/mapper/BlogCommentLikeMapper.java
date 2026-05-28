package com.ruoyi.blog.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruoyi.blog.domain.BlogCommentLike;

@Mapper
public interface BlogCommentLikeMapper extends BaseMapper<BlogCommentLike>
{

    @Select("""
            SELECT id FROM blog_comment_like
            WHERE comment_id = #{commentId}
              AND ((user_id IS NOT NULL AND user_id = #{userId})
                OR (guest_key IS NOT NULL AND guest_key = #{guestKey}))
            LIMIT 1
            """)
    Long findLikeId(@Param("commentId") Long commentId, @Param("userId") Long userId, @Param("guestKey") String guestKey);
}
