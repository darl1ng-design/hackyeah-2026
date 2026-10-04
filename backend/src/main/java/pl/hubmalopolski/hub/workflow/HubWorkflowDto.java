package pl.hubmalopolski.hub.workflow;

import java.time.Instant;
import java.util.Map;

public record HubWorkflowDto(Long id, String module, Long referenceId, String status, String title,
                             Map<String, Object> payload, String author,
                             Instant createdAt, Instant updatedAt) {}
