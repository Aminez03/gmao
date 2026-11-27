# Diagrammes UML

Le [diagramme de classes persistantes en PlantUML](../diagrammes/classes-persistantes.puml) décrit l’implémentation actuelle : 34 entités JPA, deux tables techniques sans entité JPA, leurs attributs et les associations définies par les clés étrangères Flyway. Il distingue les références UUID contraintes en base des références polymorphes et des documents JSONB.

Ces diagrammes décrivent la cible ; les décisions proposées sont détaillées dans le cahier des charges. Mermaid ne propose pas de notation native complète des cas d'utilisation : le premier diagramme représente les acteurs et leurs cas sous forme de graphe. Les autres utilisent les notations classe, états et séquence.

## Acteurs et cas d'utilisation

```mermaid
flowchart LR
    A[ADMIN] --> U1([Administrer les comptes et permissions])
    R[Responsable technique] --> U2([Planifier les postes et affecter les OT])
    R --> U3([Valider interventions et achats])
    R --> U4([Gérer patrimoine et plans préventifs])
    T[Technicien] --> U5([Déclarer et exécuter une intervention])
    T --> U6([Renseigner checklist, mesures et temps])
    T --> U7([Transmettre et accuser les consignes])
    T --> U8([Saisir hors ligne et synchroniser])
    A --> U9([Commander, réceptionner et gérer le stock])
    A --> U10([Consulter les indicateurs et exports])
    E[Sous-traitant] --> U11([Traiter ses missions et remettre un rapport])
    H[Scheduler interne] --> U12([Générer les échéances et alertes])
    U5 --> U6
    U11 --> U6
    U3 --> U13([Contrôler les étapes obligatoires])
```

## Classes du noyau métier

```mermaid
classDiagram
    class User {
      UUID id
      String email
      Boolean active
    }
    class Team {
      UUID id
      String code
    }
    class ShiftInstance {
      Instant startsAt
      Instant endsAt
      validateCoverage()
    }
    class ShiftAssignment {
      UUID id
      String assignmentRole
    }
    class Handover {
      Instant issuedAt
      Instant acknowledgedAt
      acknowledge()
    }
    class Location {
      String type
      String name
    }
    class Asset {
      String code
      String criticality
      String status
    }
    class PreventivePlan {
      String schedulingMode
      String frequency
      String zoneId
      nextOccurrence()
    }
    class PreventiveOccurrence {
      Instant dueAt
      String status
    }
    class WorkOrder {
      String number
      String status
      Long version
      start()
      complete()
      validate()
    }
    class ChecklistVersion {
      Integer revision
      Instant publishedAt
    }
    class ChecklistExecution {
      Instant submittedAt
      verifyRequiredAnswers()
    }
    class ChecklistAnswer {
      String valueType
      Boolean conforming
    }
    Team "1" --> "0..*" ShiftInstance
    ShiftInstance "1" *-- "0..*" ShiftAssignment
    User "1" --> "0..*" ShiftAssignment
    ShiftInstance "1" --> "0..*" Handover : source
    ShiftInstance "1" --> "0..*" Handover : destination
    Location "0..1" --> "0..*" Location : parent
    Location "1" --> "0..*" Asset
    Asset "1" --> "0..*" WorkOrder
    Asset "1" --> "0..*" PreventivePlan
    PreventivePlan "1" --> "0..*" PreventiveOccurrence
    PreventiveOccurrence "0..1" --> "0..1" WorkOrder
    ChecklistVersion "1" --> "0..*" ChecklistExecution
    WorkOrder "1" --> "0..*" ChecklistExecution
    ChecklistExecution "1" *-- "0..*" ChecklistAnswer
```

Les relations de rôles, stocks, achats et documents sont détaillées dans le modèle de données. La cardinalité 0..* d'affectations représente aussi les postes encore incomplets ; la publication d'un planning déclenche le contrôle de couverture de 1 à 2 techniciens.

## États d'un ordre de travail

```mermaid
stateDiagram-v2
    [*] --> NOUVELLE
    NOUVELLE --> AFFECTEE : affectation autorisée
    AFFECTEE --> EN_COURS : démarrage et sécurité vérifiés
    EN_COURS --> EN_ATTENTE : motif obligatoire
    EN_ATTENTE --> EN_COURS : reprise autorisée
    EN_COURS --> TERMINEE : rapport et checklist complets
    TERMINEE --> EN_COURS : refus motivé
    TERMINEE --> VALIDEE : second validateur habilité
    NOUVELLE --> ANNULEE : superviseur et motif
    AFFECTEE --> ANNULEE : superviseur et motif
    EN_COURS --> ANNULEE : superviseur et motif
    EN_ATTENTE --> ANNULEE : superviseur et motif
    TERMINEE --> ANNULEE : superviseur et motif
    VALIDEE --> [*]
    ANNULEE --> [*]
```

## Séquence : validation préventive concurrente

```mermaid
sequenceDiagram
    actor R as Responsable
    participant UI as React
    participant API as Security et Controller
    participant S as WorkOrderService
    participant DB as PostgreSQL
    participant N as NotificationDispatcher
    R->>UI: Valider OT terminé
    UI->>API: POST validate, JWT, If-Match, Idempotency-Key
    API->>API: Authentifier et contrôler le périmètre
    API->>S: Commande avec principal serveur
    S->>DB: BEGIN, réserver clé unique et verrouiller plan puis OT
    S->>S: État, version, séparation, checklist et anomalies
    alt Conditions satisfaites
        S->>DB: Valider OT, journal et occurrence réalisée
        S->>DB: Calculer prochaine occurrence et créer OT unique
        S->>DB: Insérer outbox et réponse idempotente, COMMIT
        S-->>API: DTO et nouvelle version
        API-->>UI: 200 et ETag
        N->>DB: Lire outbox après commit
        N-->>UI: Notification persistée puis signal SSE
    else Conflit ou précondition métier
        S->>DB: ROLLBACK
        API-->>UI: 409, 412 ou 422 et motif exploitable
    end
```

Un rejeu exact restitue la réponse persistée sans nouvelle transition. Tous les traitements utilisent le même ordre de verrouillage, y compris le scheduler. Une notification externe indisponible n'annule pas la validation.

## Séquence : réception de pièces

```mermaid
sequenceDiagram
    actor A as Achats
    participant API as PurchaseService
    participant DB as PostgreSQL
    participant R as Responsable technique
    A->>API: Enregistrer réception partielle
    API->>DB: Enregistrer lignes en quarantaine
    A->>API: Contrôler quantité acceptée et refusée
    API->>DB: BEGIN, verrouiller réception et soldes
    API->>DB: Mouvement unique pour quantité acceptée
    API->>DB: Couvrir réservations selon arbitrage défini
    API->>DB: Évaluer besoins restants, audit et outbox, COMMIT
    API-->>R: Alerte OT prêt à replanifier si tous besoins couverts
    R->>API: Réaffecter et reprendre explicitement l'OT
```

## Séquence : hors ligne et conflit

```mermaid
sequenceDiagram
    actor T as Technicien
    participant P as PWA
    participant I as IndexedDB
    participant API as Sync API
    participant DB as PostgreSQL
    T->>P: Saisir mesure sans réseau
    P->>I: Transaction brouillon et opération UUID, version 7
    I-->>P: Écriture persistée
    P-->>T: Enregistré sur cet appareil, à synchroniser
    P->>API: Retour réseau, envoyer opération et pièces jointes
    API->>API: Revérifier session, droits et empreinte
    API->>DB: Comparer version serveur
    alt Version attendue valide
        API->>DB: Appliquer et mémoriser réponse atomiquement
        API-->>P: APPLIED, version 8
        P->>I: Marquer synchronisé après accusé
    else Modification concurrente
        API-->>P: CONFLICT, version serveur et champs autorisés
        P->>I: Conserver les deux versions et le brouillon
        P-->>T: Afficher conflit à résoudre
        T->>P: Comparer et confirmer nouvelle proposition
        P->>API: Nouvelle opération UUID sur version courante
    end
```
