package com.camplog.profile.dto;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostPageResponse {
    private List<PostResponse> data;
    private int total;
    private String nextCursor;
    private boolean hasNextPage;
}
