# GMAO de la polyclinique — dossier de conception

Version 1.0 — 20 novembre 2026 — **phase 1 soumise à validation**.

Mise à jour : la demande suivante « faire tout les phases » autorise la réalisation des phases 2 à 12. Voir [le suivi de réalisation](REALISATION.md) et [le guide de démarrage](../README.md). Le présent dossier reste la référence de conception initiale ; le catalogue des API réellement exposées est documenté séparément.

La mission consiste à concevoir une plateforme de maintenance pour une polyclinique ouverte 24 h/24, organisée en trois équipes. Le document fourni demande expressément de terminer la conception et d'attendre sa validation avant le développement. Aucun livrable de ce dossier ne constitue une fonctionnalité opérationnelle.

## Livrables

| Livrable demandé | Document |
|---|---|
| 1. Cahier des charges fonctionnel | [01 — Besoins et règles](phase-1/01-cahier-des-charges.md) |
| 2. Diagrammes UML | [02 — UML](phase-1/02-uml.md) |
| 3. Architecture React / Spring Boot | [03 — Architecture](phase-1/03-architecture.md) |
| 4. Modèle conceptuel de données | [04 — Données, section MCD](phase-1/04-donnees.md#mcd) |
| 5. Schéma relationnel PostgreSQL | [04 — Données, section Schéma relationnel](phase-1/04-donnees.md#schéma-relationnel) |
| 6. Liste des entités JPA | [04 — Données, correspondance entités/tables](phase-1/04-donnees.md#schéma-relationnel) |
| 7. Catalogue REST | [05 — API](phase-1/05-api.md) |
| 8. Modules frontend et backend | [03 — Architecture et organisation](phase-1/03-architecture.md#organisation-cible) |
| 9. Backlog priorisé | [06 — Réalisation et recette](phase-1/06-realisation-et-recette.md#backlog-priorisé) |
| 10. Planning prévisionnel | [06 — Planning](phase-1/06-realisation-et-recette.md#planning-prévisionnel) |
| 11. Risques techniques | [06 — Risques](phase-1/06-realisation-et-recette.md#risques) |
| 12. Critères de validation | [06 — Recette](phase-1/06-realisation-et-recette.md#critères-de-validation) |

Les diagrammes sont fournis en Mermaid, lisibles comme texte et rendus par les lecteurs Markdown compatibles.

## État du dépôt examiné

Le répertoire de travail contient le projet `lamg-clinique-gmao/`. L'onglet `docs/BRIEF.md` signalé dans l'IDE n'existait pas sur disque au début de l'analyse ; ce fichier devient l'entrée du dossier de conception.

L'ébauche existante contient Java 21, un POM Spring Boot 3.3.5, une entité `WorkOrder`, un service, un contrôleur partiel et des scripts SQL. Les repositories et plusieurs classes importées par le service sont absents. `MaintenanceController.java` se termine au milieu d'un appel. `docker-compose.yml` se termine par une ligne de prose incompatible avec la configuration attendue. Le dossier frontend ne contient pas de sources recensées.

Le SQL contient déjà des idées utiles : historique, contrôle qualité, contraintes de stock et unicité d'un OT préventif ouvert. Cependant, les acteurs sont souvent des chaînes libres, les dates sont sans fuseau, les équipes ne sont pas réellement modélisées, le cycle quotidien manque et les rôles/statuts divergent du besoin. Les requêtes du contrôleur acceptent un rôle déclaré par le client : cela ne peut pas constituer une autorisation sécurisée.

Ces éléments sont conservés sans modification. Aucune compilation, migration, installation ou exécution Docker n'a été lancée durant cette phase documentaire. Une reprise contrôlée et des tests seront nécessaires après validation ; l'ébauche n'est pas réputée exécutable.

## Hypothèses à valider

- Une seule polyclinique au démarrage ; toutes les ressources restent rattachées à son établissement.
- Horaires de référence proposés : 06:00–14:00, 14:00–22:00, 22:00–06:00, fuseau `Africa/Tunis` configurable.
- Trois équipes identifiées A/B/C, mais renforts et remplacements indispensables pour couvrir congés, repos et absences. Trois équipes de taille minimale ne suffisent pas à garantir à elles seules une couverture permanente.
- Aucun dossier patient dans la GMAO ; les photos et commentaires doivent éviter les données de santé identifiantes.
- La gestion administrative du workflow achat est portée par `ADMIN`. Une séparation budgétaire supplémentaire reste à décider.
- Budget, parc réel, nombre d'utilisateurs, reprise des données, durées de conservation, délai d'astreinte et hébergement restent à confirmer.

## Décision attendue

Valider le périmètre, les règles de calcul préventif, la matrice des permissions, l'organisation des postes, les objectifs de continuité et les moyens du planning. Les propositions non confirmées restent identifiées comme telles. **La phase 2 et le développement attendent cette validation.**
