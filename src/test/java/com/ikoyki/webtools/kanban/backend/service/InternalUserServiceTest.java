package com.ikoyki.webtools.kanban.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ikoyki.webtools.kanban.backend.entity.UserEntity;
import com.ikoyki.webtools.kanban.backend.entity.UserProviderEntity;
import com.ikoyki.webtools.kanban.backend.repository.UserProviderRepository;
import com.ikoyki.webtools.kanban.backend.repository.UserRepository;
import com.ikoyki.webtools.kanban.backend.service.InternalUserService.FederatedResult.Status;

@ExtendWith(MockitoExtension.class)
class InternalUserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private UserProviderRepository providerRepository;
    @InjectMocks private InternalUserService service;

    private static UserEntity user(UUID id, String email) {
        UserEntity u = new UserEntity();
        u.setId(id);
        u.setEmail(email);
        return u;
    }

    @Test
    void knownIdentitySignsInAsThatUser() {
        UUID id = UUID.randomUUID();
        UserEntity existing = user(id, "a@example.com");
        UserProviderEntity identity = new UserProviderEntity();
        identity.setUser(existing);
        when(providerRepository.findByProviderTypeAndProviderId("GOOGLE", "sub-1")).thenReturn(Optional.of(identity));

        var result = service.federated("google", "sub-1", "A@Example.com", true, "Ann", null);

        assertEquals(Status.OK, result.status());
        assertEquals(id.toString(), result.user().id());
        verify(userRepository, never()).saveAndFlush(any());
    }

    @Test
    void firstSignInCreatesUserAndIdentityWithNormalizedEmail() {
        when(providerRepository.findByProviderTypeAndProviderId("GOOGLE", "sub-2")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("new@example.com")).thenReturn(Optional.empty());
        UUID newId = UUID.randomUUID();
        when(userRepository.saveAndFlush(any(UserEntity.class))).thenAnswer(inv -> {
            UserEntity u = inv.getArgument(0);
            u.setId(newId);
            return u;
        });

        var result = service.federated("GOOGLE", "sub-2", "  New@Example.com ", true, "New User", null);

        assertEquals(Status.OK, result.status());
        assertEquals("new@example.com", result.user().email());
        assertEquals(newId.toString(), result.user().id());
        verify(providerRepository).saveAndFlush(any(UserProviderEntity.class));
    }

    @Test
    void newIdentityWithEmailOfAnotherUserIsRefusedNotMerged() {
        when(providerRepository.findByProviderTypeAndProviderId("GOOGLE", "sub-3")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("taken@example.com"))
                .thenReturn(Optional.of(user(UUID.randomUUID(), "taken@example.com")));

        var result = service.federated("GOOGLE", "sub-3", "taken@example.com", true, null, null);

        assertEquals(Status.EMAIL_CONFLICT, result.status());
        verify(userRepository, never()).saveAndFlush(any());
        verify(providerRepository, never()).saveAndFlush(any());
    }

    @Test
    void unverifiedEmailIsRejected() {
        var result = service.federated("GOOGLE", "sub-4", "x@example.com", false, null, null);
        assertEquals(Status.EMAIL_NOT_VERIFIED, result.status());
    }

    @Test
    void unknownProviderOrBlankSubjectIsInvalid() {
        assertEquals(Status.INVALID, service.federated("GITHUB", "s", "x@example.com", true, null, null).status());
        assertEquals(Status.INVALID, service.federated("GOOGLE", " ", "x@example.com", true, null, null).status());
        assertEquals(Status.INVALID, service.federated("GOOGLE", "s", " ", true, null, null).status());
    }

    @Test
    void lookupByIdAndByEmail() {
        UUID id = UUID.randomUUID();
        UserEntity u = user(id, "a@example.com");
        when(userRepository.findById(id)).thenReturn(Optional.of(u));
        when(userRepository.findByEmail("a@example.com")).thenReturn(Optional.of(u));

        assertEquals(id.toString(), service.lookup(id.toString(), null).orElseThrow().id());
        assertEquals(id.toString(), service.lookup(null, " A@example.com ").orElseThrow().id());
        assertTrue(service.lookup("not-a-uuid", null).isEmpty());
        assertTrue(service.lookup(null, null).isEmpty());
    }
}
