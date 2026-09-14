package com.example.BlogAPI.user;

import com.example.BlogAPI.kafka.events.UserDeletedEvent;
import com.example.BlogAPI.kafka.events.UserRegisteredEvent;
import com.example.BlogAPI.kafka.events.UserUpdatedEvent;
import com.example.BlogAPI.user.dto.UserRequest;
import com.example.BlogAPI.user.dto.UserResponse;
import com.example.BlogAPI.user.dto.UserShortResponse;
import com.example.BlogAPI.user.dto.UserUpdate;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsersServiceTest {

    @Mock
    private UsersRepository usersRepository;

    @Mock
    private ModelMapper modelMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private Authentication authentication;

    private UsersService usersService;

    private User currentUser;

    @BeforeEach
    void setUp() {
        usersService = new UsersService(
                usersRepository,
                modelMapper,
                eventPublisher
        );

        currentUser = new User();
        currentUser.setId(1L);
        currentUser.setUsername("john");
    }


    @Nested
    class GetAllUsers {

        @Test
        void returnsMappedUsers() {
            User secondUser = new User();
            secondUser.setId(2L);

            when(usersRepository.findAll()).thenReturn(List.of(currentUser, secondUser));

            UserShortResponse response1 = new UserShortResponse();
            response1.setId(1L);
            UserShortResponse response2 = new UserShortResponse();
            response2.setId(2L);

            when(modelMapper.map(currentUser, UserShortResponse.class)).thenReturn(response1);
            when(modelMapper.map(secondUser, UserShortResponse.class)).thenReturn(response2);

            List<UserShortResponse> result = usersService.getAllUsers();

            assertThat(result).hasSize(2);
            assertThat(result).extracting(UserShortResponse::getId).containsExactly(1L, 2L);
            verify(usersRepository).findAll();
        }

        @Test
        void returnsEmptyListWhenNoUsersExist() {
            when(usersRepository.findAll()).thenReturn(Collections.emptyList());

            List<UserShortResponse> result = usersService.getAllUsers();

            assertThat(result).isEmpty();
            verifyNoInteractions(modelMapper);
        }
    }


    @Nested
    class GetUserById {

        @Test
        void returnsMappedUser() {
            when(usersRepository.findById(1L)).thenReturn(Optional.of(currentUser));

            UserResponse mapped = new UserResponse();
            mapped.setId(1L);
            when(modelMapper.map(currentUser, UserResponse.class)).thenReturn(mapped);

            UserResponse result = usersService.getUserById(1L);

            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        void throwsWhenUserNotFound() {
            when(usersRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usersService.getUserById(999L))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }


    @Nested
    class CreateUser {

        @Test
        void createsUserAndPublishesEvent() {
            UserRequest request = new UserRequest();
            request.setUsername("newuser");

            User mappedUser = new User();
            when(modelMapper.map(request, User.class)).thenReturn(mappedUser);

            User savedUser = new User();
            savedUser.setId(50L);
            savedUser.setUsername("newuser");
            savedUser.setEmail("newuser@example.com");
            savedUser.setCreatedAt(LocalDateTime.now());
            when(usersRepository.save(mappedUser)).thenReturn(savedUser);

            UserResponse mappedResponse = new UserResponse();
            mappedResponse.setId(50L);
            when(modelMapper.map(savedUser, UserResponse.class)).thenReturn(mappedResponse);

            UserResponse result = usersService.createUser(request);

            assertThat(result.getId()).isEqualTo(50L);

            ArgumentCaptor<UserRegisteredEvent> eventCaptor = ArgumentCaptor.forClass(UserRegisteredEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            UserRegisteredEvent publishedEvent = eventCaptor.getValue();
            assertThat(publishedEvent.getUserId()).isEqualTo(50L);
            assertThat(publishedEvent.getUsername()).isEqualTo("newuser");
        }
    }


    @Nested
    class UpdateUser {

        @Test
        void updatesOwnProfileAndPublishesEvent() {
            UserUpdate update = new UserUpdate();
            update.setUsername("john-updated");

            when(usersRepository.findById(1L)).thenReturn(Optional.of(currentUser));
            when(authentication.getName()).thenReturn("john");

            UserResponse mappedResponse = new UserResponse();
            mappedResponse.setId(1L);
            when(modelMapper.map(currentUser, UserResponse.class)).thenReturn(mappedResponse);

            UserResponse result = usersService.updateUser(1L, update, authentication);

            assertThat(currentUser.getUsername()).isEqualTo("john-updated");
            assertThat(result.getId()).isEqualTo(1L);

            verify(eventPublisher).publishEvent(any(UserUpdatedEvent.class));
        }

        @Test
        void throwsWhenUserNotFound() {
            UserUpdate update = new UserUpdate();

            when(usersRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usersService.updateUser(999L, update, authentication))
                    .isInstanceOf(EntityNotFoundException.class);

            verifyNoInteractions(eventPublisher);
        }

        @Test
        void throwsAccessDeniedWhenNotOwner() {
            UserUpdate update = new UserUpdate();
            update.setUsername("hacked");

            when(usersRepository.findById(1L)).thenReturn(Optional.of(currentUser));
            when(authentication.getName()).thenReturn("someone-else");

            assertThatThrownBy(() -> usersService.updateUser(1L, update, authentication))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("your own profile");

            verifyNoInteractions(eventPublisher);
            assertThat(currentUser.getUsername()).isEqualTo("john");
        }
    }


    @Nested
    class DeleteUser {

        @Test
        void deletesOwnProfileAndPublishesEvent() {
            when(usersRepository.findById(1L)).thenReturn(Optional.of(currentUser));
            when(authentication.getName()).thenReturn("john");

            usersService.deleteUser(1L, authentication);

            verify(usersRepository).delete(currentUser);

            ArgumentCaptor<UserDeletedEvent> eventCaptor = ArgumentCaptor.forClass(UserDeletedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().getUserId()).isEqualTo(1L);
        }

        @Test
        void throwsWhenUserNotFound() {
            when(usersRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usersService.deleteUser(999L, authentication))
                    .isInstanceOf(EntityNotFoundException.class);

            verify(usersRepository, never()).delete(any());
            verifyNoInteractions(eventPublisher);
        }

        @Test
        void throwsAccessDeniedWhenNotOwner() {
            when(usersRepository.findById(1L)).thenReturn(Optional.of(currentUser));
            when(authentication.getName()).thenReturn("someone-else");

            assertThatThrownBy(() -> usersService.deleteUser(1L, authentication))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("your own profile");

            verify(usersRepository, never()).delete(any());
            verifyNoInteractions(eventPublisher);
        }
    }


    @Nested
    class SearchUsers {

        @Test
        void returnsMappedMatchingUsers() {
            when(usersRepository.findByUsernameContainingIgnoreCase("jo"))
                    .thenReturn(List.of(currentUser));

            UserResponse mapped = new UserResponse();
            mapped.setId(1L);
            when(modelMapper.map(currentUser, UserResponse.class)).thenReturn(mapped);

            List<UserResponse> result = usersService.searchUsers("jo");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(1L);
            verify(usersRepository).findByUsernameContainingIgnoreCase("jo");
        }

        @Test
        void returnsEmptyListWhenNoMatches() {
            when(usersRepository.findByUsernameContainingIgnoreCase("xyz"))
                    .thenReturn(Collections.emptyList());

            List<UserResponse> result = usersService.searchUsers("xyz");

            assertThat(result).isEmpty();
            verifyNoInteractions(modelMapper);
        }
    }


    @Nested
    class GetCurrentUser {

        @Test
        void returnsCurrentAuthenticatedUser() {
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn("john");
            when(usersRepository.findByUsername("john")).thenReturn(Optional.of(currentUser));

            User result = usersService.getCurrentUser(authentication);

            assertThat(result).isEqualTo(currentUser);
        }

        @Test
        void throwsWhenAuthenticationIsNull() {
            assertThatThrownBy(() -> usersService.getCurrentUser(null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No authenticated user found");

            verifyNoInteractions(usersRepository);
        }

        @Test
        void throwsWhenNotAuthenticated() {
            when(authentication.isAuthenticated()).thenReturn(false);

            assertThatThrownBy(() -> usersService.getCurrentUser(authentication))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("No authenticated user found");

            verifyNoInteractions(usersRepository);
        }

        @Test
        void throwsWhenUserRecordNotFound() {
            when(authentication.isAuthenticated()).thenReturn(true);
            when(authentication.getName()).thenReturn("ghost");
            when(usersRepository.findByUsername("ghost")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> usersService.getCurrentUser(authentication))
                    .isInstanceOf(EntityNotFoundException.class)
                    .hasMessageContaining("Current user not found");
        }
    }
}