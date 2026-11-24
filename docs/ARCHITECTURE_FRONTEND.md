# Architecture du frontend

Chaque ressource métier dispose d’une page React dédiée. Les pages sont regroupées par domaine ; les tableaux, formulaires et fiches détaillées restent partagés pour maintenir un comportement cohérent.

```text
frontend/src/
  app/
    AppRoutes.tsx        # Routes de l’application et chargement différé
    moduleRoutes.ts      # Assemblage des routes métier
    routeTypes.ts        # Contrat des routes métier
    Shell.tsx            # Navigation, barre supérieure et profil
    Auth.tsx             # Session utilisateur
  features/
    patrimoine/
      routes.ts
      pages/             # Équipements, localisations, documents
    equipes/
      routes.ts
      pages/             # Équipes, postes, habilitations, absences, planning
    maintenance/
      routes.ts
      pages/             # Interventions, préventif, signalements, anomalies
    achats/
      routes.ts
      pages/             # Articles, réservations, achats, réceptions
    prestataires/
      routes.ts
      pages/             # Entreprises, contrats, missions, demandes de matériel
    pilotage/
      routes.ts
      pages/             # Notifications et audit
    dashboard/pages/
    users/pages/
    auth/pages/
    scan/pages/
    sync/pages/
  components/
    resources/
      ResourcePage.tsx   # Tableau, recherche, pagination et création
      ResourceDetail.tsx # Détails et actions métier
      ChecklistExecution.tsx
      Attachments.tsx
      AssetHistory.tsx
    ResourceForm.tsx
    ReferenceValue.tsx
    EntityDisplay.tsx
    domain.ts           # Catalogue, libellés et permissions
    actions.ts          # Actions des workflows
  services/             # Client HTTP, cache et synchronisation hors ligne
  types/                # Types transverses
  test/
```

## Pages et routes

Les 32 ressources du catalogue ont chacune leur propre composant. Par exemple, `/m/work-orders` charge `features/maintenance/pages/WorkOrdersPage.tsx`, tandis que `/m/inventory/items` charge `features/achats/pages/InventoryItemsPage.tsx`.

Chaque domaine déclare ses pages dans `routes.ts`. `app/moduleRoutes.ts` rassemble ces déclarations et `app/AppRoutes.tsx` les transforme en routes explicites. Les pages sont chargées à la demande avec `React.lazy`. Les liens existants, y compris `?id=...` pour ouvrir une fiche, sont conservés. Une URL inconnue affiche un message « Page introuvable ».

## Responsabilités

- La page dédiée choisit sa ressource et ses filtres d’état. Elle peut accueillir de futurs indicateurs ou comportements propres au métier.
- `components/resources/ResourcePage.tsx` reçoit cette configuration par propriétés. Il ne déduit plus la ressource à partir du chemin de l’URL.
- Le changement de page recrée son état local : recherche, pagination, formulaire et sélection ne passent pas au module suivant.
- Les permissions sont vérifiées avant le chargement des données. Le backend conserve le contrôle effectif des autorisations.
- Les références utilisateurs continuent d’afficher les noms fournis par l’API. Les UUID restent disponibles pour les opérations métier.
- Les composants partagés utilisent les services transverses ; ils n’importent aucune page métier.

## Ajouter une page métier

1. Définir la ressource et ses champs dans le catalogue, en accord avec l’API.
2. Créer sa page dans `features/<domaine>/pages/` et lui transmettre sa configuration.
3. Déclarer son chargement différé dans `features/<domaine>/routes.ts`.
4. Vérifier les permissions et les opérations autorisées.
5. Exécuter `npm run typecheck`, `npm test` et `npm run build` depuis `frontend/`.

Les tests de routes vérifient la correspondance avec tout le catalogue, les accès directs, les paramètres de fiche et l’isolation de l’état entre pages. Les tests de permissions vérifient qu’un module interdit ne déclenche aucune requête de données.
