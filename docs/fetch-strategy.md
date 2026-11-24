# Workshop 2: association, fetch and cascade choices

The project retains the French domain names. The assignment's `vehicle_equipment`
join table is named `vehicule_equipement` consistently with these entities.
All associations explicitly request `FetchType.LAZY`.

| Association | Owning side and database mapping | Fetch justification | Cascade justification |
| --- | --- | --- | --- |
| Agence / Vehicule | `Vehicule.agence` owns `vehicule.agence_id_agence`; `Agence.vehicules` uses `mappedBy = "agence"`. | Load an agency's fleet only when needed, and avoid loading the agency for every vehicle read. | None in either direction. Vehicles exist independently and must survive agency deletion. Persist vehicles separately. |
| Agence / Employe | `Employe.agence` owns `employe.agence_id_agence`; `Agence.employes` uses `mappedBy = "agence"`. | Reading an agency does not require loading all employees, or vice versa. | None in either direction. Employees have an independent lifecycle and may move to another agency. |
| Vehicule / Equipement | `Vehicule.equipements` owns `vehicule_equipement`, with `vehicule_id_vehicule` and `equipement_id_equipement`. `Equipement.vehicules` uses `mappedBy = "equipements"`. Both use `Set`. | Shared equipment is loaded only when its collection is accessed. | None. An equipment record can belong to several vehicles; saving or deleting a vehicle must not save or delete shared equipment. Persist equipment separately. |
| Client / Reservation | `Reservation.client` owns `reservation.client_id_client`; `Client.reservations` uses `mappedBy = "client"`. | Do not load a client's complete reservation history for each client read. | `PERSIST` only on `Client.reservations`: saving a new client can save new reservations. No cascade from reservation to client and no cascading deletion of history. |
| Reservation / Vehicule | `Reservation.vehicule` owns `reservation.vehicule_id_vehicule`; `Vehicule.reservations` uses `mappedBy = "vehicule"`. | A reservation can be read without immediately loading its vehicle, and vehicles without all their reservations. | None in either direction. A reservation refers to an independently persisted vehicle; deletion must not delete the vehicle or its reservation history. |
| Reservation / Contrat | `Contrat.reservation` owns `contrat.reservation_id_reservation`, with a unique constraint; `Reservation.contrat` uses `mappedBy = "reservation"`. | Request deferred loading when the contract or reservation details are not needed. See the one-to-one caveat below. | `ALL` only on `Reservation.contrat`: the contract belongs to the reservation lifecycle. Removing a reservation removes its contract, but removing a contract does not remove the reservation. |
| Vehicule / Maintenance | `Maintenance.vehicule` owns `maintenance.vehicule_id_vehicule`; `Vehicule.maintenances` uses `mappedBy = "vehicule"`. | Maintenance history is loaded only when needed. | `PERSIST` only on `Vehicule.maintenances`: saving a new vehicle can save its maintenance entries. No cascade back to the vehicle and no cascading removal of maintenance history. |
| Contrat / Paiement (guided exercise and section 12) | `Paiement.contrat` owns `paiement.contrat_id_contrat`; `Contrat.paiements` uses `mappedBy = "contrat"`. | The LAZY/EAGER experiment showed a deferred payment query with LAZY and a join with EAGER. Final configuration is LAZY. | `ALL` and `orphanRemoval = true` only on `Contrat.paiements`: payments depend on their contract. Removing a contract removes its payments; removing a payment from its collection deletes that payment at flush. No cascade from payment to contract. |

## Ownership and keeping both sides consistent

Only the owning side writes a foreign key or join-table link. Use
`Agence.addVehicule` / `removeVehicule`, `Agence.addEmploye` / `removeEmploye`,
`Client.addReservation` / `removeReservation`,
`Vehicule.addReservation` / `removeReservation`,
`Vehicule.addMaintenance` / `removeMaintenance`,
`Vehicule.addEquipement` / `removeEquipement`, and `Reservation.setContrat`.
These methods update both Java objects. The collection helpers also detach the
previous parent when moving an independent entity. Use `Contrat.addPaiement` and
`Contrat.removePaiement` for payments. A payment already attached to another
contract cannot be moved using `addPaiement`: payments have a private lifecycle,
and orphan removal makes reparenting an existing payment unsafe.
When attaching a reservation to both a client and a vehicle, call both helpers.

Collections are initialized. `Set` avoids duplicate equipment links for the same
managed entity instance; the join table has a composite primary key. Entities keep
targeted Lombok annotations instead of `@Data`, avoiding recursive generated
`toString`, `equals` and `hashCode` across bidirectional links.

## Deletion and existing data

Cascade is a JPA lifecycle rule, not a database `ON DELETE CASCADE` rule.
Foreign keys remain nullable so existing rows can receive the new mappings
without fabricated parent records. The database still enforces referential
integrity when a foreign-key value is present.

Before removing an agency, detach or reassign its vehicles and employees, then
flush the owning-side updates. Similarly, detach reservations before deleting a
client or vehicle, and detach maintenance entries before deleting a vehicle.
Otherwise the database can reject deletion because references still exist.
Removing a vehicle/equipment link does not delete the equipment record.

Removing a reservation cascades through its contract to its payments. Section 12
also requires composition for contract/payments: `Contrat.removePaiement` removes
the payment from the collection and clears its contract reference; Hibernate
deletes that orphan at flush. The contract and its other payments remain.
This applies to managed, persisted entities inside a transaction. Other
associations do not enable orphan removal. Removing or replacing a reservation's
contract link does not delete the old contract automatically.

## LAZY and transaction boundaries

Read deferred relationships inside a transaction. For data needed afterward,
use an explicit fetch join, entity graph or DTO projection rather than making all
relationships EAGER. Avoid serializing the bidirectional entities directly.

`LAZY` on a one-to-one association is a JPA hint. In particular, Hibernate may
query the inverse `Reservation.contrat` side to determine whether an optional
contract exists. Strict deferred loading there may require bytecode enhancement;
the current project does not enable enhancement. Inspect actual SQL rather than
assuming every to-one association stays unloaded.

## Verification

Run `AssociationMappingTests` with `DB_PASSWORD` set in its IntelliJ JUnit run
configuration. Its transactions roll back the sample data. It checks owning-side
round trips, shared equipment, lazy equipment loading, persist cascades, the
reservation/contract/payment removal chain, payment orphan removal, and
preservation of independent entities after links are detached. Run `FetchStrategyTests` for the guided fetch
comparison, keeping the final mapping LAZY.

Restart the application with `spring.jpa.hibernate.ddl-auto=update` and inspect
the Hibernate DDL. There should be nine entity tables and one join table:

```sql
USE autoloc_db;
SHOW TABLES;
SHOW CREATE TABLE vehicule;
SHOW CREATE TABLE employe;
SHOW CREATE TABLE reservation;
SHOW CREATE TABLE contrat;
SHOW CREATE TABLE maintenance;
SHOW CREATE TABLE vehicule_equipement;

SELECT TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE CONSTRAINT_SCHEMA = 'autoloc_db'
  AND REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY TABLE_NAME, COLUMN_NAME;
```

Expected foreign keys: vehicle to agency, employee to agency, reservation to
client and vehicle, contract to reservation, maintenance to vehicle, both join
table columns, and the existing payment to contract link (nine in total).
The contract's reservation column must also have a unique constraint. No extra
join tables should be created for inverse `mappedBy` collections.

### Validation results

All production and test sources compiled with the installed JDK, Lombok and a
Java 17 target. All nine tests passed against the local MySQL database: seven
association tests, the guided fetch test and the application context test.
Database inspection confirmed exactly ten tables, all nine expected foreign
keys and the unique constraint on `contrat.reservation_id_reservation`.
The sample rows were rolled back; the schema changes remain applied.

Maven's offline build was blocked by uncached build-plugin dependencies, so the
checks used the installed application libraries and the JUnit Platform launcher
directly. Mockito was loaded as a startup Java agent for this verification run.
