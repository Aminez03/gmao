# Cahier des charges fonctionnel

## Objectifs et périmètre

Assurer la continuité des équipements techniques, organiser les interventions et tracer les décisions sur les trois postes. Couvrir le patrimoine, la maintenance préventive et corrective, les checklists, les consignes, les pièces et achats, les prestataires et le pilotage. Livrer une interface française responsive, claire/sombre et une PWA permettant la saisie temporaire hors connexion.

La plateforme assiste les équipes ; les procédures de consignation, autorisations de travaux et mesures de sécurité restent sous responsabilité des professionnels habilités. Une validation logicielle ne remplace pas ces procédures. Les équipements biomédicaux peuvent être référencés, mais les exigences spécifiques de matériovigilance et les intégrations SIH ne sont pas présumées couvertes.

Hors périmètre initial : dossiers médicaux, facturation des soins, paie, comptabilité générale, achats automatiques auprès de fournisseurs, télémétrie IoT et connexion native aux automates. Les exports et la messagerie ne doivent pas envoyer de données réelles avant configuration et autorisation métier.

## Acteurs et droits

Toutes les lectures et écritures exigent une authentification, à l'exception de la connexion, de la demande de réinitialisation et des sondes techniques minimales. Un rôle accorde des capacités ; le périmètre de l'objet limite ensuite l'accès. Le rôle n'est jamais accepté comme preuve depuis le corps HTTP.

| Action | ADMIN | RESPONSABLE_TECHNIQUE | TECHNICIEN | SOUS_TRAITANT |
| --- | --- | --- | --- | --- |
| Utilisateurs, rôles, paramètres | Gérer | Consulter annuaire technique | Profil propre | Profil propre |
| Équipes, horaires, affectations | Gérer | Gérer | Lire son planning | Visites propres |
| Patrimoine et gammes | Gérer | Gérer | Lire périmètre attribué | Équipements des missions autorisées |
| Déclarer une panne | Oui | Oui | Oui | Seulement mission propre |
| Affecter, suspendre, annuler un OT | Oui | Oui | — | — |
| Exécuter un OT/checklist | Si affecté et habilité | Si affecté et habilité | OT affectés | Missions affectées |
| Valider ou refuser un OT terminé | Oui, sauf propre exécution | Oui, sauf propre exécution | — | — |
| Consignes | Superviser | Gérer | Émettre/recevoir pour son poste | — |
| Demande d'achat | Créer | Créer et approuver | Créer | Demande de matériel liée à mission |
| Commande, réception, stock | Gérer | Lire, contrôle technique | Sortie autorisée sur OT | — |
| Rapports/coûts | Tous | Tous techniques | Ses temps et activités | Ses prestations |
| Audit | Lire | Lire activités techniques | Historique de ses OT | Historique de ses missions |

`ADMIN` n'échappe pas à la séparation exécutant/validateur. Un second validateur habilité doit être disponible. Les comptes de prestataires expirent à la fin du contrat ; accès par entreprise ET mission affectée, pas par identifiant devinable. Les changements de rôle révoquent les sessions concernées.

## Exigences fonctionnelles

| ID | Module | Exigences et résultat attendu |
|---|---|---|
| F01 | Authentification | Connexion/déconnexion, renouvellement et révocation des sessions, profil, changement et réinitialisation du mot de passe ; administration des quatre rôles ; journal de connexion. Désactivation plutôt que suppression d'un compte utilisé. |
| F02 | Équipes et postes | Équipes A/B/C, compétences et habilitations datées, horaires configurables, calendrier de rotation, 1 à 2 techniciens par poste, absences et remplacements approuvés. Alerte de sous-effectif et de défaut de compétence. |
| F03 | Consignes | Équipe/poste émetteur et récepteur, auteur, date, priorité, anomalies, OT ouverts et équipements à surveiller, commentaire, accusé nominatif horodaté ; relance des consignes non reçues. |
| F04 | Patrimoine | Hiérarchie établissement → bâtiment → étage → zone → local, équipements et sous-équipements ; code, catégorie, marque, modèle, série, mise en service, fournisseur, garantie, criticité C1–C4, état, QR, documents, photos et historique. CRUD avec archivage. |
| F05 | Préventif | Plans par équipement, fréquence quotidienne à annuelle, intervalle, début, fuseau, équipe, gamme versionnée, anticipation et mode fixe/glissant ; premier OT automatique, échéances et retards, suspension motivée sans effacer l'historique. |
| F06 | Checklists | Éditeur ordonné ; cases, conformité, nombre, texte, mesure, photo, attestation ; champs obligatoires, unités et bornes ; exécution figée par version, commentaires, anomalie et éventuel ticket correctif lié. |
| F07 | Correctif/OT | Ticket numéroté, déclarant, équipement/local, description, photos, priorité, affectation, temps, diagnostic, cause, solution, pièces et rapport. États et transitions contrôlés, réception technique avant validation. |
| F08 | Achats | Besoin lié ou non à un OT, lignes de pièces, quantités, justification ; soumission, approbation/refus motivé, transmission administrative, commande, réceptions partielles, contrôle qualité et disponibilité. |
| F09 | Stocks | Articles, magasins/emplacements, fournisseurs, seuils, quantités disponibles/réservées, entrées, sorties, retours et ajustements motivés, journal inaltérable et alertes. Réservation et consommation liées aux OT. |
| F10 | Sous-traitance | Entreprises, contacts, domaines, contrats, visites, affectations, assistance sur site, matériel, justificatifs, photos, rapport et validation de prestation par responsable interne. |
| F11 | Mobile/PWA | Interventions du poste, fiche équipement et scan QR avec saisie du code en secours, checklists/mesures/photos/temps, brouillons, file d'attente persistante, synchronisation et résolution des conflits visibles. |
| F12 | Pilotage | Dashboard technique et administrateur, filtres période/équipe/équipement, pagination, Recharts, exports PDF et Excel soumis aux mêmes permissions ; indicateurs définis ci-dessous. |
| F13 | Notifications | Nouvelle tâche, affectation, échéance/retard, panne critique, demande/réception achat, consigne, visite et échec de synchronisation ; centre in-app avec lu/non lu, lien vers objet autorisé. Courriel optionnel. |
| F14 | Traçabilité | Auteur réel, instant serveur, action, objet, versions et motif ; historique des transitions, mesures, indisponibilités et modifications sensibles ; exports audités. |

## Règles métier

### Rotations et continuité

**R01.** Distinguer le modèle horaire, le poste réel daté, l'équipe et ses membres. Un poste de nuit du 20 novembre couvre `[20/11 22:00, 21/11 06:00[` en heure locale ; il appartient à la journée de début. Stocker ses bornes absolues en UTC.

**R02.** La rotation est un calendrier configurable, pas une hypothèse permanente « A=matin ». Proposition de cycle à étudier : matin → après-midi → nuit avec jours de repos définis par RH. Ne pas activer de cycle sans validation des repos et des effectifs. Interdire les affectations simultanées d'un technicien à deux postes et les absences chevauchantes. Les seuils de repos et durées maximales sont paramétrés après validation RH, sans présenter cette conception comme une conformité juridique.

**R03.** Vérifier au moins un technicien habilité disponible sur chaque poste, maximum deux en couverture normale ; les renforts exceptionnels exigent un motif et une validation. Les compétences et les autorisations critiques ont une date d'expiration. Toute dérogation est visible et auditée ; l'algorithme propose, le responsable décide.

**R04.** Une passation associe deux postes successifs. L'équipe entrante confirme nominativement la réception ; un document reçu reste immuable, un complément crée un addendum. Une absence d'accusé après 15 minutes déclenche une alerte au responsable (seuil proposé). La passation ne termine ni ne réaffecte automatiquement les OT.

### Interventions et checklists

**R05.** États normatifs : `NOUVELLE → AFFECTEE → EN_COURS → EN_ATTENTE → EN_COURS → TERMINEE → VALIDEE`. Le passage `EN_COURS → TERMINEE` sans attente est permis. L'attente exige un motif (`PIECE`, `PRESTATAIRE`, `AUTORISATION`, `AUTRE`) et une note. Le refus de validation ramène à `EN_COURS` avec motif. `ANNULEE` est possible avant validation, par superviseur, avec raison. Une correction après validation crée un OT de reprise lié, sans réécrire l'original.

**R06.** Affectation : technicien/équipe ou mission externe autorisée ; démarrage réservé à l'exécutant affecté, habilitation valide et prérequis de sécurité renseignés. `TERMINEE` signifie soumise par l'exécutant avec `completed_at`. `VALIDEE` ajoute `validated_at` et le validateur distinct. Conserver séparément début réel, fin réelle, validation, temps de travail cumulé et périodes d'attente ; le temps passé n'est pas la durée calendaire.

**R07.** Une étape obligatoire doit avoir une réponse du bon type, un auteur et un horodatage. Une mesure hors bornes est enregistrée, jamais perdue ; elle crée une anomalie et empêche la clôture tant qu'une correction ou une disposition motivée du responsable n'est pas tracée. La checklist ne peut pas être contournée par un appel direct à l'API. L'attestation de l'exécutant est une validation nominative, sans promesse de signature électronique qualifiée.

**R08.** C1 est la criticité patrimoniale maximale ; P1 est la priorité opérationnelle maximale. La priorité proposée tient compte de la criticité, mais peut être augmentée selon l'impact de la panne. Les délais de prise en charge P1/P2/P3/P4 sont à contractualiser ; aucun arrêt technique ne doit être automatiquement considéré comme acceptable.

### Planification préventive

**R09.** Fréquences : jour, semaine, mois, trimestre (3 mois), semestre (6 mois), année, multipliées par un intervalle entier positif. L'heure locale et le fuseau appartiennent au plan. Pour une échéance mensuelle au 31, utiliser le dernier jour du mois si nécessaire, tout en conservant le jour d'ancrage 31 pour les mois suivants. Pour le 29 février annuel, utiliser le dernier jour de février les années non bissextiles.

**R10.** Mode glissant : la prochaine échéance = **instant de validation** converti dans le fuseau du plan + période. La fin réelle de travaux reste enregistrée séparément. Mode fixe : les occurrences sont calculées depuis l'ancre d'origine, sans dérive liée au retard. Exemple : ancre 31/01, mois suivant 28/02 puis 31/03 ; validation le 05/03 → prochaine occurrence future au 31/03.

**R11.** Choix proposé : un seul OT non terminal par plan. Le premier est généré dans la fenêtre d'anticipation ; les suivants sont générés lors de la validation du précédent, ou par reprise du scheduler si nécessaire. Chaque OT préventif porte un identifiant d'occurrence et l'échéance. Les créneaux fixes dépassés pendant un OT ouvert sont consignés comme occurrences `MANQUEE`, avec compteur et motif ; aucune multiplication silencieuse de tickets. Ce choix doit être validé pour les contrôles réglementaires exigeant un constat à chaque échéance.

**R12.** Validation, journal et calcul de la prochaine occurrence sont atomiques. Verrouiller le plan et contrôler l'unicité en base ; deux schedulers ou deux validations ne produisent pas deux OT. La désactivation d'un plan conserve les OT déjà émis et exige leur traitement explicite. Une annulation d'OT ne fait pas avancer l'échéance sans décision motivée.

### Achats et stock

**R13.** Parcours : `BROUILLON → SOUMISE → APPROUVEE_TECHNIQUE → TRANSMISE_ADMIN → COMMANDEE → PARTIELLEMENT_RECUE → RECUE → CONTROLEE → DISPONIBLE`. La branche de refus d'approbation revient au demandeur ; les pièces non conformes restent en quarantaine et font l'objet d'un retour fournisseur, distinct du refus de la demande.

**R14.** Gérer les quantités par ligne, y compris reliquats et retours. Le contrôle qualité n'augmente le stock utilisable que de la quantité acceptée, une seule fois. La quantité totale reçue ne dépasse pas la commande hors tolérance approuvée. Réserver les pièces pour les OT ; une réparation ne devient prête à replanifier que lorsque **toutes** ses réservations manquantes sont couvertes. Notifier le responsable, sans démarrer automatiquement une intervention.

**R15.** Une sortie doit référencer un OT actif, ne peut rendre le disponible négatif et crée un mouvement avec prix historique. Une correction de stock se fait par mouvement compensatoire motivé, jamais par modification du journal. Devise par défaut proposée : TND, paramétrable ; montants décimaux et prix figés au mouvement.

### Mobilité, sécurité et données

**R16.** Autoriser hors ligne les brouillons, mesures, réponses, photos et temps des OT précédemment téléchargés. Les validations finales, approbations, affectations et mouvements de stock nécessitent une connexion pour arbitrer les accès et la concurrence. Afficher « enregistré sur cet appareil » séparément de « synchronisé ».

**R17.** Chaque opération possède un UUID, une version attendue et une empreinte de contenu ; une répétition identique renvoie le même résultat. Une même clé avec un contenu différent est refusée. Une version divergente crée un conflit visible, jamais un écrasement automatique. Les droits sont revérifiés lors de la synchronisation, même si la saisie a eu lieu avant révocation.

**R18.** Dates serveur en UTC, présentation dans le fuseau de l'établissement. Horloge serveur pour les transitions, heure de saisie du terminal conservée comme information distincte. Archivage des références utilisées ; aucune suppression en cascade de l'historique technique. Durées de rétention et purge après décision de la clinique.

## Indicateurs et filtres

Périodes interprétées dans le fuseau de la clinique, bornes `[début, fin[`, calcul et provenance affichés. Exclure les OT annulés et les données d'essai. Les indicateurs d'équipement agrègent les indisponibilités fusionnées afin de ne pas doubler les temps de pannes superposées.

| Indicateur | Définition proposée |
|---|---|
| En cours / retard / critiques | Comptages par état actif ; retard = échéance dépassée et non terminé/validé ; critiques = correctifs P1 actifs. |
| Disponibilité | `(temps de service prévu − indisponibilité constatée sur ce temps) / temps de service prévu`. Base 24/7 sauf calendrier d'équipement défini ; sans historique fiable, afficher non disponible. |
| MTTR | Somme des durées de réparation des pannes réparées sur la période / nombre de ces pannes ; temps d'attente présenté séparément. |
| MTBF | Temps de fonctionnement observé / nombre de pannes sur la même période et périmètre ; sans panne, afficher « aucune panne observée », pas une valeur infinie. |
| Réalisation préventive | Occurrences exigibles réalisées et validées à l'échéance / occurrences exigibles de la période, y compris les occurrences manquées. Afficher séparément le rattrapage tardif. |
| Coût de maintenance | Pièces consommées aux prix historiques + temps valorisé au tarif historique autorisé + prestations acceptées ; devise et disponibilité des postes de coût affichées. |
| Charge / performance | Temps affecté et temps réel par poste/technicien, réalisé/planifié par équipe ; ne pas assimiler une mesure de volume à une évaluation individuelle. |

## Exigences non fonctionnelles proposées

| Domaine | Cible de recette à confirmer |
|---|---|
| Charge | Hypothèse de dimensionnement : 5 000 équipements, 100 000 OT, 50 utilisateurs simultanés ; mesurer sur données représentatives. |
| Performance | p95 API de lecture < 500 ms, écriture < 1 s hors fichiers/exports ; page principale utilisable < 3 s sur réseau clinique représentatif. |
| Disponibilité | Objectif applicatif mensuel 99,9 % ; le Compose mono-hôte de développement ne garantit pas cet objectif. Hébergement redondant et exploitation nécessaires. |
| Reprise | RPO ≤ 15 min et RTO ≤ 2 h proposés ; sauvegardes chiffrées hors hôte, archivage WAL et test de restauration. Synchroniser aussi les objets MinIO. |
| Protection | HTTPS, mots de passe hachés, sessions révocables, contrôles objet, MFA pour comptes sensibles proposé, secrets hors dépôt, limitation des tentatives. |
| Utilisabilité | Interface française ; clavier, labels et messages explicites ; cibles tactiles adaptées, largeur 360 px à grand écran, contraste et objectif WCAG 2.2 AA à vérifier. |
| Hors ligne | 24 h de travail local proposées ; plafond de fichiers explicite, détection quota, aucune purge silencieuse d'une opération en attente. |
| Exploitabilité | Logs structurés corrélés sans secrets, métriques, alertes scheduler/stock/sync/sauvegarde ; procédures de poste en cas de panne informatique. |

## Points ouverts avant engagement

Valider effectifs et remplaçants, matrice réelle de compétences, cycle RH, catalogue d'équipements, criticités, procédures de sécurité, validation indépendante, tolérances d'achat, seuils SLA et conservation des données. Choisir un parc de navigateurs/appareils gérés pour limiter les risques de perte locale. Identifier les référents technique, achats, direction, informatique et protection des données.
