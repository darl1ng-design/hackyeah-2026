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
    @Column(nullable = false)
    private String url;
    private String kind;

    protected Resource() {
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

    public String getKind() {
        return kind;
    }
}