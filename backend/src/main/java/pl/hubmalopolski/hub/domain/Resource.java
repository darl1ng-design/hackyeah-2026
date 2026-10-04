package pl.hubmalopolski.hub.domain;

import jakarta.persistence.*;

/**
 * Link do zewnetrznego zasobu ROPS (Biblioteka, Mapa Wyzwan, Canvas...) — ze strony hubMI.pl.
 */
@Entity
@Table(name = "resource")
public class Resource {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, length = 500)
    private String url;
    @Enumerated(EnumType.STRING)
    private ResourceKind kind;
    @Column(nullable = false)
    private boolean published = true;

    protected Resource() {
    }

    public Resource(String name, String url, ResourceKind kind, boolean published) {
        this.name = name;
        this.url = url;
        this.kind = kind;
        this.published = published;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUrl() {
        return url;
    }

    public ResourceKind getKind() {
        return kind;
    }

    public boolean isPublished() { return published; }
    public void setName(String name) { this.name = name; }
    public void setUrl(String url) { this.url = url; }
    public void setKind(ResourceKind kind) { this.kind = kind; }
    public void setPublished(boolean published) { this.published = published; }
}
