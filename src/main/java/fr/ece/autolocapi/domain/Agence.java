package fr.ece.autolocapi.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "agence")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Agence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAgence;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String ville;

    @Column(nullable = false, length = 255)
    private String adresse;

    @Column(nullable = false, length = 30)
    private String telephone;

    @OneToMany(mappedBy = "agence", fetch = FetchType.LAZY)
    private List<Vehicule> vehicules = new ArrayList<>();

    @OneToMany(mappedBy = "agence", fetch = FetchType.LAZY)
    private List<Employe> employes = new ArrayList<>();

    public void addVehicule(Vehicule vehicule) {
        if (vehicule.getAgence() != null && vehicule.getAgence() != this) {
            vehicule.getAgence().getVehicules().remove(vehicule);
        }
        if (!vehicules.contains(vehicule)) {
            vehicules.add(vehicule);
        }
        vehicule.setAgence(this);
    }

    public void removeVehicule(Vehicule vehicule) {
        vehicules.remove(vehicule);
        if (vehicule.getAgence() == this) {
            vehicule.setAgence(null);
        }
    }

    public void addEmploye(Employe employe) {
        if (employe.getAgence() != null && employe.getAgence() != this) {
            employe.getAgence().getEmployes().remove(employe);
        }
        if (!employes.contains(employe)) {
            employes.add(employe);
        }
        employe.setAgence(this);
    }

    public void removeEmploye(Employe employe) {
        employes.remove(employe);
        if (employe.getAgence() == this) {
            employe.setAgence(null);
        }
    }
}
