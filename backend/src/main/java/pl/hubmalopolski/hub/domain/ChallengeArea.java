package pl.hubmalopolski.hub.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "challenge_area")
public class ChallengeArea {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String name;
    @Column(length = 1000)
    private String description;

    protected ChallengeArea() {
    }

    public ChallengeArea(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
}
