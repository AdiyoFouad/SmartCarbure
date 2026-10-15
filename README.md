# SmartCarbure

> Projet Développement Logiciel (PDL) – Master 1 MIAGE
> ISTIC, Université de Rennes – Année 2026/2027
> Client : Adrien Le Roch

---

## Sommaire

1. [But du projet](#but-du-projet)
2. [Contexte du projet](#contexte-du-projet)
3. [Fonctionnalités supportées](#fonctionnalités-supportées)
4. [Fonctionnalités à développer](#fonctionnalités-à-développer)
5. [Couverture des tests](#couverture-des-tests)
6. [Données ouvertes utilisées](#données-ouvertes-utilisées)
7. [Technologies utilisées](#technologies-utilisées)
8. [Participants](#participants)
9. [Documentation](#documentation)

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

## Fonctionnalités supportées

> Cette section sera mise à jour au fur et à mesure des livraisons (V1 le 22 novembre 2026, V2 le 20 décembre 2026).

*Aucune fonctionnalité n'est encore livrée : le projet est en phase de conception.*

---

## Fonctionnalités à développer

### Recherche de stations

| # | Fonctionnalité | Priorité |
|---|---|---|
| F1 | Rechercher les stations les plus proches et/ou les moins coûteuses en fonction de sa position (géolocalisation) | Essentielle |
| F2 | Rechercher les stations autour d'une ville ou d'une adresse saisie | Essentielle |
| F3 | Définir un rayon de recherche (en km) | Essentielle |
| F4 | Filtrer par type de carburant (Gazole, SP95, SP98, SP95-E10, E85, GPLc) | Essentielle |
| F5 | Trier les résultats par prix ou par distance | Essentielle |
| F6 | Rechercher les stations situées le long d'un trajet (point de départ → destination) | Importante |

### Informations sur les stations

| # | Fonctionnalité | Priorité |
|---|---|---|
| F7 | Consulter la fiche détaillée d'une station (adresse, horaires, services, prix, date de mise à jour) | Essentielle |
| F8 | Être informé des ruptures de stock d'un carburant | Importante |
| F9 | Afficher les stations sur une carte interactive | Importante |
| F10 | Afficher un message d'erreur clair si l'API est indisponible | Importante |

### Compte utilisateur

| # | Fonctionnalité | Priorité |
|---|---|---|
| F11 | Créer un compte utilisateur | Importante |
| F12 | Se connecter à un compte utilisateur | Importante |
| F13 | Enregistrer son carburant et son rayon de recherche préférés | Bonus |
| F14 | Gérer une liste de stations favorites | Bonus |

### Comparaison et économies

| # | Fonctionnalité | Priorité |
|---|---|---|
| F15 | Calculer l'économie réalisée sur un plein par rapport au prix moyen local | Bonus |
| F16 | Comparer les prix moyens entre deux villes ou deux départements | Bonus |
| F17 | Suivre l'évolution des prix d'un carburant dans le temps | Bonus |
| F18 | Exporter les résultats d'une recherche au format CSV | Bonus |

*Ces fonctionnalités et leurs priorités seront validées avec le client et pourront évoluer au cours du projet. Les changements seront justifiés lors de la soutenance.*

---

## Couverture des tests

> Objectif : au moins **50 %** de couverture pour la V1, la plus élevée possible pour la V2.

| Fonctionnalité / composant testé | Type de test | Statut |
|---|---|---|
| Calcul de la distance entre deux points GPS | Unitaire (JUnit) | À faire |
| Recherche des stations dans un rayon donné | Unitaire (JUnit) | À faire |
| Filtrage par type de carburant | Unitaire (JUnit) | À faire |
| Tri des stations par prix et par distance | Unitaire (JUnit) | À faire |
| Lecture et conversion des données JSON de l'API | Unitaire (JUnit) | À faire |
| Gestion des données manquantes ou mal formées | Unitaire (JUnit) | À faire |
| Comportement en cas d'indisponibilité de l'API | Unitaire (JUnit + Mockito) | À faire |
| Création de compte et connexion | Intégration | À faire |
| Parcours utilisateur depuis l'interface | Recette manuelle (voir [RECETTE.md](RECETTE.md)) | À faire |


---

## Données ouvertes utilisées

| Source | Format | Utilisation |
|---|---|---|
| [Prix des carburants – Flux instantané v2](https://www.data.economie.gouv.fr/explore/dataset/prix-des-carburants-en-france-flux-instantane-v2/) (data.economie.gouv.fr) | API Web / JSON | Localisation des stations, carburants vendus, prix, ruptures de stock, horaires et services (mise à jour toutes les 10 minutes) |
| [API Adresse](https://adresse.data.gouv.fr/) (adresse.data.gouv.fr) | API Web / JSON | Conversion d'une ville ou d'une adresse en coordonnées GPS |

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
| Backend | Java, Spring Boot |
| Frontend | React |
| Base de données | PostgreSQL |
| Modélisation objet | UML, PlantUML |
| Versionnement du code | Git, GitHub |
| Conteneurisation | Docker |

Outils complémentaires liés à l'écosystème Java :

| Domaine | Technologie |
|---|---|
| Build et dépendances | Maven |
| Tests unitaires | JUnit 5, Mockito |
| Couverture de tests |

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
| [RECETTE.md](RECETTE.md) | Cahier de recette : tests depuis l'interface |