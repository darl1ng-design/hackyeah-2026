package pl.hubmalopolski.hub.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import pl.hubmalopolski.hub.domain.AppUser;
import pl.hubmalopolski.hub.domain.AppUserRole;

import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmail(String email);
    List<AppUser> findByRoleIn(List<AppUserRole> roles);
}
