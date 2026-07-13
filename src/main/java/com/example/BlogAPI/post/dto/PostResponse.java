package com.example.BlogAPI.post.dto;

import com.example.BlogAPI.comment.dto.CommentaryRequest;
import com.example.BlogAPI.user.dto.UserRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PostResponse {
    private Long id;
    private UserRequest user;
    private String name;
    private String content;
    private List<CommentaryRequest> commentaryRequests;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
