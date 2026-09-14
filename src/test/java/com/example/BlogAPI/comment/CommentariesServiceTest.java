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
import com.example.BlogAPI.user.dto.UserRequest;
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

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class CommentariesServiceTest {

    @Mock
    private CommentariesRepository commentariesRepository;

    @Mock
    private PostsRepository postsRepository;

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private Authentication authentication;

    private CommentariesService commentariesService;

    private User currentUser;
    private Post post;
    private Commentary commentary;

    @BeforeEach
    void setUp() {
        commentariesService = new CommentariesService(
                commentariesRepository,
                postsRepository,
                usersRepository,
                modelMapper,
                eventPublisher
        );

        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("john");

        post = new Post();
        post.setId(10L);
        post.setName("Post title");
        post.setUser(currentUser);

        commentary = new Commentary();
        commentary.setId(100L);
        commentary.setText("Nice post!");
        commentary.setPost(post);
        commentary.setUser(currentUser);
    }

    @Nested
    class GetAllCommentariesByPost {

        @Test
        void returnsMappedCommentariesForPost() {
            Commentary secondCommentary = new Commentary();
            secondCommentary.setId(200L);

            when(commentariesRepository.findByPostId(10L))
                    .thenReturn(List.of(commentary, secondCommentary));

            CommentaryResponse response1 = new CommentaryResponse();
            response1.setId(100L);
            CommentaryResponse response2 = new CommentaryResponse();
            response2.setId(200L);

            when(modelMapper.map(commentary, CommentaryResponse.class)).thenReturn(response1);
            when(modelMapper.map(secondCommentary, CommentaryResponse.class)).thenReturn(response2);

            List<CommentaryResponse> result = commentariesService.getAllCommentariesByPost(10L);

            assertThat(result).hasSize(2);
            assertThat(result).extracting(CommentaryResponse::getId).containsExactly(100L, 200L);
            verify(commentariesRepository).findByPostId(10L);
        }

        @Test
        void returnsEmptyListWhenNoCommentariesExist() {
            when(commentariesRepository.findByPostId(10L)).thenReturn(Collections.emptyList());

            List<CommentaryResponse> result = commentariesService.getAllCommentariesByPost(10L);

            assertThat(result).isEmpty();
            verifyNoInteractions(modelMapper);
        }
    }

    @Nested
    class GetCommentaryById {

        @Test
        void returnsMappedCommentary() {
            when(commentariesRepository.findById(100L)).thenReturn(Optional.of(commentary));

            CommentaryResponse mapped = new CommentaryResponse();
            mapped.setId(100L);
            when(modelMapper.map(commentary, CommentaryResponse.class)).thenReturn(mapped);

            CommentaryResponse result = commentariesService.getCommentaryById(100L);

            assertThat(result.getId()).isEqualTo(100L);
        }

        @Test
        void throwsWhenCommentaryNotFound() {
            when(commentariesRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentariesService.getCommentaryById(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    class WriteCommentary {

        @Test
        void createsCommentaryAndPublishesEvent() {
            UserRequest requestUser = new UserRequest();
            requestUser.setUsername("john");

            CommentaryRequest request = new CommentaryRequest();
            request.setText("Great article!");
            request.setUser(requestUser);

            when(postsRepository.findById(10L)).thenReturn(Optional.of(post));
            when(authentication.getName()).thenReturn("john");
            when(usersRepository.findByUsername("john")).thenReturn(Optional.of(currentUser));

            Commentary mappedCommentary = new Commentary();
            when(modelMapper.map(request, Commentary.class)).thenReturn(mappedCommentary);

            Commentary savedCommentary = new Commentary();
            savedCommentary.setId(500L);
            savedCommentary.setText("Great article!");
            savedCommentary.setPost(post);
            savedCommentary.setUser(currentUser);
            savedCommentary.setCreatedAt(LocalDateTime.now());
            when(commentariesRepository.save(mappedCommentary)).thenReturn(savedCommentary);

            CommentaryResponse mappedResponse = new CommentaryResponse();
            mappedResponse.setId(500L);
            when(modelMapper.map(savedCommentary, CommentaryResponse.class)).thenReturn(mappedResponse);

            CommentaryResponse result = commentariesService.writeCommentary(10L, request, authentication);

            assertThat(result.getId()).isEqualTo(500L);
            assertThat(mappedCommentary.getPost()).isEqualTo(post);
            assertThat(mappedCommentary.getUser()).isEqualTo(currentUser);

            ArgumentCaptor<CommentCreatedEvent> eventCaptor = ArgumentCaptor.forClass(CommentCreatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            CommentCreatedEvent publishedEvent = eventCaptor.getValue();
            assertThat(publishedEvent.getPostId()).isEqualTo(10L);
            assertThat(publishedEvent.getUserId()).isEqualTo(currentUser.getId());
            assertThat(publishedEvent.getUsername()).isEqualTo("john");
        }

        @Test
        void throwsWhenRequestUserIsNull() {
            CommentaryRequest request = new CommentaryRequest();
            request.setUser(null);

            assertThatThrownBy(() -> commentariesService.writeCommentary(10L, request, authentication))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("username must not be null");

            verifyNoInteractions(postsRepository);
            verifyNoInteractions(commentariesRepository);
            verifyNoInteractions(eventPublisher);
        }

        @Test
        void throwsWhenRequestUsernameIsNull() {
            UserRequest requestUser = new UserRequest();
            requestUser.setUsername(null);

            CommentaryRequest request = new CommentaryRequest();
            request.setUser(requestUser);

            assertThatThrownBy(() -> commentariesService.writeCommentary(10L, request, authentication))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("username must not be null");

            verifyNoInteractions(postsRepository);
            verifyNoInteractions(commentariesRepository);
            verifyNoInteractions(eventPublisher);
        }

        @Test
        void throwsWhenPostNotFound() {
            UserRequest requestUser = new UserRequest();
            requestUser.setUsername("john");

            CommentaryRequest request = new CommentaryRequest();
            request.setUser(requestUser);

            when(postsRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentariesService.writeCommentary(999L, request, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("999");

            verifyNoInteractions(usersRepository);
            verifyNoInteractions(commentariesRepository);
            verifyNoInteractions(eventPublisher);
        }

        @Test
        void throwsWhenAuthenticatedUserNotFound() {
            UserRequest requestUser = new UserRequest();
            requestUser.setUsername("john");

            CommentaryRequest request = new CommentaryRequest();
            request.setUser(requestUser);

            when(postsRepository.findById(10L)).thenReturn(Optional.of(post));
            when(authentication.getName()).thenReturn("ghost");
            when(usersRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentariesService.writeCommentary(10L, request, authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("ghost");

            verifyNoInteractions(commentariesRepository);
            verifyNoInteractions(eventPublisher);
        }
    }

    @Nested
    class UpdateCommentary {

        @Test
        void updatesOwnCommentaryAndPublishesEvent() {
            CommentaryUpdate update = new CommentaryUpdate();
            update.setText("Updated text");

            when(commentariesRepository.findById(100L)).thenReturn(Optional.of(commentary));
            when(authentication.getName()).thenReturn("john");

            CommentaryResponse mappedResponse = new CommentaryResponse();
            mappedResponse.setId(100L);
            when(modelMapper.map(commentary, CommentaryResponse.class)).thenReturn(mappedResponse);

            CommentaryResponse result = commentariesService.updateCommentary(100L, update, authentication);

            assertThat(commentary.getText()).isEqualTo("Updated text");
            assertThat(result.getId()).isEqualTo(100L);

            verify(eventPublisher).publishEvent(any(CommentUpdatedEvent.class));
        }

        @Test
        void throwsWhenCommentaryNotFound() {
            CommentaryUpdate update = new CommentaryUpdate();

            when(commentariesRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentariesService.updateCommentary(999L, update, authentication))
                    .isInstanceOf(EntityNotFoundException.class);

            verifyNoInteractions(eventPublisher);
        }

        @Test
        void throwsAccessDeniedWhenNotOwner() {
            CommentaryUpdate update = new CommentaryUpdate();
            update.setText("Hacked text");

            when(commentariesRepository.findById(100L)).thenReturn(Optional.of(commentary));
            when(authentication.getName()).thenReturn("someone-else");

            assertThatThrownBy(() -> commentariesService.updateCommentary(100L, update, authentication))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("your own commentary");

            verifyNoInteractions(eventPublisher);
            assertThat(commentary.getText()).isEqualTo("Nice post!");
        }
    }

    @Nested
    class DeleteCommentary {

        @Test
        void deletesOwnCommentaryAndPublishesEvent() {
            when(commentariesRepository.findById(100L)).thenReturn(Optional.of(commentary));
            when(authentication.getName()).thenReturn("john");

            commentariesService.deleteCommentary(100L, authentication);

            verify(commentariesRepository).delete(commentary);

            ArgumentCaptor<CommentDeletedEvent> eventCaptor = ArgumentCaptor.forClass(CommentDeletedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().getCommentId()).isEqualTo(100L);
        }

        @Test
        void throwsWhenCommentaryNotFound() {
            when(commentariesRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> commentariesService.deleteCommentary(999L, authentication))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(commentariesRepository, never()).delete(any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        void throwsAccessDeniedWhenNotOwner() {
            when(commentariesRepository.findById(100L)).thenReturn(Optional.of(commentary));
            when(authentication.getName()).thenReturn("someone-else");

            assertThatThrownBy(() -> commentariesService.deleteCommentary(100L, authentication))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("your own commentary");

            verify(commentariesRepository, never()).delete(any());
            verifyNoInteractions(eventPublisher);
        }
    }
}
