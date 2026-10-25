# SmartCarbure – Cahier de recette

> Projet Développement Logiciel (PDL) – Master 1 MIAGE
> ISTIC, Université de Rennes – Année 2026/2027

Ce document décrit les tests à effectuer **depuis le frontend** pour valider les fonctionnalités de l'application. Chaque test indique les étapes à suivre et le résultat attendu.

*Ce cahier sera complété et mis à jour à chaque livraison (V1, V2). La colonne « Statut » est remplie lors de chaque campagne de recette.*

---

## Préparation

1. Lancer l'application en suivant [INSTALL.md](INSTALL.md) (`docker compose up --build`).
2. Ouvrir http://localhost:3000 dans un navigateur.
3. Vérifier que la connexion Internet est active (l'application interroge l'API Prix des carburants en temps réel).

**Légende du statut :** OK (conforme), KO (non conforme, une issue GitHub est ouverte), NT (non testé).

---

## 1. Recherche de stations

| ID | Fonctionnalité | Étapes | Résultat attendu | Statut |
|---|---|---|---|---|
| R01 | F1 – Recherche autour de sa position | Cliquer sur « Me localiser », accepter la géolocalisation, choisir Gazole, cliquer sur « Rechercher » | La liste des stations proches qui vendent du Gazole s'affiche avec la distance de chacune | NT |
| R02 | F1 – Géolocalisation refusée | Cliquer sur « Me localiser » et refuser l'accès à la position | Un message invite à saisir une ville ou un code postal, l'application ne plante pas | NT |
| R03 | F2 – Recherche par ville | Saisir « Rennes », choisir Gazole, cliquer sur « Rechercher » | Les stations de Rennes qui vendent du Gazole s'affichent | NT |
| R04 | F2 – Recherche par code postal | Saisir « 35000 », lancer la recherche | Les stations du code postal 35000 s'affichent | NT |
| R05 | F2 – Ville inconnue | Saisir « Villeinexistante », lancer la recherche | Message « Aucune station pour cette ville, vérifiez la saisie » | NT |
| R06 | F3 – Rayon de recherche | Rechercher autour de sa position avec un rayon de 5 km, puis de 20 km | Aucune station affichée n'est à plus de 5 km (puis 20 km) ; la liste à 20 km contient plus de stations | NT |
| R07 | F3 – Aucune station dans le rayon | Rechercher avec un rayon de 1 km dans une zone sans station | Message « Aucune station trouvée, élargissez le rayon » | NT |
| R08 | F4 – Filtre par carburant | Rechercher à Rennes avec E85, puis avec GPLc | Seules les stations qui vendent le carburant choisi s'affichent | NT |
| R09 | F5 – Tri par prix | Rechercher à Rennes, choisir « Trier par prix » | Les stations sont classées du prix le plus bas au plus élevé | NT |
| R10 | F5 – Tri par distance | Rechercher autour de sa position, choisir « Trier par distance » | Les stations sont classées de la plus proche à la plus éloignée | NT |
| R11 | Saisie invalide | Cliquer sur « Rechercher » sans ville ni position | Message d'erreur de saisie, aucune recherche lancée | NT |
| R12 | F6 – Recherche le long d'un trajet | Placer un marqueur de départ et un marqueur d'arrivée sur la carte, choisir SP95, lancer la recherche | L'itinéraire est tracé et les stations situées le long du trajet s'affichent, triées par prix | NT |

## 2. Informations sur les stations

| ID | Fonctionnalité | Étapes | Résultat attendu | Statut |
|---|---|---|---|---|
| S01 | F7 – Fiche d'une station | Cliquer sur une station de la liste | La fiche affiche l'adresse, les horaires, les services, les prix de chaque carburant et leur date de mise à jour | NT |
| S02 | F7 – Station 24h/24 | Ouvrir la fiche d'une station équipée d'un automate | La mention « Automate 24h/24 » est affichée | NT |
| S03 | F7 – Station d'autoroute | Ouvrir la fiche d'une station d'autoroute | La mention « Autoroute » est affichée | NT |
| S04 | F8 – Rupture de stock | Ouvrir la fiche d'une station en rupture sur un carburant | Le carburant concerné porte un badge « Rupture » | NT |
| S05 | F9 – Carte interactive | Lancer une recherche | Les stations apparaissent sur la carte ; un clic sur une icône affiche le nom et le prix | NT |
| S06 | F10 – API indisponible | Couper la connexion Internet du poste, puis lancer une recherche | Message « Service temporairement indisponible » avec un bouton « Réessayer » ; l'application ne plante pas | NT |
| S07 | F10 – Réessayer | Rétablir la connexion, cliquer sur « Réessayer » | Les résultats s'affichent normalement | NT |

## 3. Compte utilisateur

| ID | Fonctionnalité | Étapes | Résultat attendu | Statut |
|---|---|---|---|---|
| U01 | F11 – Création de compte | Remplir le formulaire d'inscription avec un email valide et un mot de passe, valider | Le compte est créé et l'utilisateur est informé du succès | NT |
| U02 | F11 – Email déjà utilisé | S'inscrire avec un email déjà enregistré | Message « Cet email est déjà utilisé » | NT |
| U03 | F11 – Email invalide | S'inscrire avec « test@ » | Message d'erreur sur le format de l'email, compte non créé | NT |
| U04 | F12 – Connexion | Se connecter avec un email et un mot de passe corrects | L'utilisateur est connecté, son nom apparaît dans le menu | NT |
| U05 | F12 – Mauvais mot de passe | Se connecter avec un mot de passe incorrect | Message « Email ou mot de passe incorrect » | NT |
| U06 | F12 – Déconnexion | Cliquer sur « Se déconnecter » | L'utilisateur est déconnecté, les pages réservées ne sont plus accessibles | NT |
| U07 | F13 – Préférences | Connecté, choisir Gazole et 15 km comme préférences, se déconnecter puis se reconnecter | Le carburant et le rayon sont pré-remplis dans la recherche | NT |
| U08 | F14 – Ajouter un favori | Connecté, cliquer sur « Ajouter aux favoris » sur une station | La station apparaît dans la page « Mes favoris » | NT |
| U09 | F14 – Retirer un favori | Dans « Mes favoris », retirer une station | La station disparaît de la liste | NT |
| U10 | F14 – Favoris sans compte | Non connecté, cliquer sur « Ajouter aux favoris » | L'utilisateur est invité à se connecter | NT |

## 4. Comparaison et économies

| ID | Fonctionnalité | Étapes | Résultat attendu | Statut |
|---|---|---|---|---|
| C01 | F15 – Calcul d'économie | Sur la fiche d'une station, saisir un plein de 50 litres | L'économie (ou le surcoût) par rapport au prix moyen local s'affiche en euros | NT |
| C02 | F16 – Comparaison de villes | Comparer Rennes et Nantes pour le Gazole | Le prix moyen de chaque ville et l'écart s'affichent | NT |
| C03 | F17 – Évolution des prix | Ouvrir la page « Évolution des prix » pour le SP95 | Un graphique de l'évolution du prix moyen s'affiche | NT |
| C04 | F18 – Export CSV | Après une recherche, cliquer sur « Exporter en CSV » | Un fichier CSV contenant les stations affichées est téléchargé | NT |

---

## Suivi des campagnes de recette

| Version | Date | Testeur | Tests OK | Tests KO | Tests NT | Issues ouvertes |
|---|---|---|---|---|---|---|
| V1 | | | | | | |
| V2 | | | | | | |

Chaque test KO donne lieu à une **issue GitHub rédigée en anglais**, puis à un test JUnit qui reproduit le bug avant sa correction.