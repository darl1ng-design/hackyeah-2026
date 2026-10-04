package pl.hubmalopolski.hub.account;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import pl.hubmalopolski.hub.domain.AppUser;
import pl.hubmalopolski.hub.domain.AppUserRole;
import pl.hubmalopolski.hub.repo.AppUserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTests {
    @Mock AppUserRepository users;
    @Mock PasswordEncoder encoder;
    @InjectMocks AccountService accounts;

    @Test
    void registrationNormalizesEmailAndStoresOnlyPasswordHash() {
        when(users.findByEmail("anna@example.org")).thenReturn(Optional.empty());
        when(encoder.encode("very-long-password")).thenReturn("encoded-password");
        when(users.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser account = accounts.register(" Anna@Example.org ", "very-long-password", " Anna ");

        assertEquals("anna@example.org", account.getEmail());
        assertEquals("encoded-password", account.getPasswordHash());
        assertEquals("Anna", account.getDisplayName());
        assertEquals(AppUserRole.MEMBER, account.getRole());
    }

    @Test
    void duplicateEmailIsRejectedBeforeSaving() {
        when(users.findByEmail("anna@example.org")).thenReturn(Optional.of(
                new AppUser("anna@example.org", "hash", "Anna", AppUserRole.MEMBER)));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> accounts.register("ANNA@EXAMPLE.ORG", "very-long-password", "Anna"));

        assertEquals(409, error.getStatusCode().value());
        verify(users, never()).saveAndFlush(any());
    }
}
