# SmartCarbure – Guide d'installation

> Projet Développement Logiciel (PDL) – Master 1 MIAGE
> ISTIC, Université de Rennes – Année 2026/2027

Ce document explique :

1. [comment construire le projet à partir du code source](#2-construire-le-projet-à-partir-du-code-source) ;
2. [comment exécuter les suites de tests](#3-exécuter-les-suites-de-tests) ;
3. [comment exécuter le logiciel](#4-exécuter-le-logiciel).

Toute l'installation se fait avec **Docker** : il n'est pas nécessaire d'installer Java, Maven, Node.js ou PostgreSQL sur sa machine.

*Ce document sera mis à jour à chaque livraison (V1, V2) pour rester conforme au code.*

---

## Sommaire

1. [Prérequis](#1-prérequis)
2. [Construire le projet à partir du code source](#2-construire-le-projet-à-partir-du-code-source)
3. [Exécuter les suites de tests](#3-exécuter-les-suites-de-tests)
4. [Exécuter le logiciel](#4-exécuter-le-logiciel)
5. [Configuration](#5-configuration)
6. [Commandes utiles](#6-commandes-utiles)
7. [Problèmes fréquents](#7-problèmes-fréquents)

---

## 1. Prérequis

| Outil | Version minimale | Vérification |
|---|---|---|
| Git | 2.x | `git --version` |
| Docker Desktop (Windows / macOS) ou Docker Engine (Linux) | 24.x | `docker --version` |
| Docker Compose | 2.x (inclus dans Docker Desktop) | `docker compose version` |

Sous Windows, Docker Desktop nécessite **WSL 2** ; il propose de l'activer lors de son installation.

Une **connexion Internet** est nécessaire : à la première construction pour télécharger les images et les dépendances, puis à l'exécution car l'application interroge en temps réel l'API Prix des carburants (data.economie.gouv.fr) et le service d'itinéraire.

---

## 2. Construire le projet à partir du code source

### 2.1 Récupérer le code source

```bash
git clone https://github.com/AdiyoFouad/SmartCarbure.git
cd SmartCarbure
```

Organisation du dépôt :

```
SmartCarbure/
├── backend/              Application Spring Boot (Java 21, Maven) + Dockerfile
├── frontend/             Application React + Dockerfile
├── docs/diagrammes/      Diagrammes UML (PlantUML)
├── docker-compose.yml    Description des conteneurs
├── .env.example          Exemple de variables d'environnement
├── README.md
├── DESIGN.md
├── INSTALL.md
└── RECETTE.md
```

### 2.2 Créer le fichier de configuration

```bash
cp .env.example .env
```

Sous Windows : `copy .env.example .env`

Complétez si besoin les valeurs du fichier `.env` (voir [section 5](#5-configuration)).

### 2.3 Construire les images

Depuis la racine du projet :

```bash
docker compose build
```

Cette commande compile le projet à partir du code source, à l'intérieur des conteneurs :

| Conteneur | Ce qui est fait pendant la construction |
|---|---|
| `backend` | Compilation du code Java avec Maven et création du fichier `.jar` |
| `frontend` | Installation des dépendances npm et construction de l'application React |
| `db` | Aucune construction : l'image PostgreSQL officielle est téléchargée |

La première construction peut prendre quelques minutes (téléchargement des images et des dépendances). Les suivantes sont beaucoup plus rapides.

---

## 3. Exécuter les suites de tests

Les tests sont lancés dans des conteneurs dédiés, définis dans `docker-compose.yml` avec le profil `test`. Ils ne démarrent pas avec l'application.

### 3.1 Tests du backend (JUnit 5 et Mockito)

```bash
docker compose run --rm backend-tests
```

Cette commande exécute `mvn verify` : elle lance tous les tests JUnit et génère le rapport de couverture.

Les tests unitaires n'appellent pas les vraies API externes : les clients d'API sont simulés avec **Mockito**. Ils ne dépendent donc ni de la disponibilité de l'API ni de la connexion Internet (hors téléchargement des dépendances).

### 3.2 Couverture de tests (JaCoCo)

Après l'exécution des tests du backend, le rapport de couverture est disponible sur votre machine dans :

```
backend/target/site/jacoco/index.html
```

Ouvrez ce fichier dans un navigateur pour voir le taux de couverture global et le détail par classe. Le taux obtenu est reporté dans la section « Couverture des tests » du [README.md](README.md).

Les résultats détaillés des tests se trouvent dans `backend/target/surefire-reports/`.

### 3.3 Lancer un seul test du backend

```bash
docker compose run --rm backend-tests mvn test -Dtest=CalculDistanceTest
```

### 3.4 Tests du frontend

```bash
docker compose run --rm frontend-tests
```

### 3.5 Tests fonctionnels depuis l'interface

Les tests à réaliser manuellement depuis le frontend pour valider les fonctionnalités sont décrits dans le cahier de recette [RECETTE.md](RECETTE.md).

---

## 4. Exécuter le logiciel

### 4.1 Lancer l'application

```bash
docker compose up
```

Pour construire et lancer en une seule commande : `docker compose up --build`

Pour lancer en arrière-plan et garder le terminal libre : `docker compose up -d`

Trois conteneurs démarrent :

| Conteneur | Rôle | Adresse |
|---|---|---|
| `frontend` | Application React | http://localhost:3000 |
| `backend` | API REST Spring Boot | http://localhost:8080/api |
| `db` | Base de données PostgreSQL | localhost:5432 |

Les tables de la base de données sont créées automatiquement au premier lancement.

### 4.2 Ouvrir l'application

Ouvrez **http://localhost:3000** dans un navigateur.

### 4.3 Vérifier que tout fonctionne

1. Saisissez « Rennes », choisissez le carburant « Gazole » et lancez la recherche.
2. La liste des stations et la carte doivent s'afficher.

Vous pouvez aussi tester directement l'API du backend :

```bash
curl "http://localhost:8080/api/stations?ville=Rennes&carburant=GAZOLE"
```

### 4.4 Arrêter l'application

```bash
docker compose down
```

Pour arrêter et supprimer aussi les données de la base (comptes, favoris) :

```bash
docker compose down -v
```

---

## 5. Configuration

La configuration se fait dans le fichier `.env` à la racine du projet.

| Variable | Rôle | Valeur par défaut |
|---|---|---|
| `POSTGRES_DB` | Nom de la base de données | `smartcarbure` |
| `POSTGRES_USER` | Utilisateur PostgreSQL | `smartcarbure` |
| `POSTGRES_PASSWORD` | Mot de passe PostgreSQL | à définir |
| `CARBURANT_API_URL` | Adresse de l'API Prix des carburants | `https://data.economie.gouv.fr/api/explore/v2.1/catalog/datasets/prix-des-carburants-en-france-flux-instantane-v2/records` |
| `ITINERAIRE_API_URL` | Adresse du service d'itinéraire | à définir |
| `ITINERAIRE_API_KEY` | Clé du service d'itinéraire (si OpenRouteService) | à définir |
| `JWT_SECRET` | Clé de signature des jetons de connexion | à définir |
| `VITE_API_URL` | Adresse du backend utilisée par le frontend | `http://localhost:8080/api` |

Le fichier `.env` contient des mots de passe et des clés : il ne doit **jamais** être envoyé sur GitHub (il est listé dans `.gitignore`). Seul `.env.example`, sans valeurs secrètes, est versionné.

Après une modification du fichier `.env`, relancez l'application avec `docker compose up --build`.

---

## 6. Commandes utiles

| Action | Commande |
|---|---|
| Voir les conteneurs en cours d'exécution | `docker compose ps` |
| Voir les journaux de tous les conteneurs | `docker compose logs -f` |
| Voir les journaux du backend uniquement | `docker compose logs -f backend` |
| Redémarrer un seul conteneur | `docker compose restart backend` |
| Reconstruire après une modification du code | `docker compose up --build` |
| Ouvrir la base de données en ligne de commande | `docker compose exec db psql -U smartcarbure -d smartcarbure` |

---

## 7. Problèmes fréquents

| Problème | Cause probable | Solution |
|---|---|---|
| `Cannot connect to the Docker daemon` ou `docker: command not found` | Docker n'est pas lancé ou pas installé | Lancer Docker Desktop et attendre qu'il soit prêt |
| Docker Desktop ne démarre pas sous Windows | WSL 2 n'est pas activé | Suivre les instructions d'activation de WSL 2 proposées par Docker Desktop |
| `port is already allocated` | Un autre programme utilise le port 3000, 8080 ou 5432 | Arrêter ce programme, ou changer le port dans `docker-compose.yml` |
| Le backend s'arrête au démarrage avec une erreur de connexion à la base | Mauvais identifiants dans `.env` | Vérifier `POSTGRES_USER` et `POSTGRES_PASSWORD`, puis `docker compose down -v` et relancer |
| Le code modifié n'est pas pris en compte | Les images n'ont pas été reconstruites | `docker compose up --build` |
| Message « Service temporairement indisponible » dans l'application | L'API Prix des carburants ne répond pas, ou pas de connexion Internet | Vérifier la connexion, puis réessayer quelques minutes plus tard |
| Le frontend s'affiche mais aucune donnée n'arrive | Le backend n'est pas démarré | `docker compose ps`, puis `docker compose logs backend` pour voir l'erreur |