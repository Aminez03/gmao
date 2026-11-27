# Backlog, planning, risques et validation

## Backlog priorisé

Priorités : P0 = condition de sécurité ou de cohérence ; P1 = fonctionnement essentiel ; P2 = périmètre complet du brief après socle. P2 ne signifie pas abandon. Chaque lot comprend analyse détaillée, données, API, tests, interface, connexion réelle et documentation. Les phases 1 à 12 restent celles du brief.

| ID | Priorité / phase | Histoire et livrable vérifiable | Dépendances | Recette |
|---|---|---|---|---|
| B01 | P0 / 1 | En tant que commanditaire, valider les 12 livrables et les hypothèses. | — | V01 |
| B02 | P0 / 2 | En tant que développeur, disposer du schéma Flyway, des contraintes et d'un jeu de données de test indépendant. | B01 | V02 |
| B03 | P0 / 2 | Reprendre l'ébauche, corriger les incohérences et compiler une base testée, sans perdre les données éventuelles. | B01 | V02 |
| B04 | P0 / 3 | Se connecter avec session rotative, déconnexion, reset et protections anti-abus. | B02/B03 | V03 |
| B05 | P0 / 3 | Administrer comptes et quatre rôles ; appliquer capacités et périmètres à toutes les routes/API. | B04 | V04 |
| B06 | P0 / 3 | Tracer les actions, versions et événements ; erreurs uniformes et audit sans secrets. | B04 | V05 |
| B07 | P1 / 4 | Gérer équipes, compétences, postes, absences et remplacements sans chevauchement. | B05 | V06 |
| B08 | P0 / 4 | Publier un planning couvert et transmettre les consignes avec accusés et alertes. | B07/B06 | V07 |
| B09 | P1 / 5 | Gérer l'arbre patrimonial, les fiches, criticités, garanties, archives et documents privés. | B05/B02 | V08 |
| B10 | P0 / 6 | Publier des versions de checklists ; enregistrer réponses typées, mesures et anomalies. | B09 | V09 |
| B11 | P0 / 6 | Créer et valider les OT préventifs avec modes fixe/glissant et anti-doublons. | B10/B07/B06 | V10 |
| B12 | P1 / 7 | Déclarer, qualifier, affecter et exécuter le correctif ; tracer attentes, temps, diagnostic et validation. | B09/B10/B06 | V11 |
| B13 | P0 / 8 | Réserver et consommer les pièces sans stock négatif, conserver les coûts historiques. | B12 | V12 |
| B14 | P1 / 8 | Suivre demande, approbation, commande, réceptions partielles et contrôle qualité. | B13/B05 | V13 |
| B15 | P1 / 8 | Reprendre la planification d'un OT lorsque toutes ses pièces sont disponibles. | B14/B12 | V13 |
| B16 | P1 / 9 | Gérer contrats, visites, demandes de matériel et rapports externes avec isolation des entreprises. | B12/B14 | V14 |
| B17 | P1 / 10 | Installer la PWA, accéder aux fiches par QR et utiliser les écrans tactiles. | B09/B12 | V15 |
| B18 | P0 / 10 | Préserver les saisies hors ligne, rejouer sans doublon et résoudre les conflits explicitement. | B17/B06/B10 | V16 |
| B19 | P1 / transverse | Recevoir les alertes in-app persistées, suivre les erreurs et relancer après coupure. | B06 puis événements de chaque module | V17 |
| B20 | P1 / 11 | Afficher les dashboards technique/administrateur et KPI à partir des données réelles. | B11/B12/B14/B16 | V18 |
| B21 | P2 / 11 | Exporter PDF/Excel avec mêmes filtres et permissions, protection des formules tableur. | B20 | V18 |
| B22 | P1 / transverse | Offrir français par défaut, accessibilité, thème sombre et interface responsive. | Dès B04 et chaque lot | V19 |
| B23 | P0 / 12 | Livrer Compose, healthchecks, HTTPS, sauvegarde/restauration et monitoring. | Préparation B02, finalisation tous lots | V20 |
| B24 | P0 / 12 | Réaliser tests de charge/sécurité/E2E, recette terrain sur trois postes et exercice de reprise. | Tous lots | V21 |

## Planning prévisionnel

Estimation initiale, non engagement : **26 semaines après validation**, dont 23 de réalisation prévue et 3 de réserve. Hypothèse : deux développeurs à temps plein (frontend et backend), QA à mi-temps, référent technique clinique disponible chaque semaine, support exploitation/sécurité à temps partiel. Soit environ 52 semaines-personnes de développement en capacité calendaire, plus QA et exploitation ; le coût dépend des tarifs et congés. Avec un développeur seul, replanifier le périmètre et les délais.

| Phase | Fenêtre relative | Sortie attendue |
|---|---|---|
| 1 — Analyse et conception | Dossier actuel ; validation avant S1 | Périmètre, risques et décisions approuvés |
| 2 — Données et socle | S1–S2 | Reprise auditée, migrations testées, CI de base |
| 3 — Authentification et utilisateurs | S3–S4 | Tests permissions/session et écrans connectés |
| 4 — Équipes 3 × 8 et consignes | S5–S6 | Planning daté, remplacements, passation réelle |
| 5 — Patrimoine | S7–S8 | Import pilote, fiches et pièces privées |
| 6 — Préventif et checklists | S9–S11 | Génération et validation concurrente testées |
| 7 — Correctif | S12–S13 | Cycle complet avec attentes et rapport |
| 8 — Achats et stock | S14–S16 | Réception partielle et remise en disponibilité |
| 9 — Sous-traitance | S17 | Isolation et validation des prestations |
| 10 — PWA, QR et hors ligne | S18–S20 | Tests sur appareils et réseau intermittents |
| 11 — Pilotage et exports | S21 | KPI rapprochés des données de référence |
| 12 — Durcissement et déploiement | S22–S23 | Charge, sécurité, restauration et recette terrain |
| Réserve | S24–S26 | Corrections, aléas de reprise et validation finale |

Les tests, la documentation, les notifications, l'accessibilité et le déploiement de préproduction avancent à chaque lot ; ils ne sont pas reportés intégralement à la fin. La phase 12 consolide les preuves. Démonstration et acceptation à chaque sortie de phase ; pas de fonctionnalités simulées présentées comme réelles.

Jalons : M0 validation phase 1 ; M1 identité et planning (fin S6) ; M2 cycle technique préventif/correctif (S13) ; M3 achats et prestataires (S17) ; M4 mobilité (S20) ; M5 recette de mise en service (S23, réserve jusqu'à S26). Dates absolues fixées après M0 et confirmation des ressources.

## Risques

| Risque | Probabilité / impact | Mesure et preuve attendue | Responsable proposé |
|---|---|---|---|
| Ébauche incomplète et données inconnues | Forte / fort | Inventaire, compilation après validation, reprise répétée et rapprochement des comptages. | Lead backend |
| Sous-effectif réel en 3 × 8 | Forte / critique | Planning nominal et absences simulées ; renforts RH validés avant exploitation. | Responsable/RH |
| Droit usurpé ou accès inter-prestataire | Moyenne / critique | Identité depuis session, filtrage objet systématique, tests négatifs par rôle et entreprise. | Référent sécurité |
| Doubles OT ou double débit stock | Moyenne / critique | Contraintes uniques, verrous ordonnés, idempotence atomique, tests concurrents PostgreSQL. | Backend |
| Dates, nuit et fin de mois ambiguës | Forte / fort | Clock injectée, zone configurable, ancre conservée, tests de limites et de changement de jour. | Backend + métier |
| Perte de saisie mobile ou conflits | Forte / critique | IndexedDB transactionnelle, quota visible, reprise après crash, appareils gérés et validation réseau réel. | Frontend + exploitation |
| Appareil perdu et données locales | Moyenne / fort | Minimisation, partition utilisateur, politique MDM/verrouillage et durée locale bornée validées. | Informatique clinique |
| Dérive du périmètre et calendrier | Forte / fort | Lots acceptés, backlog versionné, décisions explicites pour intégrations hors périmètre. | Pilotage projet |
| Versions 3.x ou bibliothèques non maintenues | Moyenne / fort | Vérifier support, dépendances et correctifs au lancement ; décision formalisée si contrainte incompatible. | Lead technique |
| Panne mono-hôte ou sauvegarde inutilisable | Moyenne / critique | Architecture d'hébergement adaptée, sauvegarde hors hôte et exercice de restauration mesuré. | Exploitation |
| Fichiers malveillants ou photos sensibles | Moyenne / fort | Types et tailles contrôlés, scan/quarantaine, buckets privés et consignes de prise de photo. | Sécurité + métier |
| KPI inexacts | Moyenne / fort | Définition validée, périodes stables, données de référence et rapprochement manuel. | Administration + QA |
| Consigne critique non reçue | Moyenne / critique | Accusé et escalade, procédure de transmission orale/téléphonique en secours. | Responsable technique |
| Notification perdue ou mail indisponible | Moyenne / moyen | Outbox, reprise, déduplication et centre in-app indépendant de SMTP. | Backend/exploitation |

## Critères de validation

Chaque critère ci-dessous exige une preuve conservée : rapport de test, capture de résultat métier ou procès-verbal signé. Ce sont des exigences de recette futures ; elles ne sont pas déclarées satisfaites par la seule documentation.

| ID | Scénario et résultat attendu |
|---|---|
| V01 | Les 12 livrables sont accessibles, les choix ouverts sont consignés, le commanditaire approuve périmètre, règles, ressources et architecture avant démarrage de phase 2. |
| V02 | Migration sur PostgreSQL vierge et copie représentative réussie ; FK, unicités, archives et absence de perte vérifiées ; aucune dépendance Java manquante ; scripts historiques non exécutés en double. |
| V03 | Connexion valide/invalide, limitation, expiration, rotation refresh, rejeu de refresh, logout et reset testés ; un token révoqué n'accède plus aux données. Aucun secret dans les logs ni stockage navigateur persistant. |
| V04 | Pour les quatre rôles, tester appels autorisés et refusés, liens directs, exports, fichiers et sync. Prestataire A ne lit ni ne modifie une mission de B ; changement de rôle effectif immédiatement. |
| V05 | Transition et auteur serveur retrouvables ; rôle falsifié dans un corps HTTP ignoré/refusé ; rollback sans journal de succès ; audit consultable uniquement dans le périmètre autorisé. |
| V06 | Poste nuit 22:00–06:00 correctement daté ; double affectation, absence chevauchante et habilitation expirée refusées ; remplacement et repos paramétrés contrôlés ; poste découvert signalé. |
| V07 | Consigne d'un poste entrant accusée par le bon utilisateur, date serveur conservée, alerte après seuil ; accusé d'une autre équipe refusé ; modification d'une consigne reçue impossible, addendum traçable. |
| V08 | CRUD/archivage équipement complet, code/QR unique, cycle de localisation refusé, fichier privé non accessible anonymement ; historique conservé après archivage. |
| V09 | Étape obligatoire absente ou de mauvais type bloque soumission/clôture via UI et API. Mesure hors bornes conservée et anomalie exigée ; modèle publié immuable, ancien OT inchangé après nouvelle version. |
| V10 | Tester les six fréquences, intervalle > 1, fin de mois et année bissextile. Glissant depuis validation ; fixe depuis ancre avec occurrences manquées visibles. Deux schedulers et double validation simultanés produisent exactement un prochain OT. Annulation/désactivation traitées explicitement. |
| V11 | Parcours nouvelle → affectée → en cours → attente → reprise → terminée → validée ; transitions invalides refusées ; auteur de travail ne valide pas son OT ; refus motivé, temps et attentes distincts, reprise liée après validation. |
| V12 | Deux sorties concurrentes ne créent ni stock négatif ni double consommation. Réservation, consommation, retour et ajustement rapprochés du journal ; prix historique inchangé après modification du catalogue. |
| V13 | Commande de 10 pièces reçue en 4 puis 6 ; seules quantités acceptées disponibles, refusées en quarantaine ; rejeu d'inspection sans double crédit. OT attendant deux articles reste bloqué si un seul arrive ; notification de replanification quand tous besoins couverts. |
| V14 | Compte externe limité à entreprise/mission et dates de contrat ; upload/rapport autorisés, validation interne exigée ; matériel demandé sans approbation externe ; expiration retire l'accès. |
| V15 | PWA installable en HTTPS sur appareils cibles ; QR réel ouvre seulement une fiche autorisée ; permission caméra refusée → saisie manuelle du code possible ; écran 360 px utilisable. |
| V16 | En mode avion, saisir checklist/temps/photo, fermer puis rouvrir la PWA : données conservées. Rejouer 10 fois une opération → un effet. Même clé/contenu différent refusé ; modification concurrente → conflit visible avec brouillon intact. Tester quota plein, crash, session expirée, droits révoqués et curseur expiré. Aucun écrasement silencieux. |
| V17 | Événement persisté puis notification unique après reprise du dispatcher ; coupure SSE récupérée via historique ; destinataire différent inaccessible ; échec prolongé visible dans monitoring. |
| V18 | KPI comparés à un jeu de référence incluant attente, pannes superposées et échéances manquées. Filtres identiques écran/PDF/XLSX ; coûts masqués selon rôle, texte commençant par `=`, `+`, `-`, `@` exporté sans exécution de formule injectée. |
| V19 | Parcours login, affectation et checklist au clavier et tactile, labels lisibles, erreurs de formulaire accessibles ; français par défaut, clair/sombre ; contrôle des contrastes et tests sur navigateurs retenus. |
| V20 | Compose validé et services prêts sans secrets codés ; TLS, volumes et logs configurés. Restaurer PostgreSQL ET MinIO depuis sauvegardes hors hôte, mesurer RPO/RTO ; scénario de panne exécuté avec procédure d'exploitation. |
| V21 | Charge représentative dans les seuils convenus ; aucune anomalie sécurité critique/haute non traitée ; parcours E2E sur les trois postes ; clinique forme référents, valide procédure dégradée et signe la recette avant production. |

## Stratégie de tests et définition de terminé

Backend : JUnit 5/Mockito pour calculs, permissions et règles isolées ; Spring Boot Test avec une base PostgreSQL dédiée (service Docker en CI, connexion configurée par `GMAO_TEST_URL`, `GMAO_TEST_USER` et `GMAO_TEST_PASSWORD`) pour repositories, migrations, verrous, idempotence et contraintes. Ne pas utiliser H2 comme preuve de comportement PostgreSQL. Spring Security Test pour les refus de rôle et de périmètre.

Frontend : Vitest/React Testing Library pour formulaires, routes, erreurs API et état de synchronisation ; mocks limités aux tests, pas aux fonctions présentées comme livrées. Tests E2E navigateur avec API et PostgreSQL réels en préproduction pour les cycles critiques. Tests PWA sur appareils réels, notamment persistance de photos et réseau intermittent.

Chaque lot terminé possède : entrées validées, permissions côté serveur, erreurs uniformes, transactions et historique, interface connectée, tests pertinents passants, OpenAPI et documentation mises à jour, absence de données simulées cachées, revue de code et démonstration métier. Les correctifs de sécurité et la restauration restent des conditions de mise en service.

## Décisions à recueillir à la validation

1. Horaires, effectifs, cycle de repos et modalités de renfort des équipes A/B/C.
2. Validateur indépendant et délégations achats/qualité ; seuils de dépense et SLA des priorités.
3. Mode glissant depuis validation, politique d'échéances fixes manquées et contrôles réglementaires éventuels.
4. Hébergement, objectif 99,9 %, RPO/RTO proposés, appareils gérés et durée hors ligne.
5. Parc, volumes, données à reprendre, devise, conservation et responsables métier.
6. Stack exacte supportée au démarrage, namespace Java conservé, ressources et calendrier relatifs.

Le développement et la création des migrations attendent la validation de ce dossier, conformément à la première mission du document fourni.
