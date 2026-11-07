# SmartCarbure : contrat d'API entre le frontend et le backend

Ce document décrit les routes REST que le frontend appelle. Le backend doit les respecter (chemins, paramètres, format JSON, codes d'erreur). Toute modification doit être décidée par les deux équipes, puis reportée ici.

## Sommaire

1. [Conventions](#conventions)
2. [Codes d'erreur](#codes-derreur)
3. [Récapitulatif des routes](#récapitulatif-des-routes)
4. [Modèles JSON](#modèles-json)
5. [Routes](#routes)
6. [Export CSV (F18)](#export-csv-f18)
7. [Décisions prises](#décisions-prises)
8. [Points à valider par le groupe](#points-à-valider-par-le-groupe)
9. [DTO cités par le contrat](#dto-cités-par-le-contrat)

## Conventions

- **Préfixe :** toutes les routes commencent par `/api`. Le frontend utilise uniquement des URL relatives :
  - en développement, Vite redirige `/api` vers `http://localhost:8080` (voir `frontend/vite.config.js`) ;
  - dans Docker, c'est Nginx qui redirige `/api/` vers `http://backend:8080/api/` (voir `frontend/nginx.conf`).
- **Format :** les requêtes et les réponses sont en JSON (`Content-Type: application/json`), encodées en UTF-8.
- **Dates :** format ISO 8601 en UTC, avec le suffixe `Z` (`2026-11-03T14:05:00Z`). L'API Prix des carburants fournit des heures locales (Europe/Paris) : le backend les convertit en UTC.
- **Prix :** en euros par litre, nombre décimal avec trois décimales (`1.789`). Les montants (économie) sont arrondis au centime (`1.10`).
- **Distances :** en kilomètres, arrondies à une décimale (`3.2`).
- **Codes carburant :** `GAZOLE`, `SP95`, `SP98`, `E10`, `E85`, `GPLC`. Un autre code renvoie une erreur 400.
- **Tri :** `PRIX` ou `DISTANCE`.
- **Rayon de recherche :** entier de 1 à 50 (km).
- **Routes protégées :** elles attendent l'en-tête `Authorization: Bearer <jeton>`, où le jeton est celui renvoyé par `POST /api/auth/connexion`. Sans jeton, ou avec un jeton invalide ou expiré, elles renvoient **401** (jamais 403).
- **Champs absents :** un champ sans valeur est renvoyé avec `null`, jamais omis, sauf mention contraire dans la description du modèle.

## Codes d'erreur

Toutes les erreurs, sans exception, renvoient un corps JSON de ce format :

```json
{ "statut": 400, "message": "Le rayon doit être compris entre 1 et 50 km." }
```

| Champ | Contenu |
|---|---|
| `statut` | le code HTTP de la réponse (même valeur que le statut HTTP) |
| `message` | une explication lisible en français, utile pour le débogage |

Le frontend affiche ses propres messages, choisis selon le code HTTP :

| Code | Signification | Message affiché par le frontend |
|---|---|---|
| 400 | Saisie invalide (paramètre manquant, mal formé ou hors limites) | « Saisie invalide, vérifiez les champs du formulaire. » |
| 401 | Non connecté, jeton invalide ou expiré, ou identifiants incorrects à la connexion | « Vous devez être connecté… » ou « Email ou mot de passe incorrect » |
| 404 | Ressource introuvable : station inconnue (`/api/stations/{id}`, `/api/economie`, `POST /api/favoris`) ou favori absent (`DELETE /api/favoris/{idStation}`) | « L'élément demandé est introuvable. » |
| 409 | Conflit : email déjà utilisé à l'inscription, ou station déjà en favori | « Cet email est déjà utilisé » ou « Cette station est déjà dans vos favoris » |
| 503 | API Prix des carburants ou service d'itinéraire indisponible | « Service temporairement indisponible… » avec un bouton « Réessayer » |

Le frontend traite aussi comme un 503 :
- une panne réseau ;
- un délai d'attente dépassé (10 secondes) ;
- les codes 502 et 504 renvoyés par Nginx quand le backend est arrêté.

Une ville ou un code postal inconnu **n'est pas une erreur** : la recherche renvoie 200 avec `[]`.

## Récapitulatif des routes

| Méthode | Route | Protégée | Fonctionnalité | Responsable |
|---|---|---|---|---|
| GET | `/api/stations?lat=&lon=&rayon=&carburant=&tri=` | non | F1, F3, F4, F5 | ODJOUOYE Adiyo Fouad |
| GET | `/api/stations?ville=&carburant=&tri=` | non | F2, F4, F5 | ODJOUOYE Adiyo Fouad |
| GET | `/api/comparer?type=&zone1=&zone2=&carburant=` | non | F16 | ODJOUOYE Adiyo Fouad |
| GET | `/api/stations/{id}` | non | F7, F8 | BALDE Ibrahim |
| GET | `/api/preferences` | oui | F13 | BALDE Ibrahim |
| PUT | `/api/preferences` | oui | F13 | BALDE Ibrahim |
| GET | `/api/trajet?depart=&arrivee=&carburant=` | non | F6, F9 | NGUETSA NDZANA Rodrigue Duran |
| POST | `/api/auth/inscription` | non | F11 | DIARRA Sartière Fatim |
| POST | `/api/auth/connexion` | non | F12 | DIARRA Sartière Fatim |
| GET | `/api/favoris` | oui | F14 | BAH Ibrahima Kalil |
| POST | `/api/favoris` | oui | F14 | BAH Ibrahima Kalil |
| DELETE | `/api/favoris/{idStation}` | oui | F14 | BAH Ibrahima Kalil |
| GET | `/api/economie?idStation=&carburant=&litres=` | non | F15 | BAH Ibrahima Kalil |
| — | Export CSV (pas de route, généré par le frontend) | — | F18 | BAH Ibrahima Kalil |

La carte (F9) n'a pas de route propre : elle affiche les positions renvoyées par les routes de recherche et de trajet.

## Modèles JSON

### Station

Utilisée dans toutes les réponses qui contiennent des stations (recherche, fiche, trajet, favoris).

```json
{
  "id": "35000001",
  "nom": "Station Rennes Centre",
  "adresse": "12 boulevard de la Liberté",
  "ville": "Rennes",
  "codePostal": "35000",
  "departement": { "code": "35", "nom": "Ille-et-Vilaine" },
  "region": { "code": "53", "nom": "Bretagne" },
  "position": { "latitude": 48.1066, "longitude": -1.6793 },
  "emplacement": "ROUTE",
  "automate24h": true,
  "horaires": [
    { "jour": "lundi", "ouverture": "07:00", "fermeture": "21:00" },
    { "jour": "mardi", "ouverture": "07:00", "fermeture": "21:00" },
    { "jour": "mercredi", "ouverture": "07:00", "fermeture": "21:00" },
    { "jour": "jeudi", "ouverture": "07:00", "fermeture": "21:00" },
    { "jour": "vendredi", "ouverture": "07:00", "fermeture": "21:00" },
    { "jour": "samedi", "ouverture": "08:00", "fermeture": "20:00" },
    { "jour": "dimanche", "ouverture": null, "fermeture": null }
  ],
  "services": ["Boutique alimentaire", "Gonflage"],
  "carburants": [
    {
      "type": "GAZOLE",
      "prix": 1.749,
      "dateMaj": "2026-11-03T13:40:00Z",
      "disponible": true,
      "typeRupture": null,
      "debutRupture": null
    },
    {
      "type": "E85",
      "prix": 0.899,
      "dateMaj": "2026-11-02T09:15:00Z",
      "disponible": false,
      "typeRupture": "TEMPORAIRE",
      "debutRupture": "2026-11-02T09:15:00Z"
    }
  ],
  "distanceKm": 1.4
}
```

| Champ | Valeurs |
|---|---|
| `id` | identifiant de la station dans l'API Prix des carburants (chaîne de caractères) |
| `departement`, `region` | code et nom ; utilisés par la comparaison par département (F16) |
| `emplacement` | `ROUTE` ou `AUTOROUTE` (champ `pop` de l'API : R ou A) |
| `automate24h` | `true` si la station a un automate de paiement 24h/24 |
| `horaires` | sept éléments, un par jour (`lundi` … `dimanche`) ; `ouverture` et `fermeture` à `null` si la station est fermée ce jour-là ; `[]` si l'API ne fournit pas d'horaires |
| `services` | libellés fournis par l'API ; `[]` si aucun |
| `carburants[].type` | `GAZOLE`, `SP95`, `SP98`, `E10`, `E85`, `GPLC` |
| `carburants[].disponible` | `false` si le carburant est en rupture (F8) |
| `carburants[].typeRupture` | `TEMPORAIRE`, `DEFINITIVE`, ou `null` si le carburant est disponible |
| `carburants[].debutRupture` | date de début de la rupture, ou `null` |
| `distanceKm` | distance à la position recherchée, ou au trajet pour `/api/trajet` ; `null` pour une recherche par ville ; **absent** de `GET /api/stations/{id}` et de `GET /api/favoris` |

### Utilisateur

```json
{
  "id": 1,
  "nom": "Camille Martin",
  "email": "demo@smartcarbure.fr",
  "carburantPrefere": "GAZOLE",
  "rayonPrefere": 15
}
```

`carburantPrefere` et `rayonPrefere` peuvent valoir `null`. Le mot de passe n'est jamais renvoyé.

## Routes

### Rechercher des stations autour d'une position (F1, F3, F4, F5)

`GET /api/stations?lat=48.1173&lon=-1.6778&rayon=10&carburant=GAZOLE&tri=PRIX`

**Protégée :** non.

| Paramètre | Obligatoire | Valeurs |
|---|---|---|
| `lat`, `lon` | oui | coordonnées GPS (nombres décimaux) |
| `rayon` | oui | entier de 1 à 50 (km) |
| `carburant` | oui | code du carburant |
| `tri` | non | `PRIX` (par défaut) ou `DISTANCE` |

**Réponse 200 :** un tableau de stations, triées selon le paramètre `tri`, avec `distanceKm` renseigné. Seules les stations qui vendent le carburant demandé sont renvoyées. Les stations en rupture sur ce carburant sont incluses, avec `disponible: false`. Quand le tri est `PRIX`, elles sont placées en fin de liste.

```json
[
  {
    "id": "35200001",
    "nom": "Supermarché Rennes Sud",
    "adresse": "2 avenue des Pays-Bas",
    "ville": "Rennes",
    "codePostal": "35200",
    "departement": { "code": "35", "nom": "Ille-et-Vilaine" },
    "region": { "code": "53", "nom": "Bretagne" },
    "position": { "latitude": 48.0889, "longitude": -1.6512 },
    "emplacement": "ROUTE",
    "automate24h": true,
    "horaires": [
      { "jour": "lundi", "ouverture": "08:30", "fermeture": "20:30" },
      { "jour": "mardi", "ouverture": "08:30", "fermeture": "20:30" },
      { "jour": "mercredi", "ouverture": "08:30", "fermeture": "20:30" },
      { "jour": "jeudi", "ouverture": "08:30", "fermeture": "20:30" },
      { "jour": "vendredi", "ouverture": "08:30", "fermeture": "20:30" },
      { "jour": "samedi", "ouverture": "08:30", "fermeture": "20:30" },
      { "jour": "dimanche", "ouverture": null, "fermeture": null }
    ],
    "services": ["Lavage haute pression", "Boutique alimentaire"],
    "carburants": [
      { "type": "GAZOLE", "prix": 1.689, "dateMaj": "2026-11-03T13:50:00Z", "disponible": true, "typeRupture": null, "debutRupture": null },
      { "type": "SP95", "prix": 1.789, "dateMaj": "2026-11-03T13:50:00Z", "disponible": true, "typeRupture": null, "debutRupture": null }
    ],
    "distanceKm": 3.6
  }
]
```

**Aucune station dans le rayon :** réponse 200 avec `[]`. Le frontend affiche « Aucune station trouvée, élargissez le rayon ».

**Erreurs :**
- 400 si `lat` ou `lon` est mal formé, si `rayon` est hors limites, ou si `carburant` ou `tri` est inconnu ;
- 503 si l'API Prix des carburants est indisponible.

### Rechercher des stations par ville ou code postal (F2, F4, F5)

`GET /api/stations?ville=Rennes&carburant=GAZOLE&tri=PRIX`

**Protégée :** non.

| Paramètre | Obligatoire | Valeurs |
|---|---|---|
| `ville` | oui | nom de ville (`Rennes`) ou code postal (`35000`) ; la casse et les accents sont ignorés |
| `carburant` | oui | code du carburant |
| `tri` | non | `PRIX` (par défaut) ou `DISTANCE` |

Si `ville` est fourni, `lat`, `lon` et `rayon` sont ignorés.

**Réponse 200 :** un tableau de stations au même format que la recherche par position, avec `distanceKm: null`.

```json
[
  {
    "id": "35000001",
    "nom": "Station Rennes Centre",
    "adresse": "12 boulevard de la Liberté",
    "ville": "Rennes",
    "codePostal": "35000",
    "departement": { "code": "35", "nom": "Ille-et-Vilaine" },
    "region": { "code": "53", "nom": "Bretagne" },
    "position": { "latitude": 48.1066, "longitude": -1.6793 },
    "emplacement": "ROUTE",
    "automate24h": true,
    "horaires": [],
    "services": ["Boutique alimentaire", "Gonflage"],
    "carburants": [
      { "type": "GAZOLE", "prix": 1.749, "dateMaj": "2026-11-03T13:40:00Z", "disponible": true, "typeRupture": null, "debutRupture": null }
    ],
    "distanceKm": null
  }
]
```

**Ville ou code postal inconnu :** réponse **200 avec `[]`**, et non un 404 (voir le diagramme `sequence_erreurs` de DESIGN.md et le test R05). Le frontend affiche « Aucune station pour cette ville, vérifiez la saisie ».

**Erreurs :**
- 400 si ni `ville` ni `lat`/`lon` ne sont fournis, ou si `carburant` ou `tri` est inconnu ;
- 503 si l'API Prix des carburants est indisponible.

### Fiche d'une station (F7, F8)

`GET /api/stations/35000001`

**Protégée :** non.

**Réponse 200 :** une station complète, sans le champ `distanceKm`. La liste `carburants` contient **tous** les carburants de la station, y compris ceux en rupture (F8).

```json
{
  "id": "35000002",
  "nom": "Relais de Cleunay",
  "adresse": "45 rue Jules Lallemand",
  "ville": "Rennes",
  "codePostal": "35000",
  "departement": { "code": "35", "nom": "Ille-et-Vilaine" },
  "region": { "code": "53", "nom": "Bretagne" },
  "position": { "latitude": 48.1031, "longitude": -1.7095 },
  "emplacement": "ROUTE",
  "automate24h": false,
  "horaires": [
    { "jour": "lundi", "ouverture": "08:00", "fermeture": "20:00" },
    { "jour": "mardi", "ouverture": "08:00", "fermeture": "20:00" },
    { "jour": "mercredi", "ouverture": "08:00", "fermeture": "20:00" },
    { "jour": "jeudi", "ouverture": "08:00", "fermeture": "20:00" },
    { "jour": "vendredi", "ouverture": "08:00", "fermeture": "20:00" },
    { "jour": "samedi", "ouverture": "08:00", "fermeture": "20:00" },
    { "jour": "dimanche", "ouverture": null, "fermeture": null }
  ],
  "services": ["Lavage automatique", "Boutique alimentaire", "Toilettes publiques"],
  "carburants": [
    { "type": "GAZOLE", "prix": 1.719, "dateMaj": "2026-11-03T13:25:00Z", "disponible": true, "typeRupture": null, "debutRupture": null },
    { "type": "E10", "prix": 1.779, "dateMaj": "2026-11-03T13:25:00Z", "disponible": true, "typeRupture": null, "debutRupture": null },
    { "type": "E85", "prix": 0.899, "dateMaj": "2026-11-02T09:15:00Z", "disponible": false, "typeRupture": "TEMPORAIRE", "debutRupture": "2026-11-02T09:15:00Z" }
  ]
}
```

**Erreurs :**
- 404 si la station n'existe pas ;
- 503 si l'API Prix des carburants est indisponible.

### Stations le long d'un trajet (F6)

`GET /api/trajet?depart=48.1173,-1.6778&arrivee=47.2184,-1.5536&carburant=SP95`

**Protégée :** non.

| Paramètre | Obligatoire | Valeurs |
|---|---|---|
| `depart` | oui | `latitude,longitude` du point de départ |
| `arrivee` | oui | `latitude,longitude` du point d'arrivée |
| `carburant` | oui | code du carburant |

**Réponse 200 :**

```json
{
  "trajet": {
    "depart": { "latitude": 48.1173, "longitude": -1.6778 },
    "arrivee": { "latitude": 47.2184, "longitude": -1.5536 },
    "points": [
      { "latitude": 48.1173, "longitude": -1.6778 },
      { "latitude": 47.8431, "longitude": -1.6819 },
      { "latitude": 47.2184, "longitude": -1.5536 }
    ],
    "distanceKm": 107.4
  },
  "stations": [
    {
      "id": "35470001",
      "nom": "Station du Pont de Bain",
      "adresse": "5 route de Rennes",
      "ville": "Bain-de-Bretagne",
      "codePostal": "35470",
      "departement": { "code": "35", "nom": "Ille-et-Vilaine" },
      "region": { "code": "53", "nom": "Bretagne" },
      "position": { "latitude": 47.8431, "longitude": -1.6819 },
      "emplacement": "ROUTE",
      "automate24h": true,
      "horaires": [],
      "services": ["Gonflage", "Boutique alimentaire"],
      "carburants": [
        { "type": "SP95", "prix": 1.809, "dateMaj": "2026-11-03T13:10:00Z", "disponible": true, "typeRupture": null, "debutRupture": null }
      ],
      "distanceKm": 0.2
    }
  ]
}
```

- `points` contient les points de l'itinéraire, dans l'ordre, du départ à l'arrivée. Le frontend les relie pour tracer le trajet sur la carte.
- `distanceKm` (dans `trajet`) est la longueur de l'itinéraire.
- `stations` contient les stations le long du trajet, sans doublon, filtrées par carburant et triées par prix. Pour chaque station, `distanceKm` est la distance au trajet.
- Aucune station le long du trajet : `stations` vaut `[]` (réponse 200).

**Erreurs :**
- 400 si un paramètre manque, si les coordonnées sont mal formées, ou si le carburant est inconnu ;
- 503 si le service d'itinéraire ou l'API carburants est indisponible.

### Comparer deux zones (F16)

`GET /api/comparer?type=VILLE&zone1=Rennes&zone2=Nantes&carburant=GAZOLE`

**Protégée :** non.

| Paramètre | Obligatoire | Valeurs |
|---|---|---|
| `type` | oui | `VILLE` (nom ou code postal) ou `DEPARTEMENT` (code, par exemple `35`, ou nom) |
| `zone1`, `zone2` | oui | zones à comparer |
| `carburant` | oui | code du carburant |

**Réponse 200 :**

```json
{
  "type": "VILLE",
  "carburant": "GAZOLE",
  "zone1": { "nom": "Rennes", "prixMoyen": 1.742, "nombreStations": 4 },
  "zone2": { "nom": "Nantes", "prixMoyen": 1.746, "nombreStations": 3 },
  "ecart": -0.004
}
```

Exemple par département : `GET /api/comparer?type=DEPARTEMENT&zone1=35&zone2=44&carburant=GAZOLE`

```json
{
  "type": "DEPARTEMENT",
  "carburant": "GAZOLE",
  "zone1": { "nom": "Ille-et-Vilaine (35)", "prixMoyen": 1.733, "nombreStations": 7 },
  "zone2": { "nom": "Loire-Atlantique (44)", "prixMoyen": 1.755, "nombreStations": 6 },
  "ecart": -0.022
}
```

- `nom` est le nom officiel de la zone, tel qu'il doit être affiché.
- Le prix moyen ne compte que les stations où le carburant est disponible (stations en rupture exclues).
- `ecart` = prix moyen de la zone 1 moins prix moyen de la zone 2.
- Une zone sans station renvoie `prixMoyen: null` et `nombreStations: 0`. Dans ce cas, `ecart` vaut `null` (réponse 200, pas d'erreur).

**Erreurs :**
- 400 si un paramètre manque, si `type` ou `carburant` est inconnu ;
- 503 si l'API Prix des carburants est indisponible.

### Économie sur un plein (F15)

`GET /api/economie?idStation=35000001&carburant=GAZOLE&litres=50`

**Protégée :** non.

| Paramètre | Obligatoire | Valeurs |
|---|---|---|
| `idStation` | oui | identifiant de la station |
| `carburant` | oui | code du carburant |
| `litres` | oui | volume du plein, nombre strictement positif |

Le prix moyen local est calculé par le backend, sur les stations de la même ville qui vendent ce carburant (stations en rupture exclues).

**Réponse 200 :**

```json
{
  "prixStation": 1.749,
  "prixMoyenLocal": 1.771,
  "litres": 50,
  "montant": 1.10
}
```

`montant` = (`prixMoyenLocal` − `prixStation`) × `litres`, arrondi au centime : positif pour une économie, négatif pour un surcoût.

**Erreurs :**
- 400 si `litres` n'est pas un nombre positif, ou si la station ne vend pas ce carburant (ou l'a en rupture) ;
- 404 si la station n'existe pas ;
- 503 si l'API Prix des carburants est indisponible.

### Inscription (F11)

`POST /api/auth/inscription`

**Protégée :** non.

```json
{ "nom": "Camille Martin", "email": "camille@exemple.fr", "motDePasse": "motdepasse" }
```

| Champ | Règle |
|---|---|
| `nom` | obligatoire, non vide |
| `email` | obligatoire, format valide (`test@` est refusé) |
| `motDePasse` | obligatoire, 8 caractères minimum |

**Réponse 201 :** l'utilisateur créé. L'utilisateur doit ensuite se connecter.

```json
{
  "id": 2,
  "nom": "Camille Martin",
  "email": "camille@exemple.fr",
  "carburantPrefere": null,
  "rayonPrefere": null
}
```

**Erreurs :**
- 400 si un champ manque, si l'email est mal formé ou si le mot de passe est trop court ;
- 409 si l'email est déjà utilisé. Le frontend affiche « Cet email est déjà utilisé ».

### Connexion (F12)

`POST /api/auth/connexion`

**Protégée :** non.

```json
{ "email": "demo@smartcarbure.fr", "motDePasse": "motdepasse" }
```

**Réponse 200 :**

```json
{
  "jeton": "eyJhbGciOiJIUzI1NiJ9...",
  "utilisateur": { "id": 1, "nom": "Camille Martin", "email": "demo@smartcarbure.fr", "carburantPrefere": "GAZOLE", "rayonPrefere": 15 }
}
```

Les préférences sont renvoyées dans `utilisateur` pour que le formulaire de recherche soit pré-rempli dès la connexion (test U07).

**Erreurs :**
- 400 si un champ manque ;
- 401 si l'email ou le mot de passe est incorrect. Le frontend affiche « Email ou mot de passe incorrect ».

### Favoris (F14), connexion requise

**Protégées :** oui, les trois routes. Elles renvoient 401 sans jeton valide.

#### Lister ses favoris

`GET /api/favoris`

**Réponse 200 :** un tableau de stations (modèle Station, sans `distanceKm`), de la plus récemment ajoutée à la plus ancienne. Chaque élément porte en plus le champ `dateAjout`. `[]` si l'utilisateur n'a aucun favori.

```json
[
  {
    "id": "44000001",
    "nom": "Station Nantes Centre",
    "adresse": "3 quai de la Fosse",
    "ville": "Nantes",
    "codePostal": "44000",
    "departement": { "code": "44", "nom": "Loire-Atlantique" },
    "region": { "code": "52", "nom": "Pays de la Loire" },
    "position": { "latitude": 47.2116, "longitude": -1.5655 },
    "emplacement": "ROUTE",
    "automate24h": true,
    "horaires": [],
    "services": ["Boutique alimentaire"],
    "carburants": [
      { "type": "GAZOLE", "prix": 1.779, "dateMaj": "2026-11-03T13:35:00Z", "disponible": true, "typeRupture": null, "debutRupture": null }
    ],
    "dateAjout": "2026-11-02T18:20:00Z"
  }
]
```

**Erreurs :**
- 401 sans jeton valide ;
- 503 si l'API Prix des carburants est indisponible (les stations sont relues en temps réel).

#### Ajouter un favori

`POST /api/favoris`

```json
{ "idStation": "35000001" }
```

**Réponse 201 :**

```json
{ "idStation": "35000001", "dateAjout": "2026-11-07T10:12:00Z" }
```

**Erreurs :**
- 400 si `idStation` manque ;
- 401 sans jeton valide ;
- 404 si la station n'existe pas ;
- 409 si la station est déjà dans les favoris de l'utilisateur.

#### Retirer un favori

`DELETE /api/favoris/35000001`

**Réponse 204 :** aucun corps.

**Erreurs :**
- 401 sans jeton valide ;
- 404 si la station n'est pas dans les favoris de l'utilisateur.

### Préférences (F13), connexion requise

**Protégées :** oui, les deux routes.

#### Lire ses préférences

`GET /api/preferences`

**Réponse 200 :**

```json
{ "carburantPrefere": "GAZOLE", "rayonPrefere": 15 }
```

Un utilisateur qui n'a jamais enregistré de préférences reçoit `{ "carburantPrefere": null, "rayonPrefere": null }`.

#### Enregistrer ses préférences

`PUT /api/preferences`

```json
{ "carburantPrefere": "GAZOLE", "rayonPrefere": 15 }
```

Les deux champs sont facultatifs et peuvent valoir `null` (aucune préférence).

**Réponse 200 :** les préférences enregistrées, au même format.

```json
{ "carburantPrefere": "GAZOLE", "rayonPrefere": 15 }
```

**Erreurs (pour les deux routes) :**
- 400 si le carburant est inconnu ou si le rayon est hors des limites (1 à 50 km) ;
- 401 sans jeton valide.

## Export CSV (F18)

L'export CSV n'a **pas de route** : le frontend génère le fichier à partir des stations déjà affichées, sans nouvel appel au backend.

| Propriété | Valeur |
|---|---|
| Nom du fichier | `smartcarbure-stations-AAAA-MM-JJ.csv` (date du jour) |
| Encodage | UTF-8 avec BOM, pour qu'Excel affiche correctement les accents |
| Séparateur | point-virgule `;` |
| Fin de ligne | `\r\n` |
| Décimales | virgule (`1,749`) |
| Texte contenant `;`, `"` ou un retour à la ligne | entouré de guillemets, les guillemets internes doublés |

Ligne d'en-tête exacte :

```
Nom;Adresse;Code postal;Ville;Carburant;Prix (€/L);Disponible;Distance (km);Mise à jour
```

Une ligne par station, pour le carburant recherché :

```
Station Rennes Centre;12 boulevard de la Liberté;35000;Rennes;Gazole;1,749;Oui;1,4;2026-11-03T13:40:00Z
```

| Colonne | Contenu |
|---|---|
| Carburant | libellé affiché (`Gazole`, `SP95`, `SP98`, `SP95-E10`, `E85`, `GPLc`) |
| Disponible | `Oui` ou `Non` (rupture) |
| Distance (km) | vide pour une recherche par ville |
| Mise à jour | champ `dateMaj` du carburant, tel que reçu |

## Décisions prises

Ces choix ont été tranchés lors de la rédaction du contrat (tâche C1). Les autres formats proposés ont été écartés.

| Sujet | Décision | Format écarté | Raison |
|---|---|---|---|
| Réponse de connexion | `{ jeton, utilisateur: { id, nom, email, carburantPrefere, rayonPrefere } }` | `{ jeton, nom, email }` | Les préférences arrivent dès la connexion : le formulaire de recherche est pré-rempli sans appel supplémentaire (test U07). |
| Réponse d'inscription | 201 avec l'utilisateur créé ; l'utilisateur se connecte ensuite | 201 `{ jeton, nom, email }` (connexion automatique) | Un seul endroit délivre un jeton (`/api/auth/connexion`) ; le test U01 demande seulement d'informer l'utilisateur du succès. |
| Contenu de `GET /api/favoris` | Stations complètes, avec le champ `dateAjout` | `{ idStation, dateAjout }` seulement | La page « Mes favoris » s'affiche en un seul appel, sans un `GET /api/stations/{id}` par favori. `POST /api/favoris` renvoie bien `{ idStation, dateAjout }`. |
| Nom du champ du carburant préféré | `carburantPrefere`, partout (Utilisateur et `/api/preferences`) | `codeCarburantPrefere` | Un seul nom pour le même champ dans tous les objets JSON ; il contient toujours un code carburant (`GAZOLE`…). |
| Utilisateur non connecté sur une route protégée | 401, jamais 403 | 403 (comportement par défaut de Spring Security) | Le frontend redirige vers la page de connexion sur un 401 ; `SecurityConfig` doit configurer ce statut. |

## Points à valider par le groupe

Ces points ne sont pas tranchés. Tant qu'ils ne le sont pas, la version écrite plus haut fait foi.

1. **Rayon pour une recherche par ville.** Le contrat ignore `rayon` quand `ville` est fourni (stations de la commune uniquement). F2 parle de stations « autour d'une ville » : faut-il plutôt chercher dans un rayon autour du centre de la ville ?
2. **Tri `DISTANCE` sans position.** Pour une recherche par ville, `distanceKm` vaut `null`. Proposition : le backend trie alors par prix, sans erreur.
3. **Distance maximale au trajet (F6).** Le contrat ne fixe pas à partir de quelle distance une station est « le long du trajet ». Proposition : 5 km, avec un paramètre `rayon` facultatif.
4. **Prix d'un carburant en rupture.** L'API ne fournit pas toujours de prix pour un carburant en rupture définitive. Faut-il renvoyer `prix: null` dans ce cas ? Le frontend doit alors l'accepter.
5. **Zone de référence de l'économie (F15).** Le contrat prend « les stations de la même ville ». Pour une petite commune avec une seule station, l'économie vaut toujours 0. Faut-il plutôt un rayon autour de la station, ou le département ?
6. **Nombre de résultats.** Aucune limite n'est fixée pour la recherche par position avec un grand rayon. Faut-il un maximum (par exemple 100 stations) ?
7. **Durée de validité du jeton.** À fixer (par exemple 24 h). À l'expiration, les routes protégées renvoient 401 et le frontend redirige vers la page de connexion.

## DTO cités par le contrat

Ces classes appartiennent au package `fr.miage.smartcarbure.dto`. Le contrat les cite ; celles qui manquent restent à créer par l'équipe backend. La colonne « Existe déjà » concerne les classes vides de la branche `feature/structure-backend`.

| DTO | Rôle dans le contrat | Existe déjà |
|---|---|---|
| `StationDTO` | modèle Station (recherche, fiche, trajet, favoris) | oui |
| `CritereRecherche` | paramètres de `GET /api/stations` | oui |
| `TypeTri` | `PRIX` ou `DISTANCE` | oui |
| `TrajetResponse` | réponse de `GET /api/trajet` | oui |
| `InscriptionRequest` | corps de `POST /api/auth/inscription` | oui |
| `ConnexionRequest` | corps de `POST /api/auth/connexion` | oui |
| `AuthResponse` | réponse de `POST /api/auth/connexion` | oui |
| `FavoriDTO` | corps et réponse de `POST /api/favoris` (`idStation`, `dateAjout`) | oui |
| `PreferencesDTO` | corps et réponse de `/api/preferences` | oui |
| `EconomieRequest` | paramètres de `GET /api/economie` | oui |
| `EconomieResponse` | réponse de `GET /api/economie` | oui |
| `PositionDTO` | `position`, `depart`, `arrivee` et `points` | non |
| `ZoneDTO` | `departement` et `region` (`code`, `nom`) | non |
| `HoraireDTO` | élément de `horaires` | non |
| `PrixCarburantDTO` | élément de `carburants` | non |
| `TrajetDTO` | objet `trajet` de la réponse de `GET /api/trajet` | non |
| `UtilisateurDTO` | utilisateur renvoyé à l'inscription et à la connexion | non |
| `FavoriStationDTO` | élément de `GET /api/favoris` (station avec `dateAjout`) | non |
| `ComparaisonResponse` | réponse de `GET /api/comparer` | non |
| `ZoneComparaisonDTO` | `zone1` et `zone2` de la comparaison | non |
| `TypeZone` | `VILLE` ou `DEPARTEMENT` | non |
| `ErreurDTO` | corps de toutes les erreurs (`statut`, `message`) | non |
