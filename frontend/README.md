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

`VITE_USE_MOCK=true` dans `.env` fait renvoyer aux services les données fictives de `src/data` au lieu d'appeler le backend. Ce mode démonstration reste à implémenter.

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
