package fr.ece.autolocapi.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contrat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Contrat {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idContrat;

    @Column(nullable = false)
    private LocalDate dateSignature;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal montantTotal;

    @Column(nullable = false)
    private boolean valide;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id_reservation", unique = true)
    private Reservation reservation;

    @OneToMany(
            mappedBy = "contrat",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )
    private List<Paiement> paiements = new ArrayList<>();

    public void addPaiement(Paiement paiement) {
        if (paiement.getContrat() != null && paiement.getContrat() != this) {
            throw new IllegalArgumentException("Un paiement appartient deja a un autre contrat");
        }
        if (!paiements.contains(paiement)) {
            paiements.add(paiement);
        }
        paiement.setContrat(this);
    }

    public void removePaiement(Paiement paiement) {
        paiements.remove(paiement);
        if (paiement.getContrat() == this) {
            paiement.setContrat(null);
        }
    }
}
