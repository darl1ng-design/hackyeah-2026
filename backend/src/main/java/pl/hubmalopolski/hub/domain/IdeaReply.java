package pl.hubmalopolski.hub.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "idea_reply")
public class IdeaReply {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "idea_id", nullable = false)
    private Idea idea;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "author_user_id", nullable = false)
    private AppUser author;
    @Column(nullable = false, columnDefinition = "text")
    private String body;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected IdeaReply() {}

    public IdeaReply(Idea idea, AppUser author, String body) {
        this.idea = idea;
        this.author = author;
        this.body = body;
    }

    public Long getId() { return id; }
    public Idea getIdea() { return idea; }
    public AppUser getAuthor() { return author; }
    public String getBody() { return body; }
    public Instant getCreatedAt() { return createdAt; }
}
