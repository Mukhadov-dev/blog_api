package com.example.BlogAPI.post;

import com.example.BlogAPI.post.dto.PostRequest;
import com.example.BlogAPI.post.dto.PostResponse;
import com.example.BlogAPI.post.dto.PostUpdate;
import com.example.BlogAPI.user.UsersService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts")
public class PostsController {

    private final PostsService postsService;
    private final PostStatsService postStatsService;

    @GetMapping()
    public ResponseEntity<List<PostResponse>> getAllPosts() {
        return ResponseEntity.ok(postsService.getAllPosts());
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable Long postId) {
        return ResponseEntity.ok(postsService.getPostByIdWithComments(postId));
    }

    @PostMapping()
    public ResponseEntity<Object> createPost(@Valid @RequestBody PostRequest postRequest, Authentication authentication, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return new  ResponseEntity<>(bindingResult.getAllErrors(), HttpStatus.BAD_REQUEST);
        }

        PostResponse createdPost = postsService.writePost(postRequest, authentication);

        return new ResponseEntity<>(createdPost, HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Object> updatePost(@PathVariable Long id, @Valid @RequestBody PostUpdate postUpdate,
                                             Authentication authentication, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return new ResponseEntity<>("Post ID is invalid", HttpStatus.BAD_REQUEST);
        }

        PostResponse updatedPost = postsService.updatePost(id, postUpdate, authentication);

        return ResponseEntity.ok(updatedPost);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id, Authentication authentication) {
        postsService.deletePost(id, authentication);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Map<String, Object>> likePost(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(postStatsService.like(id, authentication));
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<Map<String, Object>> unlikePost(
            @PathVariable Long id,
            Authentication authentication) {
        return ResponseEntity.ok(postStatsService.unlike(id, authentication));
    }
}
