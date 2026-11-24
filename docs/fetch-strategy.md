# Fetch and cascade choices

All associations request `LAZY`. French naming is retained, including the `vehicule_equipement` join table.

| Association | Owning field | Fetch reason | Cascade and reason |
| --- | --- | --- | --- |
| Agence / Vehicule | `Vehicule.agence` | LAZY: load the fleet or agency when needed. | None: vehicles survive agency deletion. |
| Agence / Employe | `Employe.agence` | LAZY: avoid loading every employee with the agency. | None: employees have independent lifecycles. |
| Vehicule / Equipement | `Vehicule.equipements` | LAZY: load equipment on demand. | None: equipment is shared. Both sides use `Set` to avoid duplicate links for the same managed instance. |
| Client / Reservation | `Reservation.client` | LAZY: load reservation history on demand. | PERSIST on `Client.reservations`: save new reservations with the client; preserve them on deletion. |
| Reservation / Vehicule | `Reservation.vehicule` | LAZY: load the vehicle or its reservations when needed. | None: vehicles and reservation history are independent. |
| Reservation / Contrat | `Contrat.reservation` | LAZY: defer contract details where supported. | ALL on `Reservation.contrat`: the contract follows the reservation lifecycle. |
| Vehicule / Maintenance | `Maintenance.vehicule` | LAZY: load maintenance history on demand. | PERSIST on `Vehicule.maintenances`: save new entries with the vehicle; preserve history on deletion. |
| Contrat / Paiement | `Paiement.contrat` | LAZY: the fetch experiment confirmed payment loading is deferred until collection access. | ALL + orphanRemoval on `Contrat.paiements`: deleting the contract deletes its payments; removing one payment deletes that payment. |

## Mapping rules

- The owning side writes the foreign key; inverse sides use `mappedBy` with the exact owning field name.
- Use the add/remove helpers to update both sides. Use `Reservation.setContrat` for the one-to-one link.
- Persist independent entities separately; no cascade runs from a child back to its parent.
- `Contrat.reservation_id_reservation` is unique. `vehicule_equipement` has two foreign keys and a composite primary key.
- Keep targeted Lombok annotations; avoid `@Data` on bidirectional entities.

## Lifecycle and loading

Detach or reassign references, then flush before deleting an agency, client or vehicle. Otherwise foreign keys can block deletion. Removing equipment links preserves the equipment.

Only payments use orphan removal. Unlinking other children does not delete them. Cascade is handled by JPA, not database `ON DELETE CASCADE`.

Access lazy relationships inside a transaction; use fetch joins or DTOs when data is needed afterward. Inverse one-to-one LAZY loading is a hint: Hibernate may query `Reservation.contrat` to check whether it exists. Strict deferral may require bytecode enhancement.

## Verification

Run `AssociationMappingTests` and `FetchStrategyTests` with `DB_PASSWORD` set. Keep the final fetch setting LAZY. Test rows are rolled back.

Restart the application and inspect Hibernate DDL. Expect ten tables, nine foreign keys and the unique reservation constraint:

```sql
USE autoloc_db;
SHOW TABLES;
SHOW CREATE TABLE contrat;
SHOW CREATE TABLE vehicule_equipement;

SELECT TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
FROM information_schema.KEY_COLUMN_USAGE
WHERE CONSTRAINT_SCHEMA = 'autoloc_db'
  AND REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY TABLE_NAME, COLUMN_NAME;
```

Validated against local MySQL: nine JUnit tests passed; table, foreign-key and unique-constraint checks passed. Checks used the JDK and JUnit launcher because Maven build-plugin dependencies were unavailable offline.
