package com.pmh.disosang.review.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewRequest {
    private long storeId; // 어느 가게인지
    private int rating;          // 몇 점인지
    private String content;
}
