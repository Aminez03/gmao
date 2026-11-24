# API réellement implémentée

Ce catalogue complète le document de conception. La liste initiale était une cible ; les contrats ci-dessous décrivent le code livré. Préfixe commun `/api/v1`.

## Authentification et comptes

`POST /auth/login`, `/auth/refresh`, `/auth/logout`, `/auth/forgot-password`, `/auth/reset-password` ; `GET /auth/me` ; `POST /profile/password`.

Login : `{email,password}`. Reset : `{token,password}`. Changement authentifié : `{currentPassword,newPassword}`. Les POST publics de l'authentification vérifient `Origin` et limitent les tentatives. Refresh utilise un cookie HttpOnly ; les tokens d'accès restent en mémoire du navigateur. Logout révoque les sessions du compte.

`GET /users` (admin ou annuaire limité pour responsable), `POST /users`, `PUT /users/{id}` (admin). Corps : `{email,name,role,active,contractorId?,password?}`. Six rôles fixes, un rôle principal par compte. L'édition des comptes et mots de passe invalide leurs sessions.

## Ressources

Chaque ressource ci-dessous expose `GET /<ressource>`, `GET /<ressource>/{id}`. Les ressources éditables exposent également POST, PUT et DELETE d'archivage, selon leurs permissions et leur état. Une route générique ne signifie pas que toutes les mutations sont autorisées : les écritures des journaux, stocks, réservations et notifications sont réservées aux commandes métier.

```text
locations, assets, teams, skills, user-skills, team-members,
shifts, shift-assignments, absences, handovers,
checklists, preventive-plans, preventive-occurrences,
work-orders, failure-reports, time-entries, anomalies, downtimes,
suppliers, inventory/items, inventory/reservations, inventory/movements,
purchase-requests, purchase-orders, goods-receipts,
subcontractors, contracts, external-interventions, material-requests,
notifications, audit-events, attachments
```

Liste : `page`, `size` ≤ 100, `search`, `status`. Résultat : `{items,page,size,totalElements,totalPages}`. Création/édition : `{fields:{...}}` avec seuls les champs métier autorisés ; les champs serveur sont refusés. Le [catalogue TypeScript](../frontend/src/catalog.json) définit noms et types partagés avec les entités générées ; les annotations et contrôles serveur restent l'autorité.

## Commandes

`POST /<ressource>/{id}/<action>`, avec `Idempotency-Key` et `If-Match`. Corps `{}` pour une action sans paramètres.

| Ressource | Actions et corps |
|---|---|
| work-orders | `assign {technicianId?,teamId?,contractorId?}` ; `start {safetyConfirmed}` ; `wait {reason,note}` ; `resume {}` ; `answers {answers:{clé:valeur}}` ; `time {startsAt,endsAt}` ; `complete {diagnostic,cause,solution,report}` ; `validate {}` ; `reject-validation {reason}` ; `cancel {reason}` ; `reserve {itemId,quantity}` ; `consume {reservationId,quantity}` |
| checklists | `publish {}` |
| preventive-plans | `generate {}` ; `disable {reason}` |
| failure-reports | `qualify {title}` |
| shifts | `publish {}` |
| shift-assignments | `withdraw {reason}` |
| absences | `approve {}` ; `reject {reason}` |
| handovers | `acknowledge {}` |
| anomalies | `resolve {reason}` ; `corrective-request {}` |
| inventory/items | `adjust {type:ADJUST_IN ou ADJUST_OUT,quantity,reason}` |
| inventory/reservations | `release {reason}` |
| purchase-requests | `submit {}` ; `approve {}` ; `reject {reason}` ; `send-admin {}` |
| purchase-orders | `place {}` ; `cancel {reason}` |
| goods-receipts | `quality {accepted,rejected,note}` ; accepté + refusé = reçu |
| external-interventions | `submit-report {report}` ; `approve {}` ; `reject {reason}` |
| material-requests | `approve {}` ; `reject {}` |
| notifications | `read {}` |

## Planning, fichiers, reporting et mobilité

- `GET /planning?from=YYYY-MM-DD` : sept jours, équipes et noms des techniciens ; `POST /planning/generate {startDate,days,cycleDays}` : brouillons tournants A/B/C, 1–31 jours, clé idempotente requise.
- `POST /files` multipart `targetType`, `targetId`, `file` ; `GET /files/{id}/download`, autorisation liée à la ressource. Le contenu est stock? dans PostgreSQL et n'est servi qu'apr?s v?rification des droits.
- `GET /assets/{id}/qr` PNG et `GET /assets/by-qr/{token}` : le QR ne porte aucun droit.
- `GET /reports/dashboard`, `/reports/maintenance` : `from`, `to` (ISO 8601), `teamId`, `assetId` ; `GET /reports/export?format=pdf|xlsx` avec les mêmes filtres.
- `GET /sync/bootstrap` : première page du périmètre accessible, au maximum 100 fiches ; les listes paginées permettent les téléchargements complémentaires. Le client conserve aussi les fiches effectivement consultées.
- `POST /sync/operations` : lot de 1–100 `{operationId,resourceId,action,expectedVersion,payload}` ; seules les actions `answers` et `time` sont acceptées hors ligne. Résultat individuel APPLIED, CONFLICT, REJECTED ou BLOCKED. Une erreur n'efface pas les opérations suivantes du terminal.

Les notifications sont persistées et consultées périodiquement ; SSE n'est pas nécessaire à cette version. Il n'y a pas de connexion à un service de messagerie réel tant que SMTP n'est pas configuré.
