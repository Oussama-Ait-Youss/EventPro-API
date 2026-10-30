# 🎟️ EventPro API — Plateforme de billetterie événementielle

API REST de gestion d'événements et de vente de billets, conçue avec **Java et Spring Boot**. Le projet met l'accent sur la fiabilité du stock, les réservations temporaires, les paiements simulés et le contrôle des billets à l'entrée.

> **Source fonctionnelle :** cahier des charges *EventPro Billetterie API*. Ce README présente les fonctionnalités **attendues** par les spécifications ; leur présence effective dans le code doit être vérifiée avant de déclarer le projet terminé.

## Sommaire

- [Objectifs](#objectifs)
- [Stack technique](#stack-technique)
- [Rôles](#rôles)
- [Fonctionnalités et règles métier](#fonctionnalités-et-règles-métier)
- [Architecture](#architecture)
- [Modèle de données](#modèle-de-données)
- [Démarrage local](#démarrage-local)
- [Exemple de réservation](#exemple-de-réservation)
- [API et documentation](#api-et-documentation)
- [Tests](#tests)
- [Points de contrôle avant livraison](#points-de-contrôle-avant-livraison)

## Objectifs

Le parcours prévu est le suivant :

1. Un client consulte les événements publiés et leurs disponibilités.
2. Il réserve des billets : les places sont bloquées pendant **10 minutes**.
3. Il peut appliquer un **code promotionnel facultatif**.
4. Il simule le paiement de sa réservation.
5. Après confirmation, des billets électroniques à **code unique** sont émis.
6. À l'entrée, un agent contrôle les billets : **un seul passage** est autorisé.

**Principe essentiel :** aucune vente ne doit dépasser la quantité de billets disponibles, y compris lorsque plusieurs clients réservent simultanément.

## Stack technique

Les versions ci-dessous sont celles **demandées par le cahier des charges**, pas une vérification du `pom.xml` actuel.

| Domaine | Technologie / exigence |
| --- | --- |
| Langage | Java 17 |
| Framework | Spring Boot 3.2.x, Spring Web MVC |
| Persistance | Spring Data JPA, Hibernate ORM 6.4.x |
| Base de données | PostgreSQL 15+ |
| Transactions concurrentes | `PESSIMISTIC_WRITE` / `SELECT ... FOR UPDATE` |
| Validation | Jakarta Bean Validation / Hibernate Validator 8.x |
| Mapping | MapStruct |
| Génération de code | Lombok, notamment `@Builder` |
| Calculs monétaires | `BigDecimal`, devise **MAD** |
| Documentation | Springdoc OpenAPI 2.x / Swagger UI |
| Automatisation | Spring Scheduling (`@Scheduled`) |
| Tests | Maven, JUnit 5, Mockito ; Testcontainers en bonus |

## Rôles

| Rôle | Responsabilités prévues |
| --- | --- |
| **ADMIN** | Consulter les utilisateurs, gérer les promotions générales, superviser les réservations et les annulations exceptionnelles. |
| **ORGANIZER** | Créer, modifier, publier ou annuler ses événements ; gérer les catégories et prix ; suivre les ventes. |
| **CLIENT** | Consulter les événements, réserver, payer, récupérer ses billets et demander un remboursement. |
| **AGENT** | Vérifier les billets, empêcher les doubles entrées et enregistrer le passage. |

## Fonctionnalités et règles métier

### Événements et catégories

- Un organisateur gère plusieurs événements.
- Un événement possède une ou plusieurs catégories tarifaires (VIP, standard, etc.).
- Chaque catégorie définit son prix, sa quantité totale et un quota maximal par client.
- Seuls les événements publiés peuvent être réservés.

### Disponibilité et concurrence

```text
placesDisponibles = quantiteTotale - (placesReservees + placesVendues)
```

Une réservation est rejetée si la quantité demandée dépasse le stock disponible. Le verrouillage pessimiste (`PESSIMISTIC_WRITE`) sur les catégories concernées évite la survente lors des accès concurrents.

### Réservations temporaires

- À la création, la réservation bloque les places pour **10 minutes**.
- Si le paiement n'est pas effectué à temps, une tâche planifiée doit expirer la réservation et libérer le stock.
- Après paiement réussi, les quantités passent de **réservées** à **vendues**.

> **Note d'intégration :** le cahier des charges nomme le statut d'attente `PENDING_PAYMENT`. Le code de réservation partagé durant le développement utilise `PENDING`. Aligner la documentation et les enums sur la version réellement retenue.

### Promotions

Le code promo est **optionnel**. Lorsqu'il est fourni, vérifier notamment sa période de validité et sa limite d'utilisation, puis appliquer la remise :

```text
PERCENTAGE  : remise = sousTotal × valeur / 100
FIXED_AMOUNT: remise = min(valeur, sousTotal)
total       = max(0, sousTotal - remise)
```

Les montants sont exprimés en **MAD** et calculés avec `BigDecimal`.

### Paiements et billets

- Le paiement est **simulé** et doit vérifier le montant attendu.
- Une clé d'**idempotence** évite de traiter plusieurs fois la même intention de paiement.
- Chaque billet généré possède un **code unique** et commence au statut `VALID`.
- Au premier contrôle, le billet devient `USED` ; une seconde tentative doit renvoyer **HTTP 409 Conflict**.

### Annulations et remboursements

| Situation | Remboursement attendu | Conséquence |
| --- | --- | --- |
| Demande **≥ 48 h** avant le début | **100 %** | Billets annulés et places à nouveau vendables. |
| Demande **< 48 h** avant le début | **0 %** | Demande refusée ; billets conservés. |
| Annulation de l'événement par l'organisateur | **100 % automatique** | Billets invalidés et acheteurs remboursés. |

Une **annulation de réservation en attente de paiement** est distincte du **remboursement d'un achat déjà payé**.

## Architecture

Organisation **package-by-layer** prévue par le cahier des charges :

```text
src/main/java/com/eventpro/
├── controller/     # Contrôleurs et endpoints REST
├── service/        # Règles métier et transactions
├── repository/     # Accès aux données avec Spring Data JPA
├── entity/         # Entités JPA (ou model/ selon le code réel)
├── dto/            # Objets Request et Response
├── mapper/         # Conversions MapStruct
├── scheduler/      # Expiration des réservations
├── advice/         # Gestion centralisée des erreurs
└── exception/      # Exceptions métier
```

> Dans les fichiers de service partagés, le package réel est `com.eventpro.api` et les entités se trouvent dans `model/`. **Conserver la structure effective du dépôt** plutôt que renommer les packages uniquement pour suivre ce schéma théorique.

## Modèle de données

Entités principales prévues :

- **Utilisateur** (abstrait), spécialisé en **Administrateur**, **Organisateur**, **Client** et **AgentControle**.
- **Evenement** et ses **CategorieBillet**.
- **Promotion**.
- **Reservation** et ses **LigneReservation**.
- **Paiement** et **Remboursement**.
- **Billet**.

Enums définis dans le cahier des charges :

```text
StatutEvenement     : DRAFT, PUBLISHED, COMPLETED, CANCELLED
TypeReduction       : PERCENTAGE, FIXED_AMOUNT
StatutReservation   : PENDING_PAYMENT, CONFIRMED, EXPIRED, CANCELLED, REFUNDED
StatutPaiement      : PENDING, SUCCESS, FAILED
StatutRemboursement : PENDING, APPROVED, REJECTED
StatutBillet        : VALID, USED, CANCELLED
```

Les noms Java exacts peuvent différer (par exemple `Event`, `ReservationLine`, `TicketCategoriesPerEvent`, `ReservationStatus`).

## Démarrage local

### Prérequis

- JDK compatible avec le `pom.xml` (**Java 17** selon le cahier des charges).
- Apache Maven, ou Maven Wrapper si présent (`mvnw`).
- PostgreSQL 15+ accessible localement ou via Docker.

### 1. Récupérer le projet

```bash
git clone <URL_DU_DEPOT>
cd <DOSSIER_DU_PROJET>
```

Remplacer les valeurs entre chevrons par celles du dépôt réel.

### 2. Configurer PostgreSQL

Créer une base PostgreSQL et configurer les propriétés de connexion selon les fichiers `application.properties` / `application.yml` du projet. Exemple **indicatif** :

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/eventpro
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
spring.jpa.hibernate.ddl-auto=update
```

`ddl-auto=update` convient uniquement à un environnement de développement contrôlé. Ne jamais committer de vrais identifiants de base de données.

Si le dépôt contient un `compose.yaml` ou `docker-compose.yml` valide, il est possible de démarrer PostgreSQL avec :

```bash
docker compose up -d
```

### 3. Lancer l'application

```bash
mvn clean install
mvn spring-boot:run
```

Par défaut, Spring Boot utilise le port `8080`, sauf configuration contraire.

## Exemple de réservation

**Endpoint utilisé pendant le développement :**

```http
POST /api/clients/1/reservations
Content-Type: application/json
```

**Avec code promo :**

```json
{
  "eventId": 1,
  "promoCode": "PROMO2027",
  "lines": [
    {
      "eventTicketCategoryId": 1,
      "quantity": 2
    }
  ]
}
```

**Sans code promo :**

```json
{
  "eventId": 1,
  "lines": [
    {
      "eventTicketCategoryId": 1,
      "quantity": 2
    }
  ]
}
```

Les identifiants, le code promo et les dates de validité doivent correspondre à des données réellement présentes en base. Par exemple, deux billets à **250 MAD** avec une réduction de **50 %** donnent un sous-total de **500 MAD** et un total de **250 MAD**.

## API et documentation

Le cahier des charges prévoit une documentation OpenAPI / Swagger UI. **Si Springdoc est installé et configuré**, les chemins courants sont :

- `/swagger-ui/index.html`
- `/v3/api-docs`

### Routes de réservation

La création a été utilisée pendant le développement ; les autres routes ci-dessous correspondent à l'interface de service envisagée et sont **à confirmer dans le contrôleur final** :

| Méthode | Route | Action |
| --- | --- | --- |
| `POST` | `/api/clients/{clientId}/reservations` | Créer une réservation |
| `GET` | `/api/clients/{clientId}/reservations` | Lister les réservations du client |
| `GET` | `/api/clients/{clientId}/reservations/{reservationId}` | Consulter une réservation |
| `PATCH` | `/api/clients/{clientId}/reservations/{reservationId}/cancel` | Annuler une réservation en attente |
| `PUT` | `/api/clients/{clientId}/reservations/{reservationId}` | Modifier une réservation en attente (à implémenter / vérifier) |

Les routes des événements, promotions, paiements, remboursements et contrôles de billets doivent être documentées à partir des contrôleurs effectifs du dépôt ; elles ne sont pas fixées par le cahier des charges.

### Codes HTTP attendus

| Code | Signification / exemple |
| --- | --- |
| `200 OK` / `201 Created` | Lecture ou création réussie |
| `400 Bad Request` | Validation invalide (quantité, dates, etc.) |
| `404 Not Found` | Ressource inexistante |
| `409 Conflict` | Stock insuffisant, billet déjà utilisé, conflit d'idempotence ou réservation expirée |
| `500 Internal Server Error` | Erreur inattendue |

Le format d'erreur centralisé prévu comprend `timestamp`, `status`, `error`, `message`, `path` et `fieldErrors`.

## Tests

Lancer les tests Maven :

```bash
mvn test
```

Cas importants à couvrir :

- Refus d'une réservation dépassant le stock ou le quota par client.
- Réservations simultanées sur la dernière place disponible.
- Expiration après 10 minutes et restitution du stock.
- Promotion en pourcentage, montant fixe, dates invalides et limite d'utilisation.
- Paiement répété avec la même clé d'idempotence.
- Génération d'un billet unique par place payée.
- Refus du deuxième scan d'un même billet (`409`).
- Remboursement avant / après la limite des 48 heures.

**Outils prévus :** JUnit 5, Mockito et, en bonus, Testcontainers avec PostgreSQL.

## Points de contrôle avant livraison

- [ ] Vérifier les versions réelles dans `pom.xml` et les aligner avec les exigences retenues.
- [ ] Vérifier que les réservations expirent et libèrent effectivement le stock.
- [ ] Vérifier les verrous pessimistes et la résistance aux réservations concurrentes.
- [ ] Terminer et tester les opérations de lecture, annulation et modification des réservations.
- [ ] Vérifier la validation des codes promo et le calcul monétaire.
- [ ] Vérifier l'idempotence des paiements et la génération des billets.
- [ ] Vérifier le contrôle à usage unique des billets.
- [ ] Vérifier les remboursements et annulations d'événements.
- [ ] Vérifier Swagger, les erreurs HTTP et les tests automatisés.
- [ ] Remplacer les placeholders d'installation par les informations réelles du dépôt.

---

**EventPro API** — Projet pédagogique de billetterie événementielle basé sur le cahier des charges fourni.
