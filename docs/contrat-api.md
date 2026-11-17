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
- **Dates :** format ISO 8601 en UTC, avec le suffixe `Z` (`2026-11-03T14:05:00Z`). L'API Prix des carburants marque ses heures `+00:00`, mais ce sont en réalité des heures de Paris (Europe/Paris) : le backend ignore ce décalage et les convertit en UTC avec `Z`, en tenant compte de l'heure d'été (14:30 à Paris donne `13:30Z` en hiver, `12:30Z` en été).
- **Prix :** en euros par litre, nombre décimal avec trois décimales (`1.789`). Les montants (économie) sont arrondis au centime (`1.10`).
- **Distances :** en kilomètres, arrondies à une décimale (`3.2`).
- **Codes carburant :** `GAZOLE`, `SP95`, `SP98`, `E10`, `E85`, `GPLC`. Un autre code renvoie une erreur 400.
- **Tri :** `PRIX` ou `DISTANCE`.
- **Rayon de recherche :** entier de 1 à 50 (km).
- **Routes protégées :** elles attendent l'en-tête `Authorization: Bearer <jeton>`, où le jeton est celui renvoyé par `POST /api/auth/connexion`. Sans jeton, ou avec un jeton invalide ou expiré, elles renvoient **401** (jamais 403), avec le corps `{ "statut": 401, "message": "Authentification requise : jeton absent, invalide ou expiré." }`. Le jeton est valable 24 heures (réglage `jwt.expiration-heures`). Sur une route publique, un jeton absent, invalide ou expiré est simplement ignoré.
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
| 405 | Méthode HTTP non autorisée sur une route existante (par exemple `DELETE /api/stations/35000001`) | « Une erreur est survenue. » (erreur générique) |
| 409 | Conflit : email déjà utilisé à l'inscription, ou station déjà en favori | « Cet email est déjà utilisé » ou « Cette station est déjà dans vos favoris » |
| 415 | Corps de requête qui n'est pas en JSON (`Content-Type` différent de `application/json`) | « Une erreur est survenue. » (erreur générique) |
| 500 | Erreur interne inattendue du backend ; le message est toujours générique (« Erreur interne du serveur. »), le détail n'apparaît que dans les logs | « Une erreur est survenue, réessayez plus tard. » |
| 503 | API Prix des carburants ou service d'itinéraire indisponible | « Service temporairement indisponible… » avec un bouton « Réessayer » |

Une route inexistante (par exemple `GET /api/inconnue`) renvoie 404 avec le message « Route inexistante : GET /api/inconnue. ». Les codes 405 et 415 signalent une erreur de programmation du frontend : ils ne doivent pas apparaître en utilisation normale.

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
| GET | `/api/trajet?depart=&arrivee=&carburant=&rayon=` | non | F6, F9 | NGUETSA NDZANA Rodrigue Duran |
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
  "nom": null,
  "adresse": "12 boulevard de la Liberté",
  "ville": "Rennes",
  "codePostal": "35000",
  "departement": { "code": "35", "nom": "Ille-et-Vilaine" },
  "region": { "code": "53", "nom": "Bretagne" },
  "position": { "latitude": 48.1066, "longitude": -1.6793 },
  "emplacement": "ROUTE",
  "automate24h": true,
  "horaires": [
    { "jour": "lundi", "ferme": false, "ouverture": "07:00", "fermeture": "21:00" },
    { "jour": "mardi", "ferme": false, "ouverture": "07:00", "fermeture": "21:00" },
    { "jour": "mercredi", "ferme": false, "ouverture": "07:00", "fermeture": "21:00" },
    { "jour": "jeudi", "ferme": false, "ouverture": "07:00", "fermeture": "21:00" },
    { "jour": "vendredi", "ferme": false, "ouverture": "07:00", "fermeture": "21:00" },
    { "jour": "samedi", "ferme": false, "ouverture": "08:00", "fermeture": "20:00" },
    { "jour": "dimanche", "ferme": true, "ouverture": null, "fermeture": null }
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
| `nom` | toujours `null` : l'API Prix des carburants ne fournit pas de nom de station |
| `departement`, `region` | code et nom ; utilisés par la comparaison par département (F16) |
| `position` | `latitude` et `longitude` en degrés décimaux ; elles peuvent valoir `null` si l'API ne fournit pas de coordonnées pour la station |
| `emplacement` | `ROUTE` ou `AUTOROUTE` (champ `pop` de l'API : R ou A) |
| `automate24h` | `true` si la station a un automate de paiement 24h/24 |
| `horaires` | sept éléments, un par jour (`lundi` … `dimanche`), ou `[]` si l'API ne fournit pas d'horaires. Chaque jour : `ferme` = `true` si la station est fermée ce jour-là (`ouverture` et `fermeture` à `null`) ; `ferme` = `false` avec `ouverture` et `fermeture` à `null` = horaires non communiqués. Quand l'API donne plusieurs plages dans la journée, `ouverture` est la première ouverture et `fermeture` la dernière fermeture |
| `services` | libellés fournis par l'API ; `[]` si aucun |
| `carburants[].type` | `GAZOLE`, `SP95`, `SP98`, `E10`, `E85`, `GPLC` |
| `carburants[].prix` | prix en €/L, ou `null` si le carburant est en rupture sans prix connu |
| `carburants[].dateMaj` | date de la dernière mise à jour du prix, ou `null` s'il n'y a pas de prix |
| `carburants[].disponible` | `false` si le carburant est en rupture (F8) |
| `carburants[].typeRupture` | `TEMPORAIRE`, `DEFINITIVE`, ou `null` si le carburant est disponible |
| `carburants[].debutRupture` | date de début de la rupture, ou `null` |
| `distanceKm` | toujours présent : distance à la position recherchée, ou au trajet pour `/api/trajet` ; `null` quand il n'a pas de sens (fiche `GET /api/stations/{id}`, favoris `GET /api/favoris`, recherche par ville) |

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
| `lat`, `lon` | oui | coordonnées GPS (nombres décimaux) : latitude de -90 à 90, longitude de -180 à 180 |
| `rayon` | oui | entier de 1 à 50 (km) |
| `carburant` | oui | code du carburant |
| `tri` | non | `PRIX` (par défaut) ou `DISTANCE` |

**Réponse 200 :** un tableau de stations, triées selon le paramètre `tri`, avec `distanceKm` renseigné. Seules les stations qui vendent le carburant demandé sont renvoyées. Les stations en rupture sur ce carburant sont incluses, avec `disponible: false`. Quand le tri est `PRIX`, elles sont placées en fin de liste. La réponse contient au plus 500 stations (réglage `smartcarbure.api-carburants.max-resultats`) : au-delà, seules les plus proches sont gardées.

```json
[
  {
    "id": "35200001",
    "nom": null,
    "adresse": "2 avenue des Pays-Bas",
    "ville": "Rennes",
    "codePostal": "35200",
    "departement": { "code": "35", "nom": "Ille-et-Vilaine" },
    "region": { "code": "53", "nom": "Bretagne" },
    "position": { "latitude": 48.0889, "longitude": -1.6512 },
    "emplacement": "ROUTE",
    "automate24h": true,
    "horaires": [
      { "jour": "lundi", "ferme": false, "ouverture": "08:30", "fermeture": "20:30" },
      { "jour": "mardi", "ferme": false, "ouverture": "08:30", "fermeture": "20:30" },
      { "jour": "mercredi", "ferme": false, "ouverture": "08:30", "fermeture": "20:30" },
      { "jour": "jeudi", "ferme": false, "ouverture": "08:30", "fermeture": "20:30" },
      { "jour": "vendredi", "ferme": false, "ouverture": "08:30", "fermeture": "20:30" },
      { "jour": "samedi", "ferme": false, "ouverture": "08:30", "fermeture": "20:30" },
      { "jour": "dimanche", "ferme": true, "ouverture": null, "fermeture": null }
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
- 400 si ni `ville` ni `lat`/`lon` ne sont fournis, si `lat` ou `lon` est mal formé ou hors limites, si `rayon` est absent ou hors limites, si `carburant` est absent, ou si `carburant` ou `tri` est inconnu ;
- 503 si l'API Prix des carburants est indisponible.

### Rechercher des stations par ville ou code postal (F2, F4, F5)

`GET /api/stations?ville=Rennes&carburant=GAZOLE&tri=PRIX`

**Protégée :** non.

| Paramètre | Obligatoire | Valeurs |
|---|---|---|
| `ville` | oui | nom exact de la commune (`Rennes` ; `Renne` ne trouve rien) ou code postal de 5 chiffres (`35000`) ; la casse et les accents sont ignorés |
| `carburant` | oui | code du carburant |
| `tri` | non | `PRIX` (par défaut) ou `DISTANCE` |

Si `ville` est fourni, `lat`, `lon` et `rayon` sont ignorés : seules les stations de la commune (ou du code postal) sont renvoyées. Comme `distanceKm` vaut `null`, `tri=DISTANCE` est accepté sans erreur et les stations sont alors triées par prix.

**Réponse 200 :** un tableau de stations au même format que la recherche par position, avec `distanceKm: null`.

```json
[
  {
    "id": "35000001",
    "nom": null,
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

**Réponse 200 :** une station complète, avec `distanceKm: null`. La liste `carburants` contient **tous** les carburants de la station, y compris ceux en rupture (F8).

```json
{
  "id": "35000002",
  "nom": null,
  "adresse": "45 rue Jules Lallemand",
  "ville": "Rennes",
  "codePostal": "35000",
  "departement": { "code": "35", "nom": "Ille-et-Vilaine" },
  "region": { "code": "53", "nom": "Bretagne" },
  "position": { "latitude": 48.1031, "longitude": -1.7095 },
  "emplacement": "ROUTE",
  "automate24h": false,
  "horaires": [
    { "jour": "lundi", "ferme": false, "ouverture": "08:00", "fermeture": "20:00" },
    { "jour": "mardi", "ferme": false, "ouverture": "08:00", "fermeture": "20:00" },
    { "jour": "mercredi", "ferme": false, "ouverture": "08:00", "fermeture": "20:00" },
    { "jour": "jeudi", "ferme": false, "ouverture": "08:00", "fermeture": "20:00" },
    { "jour": "vendredi", "ferme": false, "ouverture": "08:00", "fermeture": "20:00" },
    { "jour": "samedi", "ferme": false, "ouverture": "08:00", "fermeture": "20:00" },
    { "jour": "dimanche", "ferme": true, "ouverture": null, "fermeture": null }
  ],
  "services": ["Lavage automatique", "Boutique alimentaire", "Toilettes publiques"],
  "carburants": [
    { "type": "GAZOLE", "prix": 1.719, "dateMaj": "2026-11-03T13:25:00Z", "disponible": true, "typeRupture": null, "debutRupture": null },
    { "type": "E10", "prix": 1.779, "dateMaj": "2026-11-03T13:25:00Z", "disponible": true, "typeRupture": null, "debutRupture": null },
    { "type": "E85", "prix": 0.899, "dateMaj": "2026-11-02T09:15:00Z", "disponible": false, "typeRupture": "TEMPORAIRE", "debutRupture": "2026-11-02T09:15:00Z" }
  ],
  "distanceKm": null
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
| `rayon` | non | distance maximale d'une station à la route, en km, de 1 à 10 (5 par défaut) |

Le trajet est limité à 500 km.

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
      "nom": null,
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
- `stations` contient les stations situées à moins de `rayon` km de la route, sans doublon, filtrées par carburant et triées par prix. Les stations en rupture sur ce carburant sont incluses, avec `disponible: false`, et placées en fin de liste. Pour chaque station, `distanceKm` est la distance au trajet.
- Aucune station le long du trajet : `stations` vaut `[]` (réponse 200).

**Erreurs :**
- 400 si un paramètre manque, si les coordonnées sont mal formées, si le carburant est inconnu, si `rayon` est hors limites, si le trajet dépasse 500 km, ou si aucun itinéraire routier n'existe entre les deux points ;
- 503 si le service d'itinéraire ou l'API carburants est indisponible (y compris clé du service d'itinéraire absente ou quota journalier atteint).

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

Le prix moyen local est calculé par le backend, sur les stations de la même ville qui vendent ce carburant (stations en rupture exclues). La station choisie fait toujours partie du calcul. Le prix moyen est arrondi à trois décimales.

**Réponse 200 :**

```json
{
  "prixStation": 1.749,
  "prixMoyenLocal": 1.771,
  "litres": 50.0,
  "montant": 1.10
}
```

`litres` est un nombre décimal : le volume saisi est renvoyé tel quel (`50.0` pour 50).

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
| `nom` | obligatoire, non vide, 100 caractères maximum |
| `email` | obligatoire, format valide (`test@` est refusé) ; enregistré sans espaces et en minuscules |
| `motDePasse` | obligatoire, de 8 à 72 caractères |

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

**Réponse 200 :** un tableau de stations (modèle Station, avec `distanceKm: null`), de la plus récemment ajoutée à la plus ancienne. Chaque élément porte en plus le champ `dateAjout`. `[]` si l'utilisateur n'a aucun favori.

```json
[
  {
    "id": "44000001",
    "nom": null,
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
    "distanceKm": null,
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
;12 boulevard de la Liberté;35000;Rennes;Gazole;1,749;Oui;1,4;2026-11-03T13:40:00Z
```

| Colonne | Contenu |
|---|---|
| Nom | toujours vide : l'API Prix des carburants ne fournit pas de nom de station (`nom` vaut `null`) |
| Carburant | libellé affiché (`Gazole`, `SP95`, `SP98`, `SP95-E10`, `E85`, `GPLc`) |
| Disponible | `Oui` ou `Non` (rupture) |
| Distance (km) | vide pour une recherche par ville |
| Mise à jour | champ `dateMaj` du carburant, tel que reçu |

## Décisions prises

Ces choix ont été tranchés lors de la rédaction du contrat (tâche C1), puis pendant le développement (anciens « Points à valider par le groupe »). Les autres formats proposés ont été écartés.

| Sujet | Décision | Format écarté | Raison |
|---|---|---|---|
| Réponse de connexion | `{ jeton, utilisateur: { id, nom, email, carburantPrefere, rayonPrefere } }` | `{ jeton, nom, email }` | Les préférences arrivent dès la connexion : le formulaire de recherche est pré-rempli sans appel supplémentaire (test U07). |
| Réponse d'inscription | 201 avec l'utilisateur créé ; l'utilisateur se connecte ensuite | 201 `{ jeton, nom, email }` (connexion automatique) | Un seul endroit délivre un jeton (`/api/auth/connexion`) ; le test U01 demande seulement d'informer l'utilisateur du succès. |
| Contenu de `GET /api/favoris` | Stations complètes, avec le champ `dateAjout` | `{ idStation, dateAjout }` seulement | La page « Mes favoris » s'affiche en un seul appel, sans un `GET /api/stations/{id}` par favori. `POST /api/favoris` renvoie bien `{ idStation, dateAjout }`. |
| Nom du champ du carburant préféré | `carburantPrefere`, partout (Utilisateur et `/api/preferences`) | `codeCarburantPrefere` | Un seul nom pour le même champ dans tous les objets JSON ; il contient toujours un code carburant (`GAZOLE`…). |
| Utilisateur non connecté sur une route protégée | 401, jamais 403 | 403 (comportement par défaut de Spring Security) | Le frontend redirige vers la page de connexion sur un 401 ; `SecurityConfig` doit configurer ce statut. |
| Rayon pour une recherche par ville | `rayon` est ignoré : seules les stations de la commune (ou du code postal) sont renvoyées | Rayon autour du centre de la ville | L'API Prix des carburants filtre directement par commune ; aucun géocodage de la ville n'est nécessaire. |
| Tri `DISTANCE` sans position | Accepté sans erreur ; les stations sont triées par prix | Erreur 400 | Pour une recherche par ville, `distanceKm` vaut `null` : il n'y a pas de distance à trier. |
| Distance maximale au trajet (F6) | 5 km par défaut, paramètre `rayon` facultatif de 1 à 10 km ; trajet limité à 500 km | Distance fixe, sans limite de longueur | Étude validée (`docs/etude-trajet.md`) : le nombre d'appels à l'API reste raisonnable. |
| Prix d'un carburant en rupture | `prix: null` (et `dateMaj: null`) quand l'API ne fournit pas de prix ; le frontend l'accepte | Carburant absent de la liste | La fiche (F8) doit afficher tous les carburants, y compris ceux en rupture. |
| Zone de référence de l'économie (F15) | Les stations de la même ville que la station choisie | Rayon autour de la station, ou département | Règle codée dans `EconomieService` (PR #17). Pour une commune avec une seule station, l'économie vaut 0. |
| Nombre de résultats | 500 stations au plus (réglage `smartcarbure.api-carburants.max-resultats`) ; pour une recherche par position, les plus proches sont gardées | Aucune limite | Limite le nombre d'appels à l'API (pages de 100 stations) et le volume affiché. |
| Durée de validité du jeton | 24 heures (réglage `jwt.expiration-heures`) ; à l'expiration, les routes protégées renvoient 401 et le frontend redirige vers la page de connexion | — | Une journée d'utilisation sans reconnexion. |

## Points à valider par le groupe

Aucun point en attente : les points précédents ont été tranchés et figurent dans « Décisions prises ». Les nouveaux points à discuter seront ajoutés ici.

## DTO cités par le contrat

Ces classes appartiennent au package `fr.miage.smartcarbure.dto` (les énumérations `TypeTri` et `TypeZone` au package `fr.miage.smartcarbure.enums`). Le contrat les cite ; celles qui manquent restent à créer par l'équipe backend. La colonne « État sur main » indique si la classe est écrite, encore vide, ou absente.

| DTO | Rôle dans le contrat | État sur main |
|---|---|---|
| `StationDTO` | modèle Station (recherche, fiche, trajet, favoris) | écrite |
| `PositionDTO` | `position`, `depart`, `arrivee` et `points` | écrite |
| `DepartementDTO` | `departement` (`code`, `nom`) | écrite |
| `RegionDTO` | `region` (`code`, `nom`) | écrite |
| `HoraireDTO` | élément de `horaires` (`jour`, `ferme`, `ouverture`, `fermeture` au format `HH:mm`) | écrite |
| `PrixCarburantDTO` | élément de `carburants` | écrite |
| `CritereRechercheDTO` | paramètres de `GET /api/stations` | écrite |
| `TypeTri` | `PRIX` ou `DISTANCE` | écrite |
| `InscriptionRequest` | corps de `POST /api/auth/inscription` | écrite |
| `ConnexionRequest` | corps de `POST /api/auth/connexion` | écrite |
| `AuthResponse` | réponse de `POST /api/auth/connexion` | écrite |
| `UtilisateurDTO` | utilisateur renvoyé à l'inscription et à la connexion | écrite |
| `EconomieResponse` | réponse de `GET /api/economie` | écrite |
| `ErreurDTO` | corps de toutes les erreurs (`statut`, `message`) | écrite |
| `EconomieRequest` | paramètres de `GET /api/economie` (le contrôleur lit les paramètres directement) | vide |
| `TrajetResponse` | réponse de `GET /api/trajet` | vide |
| `FavoriDTO` | corps et réponse de `POST /api/favoris` (`idStation`, `dateAjout`) | vide |
| `PreferencesDTO` | corps et réponse de `/api/preferences` | vide |
| `TrajetDTO` | objet `trajet` de la réponse de `GET /api/trajet` | absente |
| `FavoriStationDTO` | élément de `GET /api/favoris` (station avec `dateAjout`) | absente |
| `ComparaisonResponse` | réponse de `GET /api/comparer` | absente |
| `ZoneComparaisonDTO` | `zone1` et `zone2` de la comparaison | absente |
| `TypeZone` | `VILLE` ou `DEPARTEMENT` | absente |
