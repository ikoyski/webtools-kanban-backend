package com.ikoyki.webtools.kanban.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import com.ikoyki.webtools.kanban.backend.entity.UserEntity;
import com.ikoyki.webtools.kanban.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserProvisioningServiceTest {

    @Mock private JdbcTemplate jdbc;
    @Mock private UserRepository users;
    @InjectMocks private UserProvisioningService service;

    @Test
    void createsLocalProfileUsingTheCentralIdentityId() {
        UUID centralId = UUID.randomUUID();
        UserEntity local = new UserEntity();
        local.setId(centralId);
        local.setEmail("person@example.com");
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);
        when(users.findById(centralId)).thenReturn(Optional.of(local));

        UserEntity result = service.ensureLocalUser(
                centralId, " Person@Example.com ", " Person ", " https://example.com/avatar.png ");

        assertEquals(centralId, result.getId());
        assertEquals("person@example.com", result.getEmail());
        verify(jdbc).update(anyString(), any(Object[].class));
    }

    @Test
    void rejectsMissingIdentityEmail() {
        assertThrows(ResponseStatusException.class,
                () -> service.ensureLocalUser(UUID.randomUUID(), " ", null, null));
    }
}
