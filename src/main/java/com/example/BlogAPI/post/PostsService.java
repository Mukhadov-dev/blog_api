package com.example.BlogAPI.post;

import com.example.BlogAPI.comment.dto.CommentaryRequest;
import com.example.BlogAPI.kafka.events.PostCreatedEvent;
import com.example.BlogAPI.kafka.events.PostDeletedEvent;
import com.example.BlogAPI.kafka.events.PostUpdatedEvent;
import com.example.BlogAPI.post.dto.PostRequest;
import com.example.BlogAPI.post.dto.PostResponse;
import com.example.BlogAPI.post.dto.PostUpdate;
import com.example.BlogAPI.user.User;
import com.example.BlogAPI.user.UsersRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostsService {

    private final PostsRepository postsRepository;
    private final UsersRepository usersRepository;
    private final ModelMapper modelMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final PostStatsService postStatsService;

    public List<PostResponse> getAllPosts() {
        log.info("Getting all posts");
        return postsRepository.findAll().stream()
                .map(this::convertToPostResponseWithoutComments)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "posts", key = "#id")
    public PostResponse getPostByIdWithComments(Long id) {
        Post post = postsRepository.findById(id)
                .orElseThrow(() ->
                        new EntityNotFoundException("Post with id " + id + " not found"));

        Long views = postStatsService.incrementViews(id);
        Long likes = postStatsService.getLikes(id);

        post.setViewsCount(views);
        post.setLikesCount(likes);

        return convertToPostResponseWithComments(post);
    }

    @Transactional
    public PostResponse writePost(PostRequest postRequest, Authentication authentication) {
        log.info("Creating new post: {}", postRequest.getName());

        User currentUser = usersRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new EntityNotFoundException("User with name " + authentication.getName() + " not found"));

        Post post = convertToPost(postRequest);
        post.setUser(currentUser);

        Post savedPost = postsRepository.save(post);

        eventPublisher.publishEvent(buildPostCreatedEvent(savedPost, currentUser));
        log.info("Post created, event published: postId={}", savedPost.getId());

        return convertToPostResponseWithoutComments(savedPost);
    }

    @CachePut(value = "posts", key = "#result.id")
    @Transactional
    public PostResponse updatePost(Long postId, PostUpdate postUpdate, Authentication authentication) {
        log.info("Updating post: {}", postId);

        Post postToUpdate = postsRepository.findById(postId)
                .orElseThrow(() ->
                        new EntityNotFoundException("Post not found"));

        if (!postToUpdate.getUser().getUsername().equals(authentication.getName())) {
            throw new AccessDeniedException("You can only edit your own posts");
        }

        postToUpdate.setName(postUpdate.getName());
        postToUpdate.setContent(postUpdate.getContent());

        eventPublisher.publishEvent(buildPostUpdatedEvent(postToUpdate));
        log.info("Post updated, event published: postId={}", postId);

        return convertToPostResponseWithComments(postToUpdate);
    }

    @CacheEvict(value = "posts", key = "#id")
    @Transactional
    public void deletePost(Long id, Authentication authentication) {
        Post post = postsRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Post with id " + id + " not found"));

        if (!post.getUser().getUsername().equals(authentication.getName())) {
            throw new AccessDeniedException("You can only delete your own posts");
        }

        postsRepository.delete(post);
        eventPublisher.publishEvent(new PostDeletedEvent(id));
        log.info("Post deleted, event published: postId={}", id);
    }

    private PostCreatedEvent buildPostCreatedEvent(Post post, User user) {
        return new PostCreatedEvent(
                post.getId(),
                user.getId(),
                user.getUsername(),
                post.getName(),
                post.getContent(),
                post.getCreatedAt()
        );
    }

    private PostUpdatedEvent buildPostUpdatedEvent(Post post) {
        return new PostUpdatedEvent(
                post.getId(),
                post.getName(),
                post.getContent(),
                post.getUser().getId(),
                post.getUpdatedAt()
        );
    }

    private Post convertToPost(PostRequest postRequest) {
        return modelMapper.map(postRequest, Post.class);
    }

    private PostResponse convertToPostResponseWithoutComments(Post post) {
        return modelMapper.map(post, PostResponse.class);
    }

    private PostResponse convertToPostResponseWithComments(Post post) {
        PostResponse response = modelMapper.map(post, PostResponse.class);

        List<CommentaryRequest> comments = post.getComments().stream()
                .map(c -> modelMapper.map(c, CommentaryRequest.class))
                .collect(Collectors.toList());

        response.setCommentaryRequests(comments);
        return response;
    }
}
