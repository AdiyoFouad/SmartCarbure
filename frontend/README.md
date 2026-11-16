# SmartCarbure : frontend

Interface web de SmartCarbure, construite avec React 19 et Vite.

## Commandes

```bash
npm install        # installer les dépendances
cp .env.example .env
npm run dev        # serveur de développement sur http://localhost:3000 (/api redirigé vers localhost:8080)
npm test           # lancer les tests (Vitest)
npm run build      # construire la version de production dans dist/
npm run preview    # servir la version construite
```

## Mode démonstration

`VITE_USE_MOCK=true` dans `.env` fait renvoyer aux services les données fictives de `src/data` au lieu d'appeler le backend. Avec `VITE_USE_MOCK=false` (ou sans `.env`), les services appellent le vrai backend sur `/api`. Il faut relancer `npm run dev` après avoir modifié `.env`.

- Les pages ne savent pas d'où viennent les données : seuls les services lisent `MODE_DEMO` (`services/mock.js`).
- Un service passe par `reponseSimulee(() => …)` : réponse après 300 ms, copie des données fictives, et une `ApiError` levée dans la fonction (par exemple un 404 pour un id inconnu) est propagée telle quelle.
- `src/data/stations.js` contient 6 stations au format exact du contrat (Rennes et Nantes) : station complète, ruptures, station d'autoroute aux horaires non communiqués, jour fermé.
- **Simuler une panne :** ajouter `?simuler=503` à l'URL de la page (par exemple `http://localhost:3000/stations/35000101?simuler=503`). Les appels échouent en 503 pendant 3 secondes, puis réussissent : on peut tester le bouton « Réessayer ».

## Fichiers partagés

Ces fichiers servent à plusieurs pages : gardez-les génériques et prévenez l'équipe avant de changer leur comportement.

| Fichier | Rôle |
|---|---|
| `services/api.js` | client HTTP unique, `ApiError` (statut + message), `MESSAGES_ERREUR` par code |
| `services/mock.js` | `MODE_DEMO`, `reponseSimulee`, panne simulée `?simuler=503` |
| `hooks/useRequete.js` | `const { donnees, chargement, erreur, relancer } = useRequete(() => service(x), [x])` |
| `components/MessageEtat.jsx` | `<MessageEtat type="chargement" \| "vide" \| "erreur" message="…" onReessayer={relancer} />` (`role="status"`) |
| `utils/format.js` | `formaterPrix` (« 1,789 € », « Prix indisponible »), `formaterDistance`, `formaterDateMaj`, `formaterDate` |
| `components/Badge*.jsx`, `TableauCarburants.jsx`, `Horaires.jsx` | affichage d'une station au format du contrat |
| `data/stations.js` | stations fictives du mode démonstration |

## Organisation de `src/`

| Dossier | Contenu |
|---|---|
| `pages/` | un écran complet par route |
| `components/` | éléments réutilisables, sans appel au backend |
| `services/` | tous les appels HTTP, via le client unique `api.js` (préfixe `/api`, gestion des erreurs et du délai d'attente) |
| `context/` | état partagé : l'utilisateur connecté (`AuthContext`) |
| `hooks/` | logique réutilisable (`useAuth`, `useRequete`, `useGeolocalisation`) |
| `utils/` | fonctions de formatage et export CSV |
| `constants/` | carburants, types d'emplacement, types de rupture, tris |
| `data/` | données fictives du mode démonstration |

Chaque composant et chaque page a son propre fichier `.css`. Les couleurs et les espacements sont définis par des variables dans `src/index.css`.

Les routes REST attendues du backend sont décrites dans [docs/contrat-api.md](../docs/contrat-api.md) (à la racine du dépôt).
