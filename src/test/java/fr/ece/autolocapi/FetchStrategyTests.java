package fr.ece.autolocapi;

import fr.ece.autolocapi.domain.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

@SpringBootTest
@Transactional
class FetchStrategyTests {

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void compareFetchStrategies() {
        Contrat contrat = new Contrat();
        contrat.setDateSignature(LocalDate.now());
        contrat.setMontantTotal(new BigDecimal("100.00"));
        contrat.setValide(true);

        Paiement paiement = new Paiement();
        paiement.setMontant(new BigDecimal("100.00"));
        paiement.setDatePaiement(LocalDate.now());
        paiement.setModePaiement(ModePaiement.CARTE);
        contrat.addPaiement(paiement);

        entityManager.persist(contrat);
        entityManager.flush();
        Long id = contrat.getIdContrat();
        entityManager.clear();

        System.out.println("=== Loading the contract ===");
        Contrat loaded = entityManager.find(Contrat.class, id);

        System.out.println("Payments already loaded: " +
                entityManager.getEntityManagerFactory()
                        .getPersistenceUnitUtil()
                        .isLoaded(loaded, "paiements"));

        System.out.println("=== Accessing the payments ===");
        System.out.println("Payment count: " +
                loaded.getPaiements().size());
    }
}