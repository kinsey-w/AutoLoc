package fr.ece.autolocapi;

import fr.ece.autolocapi.domain.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AssociationMappingTests {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void associationsRoundTripThroughTheirOwningSides() {
        Agence agence = agence();
        Employe employe = employe();
        Vehicule vehicule = vehicule();
        Equipement equipement = new Equipement();
        equipement.setLibelle("GPS");
        Client client = client();
        Reservation reservation = reservation();
        Contrat contrat = contrat();
        Paiement paiement = paiement();
        Maintenance maintenance = maintenance();

        agence.addVehicule(vehicule);
        agence.addEmploye(employe);
        vehicule.addEquipement(equipement);
        vehicule.addEquipement(equipement);
        vehicule.addMaintenance(maintenance);
        vehicule.addReservation(reservation);
        client.addReservation(reservation);
        reservation.setContrat(contrat);
        contrat.addPaiement(paiement);

        entityManager.persist(agence);
        entityManager.persist(equipement);
        entityManager.persist(vehicule);
        entityManager.persist(employe);
        entityManager.persist(client);
        entityManager.flush();
        entityManager.clear();

        Vehicule loaded = entityManager.find(Vehicule.class, vehicule.getIdVehicule());
        assertFalse(entityManager.getEntityManagerFactory().getPersistenceUnitUtil()
                .isLoaded(loaded, "equipements"));
        assertEquals(agence.getIdAgence(), loaded.getAgence().getIdAgence());
        assertEquals(1, loaded.getEquipements().size());
        assertEquals(vehicule.getIdVehicule(), loaded.getEquipements().iterator()
                .next().getVehicules().iterator().next().getIdVehicule());
        assertEquals(maintenance.getIdMaintenance(), loaded.getMaintenances().get(0).getIdMaintenance());

        Reservation loadedReservation = entityManager.find(Reservation.class, reservation.getIdReservation());
        assertEquals(client.getIdClient(), loadedReservation.getClient().getIdClient());
        assertEquals(vehicule.getIdVehicule(), loadedReservation.getVehicule().getIdVehicule());
        assertEquals(contrat.getIdContrat(), loadedReservation.getContrat().getIdContrat());
        assertEquals(reservation.getIdReservation(), loadedReservation.getContrat()
                .getReservation().getIdReservation());
        assertEquals(paiement.getIdPaiement(), loadedReservation.getContrat()
                .getPaiements().get(0).getIdPaiement());
        assertEquals(agence.getIdAgence(), entityManager.find(Employe.class, employe.getIdEmploye())
                .getAgence().getIdAgence());
        assertEquals(1, entityManager.find(Client.class, client.getIdClient()).getReservations().size());
        assertEquals(1, entityManager.find(Agence.class, agence.getIdAgence()).getEmployes().size());
    }

    @Test
    void deletingAgencyAfterDetachingLinksPreservesVehiclesAndEmployees() {
        Agence agence = agence();
        Vehicule vehicule = vehicule();
        Employe employe = employe();
        agence.addVehicule(vehicule);
        agence.addEmploye(employe);
        entityManager.persist(agence);
        entityManager.persist(vehicule);
        entityManager.persist(employe);
        entityManager.flush();

        agence.removeVehicule(vehicule);
        agence.removeEmploye(employe);
        entityManager.flush();
        entityManager.remove(agence);
        entityManager.flush();
        entityManager.clear();

        assertNull(entityManager.find(Agence.class, agence.getIdAgence()));
        assertNull(entityManager.find(Vehicule.class, vehicule.getIdVehicule()).getAgence());
        assertNull(entityManager.find(Employe.class, employe.getIdEmploye()).getAgence());
    }

    @Test
    void deletingVehiclePreservesSharedEquipmentAndDetachedMaintenance() {
        Vehicule first = vehicule();
        Vehicule second = vehicule();
        Equipement shared = new Equipement();
        shared.setLibelle("Siege enfant");
        Maintenance maintenance = maintenance();
        first.addEquipement(shared);
        second.addEquipement(shared);
        first.addMaintenance(maintenance);
        entityManager.persist(shared);
        entityManager.persist(first);
        entityManager.persist(second);
        entityManager.flush();

        first.removeEquipement(shared);
        first.removeMaintenance(maintenance);
        entityManager.flush();
        entityManager.remove(first);
        entityManager.flush();
        entityManager.clear();

        assertNull(entityManager.find(Vehicule.class, first.getIdVehicule()));
        assertNotNull(entityManager.find(Equipement.class, shared.getIdEquipement()));
        assertEquals(1, entityManager.find(Vehicule.class, second.getIdVehicule()).getEquipements().size());
        assertEquals(1, entityManager.find(Equipement.class, shared.getIdEquipement()).getVehicules().size());
        assertNull(entityManager.find(Maintenance.class, maintenance.getIdMaintenance()).getVehicule());
    }

    @Test
    void deletingReservationCascadesToContractAndPaymentsButNotClientOrVehicle() {
        Client client = client();
        Vehicule vehicule = vehicule();
        Reservation reservation = reservation();
        Contrat contrat = contrat();
        Paiement paiement = paiement();
        client.addReservation(reservation);
        vehicule.addReservation(reservation);
        reservation.setContrat(contrat);
        contrat.addPaiement(paiement);
        entityManager.persist(vehicule);
        entityManager.persist(client);
        entityManager.flush();

        client.removeReservation(reservation);
        vehicule.removeReservation(reservation);
        entityManager.remove(reservation);
        entityManager.flush();
        entityManager.clear();

        assertNull(entityManager.find(Reservation.class, reservation.getIdReservation()));
        assertNull(entityManager.find(Contrat.class, contrat.getIdContrat()));
        assertNull(entityManager.find(Paiement.class, paiement.getIdPaiement()));
        assertNotNull(entityManager.find(Client.class, client.getIdClient()));
        assertNotNull(entityManager.find(Vehicule.class, vehicule.getIdVehicule()));
    }

    @Test
    void deletingClientAfterDetachingReservationDoesNotDeleteReservation() {
        Client client = client();
        Reservation reservation = reservation();
        client.addReservation(reservation);
        entityManager.persist(client);
        entityManager.flush();

        client.removeReservation(reservation);
        entityManager.flush();
        entityManager.remove(client);
        entityManager.flush();
        entityManager.clear();

        assertNull(entityManager.find(Client.class, client.getIdClient()));
        assertNull(entityManager.find(Reservation.class, reservation.getIdReservation()).getClient());
    }

    @Test
    void removingPaymentFromContractDeletesOnlyThatPayment() {
        Contrat contrat = contrat();
        Paiement removed = paiement();
        Paiement retained = paiement();
        contrat.addPaiement(removed);
        contrat.addPaiement(retained);
        entityManager.persist(contrat);
        entityManager.flush();
        Long contratId = contrat.getIdContrat();
        Long removedId = removed.getIdPaiement();
        Long retainedId = retained.getIdPaiement();
        entityManager.clear();

        Contrat managed = entityManager.find(Contrat.class, contratId);
        Paiement paymentToRemove = managed.getPaiements().stream()
                .filter(paiement -> paiement.getIdPaiement().equals(removedId))
                .findFirst().orElseThrow();
        managed.removePaiement(paymentToRemove);
        assertNull(paymentToRemove.getContrat());
        entityManager.flush();
        entityManager.clear();

        assertNull(entityManager.find(Paiement.class, removedId));
        assertNotNull(entityManager.find(Paiement.class, retainedId));
        Contrat reloaded = entityManager.find(Contrat.class, contratId);
        assertEquals(1, reloaded.getPaiements().size());
        assertEquals(retainedId, reloaded.getPaiements().get(0).getIdPaiement());
    }

    @Test
    void paymentCannotBeMovedBetweenContractsWithOrphanRemoval() {
        Contrat original = contrat();
        Contrat another = contrat();
        Paiement paiement = paiement();
        original.addPaiement(paiement);

        assertThrows(IllegalArgumentException.class, () -> another.addPaiement(paiement));
        assertSame(original, paiement.getContrat());
        assertTrue(another.getPaiements().isEmpty());
        assertEquals(1, original.getPaiements().size());
    }

    private Agence agence() {
        Agence agence = new Agence();
        agence.setNom("Agence test");
        agence.setVille("Paris");
        agence.setAdresse("1 rue de test");
        agence.setTelephone("0102030405");
        return agence;
    }

    private Employe employe() {
        Employe employe = new Employe();
        employe.setNom("Martin");
        employe.setPrenom("Alex");
        employe.setRole(RoleEmploye.AGENT);
        return employe;
    }

    private Vehicule vehicule() {
        Vehicule vehicule = new Vehicule();
        vehicule.setImmatriculation(UUID.randomUUID().toString().substring(0, 20));
        vehicule.setMarque("Renault");
        vehicule.setModele("Clio");
        vehicule.setCategorie(CategorieVehicule.CITADINE);
        vehicule.setTarifJournalier(new BigDecimal("50.00"));
        vehicule.setStatut(StatutVehicule.DISPONIBLE);
        return vehicule;
    }

    private Client client() {
        Client client = new Client();
        client.setNom("Durand");
        client.setPrenom("Camille");
        client.setEmail("camille@example.com");
        client.setTelephone("0102030405");
        client.setNumPermis("PERMIS-TEST");
        client.setDateInscription(LocalDate.now());
        return client;
    }

    private Reservation reservation() {
        Reservation reservation = new Reservation();
        reservation.setDateDebut(LocalDate.now());
        reservation.setDateFin(LocalDate.now().plusDays(2));
        reservation.setStatut(StatutReservation.CONFIRMEE);
        return reservation;
    }

    private Contrat contrat() {
        Contrat contrat = new Contrat();
        contrat.setDateSignature(LocalDate.now());
        contrat.setMontantTotal(new BigDecimal("100.00"));
        contrat.setValide(true);
        return contrat;
    }

    private Paiement paiement() {
        Paiement paiement = new Paiement();
        paiement.setMontant(new BigDecimal("100.00"));
        paiement.setDatePaiement(LocalDate.now());
        paiement.setModePaiement(ModePaiement.CARTE);
        return paiement;
    }

    private Maintenance maintenance() {
        Maintenance maintenance = new Maintenance();
        maintenance.setDateDebut(LocalDate.now());
        maintenance.setDateFin(LocalDate.now().plusDays(1));
        maintenance.setDescription("Revision de test");
        return maintenance;
    }
}
