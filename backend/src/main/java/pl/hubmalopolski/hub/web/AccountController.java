package pl.hubmalopolski.hub.web;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.hubmalopolski.hub.account.AccountService;
import pl.hubmalopolski.hub.domain.AppUser;
import pl.hubmalopolski.hub.domain.AppUserRole;

@RestController
@RequestMapping("/api/v1")
public class AccountController {
    public record RegisterRequest(@NotBlank @Email String email,
                                  @NotBlank @Size(min = 12, max = 128) String password,
                                  @NotBlank @Size(max = 255) String displayName) {}
    public record AccountDto(Long id, String email, String displayName, AppUserRole role) {}

    private final AccountService accounts;

    public AccountController(AccountService accounts) {
        this.accounts = accounts;
    }

    @PostMapping("/register")
    @SecurityRequirement(name = "csrfToken")
    public ResponseEntity<AccountDto> register(@RequestBody @Valid RegisterRequest request) {
        AppUser saved = accounts.register(request.email(), request.password(), request.displayName());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AccountDto(saved.getId(), saved.getEmail(), saved.getDisplayName(), saved.getRole()));
    }
}
