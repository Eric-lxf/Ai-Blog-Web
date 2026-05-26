package com.ruoyi.blog.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Data;

@Data
public class OutlineNodeDTO
{

    private String id;
    private String title;
    private List<OutlineNodeDTO> children = new ArrayList<>();
}
