package fr.ece.autolocapi.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "client")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Client {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idClient;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = false, length = 30)
    private String telephone;

    @Column(nullable = false, length = 50)
    private String numPermis;

    @Column(nullable = false)
    private LocalDate dateInscription;

    @OneToMany(mappedBy = "client", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private List<Reservation> reservations = new ArrayList<>();

    public void addReservation(Reservation reservation) {
        if (reservation.getClient() != null && reservation.getClient() != this) {
            reservation.getClient().getReservations().remove(reservation);
        }
        if (!reservations.contains(reservation)) {
            reservations.add(reservation);
        }
        reservation.setClient(this);
    }

    public void removeReservation(Reservation reservation) {
        reservations.remove(reservation);
        if (reservation.getClient() == this) {
            reservation.setClient(null);
        }
    }
}
