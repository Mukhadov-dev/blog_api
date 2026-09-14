package com.example.BlogAPI.post;

import com.example.BlogAPI.comment.Commentary;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostsServiceTest {

    @Mock
    private PostsRepository postsRepository;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PostStatsService postStatsService;

    @Mock
    private Authentication authentication;

    private PostsService postsService;

    private User currentUser;
    private Post post;

    @BeforeEach
    void setUp() {
        postsService = new PostsService(
                postsRepository,
                usersRepository,
                modelMapper,
                eventPublisher,
                postStatsService
        );

        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("john");

        post = new Post();
        post.setId(10L);
        post.setName("Title");
        post.setContent("Content");
        post.setUser(currentUser);
        post.setComments(Collections.emptyList());
    }

    @Nested
    class GetAllPosts {

        @Test
        void returnsMappedPostsWhenRepositoryHasData() {
            Post secondPost = new Post();
            secondPost.setId(20L);
            when(postsRepository.findAll()).thenReturn(List.of(post, secondPost));

            PostResponse response1 = new PostResponse();
            response1.setId(10L);
            PostResponse response2 = new PostResponse();
            response2.setId(20L);

            when(modelMapper.map(post, PostResponse.class)).thenReturn(response1);
            when(modelMapper.map(secondPost, PostResponse.class)).thenReturn(response2);

            List<PostResponse> result = postsService.getAllPosts();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(PostResponse::getId).containsExactly(10L, 20L);
            verify(postsRepository).findAll();
        }

        @Test
        void returnsEmptyListWhenNoPostsExist() {
            when(postsRepository.findAll()).thenReturn(Collections.emptyList());

            List<PostResponse> result = postsService.getAllPosts();

            assertThat(result).isEmpty();
            verifyNoInteractions(modelMapper);
        }
    }


    @Nested
    class GetPostByIdWithComments {

        @Test
        void returnsPostWithUpdatedViewsAndLikes() {
            when(postsRepository.findById(10L)).thenReturn(Optional.of(post));
            when(postStatsService.incrementViews(10L)).thenReturn(5L);
            when(postStatsService.getLikes(10L)).thenReturn(3L);

            PostResponse mapped = new PostResponse();
            mapped.setId(10L);
            when(modelMapper.map(post, PostResponse.class)).thenReturn(mapped);

            PostResponse result = postsService.getPostByIdWithComments(10L);

            assertThat(result.getId()).isEqualTo(10L);
            assertThat(post.getViewsCount()).isEqualTo(5L);
            assertThat(post.getLikesCount()).isEqualTo(3L);
            verify(postStatsService).incrementViews(10L);
            verify(postStatsService).getLikes(10L);
        }

        @Test
        void mapsCommentsOntoResponse() {
            Commentary comment = new Commentary();
            post.setComments(List.of(comment));

            when(postsRepository.findById(10L)).thenReturn(Optional.of(post));
            when(postStatsService.incrementViews(10L)).thenReturn(0L);
            when(postStatsService.getLikes(10L)).thenReturn(0L);

            PostResponse mapped = new PostResponse();
            when(modelMapper.map(post, PostResponse.class)).thenReturn(mapped);

            CommentaryRequest commentaryRequest = new CommentaryRequest();
            when(modelMapper.map(comment, CommentaryRequest.class)).thenReturn(commentaryRequest);

            PostResponse result = postsService.getPostByIdWithComments(10L);

            assertThat(result.getCommentaryRequests()).containsExactly(commentaryRequest);
        }

        @Test
        void throwsWhenPostNotFound() {
            when(postsRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postsService.getPostByIdWithComments(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("999");

            verifyNoInteractions(postStatsService);
        }
    }


    @Nested
    class WritePost {

        @Test
        void createsPostAndPublishesEvent() {
            PostRequest request = new PostRequest();
            request.setName("New post");
            request.setContent("Some content");

            when(authentication.getName()).thenReturn("john");
            when(usersRepository.findByUsername("john")).thenReturn(Optional.of(currentUser));

            Post mappedPost = new Post();
            when(modelMapper.map(request, Post.class)).thenReturn(mappedPost);

            Post savedPost = new Post();
            savedPost.setId(100L);
            savedPost.setUser(currentUser);
            savedPost.setName("New post");
            savedPost.setContent("Some content");
            savedPost.setCreatedAt(LocalDateTime.now());
            when(postsRepository.save(mappedPost)).thenReturn(savedPost);

            PostResponse mappedResponse = new PostResponse();
            mappedResponse.setId(100L);
            when(modelMapper.map(savedPost, PostResponse.class)).thenReturn(mappedResponse);

            PostResponse result = postsService.writePost(request, authentication);

            assertThat(result.getId()).isEqualTo(100L);
            assertThat(mappedPost.getUser()).isEqualTo(currentUser);

            ArgumentCaptor<PostCreatedEvent> eventCaptor = ArgumentCaptor.forClass(PostCreatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            PostCreatedEvent publishedEvent = eventCaptor.getValue();
            assertThat(publishedEvent.getPostId()).isEqualTo(100L);
            assertThat(publishedEvent.getUserId()).isEqualTo(currentUser.getId());
            assertThat(publishedEvent.getUsername()).isEqualTo("john");
        }

        @Test
        void throwsWhenUserNotFound() {
            PostRequest request = new PostRequest();
            when(authentication.getName()).thenReturn("ghost");
            when(usersRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postsService.writePost(request, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("ghost");

            verifyNoInteractions(postsRepository);
            verifyNoInteractions(eventPublisher);
        }
    }


    @Nested
    class UpdatePost {

        @Test
        void updatesOwnPostAndPublishesEvent() {
            PostUpdate update = new PostUpdate();
            update.setName("Updated title");
            update.setContent("Updated content");

            when(postsRepository.findById(10L)).thenReturn(Optional.of(post));
            when(authentication.getName()).thenReturn("john");

            PostResponse mappedResponse = new PostResponse();
            mappedResponse.setId(10L);
            when(modelMapper.map(post, PostResponse.class)).thenReturn(mappedResponse);

            PostResponse result = postsService.updatePost(10L, update, authentication);

            assertThat(post.getName()).isEqualTo("Updated title");
            assertThat(post.getContent()).isEqualTo("Updated content");
            assertThat(result.getId()).isEqualTo(10L);

            verify(eventPublisher).publishEvent(any(PostUpdatedEvent.class));
        }

        @Test
        void throwsWhenPostNotFound() {
            PostUpdate update = new PostUpdate();
            when(postsRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postsService.updatePost(999L, update, authentication))
                    .isInstanceOf(EntityNotFoundException.class);

            verifyNoInteractions(eventPublisher);
        }

        @Test
        void throwsAccessDeniedWhenNotOwner() {
            PostUpdate update = new PostUpdate();
            when(postsRepository.findById(10L)).thenReturn(Optional.of(post));
            when(authentication.getName()).thenReturn("someone-else");

            assertThatThrownBy(() -> postsService.updatePost(10L, update, authentication))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("your own posts");

            verifyNoInteractions(eventPublisher);
            // post fields should remain unchanged
            assertThat(post.getName()).isEqualTo("Title");
        }
    }


    @Nested
    class DeletePost {

        @Test
        void deletesOwnPostAndPublishesEvent() {
            when(postsRepository.findById(10L)).thenReturn(Optional.of(post));
            when(authentication.getName()).thenReturn("john");

            postsService.deletePost(10L, authentication);

            verify(postsRepository).delete(post);

            ArgumentCaptor<PostDeletedEvent> eventCaptor = ArgumentCaptor.forClass(PostDeletedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().getPostId()).isEqualTo(10L);
        }

        @Test
        void throwsWhenPostNotFound() {
            when(postsRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> postsService.deletePost(999L, authentication))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(postsRepository, never()).delete(any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        void throwsAccessDeniedWhenNotOwner() {
            when(postsRepository.findById(10L)).thenReturn(Optional.of(post));
            when(authentication.getName()).thenReturn("someone-else");

            assertThatThrownBy(() -> postsService.deletePost(10L, authentication))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("your own posts");

            verify(postsRepository, never()).delete(any());
            verifyNoInteractions(eventPublisher);
        }
    }
}
