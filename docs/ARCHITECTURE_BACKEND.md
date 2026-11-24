# Architecture backend

Le backend est organisé par domaine métier.

```text
modules/<module>/
├── controller/    # endpoints propres au domaine et cas spécialisés
├── model/         # entités JPA et objets métier persistants
├── repository/    # propriété des ressources du domaine et accès aux données
└── service/       # règles applicatives et frontière du domaine
```

Les modules `patrimoine`, `equipes`, `maintenance`, `achats`, `prestataires` et `pilotage`
utilisent cette structure. Les entités sont rangées dans leurs dossiers `model`.

## API et routage

Les URL REST existantes restent inchangées. `ResourceController` est un adaptateur HTTP commun :
il ne contient pas de règle métier. Il délègue chaque ressource à `ModuleResourceRouter`, qui
la remet au service du module concerné selon son repository :

```text
requête HTTP → ResourceController → ModuleResourceRouter
             → <Module>Service → ResourceService / WorkflowService
             → EntityStore → PostgreSQL
```

Cette couche partagée évite de recopier les mêmes endpoints CRUD pour les 32 ressources, tout
en laissant chaque module propriétaire de ses routes, modèles et règles. Les endpoints métier
spécifiques restent dans leurs contrôleurs de domaine, par exemple le planning des postes.

## Services transverses

- `ResourceService` : CRUD commun, validation et audit.
- `WorkflowService` : transitions métier qui traversent plusieurs modules.
- `EntityStore` : accès JPA générique partagé ; les repositories de module définissent le
  périmètre dont chaque service est responsable.

Lorsqu’une règle devient propre à un seul domaine, elle doit être déplacée du service transversal
vers le service de ce module afin de conserver cette frontière.
