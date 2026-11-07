# SmartCarbure : contrat d'API entre le frontend et le backend

Ce document décrit les routes REST que le frontend appelle. Le backend doit les respecter (chemins, paramètres, format JSON, codes d'erreur). Toute modification doit être décidée par les deux équipes, puis reportée ici.

## Règles générales

- Toutes les routes commencent par `/api`. Le frontend utilise uniquement des URL relatives :
  - en développement, Vite redirige `/api` vers `http://localhost:8080` (voir `vite.config.js`) ;
  - dans Docker, c'est Nginx qui redirige `/api/` vers `http://backend:8080/api/` (voir `nginx.conf`).
- Les requêtes et les réponses sont en JSON (`Content-Type: application/json`), encodées en UTF-8.
- Les dates sont au format ISO 8601 (`2026-11-03T14:05:00Z`).
- Les prix sont en euros par litre (nombre décimal, par exemple `1.789`). Les distances sont en kilomètres.
- Les routes réservées aux utilisateurs connectés attendent l'en-tête `Authorization: Bearer <jeton>`. Le jeton est celui renvoyé par `POST /api/auth/connexion`.

## Codes d'erreur

| Code | Signification | Message affiché par le frontend |
|---|---|---|
| 400 | Saisie invalide (paramètre manquant ou hors limites) | « Saisie invalide, vérifiez les champs du formulaire. » |
| 401 | Non connecté, jeton invalide, ou identifiants incorrects à la connexion | « Vous devez être connecté… » ou « Email ou mot de passe incorrect » |
| 404 | Station introuvable (`GET /api/stations/{id}` uniquement) | « L'élément demandé est introuvable. » |
| 409 | Email déjà utilisé à l'inscription | « Cet email est déjà utilisé » |
| 503 | API Prix des carburants ou service d'itinéraire indisponible | « Service temporairement indisponible… » avec un bouton « Réessayer » |

Le frontend traite aussi comme un 503 :
- une panne réseau ;
- un délai d'attente dépassé (10 secondes) ;
- les codes 502 et 504 renvoyés par Nginx quand le backend est arrêté.

Corps d'erreur conseillé (facultatif : le frontend affiche ses propres messages) :

```json
{ "statut": 400, "message": "Le rayon doit être compris entre 1 et 50 km." }
```

## Modèles JSON

### Station

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
| `emplacement` | `ROUTE` ou `AUTOROUTE` (champ `pop` de l'API : R ou A) |
| `horaires` | un élément par jour (`lundi` … `dimanche`) ; `ouverture` et `fermeture` à `null` si la station est fermée ce jour-là |
| `carburants[].type` | `GAZOLE`, `SP95`, `SP98`, `E10`, `E85`, `GPLC` |
| `carburants[].typeRupture` | `TEMPORAIRE`, `DEFINITIVE` ou `null` |
| `distanceKm` | distance à la position recherchée, ou au trajet pour `/api/trajet` ; `null` pour une recherche par ville ; absent de `GET /api/stations/{id}` |

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

| Paramètre | Obligatoire | Valeurs |
|---|---|---|
| `lat`, `lon` | oui | coordonnées GPS |
| `rayon` | oui | entier de 1 à 50 (km) |
| `carburant` | oui | code du carburant |
| `tri` | non | `PRIX` (par défaut) ou `DISTANCE` |

**Réponse 200 :** un tableau de stations, triées selon le paramètre `tri`, avec `distanceKm` renseigné. Les stations en rupture sur le carburant demandé sont incluses, avec `disponible: false`. Quand le tri est `PRIX`, elles sont placées en fin de liste.

**Aucune station dans le rayon :** réponse 200 avec `[]`. Le frontend affiche « Aucune station trouvée, élargissez le rayon ».

### Rechercher des stations par ville ou code postal (F2, F4, F5)

`GET /api/stations?ville=Rennes&carburant=GAZOLE&tri=PRIX`

`ville` accepte un nom de ville ou un code postal (`35000`).

**Réponse 200 :** un tableau de stations, avec `distanceKm: null`.

**Ville ou code postal inconnu :** réponse **200 avec `[]`**, et non un 404 (voir le diagramme `sequence_erreurs` de DESIGN.md et le test R05). Le frontend affiche « Aucune station pour cette ville, vérifiez la saisie ».

**Erreurs :**
- 400 si ni `ville` ni `lat`/`lon` ne sont fournis ;
- 503 si l'API Prix des carburants est indisponible.

### Fiche d'une station (F7, F8)

`GET /api/stations/35000001`

**Réponse 200 :** une station.

**Erreur :** 404 si la station n'existe pas. C'est la seule route qui renvoie un 404.

### Stations le long d'un trajet (F6)

`GET /api/trajet?depart=48.1173,-1.6778&arrivee=47.2184,-1.5536&carburant=SP95`

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
  "stations": []
}
```

- `points` contient les points de l'itinéraire, tracé sur la carte.
- `stations` contient les stations le long du trajet, sans doublon, filtrées par carburant et triées par prix. Pour chaque station, `distanceKm` est la distance au trajet.

**Erreurs :**
- 400 si les coordonnées sont mal formées ;
- 503 si le service d'itinéraire ou l'API carburants est indisponible.

### Comparer deux zones (F16)

`GET /api/comparer?type=VILLE&zone1=Rennes&zone2=Nantes&carburant=GAZOLE`

| Paramètre | Valeurs |
|---|---|
| `type` | `VILLE` (nom ou code postal) ou `DEPARTEMENT` (code, par exemple `35`, ou nom) |
| `zone1`, `zone2` | zones à comparer |
| `carburant` | code du carburant |

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

- `ecart` = prix moyen de la zone 1 moins prix moyen de la zone 2.
- Une zone sans station renvoie `prixMoyen: null` et `nombreStations: 0`. Dans ce cas, `ecart` vaut `null`.

### Économie sur un plein (F15)

`GET /api/economie?idStation=35000001&carburant=GAZOLE&litres=50`

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

`montant` est positif pour une économie et négatif pour un surcoût, arrondi au centime.

**Erreurs :**
- 400 si `litres` n'est pas un nombre positif, ou si la station ne vend pas ce carburant ;
- 404 si la station n'existe pas ;
- 503 si l'API Prix des carburants est indisponible.

L'export CSV des résultats (F18) n'a pas de route : le frontend génère le fichier à partir des stations déjà affichées.

### Connexion (F12)

`POST /api/auth/connexion`

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

**Erreur :** 401 si l'email ou le mot de passe est incorrect. Le frontend affiche « Email ou mot de passe incorrect ».

### Inscription (F11)

`POST /api/auth/inscription`

```json
{ "nom": "Camille Martin", "email": "camille@exemple.fr", "motDePasse": "motdepasse" }
```

**Réponse 201 :** l'utilisateur créé. L'utilisateur doit ensuite se connecter.

**Erreurs :**
- 400 si l'email est mal formé ou si le mot de passe est trop court ;
- 409 si l'email est déjà utilisé. Le frontend affiche « Cet email est déjà utilisé ».

### Favoris (F14), connexion requise

| Route | Corps | Réponse |
|---|---|---|
| `GET /api/favoris` | aucun | 200 : tableau de stations |
| `POST /api/favoris` | `{ "idStation": "35000001" }` | 201 (ou 204) ; 404 si la station n'existe pas |
| `DELETE /api/favoris/35000001` | aucun | 204 |

Ces trois routes renvoient 401 sans jeton valide.

### Préférences (F13), connexion requise

`GET /api/preferences` renvoie une réponse 200 :

```json
{ "carburantPrefere": "GAZOLE", "rayonPrefere": 15 }
```

`PUT /api/preferences` attend le même corps et renvoie une réponse 200 avec les préférences enregistrées.

**Erreurs :**
- 400 si le carburant est inconnu ou si le rayon est hors des limites (1 à 50 km) ;
- 401 sans jeton valide.
