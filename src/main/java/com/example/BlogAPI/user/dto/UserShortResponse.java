package com.example.BlogAPI.user.dto;

import com.example.BlogAPI.comment.dto.CommentaryRequest;
import com.example.BlogAPI.post.dto.PostRequest;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserShortResponse {
    private Long id;
    private String username;
    private String email;
}
