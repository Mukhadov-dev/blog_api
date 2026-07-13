package com.example.BlogAPI.comment;

import com.example.BlogAPI.comment.dto.CommentaryRequest;
import com.example.BlogAPI.comment.dto.CommentaryResponse;
import com.example.BlogAPI.comment.dto.CommentaryUpdate;
import com.example.BlogAPI.kafka.events.CommentCreatedEvent;
import com.example.BlogAPI.kafka.events.CommentDeletedEvent;
import com.example.BlogAPI.kafka.events.CommentUpdatedEvent;
import com.example.BlogAPI.post.Post;
import com.example.BlogAPI.post.PostsRepository;
import com.example.BlogAPI.user.User;
import com.example.BlogAPI.user.UsersRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentariesService {
    private final CommentariesRepository commentariesRepository;
    private final PostsRepository postsRepository;
    private final UsersRepository usersRepository;
    private final ModelMapper modelMapper;
    private final ApplicationEventPublisher eventPublisher;

    public List<CommentaryResponse> getAllCommentariesByPost(Long postId) {
        log.info("Getting all commentaries by post id {}", postId);
        return commentariesRepository.findByPostId(postId).stream()
                .map(this::convertToCommentaryResponse)
                .collect(Collectors.toList());
    }

    public CommentaryResponse getCommentaryById(Long commentaryId) {
        Commentary commentary = commentariesRepository.findById(commentaryId)
                .orElseThrow(() -> new EntityNotFoundException("Commentary with id: " + commentaryId + " not found"));

        return convertToCommentaryResponse(commentary);
    }

    @Transactional
    public CommentaryResponse writeCommentary(Long postId, CommentaryRequest commentaryRequest, Authentication authentication) {
        log.info("Creating comment for post: {}", postId);

        if (commentaryRequest.getUser() == null || commentaryRequest.getUser().getUsername() == null) {
            throw new IllegalArgumentException("Comment author username must not be null");
        }

        Post post = postsRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post with id: " + postId + " not found"));

        User user = usersRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new EntityNotFoundException("User with name " + authentication.getName() + " not found"));

        Commentary commentary = convertToCommentary(commentaryRequest);
        commentary.setPost(post);
        commentary.setUser(user);

        Commentary savedCommentary = commentariesRepository.save(commentary);

        eventPublisher.publishEvent(buildCommentCreatedEvent(post, user, savedCommentary));
        log.info("Commentary created, event published: commentId={}", commentary.getId());

        return convertToCommentaryResponse(savedCommentary);
    }

    @Transactional
    public CommentaryResponse updateCommentary(Long commentaryId, CommentaryUpdate commentaryUpdate, Authentication authentication) {
        log.info("Updating comment: {}",  commentaryId);

        Commentary commentaryToUpdate = commentariesRepository.findById(commentaryId)
                .orElseThrow(() -> new EntityNotFoundException("Commentary with id: " + commentaryId + " not found"));

        if (!commentaryToUpdate.getUser().getUsername().equals(authentication.getName())) {
            throw new AccessDeniedException("You can only edit your own commentary");
        }

        commentaryToUpdate.setText(commentaryUpdate.getText());

        eventPublisher.publishEvent(buildCommentUpdatedEvent(commentaryToUpdate));
        log.info("Commentary updated, event published: commentId={}", commentaryId);

        return convertToCommentaryResponse(commentaryToUpdate);
    }

    @Transactional
    public void deleteCommentary(Long id, Authentication authentication) {
        Commentary commentary = commentariesRepository.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Commentary with id: " + id + " not found"));

        if (!commentary.getUser().getUsername().equals(authentication.getName())) {
            throw new AccessDeniedException("You can only delete your own commentary");
        }

        commentariesRepository.delete(commentary);
        eventPublisher.publishEvent(new CommentDeletedEvent(id));
        log.info("Commentary deleted, event published: commentId={}", id);
    }

    private CommentCreatedEvent buildCommentCreatedEvent(Post post, User user, Commentary commentary) {
        return new CommentCreatedEvent(
                commentary.getId(),
                commentary.getText(),
                post.getId(),
                post.getName(),
                user.getId(),
                user.getUsername(),
                commentary.getCreatedAt()
        );
    }

    private CommentUpdatedEvent buildCommentUpdatedEvent(Commentary commentary) {
        return new CommentUpdatedEvent(
                commentary.getId(),
                commentary.getText(),
                commentary.getPost().getId(),
                commentary.getPost().getName(),
                commentary.getUser().getId(),
                commentary.getUser().getUsername(),
                commentary.getUpdatedAt()
        );
    }

    private Commentary convertToCommentary(CommentaryRequest commentaryRequest) {
        return modelMapper.map(commentaryRequest, Commentary.class);
    }

    private CommentaryResponse convertToCommentaryResponse(Commentary commentary) {
        return modelMapper.map(commentary, CommentaryResponse.class);
    }
}
