# SmartCarbure

> Projet Développement Logiciel (PDL) – Master 1 MIAGE
> ISTIC, Université de Rennes – Année 2026/2027
> Client : Adrien Le Roch

---

## Sommaire

1. [But du projet](#but-du-projet)
2. [Contexte du projet](#contexte-du-projet)
3. [Fonctionnalités supportées (V1)](#fonctionnalités-supportées-v1)
4. [Fonctionnalités à développer](#fonctionnalités-à-développer)
5. [Limites connues](#limites-connues)
6. [Lancer l'application](#lancer-lapplication)
7. [Couverture des tests](#couverture-des-tests)
8. [Données ouvertes utilisées](#données-ouvertes-utilisées)
9. [Technologies utilisées](#technologies-utilisées)
10. [Participants](#participants)
11. [Documentation](#documentation)

---

## But du projet

Permettre aux automobilistes de **trouver les stations-service les moins coûteuses et/ou les plus proches** de leur position ou de leur trajet, à partir des données ouvertes officielles publiées par l'État.

---

## Contexte du projet

Ce projet s'inscrit dans le cadre de notre formation de **Master 1 MIAGE**, au sein de l'UE **Projet de Développement Logiciel (PDL)**.

### Problématique

Les automobilistes recherchent souvent les stations de carburant les moins coûteuses et/ou les plus proches de leur emplacement ou de leur trajet. Les prix varient fortement d'une station à l'autre et changent plusieurs fois par jour, ce qui rend la comparaison difficile sans outil dédié.

### Réponse apportée

Une application web qui exploite les données ouvertes sur les prix des carburants pour centraliser en un seul endroit la recherche, la comparaison et le suivi des stations-service.

---

## Fonctionnalités supportées (V1)

> Version livrée le 22 novembre 2026. La V2 est prévue le 20 décembre 2026.

Statut : **Livrée** = utilisable depuis l'interface, avec le frontend branché sur une vraie route du backend ; **Partielle** = une partie de la fonctionnalité manque.

| # | Fonctionnalité | Page(s) | Statut |
|---|---|---|---|
| F1 | Rechercher les stations autour de sa position (géolocalisation) | Accueil, Résultats | **Livrée** : bouton de localisation, puis `GET /api/stations?lat=&lon=&rayon=` |
| F2 | Rechercher les stations d'une ville ou d'un code postal | Accueil, Résultats | **Partielle** : recherche par nom exact de commune ou par code postal ; la recherche par adresse et le rayon autour du centre d'une ville ne sont pas faits |
| F3 | Définir un rayon de recherche | Accueil, Résultats | **Livrée** : curseur de 1 à 50 km |
| F4 | Filtrer par type de carburant (Gazole, SP95, SP98, SP95-E10, E85, GPLc) | Accueil, Résultats | **Livrée** : seules les stations qui vendent le carburant choisi sont affichées |
| F5 | Trier les résultats par prix ou par distance | Résultats | **Livrée** : pour une recherche par ville, sans position, le tri se fait par prix |
| F6 | Rechercher les stations le long d'un trajet | Trajet | **Livrée** : itinéraire OpenRouteService, stations à 5 km au plus de la route (réglable de 1 à 10 km), trajet de 500 km au plus |
| F7 | Consulter la fiche détaillée d'une station | Fiche station | **Livrée** : adresse, prix et date de mise à jour, services, carte, horaires |
| F8 | Être informé des ruptures de stock | Résultats, Fiche station | **Livrée** : badge « Rupture temporaire / définitive depuis le … », dernier prix connu barré |
| F9 | Afficher les stations sur une carte interactive | Résultats, Fiche station, Trajet | **Livrée** : carte Leaflet (OpenStreetMap), avec le tracé de l'itinéraire sur la page Trajet |
| F10 | Afficher un message clair si l'API est indisponible | Résultats, Fiche station, Trajet, Comparer | **Livrée** : message « Service temporairement indisponible » avec un bouton « Réessayer » |
| F11 | Créer un compte utilisateur | Connexion | **Livrée** : `POST /api/auth/inscription`, mot de passe haché (BCrypt) |
| F12 | Se connecter à un compte utilisateur | Connexion | **Livrée** : `POST /api/auth/connexion`, jeton JWT valable 24 h, déconnexion depuis la barre de navigation |
| F15 | Calculer l'économie sur un plein par rapport au prix moyen local | Fiche station | **Livrée** : comparaison avec le prix moyen des stations de la même ville |
| F16 | Comparer les prix moyens entre deux villes ou deux départements | Comparer | **Livrée** : prix moyen, nombre de stations et écart entre les deux zones |
| F18 | Exporter les résultats d'une recherche au format CSV | Résultats | **Livrée** : fichier généré par le navigateur, sans appel au backend |

---

## Fonctionnalités à développer

Fonctionnalités prévues pour la V2 :

| # | Fonctionnalité | Priorité | Ce qui reste à faire |
|---|---|---|---|
| F2 | Rechercher les stations autour d'une ville ou d'une adresse saisie | Essentielle | Recherche par adresse et rayon autour d'une ville (la recherche par commune ou code postal est livrée) |
| F13 | Enregistrer son carburant et son rayon de recherche préférés | Bonus | Routes `GET` et `PUT /api/preferences` et page Préférences (le formulaire de recherche sait déjà se pré-remplir avec les préférences) |
| F14 | Gérer une liste de stations favorites | Bonus | Routes `/api/favoris` et page Mes favoris (l'étoile des résultats n'est pas encore enregistrée) |
| F17 | Suivre l'évolution des prix d'un carburant dans le temps | Bonus | Tout : l'API ne fournit que le prix actuel, un historique devra être enregistré |

*Ces fonctionnalités et leurs priorités seront validées avec le client et pourront évoluer au cours du projet. Les changements seront justifiés lors de la soutenance.*

---

## Limites connues

- **API Prix des carburants parfois indisponible.** Quand elle ne répond pas, l'application affiche « Service temporairement indisponible. Réessayez dans quelques instants. » avec un bouton « Réessayer » (code 503).
- **Pas de nom de station.** L'API ne fournit pas le nom des stations : l'application affiche leur adresse, et la colonne « Nom » de l'export CSV reste vide.
- **Nombre de résultats limité.** Une recherche renvoie au plus 500 stations ; pour une recherche autour d'une position, ce sont les plus proches.
- **Trajet.** La recherche le long d'un trajet nécessite une clé OpenRouteService gratuite (`ITINERAIRE_API_KEY` dans le fichier `.env`) ; sans clé, la page Trajet affiche le message d'indisponibilité.
- **Mode démonstration.** Avec `VITE_USE_MOCK=true` dans `frontend/.env`, le frontend fonctionne sans backend, avec 6 stations fictives (`frontend/src/data/stations.js`). Ce fichier ne doit pas exister avec `VITE_USE_MOCK=true` quand on construit l'image Docker, sinon l'application déployée utilise aussi les données fictives.

---

## Lancer l'application

Avec Docker (frontend, backend et base PostgreSQL), depuis la racine du projet :

```powershell
Copy-Item .env.example .env      # Linux / macOS : cp .env.example .env
docker compose up --build
```

Complétez d'abord le fichier `.env` (mot de passe PostgreSQL, secret JWT, clé OpenRouteService pour le trajet), puis ouvrez **http://localhost:3000**.

Variante démonstration, sans backend ni Docker (Node.js nécessaire) :

```powershell
cd frontend
Copy-Item .env.example .env      # contient VITE_USE_MOCK=true
npm install
npm run dev
```

L'application est alors accessible sur **http://localhost:3000**, avec les stations fictives.

Le détail (prérequis, configuration, tests, problèmes fréquents) est dans [INSTALL.md](INSTALL.md).

---

## Couverture des tests

> Objectif : au moins **50 %** de couverture pour la V1, la plus élevée possible pour la V2.

**Backend (JaCoCo), mesuré le 21 novembre 2026 : 89 % des instructions, 89 % des lignes et 77 % des branches** (249 tests JUnit, 0 échec).

- La mesure exclut `SmartCarbureApplicationTests`, qui démarre toute l'application et a besoin d'une base PostgreSQL.
- Les 2 tests de `CarburantApiClientIntegrationTest` appellent la vraie API : ils ne sont lancés qu'avec `-Dapi.reelle=true`.
- Le rapport détaillé se trouve dans `backend/target/site/jacoco/index.html`, après `.\mvnw.cmd verify "-Dtest=!SmartCarbureApplicationTests" "-Dsurefire.failIfNoSpecifiedTests=false"` dans le dossier `backend` (voir [INSTALL.md](INSTALL.md)).

**Frontend (Vitest)** : 209 tests dans 27 fichiers ; le taux de couverture n'est pas mesuré.

| Fonctionnalité / composant testé | Type de test | Statut |
|---|---|---|
| Calcul de la distance entre deux points GPS | Unitaire (JUnit) | Fait : `CalculDistanceTest` |
| Recherche des stations dans un rayon donné | Unitaire (JUnit + Mockito) | Fait : `RechercheServiceTest` |
| Filtrage par type de carburant | Unitaire (JUnit + Mockito) | Fait : `RechercheServiceTest` |
| Tri des stations par prix et par distance | Unitaire (JUnit + Mockito) | Fait : `RechercheServiceTest`, `TrajetServiceTest` |
| Lecture et conversion des données JSON de l'API | Unitaire (JUnit) | Fait : `StationJsonConverterTest`, tests des mappers |
| Gestion des données manquantes ou mal formées | Unitaire (JUnit) | Fait : `StationJsonConverterTest`, `GlobalExceptionHandlerTest` |
| Comportement en cas d'indisponibilité de l'API | Unitaire (JUnit + Mockito) | Fait : `CarburantApiClientTest`, `ItineraireClientTest`, tests des contrôleurs (503) |
| Création de compte et connexion | Unitaire (JUnit + Mockito, MockMvc) | Fait : `AuthControllerTest`, `UtilisateurServiceTest`, `JwtServiceTest` |
| Routes REST (paramètres, codes d'erreur) | Unitaire (MockMvc) | Fait : `StationControllerTest`, `TrajetControllerTest`, `EconomieControllerTest`, `ComparaisonControllerTest` |
| Pages et composants du frontend | Unitaire (Vitest, React Testing Library) | Fait : 27 fichiers `*.test.js(x)` dans `frontend/src` |
| Parcours utilisateur depuis l'interface | Recette manuelle (voir [RECETTE.md](RECETTE.md)) | À faire |


---

## Données ouvertes utilisées

| Source | Format | Utilisation |
|---|---|---|
| [Prix des carburants – Flux instantané v2](https://www.data.economie.gouv.fr/explore/dataset/prix-des-carburants-en-france-flux-instantane-v2/) (data.economie.gouv.fr) | API Web / JSON | Localisation des stations, carburants vendus, prix, ruptures de stock, horaires et services (mise à jour toutes les 10 minutes) |
| [OpenRouteService](https://openrouteservice.org/) (api.heigit.org) | API Web / JSON | Calcul de l'itinéraire routier entre deux points (recherche le long d'un trajet, F6) ; clé personnelle gratuite |
| [OpenStreetMap](https://www.openstreetmap.org/copyright) | Tuiles de carte | Fond des cartes interactives (© contributeurs OpenStreetMap) |

### Endpoint utilisé

L'application interroge directement l'API en temps réel, sans téléchargement ni stockage local des données. Les informations affichées sont donc toujours à jour.

```
https://data.economie.gouv.fr/api/explore/v2.1/catalog/datasets/prix-des-carburants-en-france-flux-instantane-v2/records
```

Exemple : les 10 stations de Rennes

```
https://data.economie.gouv.fr/api/explore/v2.1/catalog/datasets/prix-des-carburants-en-france-flux-instantane-v2/records?where=ville="Rennes"&limit=10
```

Données publiées par le Ministère de l'Économie, des Finances et de la Souveraineté industrielle et numérique, sous **Licence Ouverte / Etalab**.

---

## Technologies utilisées

| Domaine | Technologie |
|---|---|
| Backend | Java 21, Spring Boot 4.1 (Spring Web, Spring Security, Spring Data JPA), Lombok |
| Authentification | JSON Web Token (JJWT 0.13), mots de passe hachés avec BCrypt |
| Frontend | React 19, Vite 8, React Router 7 |
| Cartes | Leaflet 1.9, React-Leaflet 5, OpenStreetMap |
| Base de données | PostgreSQL 16, schéma géré par Liquibase |
| Modélisation objet | UML, PlantUML |
| Versionnement du code | Git, GitHub |
| Conteneurisation | Docker, Docker Compose (Nginx pour servir le frontend) |

Outils complémentaires liés à l'écosystème Java :

| Domaine | Technologie |
|---|---|
| Build et dépendances | Maven (Maven Wrapper `mvnw`) |
| Tests unitaires | JUnit 5, Mockito, MockMvc |
| Couverture de tests | JaCoCo 0.8.13 |

Outils de test du frontend : Vitest 5 et React Testing Library.

---

## Participants

| Membre |
|---|
| BALDE Ibrahim |
| ODJOUOYE Adiyo Fouad |
| NGUETSA NDZANA Rodrigue Duran |
| DIARRA Sartière Fatim |
| BAH Ibrahima Kalil |

---

## Documentation

| Document | Contenu |
|---|---|
| [README.md](README.md) | Présentation générale du projet |
| [DESIGN.md](DESIGN.md) | Architecture et modélisation UML |
| [INSTALL.md](INSTALL.md) | Construction du projet, exécution des tests et du logiciel |
| [docs/contrat-api.md](docs/contrat-api.md) | Contrat d'API REST entre le frontend et le backend (routes, paramètres, format JSON, codes d'erreur) |
| [RECETTE.md](RECETTE.md) | Cahier de recette : tests depuis l'interface |