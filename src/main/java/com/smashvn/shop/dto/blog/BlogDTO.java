package com.smashvn.shop.dto.blog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlogDTO {
    private Integer id;
    private String title;
    private String slug;
    private String summary;
    private String content;
    private String image;
    private String publishDate;
    private String author;
    private String category;
    private List<String> tags;
    private Integer commentsCount;
}
