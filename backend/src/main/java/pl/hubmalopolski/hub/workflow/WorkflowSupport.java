package pl.hubmalopolski.hub.workflow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import pl.hubmalopolski.hub.domain.AppUser;
import pl.hubmalopolski.hub.domain.AppUserRole;
import pl.hubmalopolski.hub.domain.HubWorkflowRecord;
import pl.hubmalopolski.hub.repo.AppUserRepository;

import java.util.Map;

@Component
public class WorkflowSupport {
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};
    private final AppUserRepository users;
    private final ObjectMapper mapper;

    public WorkflowSupport(AppUserRepository users, ObjectMapper mapper) {
        this.users = users;
        this.mapper = mapper;
    }

    public AppUser requireUser(String email) {
        return users.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    public AppUser requireMember(String email) {
        AppUser user = requireUser(email);
        if (user.getRole() != AppUserRole.MEMBER) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        return user;
    }

    public void requireStaff(String email) {
        AppUserRole role = requireUser(email).getRole();
        if (role != AppUserRole.STAFF && role != AppUserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    public void requireAdmin(String email) {
        if (requireUser(email).getRole() != AppUserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    public Map<String, Object> readPayload(String payload) {
        try { return mapper.readValue(payload, MAP_TYPE); }
        catch (Exception e) { throw new IllegalStateException("Stored workflow payload is invalid", e); }
    }

    public String writePayload(Map<String, ?> payload) {
        try { return mapper.writeValueAsString(payload); }
        catch (Exception e) { throw new IllegalArgumentException("Workflow payload could not be encoded", e); }
    }

    public HubWorkflowDto dto(HubWorkflowRecord record, boolean includeAuthor) {
        return new HubWorkflowDto(record.getId(), record.getModule(), record.getReferenceId(),
                record.getStatus(), record.getTitle(), readPayload(record.getPayload()),
                includeAuthor ? record.getOwner().getDisplayName() : null,
                record.getCreatedAt(), record.getUpdatedAt());
    }
}
