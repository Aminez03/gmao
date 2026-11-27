# Réalisation des phases 2 à 12

Développement autorisé par la demande « faire tout les phases », après remise du dossier de conception. Les anciens exemples Java, les scripts SQL incompatibles et le générateur initial ont été supprimés. Les sources applicatives et les migrations Flyway constituent la référence maintenue.

## Couverture livrée

| Phase | Réalisation |
|---|---|
| 1 — Conception | Cahier des charges, UML, architecture, données, API cible, backlog et recette : [dossier initial](BRIEF.md). |
| 2 — Données | Entités JPA par domaine, migration Flyway PostgreSQL, FK, unicités, versions et journaux protégés contre modification. |
| 3 — Authentification | JWT court, refresh rotatif, session révocable, quatre rôles, comptes et profils, changement/reset du mot de passe, contrôles par objet et origine. |
| 4 — Équipes | Équipes/membres/habilitations, planning en rotation, postes réels datés, affectations sans chevauchement, publication, absences, retrait/remplacement et consignes avec accusé nominatif. |
| 5 — Patrimoine | Arbre bâtiment/étage/zone/local, équipements et sous-équipements, garanties, criticités, état, QR et fichiers privés. |
| 6 — Préventif et checklists | Six fréquences, intervalle, règles fixe/glissante, occurrences manquées, unicité d'OT actif, scheduler, gammes publiées immuables et copie des étapes dans l'OT, réponses typées et anomalies bloquantes. |
| 7 — Correctif | Déclaration, qualification, affectation, contrôle de sécurité, démarrage/attente/reprise, temps réel, diagnostic, cause, solution, rapport et validation distincte. |
| 8 — Achats et stock | Besoin/réservation, approbation distincte, transmission, commandes fractionnées, réceptions partielles, contrôle qualité, crédit accepté, allocation et consommation transactionnelle, ajustements motivés et coûts historiques des mouvements. |
| 9 — Prestataires | Entreprises, contrats datés, comptes liés, missions et rapports, demandes de matériel, validation interne et filtrage par entreprise/mission. |
| 10 — PWA et mobilité | Application installable, scan caméra et code manuel, stockage IndexedDB par compte, brouillons, photos, file d'opérations, reprise idempotente et comparaison des conflits. |
| 11 — Pilotage | Dashboard, graphiques Recharts, filtres de période/périmètre, disponibilité issue des indisponibilités, MTTR/MTBF, préventif à l'échéance, coûts des pièces, PDF/XLSX et notifications persistées. |
| 12 — Qualité et déploiement | Tests backend PostgreSQL et frontend, CI, Docker Compose démarré, Nginx, variables/secrets, healthchecks, volumes, sauvegarde/restauration PostgreSQL et monitoring provisionné et vérifié. Configuration HTTPS et conditions de production documentées. |

## Preuves déjà obtenues

- Compilation TypeScript/Vite réussie, manifeste et service worker générés ; modules de graphiques et scan séparés pour limiter le chargement initial.
- 8 tests Vitest réussis : validation et erreurs de formulaire, isolation des caches, ordre des opérations, accusés serveur et conservation/résolution des conflits.
- 23 tests Java réussis sur chacune des versions PostgreSQL 16 (conteneur cible) et PostgreSQL 18 (instance locale isolée) : 7 calculs de périodicité et 16 scénarios d'intégration. Aucun test ignoré dans ces exécutions. Les migrations sont appliquées réellement ; les tests couvrent permissions, changement de rôle lors d'un rejeu, limitation des connexions, pagination et affectations, version, idempotence concurrente, stock, immutabilité des gammes, correctif et réception partielle.
- Audit npm des dépendances de production installées : aucune vulnérabilité signalée lors du contrôle. Ce résultat ne constitue pas un audit du code ou des images Docker.
- Configuration Compose analysée sans erreur et services démarrés. L'instance principale utilise le port 3080, avec ses propres volumes. La recette utilise exclusivement `gmao-verification`, port 13000.
- 24 vérifications HTTP réussies : rôles, rotation A/B/C, transmission et accusé nominatif, QR, préventif et validation distincte, prochain OT unique, aller-retour d'un PDF réel dans MinIO privé, masquage des coûts, réceptions partielles et contrôle qualité, reprise après disponibilité complète, consommation de stock, prestation externe, indicateurs, exports PDF/XLSX, audit et révocation.
- Connexion, dashboard et interventions vérifiés dans Edge isolé, aux formats bureau et mobile ; absence d'exception JavaScript et de débordement horizontal de page. L'état vide est lisible sur un écran de 390 pixels. Les filtres du dashboard proposent les équipes et une recherche serveur d'équipement par nom/code.
- Sauvegarde PostgreSQL au format custom et restauration exécutées dans une base temporaire isolée : mêmes nombres d'OT, mouvements, audits et pièces jointes. Exercice de 17,69 secondes sur les seules données de recette ; ce temps ne mesure pas le RTO du parc réel et n'inclut pas la restauration des objets.
- Configuration Prometheus et quatre règles validées par `promtool`. Grafana sain, tableau à huit panneaux chargé automatiquement et métrique `up{job="gmao"}=1` vérifiée via la datasource.
- Instance principale : page HTTP, connexion avec l'origine 3080, profil et dashboard vérifiés. La commande de sauvegarde du profil Compose a réellement créé un fichier custom dans `backups/` ; les données opérationnelles de la recette restent dans les volumes de vérification.

Les résultats détaillés locaux sont dans `.cache/smoke-result.json`, `.cache/browser-result.json`, `.cache/backup-result.json`, `.cache/monitoring-result.json`, `.cache/main-result.json` et les rapports Maven. Ces fichiers de travail ne sont pas versionnés. Les scripts de vérification sont conservés dans `tools/`.

## Choix d'implémentation et limites explicites

La version prend en charge une clinique et un rôle principal par compte. Les permissions des quatre rôles sont définies côté serveur ; il ne s'agit pas d'un éditeur de rôles personnalisés. Le modèle physique compact regroupe certains concepts de la conception : gammes publiées et snapshots au lieu de trois tables de versions/exécutions ; une référence par demande d'achat ; documents servis par le backend depuis MinIO.

La fenêtre d'anticipation préventive est de sept jours. Le planning généré reste à affecter et publier ; les obligations RH et renforts réels ne sont pas supposés satisfaits par l'algorithme. La validation d'une intervention ne remplace pas les procédures de sécurité techniques.

Le dashboard distingue les coûts de pièces effectivement enregistrés des postes non valorisés. La définition des KPI, la qualité des saisies et le test de charge sur le parc réel doivent être validés avec la clinique.

Les notifications sont persistées dans la même transaction métier et interrogées périodiquement. Les événements planifiés sont dédupliqués ; SSE et Redis ne sont pas requis par cette version. SMTP reste à configurer pour les réinitialisations par courrier électronique.

Le service MinIO est construit depuis la [release officielle de sécurité du 15 octobre 2025](https://github.com/minio/minio/releases/tag/RELEASE.2025-10-15T17-29-55Z), car le téléchargement de l'image Quay testée a échoué avec HTTP 401. Sa licence est copiée dans l'image. Le dépôt officiel étant archivé, son maintien en production demande une décision d'exploitation et de maintenance.

## Validation de mise en service

Le démarrage local ne vaut pas recette d'une exploitation hospitalière 24/7. Le [guide d'exploitation](EXPLOITATION.md) précise les travaux dépendant du site : domaine/certificats, SMTP, sauvegarde des objets, archivage WAL et copies hors hôte, restauration chronométrée, appareils gérés, revue sécurité et charge représentative. Ces conditions ne sont pas présentées comme déjà réalisées.
