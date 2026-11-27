# Catalogue des API REST proposées

Tous les chemins ci-dessous sont préfixés par `/api/v1`. Catalogue de conception, sans endpoints opérationnels ajoutés. La documentation OpenAPI détaillée des DTO sera produite avec les modules validés.

## Contrat commun

- JSON UTF-8, propriétés `camelCase`, UUID en chaînes, instants ISO 8601 avec offset (`Z` par défaut), dates `YYYY-MM-DD`, quantités/montants décimaux avec précision du dictionnaire. Validation serveur obligatoire ; Zod côté client reprend le contrat sans le remplacer.
- Listes : `page=0&size=25`, maximum 100, `sort=createdAt,desc` avec colonnes autorisées et identifiant comme second tri. Réponse `{items,page,size,totalElements,totalPages}` ; filtres validés et appliqués au périmètre autorisé avant comptage.
- `POST` de création : `201` et `Location`. `GET/PUT/actions` : `200` avec DTO. Archivage, logout : `204`. Export : `202` avec identifiant de tâche. Pas de PATCH libre des statuts.
- Écriture d'un objet existant : `If-Match: "7"` requis (`428` si absent), contrôle de version (`412` si périmée), `ETag: "8"` retourné. Pour un enfant, version du parent agrégat ou version enfant selon DTO, explicitée dans OpenAPI.
- `Idempotency-Key: <uuid>` obligatoire pour créations, commandes métiers, mouvements, uploads finalisés et synchronisation ; empreinte canonique incluant route, méthode, corps et préconditions. Rejeu identique : même résultat ; clé réutilisée pour contenu différent : `409`. Après authentification et contrôle des droits actuels, rechercher un résultat idempotent **avant** de réévaluer la version historique de la requête. L'écriture et le résultat sont atomiques.
- Erreurs `application/problem+json` : `{type,title,status,detail,instance,code,correlationId,fieldErrors}`. `400` syntaxe/filtre, `401` session absente/expirée, `403` capacité insuffisante, `404` objet absent ou hors périmètre, `409` conflit métier/idempotence, `412` version, `413` fichier trop gros, `422` validation métier/champs, `429` limitation. Aucune stacktrace publique.
- Codes de validation stables tels que `CHECKLIST_REQUIRED`, `UNRESOLVED_ANOMALY`, `INVALID_TRANSITION`, `INSUFFICIENT_STOCK`, `SHIFT_OVERLAP`, `SELF_VALIDATION_FORBIDDEN` ; messages français traduisibles par i18next.
- `ADMIN` peut réaliser les actions du responsable/achats selon la matrice, avec séparation des tâches conservée. Les ressources externes sont filtrées par entreprise ET mission. Aucun DTO d'entrée ne porte `actorRole`, `verifiedBy` ou un auteur accepté comme identité d'autorité.

Abréviations d'accès : **A** admin ; **R** responsable ; **T** technicien ; **E** prestataire. « Lecture métier » signifie droits du tableau du cahier des charges, pas tous les rôles sans restriction. Chaque endpoint, y compris download/export/sync, applique ces droits.

## Authentification et utilisateurs

| Méthode et chemin | DTO / résultat principal | Accès et règle |
|---|---|---|
| POST `/auth/login` | LoginRequest(email,password) → AuthResponse(accessToken,expiresAt,user) + cookie refresh | Public limité ; journal et protection tentatives |
| POST `/auth/refresh` | cookie + protection CSRF → nouveau jeton/cookie | Session valide, rotation |
| POST `/auth/logout` | révocation session et suppression cookie | Session/cookie, CSRF |
| POST `/auth/forgot-password` | email → accusé uniforme 202 | Public limité |
| POST `/auth/reset-password` | token,newPassword → 204 | Jeton à usage unique |
| GET `/auth/me` | UserProfileDto et capacités effectives | Authentifié |
| PUT `/auth/me` | ProfileUpdate(displayName,préférences) | Soi-même, pas de rôles |
| POST `/auth/change-password` | currentPassword,newPassword → 204 | Soi-même, révoque sessions |
| GET `/auth/sessions` | SessionSummaryDto[] | Sessions propres |
| DELETE `/auth/sessions/{id}` | révocation → 204 | Propriétaire ou A |
| GET, POST `/users` | UserFilter ; UserCreate(email,displayName,roles,contractorId?) | A ; R annuaire limité via vue dédiée |
| GET, PUT `/users/{id}` | UserDetail ; UserUpdate(displayName,active,contractorId?) | A ; lecture propre autorisée |
| DELETE `/users/{id}` | désactivation sans suppression physique | A, interdit dernier admin actif |
| PUT `/users/{id}/roles` | RoleAssignment(codes[]) | A, révocation sessions |
| GET `/roles` | RoleDto[] | A |
| GET `/technical-directory` | noms, compétences et disponibilités utiles | A/R, données minimisées |

La création de compte déclenche une invitation à définir le mot de passe via jeton ; aucun mot de passe retourné en DTO. L'invitation exige SMTP configuré ou une remise sécurisée explicitement organisée.

## Établissement, équipes et consignes

| Méthode et chemin | Contrat | Accès |
|---|---|---|
| GET, PUT `/facility` | FacilityDto / FacilityUpdate(name,zoneId,currency) | Lecture authentifiée minimale ; écriture A |
| GET, POST `/teams` ; GET, PUT, DELETE `/teams/{id}` | TeamDto / TeamCreateUpdate(code,name) ; DELETE archive | Lecture métier ; A/R écrivent |
| GET, POST `/teams/{id}/memberships` | MembershipDto / MembershipCreate(userId,from,until?) | A/R ; membre lit sa situation |
| PUT `/team-memberships/{id}` | dates et fin d'appartenance | A/R |
| GET, POST `/skills` ; PUT, DELETE `/skills/{id}` | SkillDto / SkillInput | A/R, lecture utile T |
| GET, POST `/users/{id}/skills` ; PUT `/user-skills/{id}` | UserSkillDto / SkillValidityInput | A/R, soi-même lit |
| GET, POST `/shift-templates` ; PUT, DELETE `/shift-templates/{id}` | ShiftTemplateInput(code,start,end,endDayOffset) | A/R |
| GET, POST `/rotation-patterns` ; PUT, DELETE `/rotation-patterns/{id}` | RotationInput(anchorDate,cycleDays,rules) | A/R |
| POST `/rotation-patterns/{id}/preview` | from,to → postes proposés et alertes, sans persistance | A/R |
| POST `/rotation-patterns/{id}/generate` | from,to → postes générés sans doublon | A/R |
| GET, POST `/shifts` ; GET, PUT `/shifts/{id}` | ShiftDto / ShiftInput(templateId,teamId,start,end) | A/R ; T ses postes |
| POST `/shifts/{id}/publish` | version et dérogations motivées → couverture validée | A/R |
| POST `/shifts/{id}/assignments` ; PUT, DELETE `/shift-assignments/{id}` | ShiftAssignmentInput(userId,start,end,replacementForId?) | A/R ; retrait historisé |
| GET, POST `/absences` ; PUT `/absences/{id}` | AbsenceInput(start,end,reasonCode) | T soi-même soumet ; A/R gèrent |
| POST `/absences/{id}/approve` ; POST `/absences/{id}/reject` | DecisionInput(reason) | A/R |
| GET, POST `/handovers` ; GET `/handovers/{id}` | HandoverInput(fromShiftId,toShiftId,priority,comment,items[]) | A/R/T du périmètre |
| POST `/handovers/{id}/acknowledge` | accusé nominatif serveur | Membre du poste entrant ou R délégué avec motif |
| POST `/handovers/{id}/addenda` | HandoverAddendum(comment,items[]) | Émetteur/R, lien original conservé |

## Patrimoine et fichiers

| Méthode et chemin | Contrat | Accès |
|---|---|---|
| GET, POST `/locations` ; GET, PUT, DELETE `/locations/{id}` | LocationInput(parentId,type,code,name) | Lecture métier ; A/R écrivent, DELETE archive |
| GET, POST `/asset-categories` ; PUT, DELETE `/asset-categories/{id}` | CategoryInput(code,name) | A/R, lecture métier |
| GET, POST `/assets` ; GET, PUT, DELETE `/assets/{id}` | AssetFilter ; AssetInput(données patrimoniales) | Lecture métier ; A/R écrivent, DELETE archive |
| GET `/assets/{id}/history` | historique paginé, OT et mesures autorisés | Lecture métier |
| GET `/assets/{id}/qr` | QR SVG/PNG représentant jeton opaque | Lecture métier ; aucun droit porté par QR |
| GET `/assets/by-qr/{token}` | AssetSummaryDto | Session et autorisation équipement |
| GET, POST `/assets/{id}/downtimes` ; PUT `/asset-downtimes/{id}` | DowntimeInput(start,end?,planned,reason) | A/R ; fin technique par exécutant autorisé |
| POST `/files/uploads` | UploadInit(name,mime,size,sha256) → fileId, URL temporaire | Authentifié avec cible autorisée |
| POST `/files/{id}/complete` | empreinte → état de scan | Upload propriétaire |
| GET `/files/{id}/download` | redirection/signature courte après vérification | Droits de l'objet lié ; fichier SCANNED |
| POST `/attachments` ; DELETE `/attachments/{id}` | AttachmentInput(fileId,target,purpose,title) | Écriture autorisée sur cible, retrait historisé |
| GET `/assets/{id}/attachments` ; GET `/work-orders/{id}/attachments` | AttachmentDto[] | Lecture métier |

La cible d'upload est vérifiée avant émission d'URL et au rattachement ; taille, empreinte et type sont revérifiés côté stockage/service. Un identifiant de fichier seul n'autorise pas le téléchargement.

## Préventif, correctif et checklists

| Méthode et chemin | Contrat | Accès |
|---|---|---|
| GET, POST `/preventive-plans` ; GET, PUT, DELETE `/preventive-plans/{id}` | PlanInput(assetId,versionChecklist,frequency,interval,mode,anchorLocal,zoneId,teamId,skills,anticipation) | A/R ; lecture T utile ; DELETE désactive |
| POST `/preventive-plans/{id}/preview` | horizon → occurrences calculées sans création | A/R |
| POST `/preventive-plans/{id}/generate` | horizon borné → occurrences/OT créés | A/R ; scheduler utilise même service |
| GET `/preventive-plans/{id}/occurrences` | OccurrenceDto[] y compris manquées | A/R, lecture métier filtrée |
| GET, POST `/checklists` ; GET, PUT, DELETE `/checklists/{id}` | TemplateInput(name,portée,description) | A/R écrivent, T/E lisent les versions affectées |
| GET, POST `/checklists/{id}/versions` | ChecklistVersionDto / DraftVersionInput | A/R |
| PUT `/checklist-versions/{id}` | steps[{position,type,required,min,max,unit,instruction}] | A/R, brouillon seulement |
| POST `/checklist-versions/{id}/publish` | version validée → immutable | A/R |
| GET, POST `/failure-reports` ; GET `/failure-reports/{id}` | FailureInput(assetId?,locationId,description,priority,photoIds) | Déclarants autorisés par matrice |
| POST `/failure-reports/{id}/qualify` | QualificationInput(assetId,priority,duplicateOfId?) → OT ou lien doublon | A/R |
| GET, POST `/work-orders` ; GET, PUT `/work-orders/{id}` | WorkOrderFilter ; WorkOrderCreate(assetId,type,title,description,dueAt) ; Update limité aux champs autorisés | Lecture métier ; création technique A/R/T, affectation A/R |
| POST `/work-orders/{id}/assign` | AssignmentInput(teamId?,userId?,shiftId?,externalMissionId?) | A/R |
| POST `/work-orders/{id}/start` | SafetyStartInput(checks) | Exécutant affecté habilité |
| POST `/work-orders/{id}/wait` | WaitInput(reason,note) | Exécutant affecté ou A/R |
| POST `/work-orders/{id}/resume` | ResumeInput(note) | Exécutant/A/R ; prérequis revérifiés |
| POST `/work-orders/{id}/complete` | CompletionInput(diagnostic,cause,solution,report) | Exécutant, bloque checklist incomplète |
| POST `/work-orders/{id}/validate` | ValidationInput(comment?) | A/R distinct de tout exécutant |
| POST `/work-orders/{id}/reject-validation` | DecisionInput(reason) | A/R |
| POST `/work-orders/{id}/cancel` | DecisionInput(reason) | A/R avant validation |
| POST `/work-orders/{id}/follow-ups` | FollowUpInput(title,reason) → nouvel OT lié | A/R |
| GET `/work-orders/{id}/transitions` | TransitionDto paginés | Lecture métier |
| GET, POST `/work-orders/{id}/time-entries` ; PUT `/time-entries/{id}` | TimeEntryInput(start,end?,breakMinutes) | Exécutant, corrections A/R motivées |
| POST `/checklist-executions` ; GET `/checklist-executions/{id}` | ExecutionCreate(workOrderId,checklistVersionId) | Exécutant et lecture métier |
| PUT `/checklist-executions/{id}/answers/{stepId}` | AnswerInput(type,value,comment?,fileIds?,deviceRecordedAt?) | Exécutant affecté, avant soumission finale |
| POST `/checklist-executions/{id}/submit` | attestation nominative → ExecutionDto | Exécutant |
| GET, POST `/work-orders/{id}/anomalies` | AnomalyDto / AnomalyInput(answerId?,description,severity) | Exécutant/A/R |
| POST `/anomalies/{id}/resolve` | DispositionInput(action,reason) | A/R, réponse hors bornes conservée |
| POST `/anomalies/{id}/corrective-request` | qualification minimale → FailureReportDto lié | Exécutant/A/R |
| GET `/assets/{id}/measurements` | MeasurementDto paginés, filtres métrique/période | Lecture métier |

Le contrat `PUT /work-orders/{id}` exclut statut, auteur, temps consolidé, validation et stock. Une déclaration portant seulement sur un local doit être qualifiée sur un équipement avant création d'un OT dans ce modèle.

## Achats, stocks et sous-traitance

| Méthode et chemin | Contrat | Accès |
|---|---|---|
| GET, POST `/suppliers` ; GET, PUT, DELETE `/suppliers/{id}` | SupplierInput(code,name,contacts) | A ; R lecture ; DELETE archive |
| GET, POST `/purchase-requests` ; GET, PUT `/purchase-requests/{id}` | RequestInput(workOrderId?,justification,lines[]) | Création A/R/T ; lecture par périmètre ; édition brouillon |
| POST `/purchase-requests/{id}/submit` | demande complète | Demandeur |
| POST `/purchase-requests/{id}/approve` ; POST `/purchase-requests/{id}/reject` | DecisionInput(reason) | A/R distinct du demandeur, rejet motivé |
| POST `/purchase-requests/{id}/send-admin` | transmission | A/R |
| GET, POST `/purchase-orders` ; GET, PUT `/purchase-orders/{id}` | OrderInput(supplierId,requestLines,quantities,prices,currency) | A |
| POST `/purchase-orders/{id}/place` ; POST `/purchase-orders/{id}/cancel` | confirmation ou motif | A, quantités reçues protégées |
| POST `/purchase-requests/{id}/receive` | ReceiptInput(orderId,deliveryReference,lines) | A ; raccourci limité aux lignes de cette demande |
| GET, POST `/goods-receipts` ; GET `/goods-receipts/{id}` | ReceiptInput(orderId,deliveryReference,lines) | A ; réception ne crédite pas le disponible |
| POST `/receipt-lines/{id}/quality-inspections` | QualityInput(accepted,rejected,notes,stockLocationId) | A ; R habilité pour contrôle technique |
| POST `/quality-inspections/{id}/supplier-return` | ReturnInput(quantity,reason) | A, mouvement compensatoire si nécessaire |
| GET, POST `/inventory/items` ; GET, PUT, DELETE `/inventory/items/{id}` | ItemInput(code,name,unit,min,cost,currency) | A écrivent ; lecture R/T utile |
| GET, POST `/inventory/locations` ; PUT, DELETE `/inventory/locations/{id}` | StockLocationInput | A ; archive seulement si soldes nuls |
| GET `/inventory/balances` | article, emplacement, physique, réservé, disponible | A/R, T périmètre utile |
| GET, POST `/inventory/reservations` ; POST `/inventory/reservations/{id}/release` | ReservationInput(workOrderId,itemId,quantity) | A/R ; T sur OT propre selon délégation |
| GET, POST `/inventory/movements` | MovementFilter / MovementInput(kind,balanceId,workOrderId?,reservationId?,quantity,reason) | Lecture métier ; sortie T propre OT, ajustement A |
| GET, POST `/subcontractors` ; GET, PUT, DELETE `/subcontractors/{id}` | ContractorInput(contacts,skills) | A/R ; E fiche propre en lecture |
| GET, POST `/contracts` ; GET, PUT, DELETE `/contracts/{id}` | ContractInput(contractorId,reference,dates,scope,documentId) | A/R ; E contrat propre autorisé |
| GET, POST `/external-interventions` ; GET, PUT `/external-interventions/{id}` | MissionInput(contractId,workOrderId,visit,internalContact) | A/R gèrent ; E lit/rapporte sa mission |
| POST `/external-interventions/{id}/submit-report` | ExternalReportInput(report,files,cost?) | E mission affectée ou référent interne |
| POST `/external-interventions/{id}/approve` ; POST `/external-interventions/{id}/reject` | DecisionInput(reason) | A/R ; ne vaut pas validation finale OT |
| GET, POST `/external-interventions/{id}/material-requests` | MaterialRequestInput(designation,quantity,justification) | E mission propre, A/R |
| POST `/external-material-requests/{id}/review` | DecisionInput → demande achat interne si acceptée | A/R |

Une entrée de stock de réception passe par le contrôle qualité, pas par un `IN` arbitraire envoyé par un technicien. Les retours et ajustements ne modifient jamais un mouvement existant.

## Reporting, notifications et synchronisation

| Méthode et chemin | Contrat | Accès |
|---|---|---|
| GET `/reports/dashboard` | filtres période/équipe/équipement → DashboardDto adapté au rôle | A/R ; autres rôles vue propre |
| GET `/reports/maintenance` | indicateurs, séries, périmètre et définitions | A/R |
| GET `/reports/purchases` ; GET `/reports/inventory` | coûts et flux filtrés | A/R selon colonnes |
| POST `/reports/exports` ; GET `/reports/exports/{id}` | ExportInput(type,format,filters) → état et lien autorisé | Même droits que le rapport, A/R |
| GET `/notifications` | page, unreadOnly → NotificationDto | Soi-même |
| POST `/notifications/{id}/read` ; POST `/notifications/read-all` | marquage lu | Soi-même |
| GET `/notifications/stream` | flux SSE d'événements sans secret | Session authentifiée, filtre destinataire |
| GET `/audit-events` | filtres acteur/objet/action/période → audit minimisé | A ; R périmètre technique |
| GET `/sync/bootstrap` | snapshots affectés, versions, curseur opaque | T/E autorisés, limites de volume |
| GET `/sync/changes?cursor=...` | changes[],revokedIds[],nextCursor,hasMore | Périmètre courant ; curseur ancien → resnapshot explicite |
| POST `/sync/operations` | opérations ordonnées → résultat par opération | T/E affectés ; liste de commandes autorisées |
| GET `/sync/operations/{operationId}` | état et résultat propre | Propriétaire ; A/R synthèse de supervision sans brouillons |

Une opération mobile contient `{operationId,deviceId,resourceType,resourceId,expectedVersion,command,payload,attachmentIds,deviceRecordedAt}`. Résultats : `APPLIED`, `CONFLICT`, `REJECTED`, avec version courante et code. Traitement transactionnel **par opération**, pas de promesse de transaction globale du lot ; les opérations dépendantes sont suspendues après un conflit. `409` pour conflit d'enveloppe/idempotence, `200` avec résultats individuels pour un lot valide, même s'il contient des conflits métier. La version de l'API et le type de commande font partie de l'empreinte.

## DTO et documentation attendus en développement

Chaque ressource sépare `CreateRequest`, `UpdateRequest`, `SummaryDto`, `DetailDto` et commandes de transition. Exemple `WorkOrderDetailDto` : identifiant, numéro, type/état/priorité, équipement résumé, affectation visible, échéances, temps, checklist, anomalies et version ; pas de hash utilisateur, session ou informations de contrat confidentielles hors habilitation.

Bornes proposées : nom 160 caractères, description 10 000, note 4 000, 100 opérations par lot sync, photo 10 Mio, document 25 Mio, export asynchrone au-delà de 5 000 lignes. Valider les limites sur les appareils réels. Les dates et quantités impossibles sont refusées côté serveur ; les montants sont invisibles pour les rôles sans accès coûts.

OpenAPI devra documenter exemples, contraintes, erreurs, security schemes et préconditions ; Swagger accessible en développement, protégé ou désactivé en production. Actuator/Prometheus appartiennent au réseau d'exploitation et ne sont pas des endpoints métier publics.
