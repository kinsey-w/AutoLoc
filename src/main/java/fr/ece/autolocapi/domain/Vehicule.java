package fr.ece.autolocapi.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "vehicule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Vehicule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idVehicule;

    @Column(nullable = false, unique = true, length = 20)
    private String immatriculation;

    @Column(nullable = false, length = 50)
    private String marque;

    @Column(nullable = false, length = 50)
    private String modele;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CategorieVehicule categorie;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal tarifJournalier;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutVehicule statut;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agence_id_agence")
    private Agence agence;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "vehicule_equipement",
            joinColumns = @JoinColumn(name = "vehicule_id_vehicule"),
            inverseJoinColumns = @JoinColumn(name = "equipement_id_equipement")
    )
    private Set<Equipement> equipements = new HashSet<>();

    @OneToMany(mappedBy = "vehicule", fetch = FetchType.LAZY)
    private List<Reservation> reservations = new ArrayList<>();

    @OneToMany(mappedBy = "vehicule", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
    private List<Maintenance> maintenances = new ArrayList<>();

    public void addEquipement(Equipement equipement) {
        equipements.add(equipement);
        equipement.getVehicules().add(this);
    }

    public void removeEquipement(Equipement equipement) {
        equipements.remove(equipement);
        equipement.getVehicules().remove(this);
    }

    public void addReservation(Reservation reservation) {
        if (reservation.getVehicule() != null && reservation.getVehicule() != this) {
            reservation.getVehicule().getReservations().remove(reservation);
        }
        if (!reservations.contains(reservation)) {
            reservations.add(reservation);
        }
        reservation.setVehicule(this);
    }

    public void removeReservation(Reservation reservation) {
        reservations.remove(reservation);
        if (reservation.getVehicule() == this) {
            reservation.setVehicule(null);
        }
    }

    public void addMaintenance(Maintenance maintenance) {
        if (maintenance.getVehicule() != null && maintenance.getVehicule() != this) {
            maintenance.getVehicule().getMaintenances().remove(maintenance);
        }
        if (!maintenances.contains(maintenance)) {
            maintenances.add(maintenance);
        }
        maintenance.setVehicule(this);
    }

    public void removeMaintenance(Maintenance maintenance) {
        maintenances.remove(maintenance);
        if (maintenance.getVehicule() == this) {
            maintenance.setVehicule(null);
        }
    }
}
