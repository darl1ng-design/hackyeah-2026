package pl.hubmalopolski.hub.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

/** Shared persistence row for separately typed, permission-checked Hub workflows. */
@Entity
@Table(name = "hub_workflow_record", indexes = {
        @Index(name = "idx_hub_workflow_owner_module_created", columnList = "owner_user_id,module,created_at,id"),
        @Index(name = "idx_hub_workflow_module_status_created", columnList = "module,status,created_at,id")
})
public class HubWorkflowRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 32)
    private String module;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private AppUser owner;
    @Column(name = "reference_id")
    private Long referenceId;
    @Column(nullable = false, length = 32)
    private String status;
    @Column(nullable = false)
    private String title;
    @Column(nullable = false, columnDefinition = "text")
    private String payload;
    @Column(name = "dedupe_key", unique = true, length = 255)
    private String dedupeKey;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected HubWorkflowRecord() {}

    public HubWorkflowRecord(String module, AppUser owner, Long referenceId, String status,
                             String title, String payload) {
        this(module, owner, referenceId, status, title, payload, null);
    }

    public HubWorkflowRecord(String module, AppUser owner, Long referenceId, String status,
                             String title, String payload, String dedupeKey) {
        this.module = module;
        this.owner = owner;
        this.referenceId = referenceId;
        this.status = status;
        this.title = title;
        this.payload = payload;
        this.dedupeKey = dedupeKey;
    }

    public void replace(String status, String title, String payload) {
        this.status = status;
        this.title = title;
        this.payload = payload;
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getModule() { return module; }
    public AppUser getOwner() { return owner; }
    public Long getReferenceId() { return referenceId; }
    public String getStatus() { return status; }
    public String getTitle() { return title; }
    public String getPayload() { return payload; }
    public String getDedupeKey() { return dedupeKey; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
