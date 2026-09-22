package devheloisacabral.tcc.com.esportiva.domain.professor;

import devheloisacabral.tcc.com.esportiva.domain.auth.AuthUser;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "professors")
public class Professor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private AuthUser user;

    @Column(nullable = false, length = 40, unique = true)
    private String cref;

    @Column(nullable = false, length = 20)
    private String status;

    protected Professor() {
    }

    public Professor(AuthUser user, String cref) {
        this.user = user;
        this.cref = cref;
        this.status = "ativo";
    }

    public Long getId() { return id; }
    public AuthUser getUser() { return user; }
    public String getCref() { return cref; }
    public String getStatus() { return status; }
    public void update(String name, String email, java.time.LocalDate birthDate, String cref) {
        user.updateRegistration(name, email, birthDate);
        this.cref = cref;
    }
    public void inactivate() { this.status = "inativo"; }
    public void reactivate() { this.status = "ativo"; }
}
