package pl.hubmalopolski.hub.account;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pl.hubmalopolski.hub.domain.AppUser;
import pl.hubmalopolski.hub.domain.AppUserRole;
import pl.hubmalopolski.hub.repo.AppUserRepository;

import java.util.Locale;

@Component
public class StaffAccountBootstrap implements ApplicationRunner {
    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String staffEmail;
    private final String staffPassword;
    private final String adminEmail;
    private final String adminPassword;

    public StaffAccountBootstrap(AppUserRepository users, PasswordEncoder passwordEncoder,
            @Value("${HUB_STAFF_EMAIL:}") String staffEmail,
            @Value("${HUB_STAFF_PASSWORD:}") String staffPassword,
            @Value("${HUB_ADMIN_EMAIL:}") String adminEmail,
            @Value("${HUB_ADMIN_PASSWORD:}") String adminPassword) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.staffEmail = staffEmail;
        this.staffPassword = staffPassword;
        this.adminEmail = adminEmail;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        upsert(staffEmail, staffPassword, "Pracownik", AppUserRole.STAFF);
        upsert(adminEmail, adminPassword, "Administrator", AppUserRole.ADMIN);
    }

    private void upsert(String email, String password, String displayName, AppUserRole role) {
        if (email.isBlank() && password.isBlank()) return;
        if (email.isBlank() || password.length() < 12) {
            throw new IllegalStateException(role + " requires an e-mail and a password of at least 12 characters");
        }
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        AppUser account = users.findByEmail(normalized).orElseGet(() ->
                new AppUser(normalized, "", displayName, role));
        account.setPasswordHash(passwordEncoder.encode(password));
        account.setDisplayName(displayName);
        account.setRole(role);
        users.save(account);
    }
}
