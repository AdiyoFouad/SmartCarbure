# Étude du trajet (L3-03)

> Fonctionnalité F6 : trouver les stations le long d'un trajet.
> Auteur : NGUETSA NDZANA Rodrigue Duran – à valider par le groupe.

## 1. Objectif

L'utilisateur place un point de départ et un point d'arrivée sur la carte, choisit un carburant, et voit les stations **proches de sa route**, de la moins chère à la plus chère.

« Proche de la route » veut dire : à **5 km maximum** de la route (un couloir de 5 km de chaque côté).

## 2. Service d'itinéraire

**OpenRouteService** *(sous réserve du vote du groupe)*

- Gratuit, sans carte bancaire, avec une clé personnelle gratuite.
- 2 000 trajets par jour : largement assez pour le projet.
- Appelé par le **backend** uniquement ; la clé est dans le fichier `.env`, jamais sur GitHub.
- Un seul appel par recherche.

Alternatives écartées : OSRM (serveur de démonstration sans garantie, peut tomber) et GraphHopper (quota plus faible).

## 3. Comment ça marche

L'API des carburants ne sait chercher que **dans un cercle autour d'un point**. On recouvre donc la route avec des cercles :

1. On demande la route à OpenRouteService.
2. On place un point **tous les 3 km** le long de la route.
3. Autour de chaque point, on récupère les stations dans un cercle un peu plus large que le couloir. La méthode `rechercherAutourDePoints` d'Ibrahim (L1-02) regroupe jusqu'à 100 points dans un seul appel et supprime déjà les doublons.
4. On ne garde que les stations à **5 km maximum** de la route.
5. On garde le carburant demandé et on trie par prix.

Exemple : Rennes → Nantes (environ 110 km) demande **1 appel** à OpenRouteService et **1 recherche** dans l'API des carburants (quelques pages de 100 stations).

## 4. Réglages

| Réglage | Valeur |
|---|---|
| Espacement des points sur la route | 3 km |
| Distance maximale d'une station à la route | 5 km par défaut, modifiable de 1 à 10 km |
| Taille des cercles de recherche | Distance maximale + 1,5 km (6,5 km par défaut) : aucune station n'est ratée, même dans les virages |

Ces valeurs sont des constantes du code, faciles à ajuster après des tests.

## 5. Décisions proposées

| Sujet | Proposition |
|---|---|
| Recalcul | Seulement au clic sur « Rechercher sur ce trajet », pas à chaque déplacement d'un marqueur (pour ménager le quota) |
| Stations en rupture | Gardées, en fin de liste, comme dans la recherche classique |
| Trajets très longs | Limite de 500 km, avec un message clair au-delà |
| Service indisponible | Erreur 503 si OpenRouteService ou l'API des carburants ne répond pas (F10) |

## 6. Modification du contrat d'API

Ajouter un paramètre **facultatif** à `GET /api/trajet` :

| Paramètre | Obligatoire | Valeurs |
|---|---|---|
| `rayon` | non | distance maximale à la route, en km, de 1 à 10 (5 par défaut) |

Et retirer le point 3 de la section « Points à valider par le groupe ».

## 7. Questions ouvertes

- **@Ibrahim** : `rechercherAutour` (L1-02) gère-t-il plus de 100 stations ? L'API en renvoie 100 maximum par appel.
- **Client** : la limite de 500 km et le rayon modifiable lui conviennent-ils ?
