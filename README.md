# GMAO Polyclinique

Application React / TypeScript et Spring Boot / Java, avec PostgreSQL et migrations Flyway. Les fichiers joints sont stockes dans PostgreSQL. Interface francaise, PWA et saisies hors ligne.

La [conception initiale](docs/BRIEF.md) couvre les douze phases. Le passage au développement a été autorisé par la demande « faire tout les phases ». Le [suivi de réalisation](docs/REALISATION.md) distingue les fonctionnalités livrées, les preuves de test et les validations d'exploitation.

## Démarrage avec Docker

Pré-requis : Docker Desktop démarré avec les conteneurs Linux et Docker Compose v2.

Depuis ce dossier, sous PowerShell :

```powershell
powershell -ExecutionPolicy Bypass -File tools/configure.ps1
# Ouvrir .env, renseigner l'adresse du premier administrateur.
docker compose up --build -d
docker compose ps
```

`configure.ps1` génère des secrets aléatoires et refuse d'écraser un `.env` existant. Sous Linux/macOS, copier `.env.example` en `.env` et remplacer tous les `CHANGE_ME` par des secrets aléatoires avant démarrage.

Ouvrir l'adresse `PUBLIC_ORIGIN` du fichier `.env` (par défaut **http://localhost:3000**). Dans cet espace de travail, l'instance utilise **http://localhost:3080**, car le port 3000 appartient déjà à un autre projet. Pour changer de port, modifier ensemble `APP_PORT` et `PUBLIC_ORIGIN`.

Utiliser `BOOTSTRAP_EMAIL` et `BOOTSTRAP_PASSWORD` du fichier `.env`. Le compte est créé seulement si la table utilisateurs est vide. Changer son mot de passe dans « Mon profil » ; les sessions sont révoquées après changement. Ne jamais committer `.env` ni partager son contenu.

Les ports PostgreSQL et backend restent internes au reseau Compose. Le port HTTP reste local. Les fichiers joints et donnees persistent dans le volume PostgreSQL ; ne jamais utiliser `docker compose down -v` pour conserver les donnees.

## Premier paramétrage métier

1. Créer les utilisateurs et le second validateur technique ; attribuer les quatre rôles selon les responsabilités. Un seul rôle principal par compte dans cette version.
2. Créer les équipes A/B/C, leurs membres, compétences et habilitations. Générer les postes depuis le planning, affecter 1 à 2 techniciens, contrôler absences et repos, puis publier.
3. Créer bâtiment → étage → zone → local, puis les équipements. Définir criticités, état, codes et photos/documents. Télécharger et imprimer les QR codes.
4. Créer une gamme avec ses étapes et la publier. Une gamme publiée est immuable : créer une nouvelle gamme pour une évolution ; les OT conservent leur copie des étapes.
5. Créer les plans préventifs et leurs règles. Le scheduler crée l'OT à l'approche de l'échéance (fenêtre de sept jours). Les échéances suivantes sont calculées après validation.
6. Pour le correctif, déclarer la panne, qualifier la déclaration en intervention, affecter l'exécutant, démarrer après contrôles de sécurité, saisir les réponses et le temps, puis soumettre. Un autre responsable valide.
7. Référencer pièces/fournisseurs et inventaire initial via l'action d'ajustement motivée. Les demandes d'achat passent par soumission, approbation distincte, transmission, commande et réception. Le contrôle qualité crédite uniquement les quantités acceptées.
8. Pour un prestataire, créer entreprise et contrat, son compte lié et une mission sur l'OT. Affecter aussi l'OT à cette entreprise. Le prestataire ne voit que ses missions autorisées dans la période de contrat.

Une demande d'achat représente une référence article ; plusieurs demandes peuvent couvrir un besoin de plusieurs références. Plusieurs commandes et réceptions peuvent satisfaire partiellement une demande. Les documents acceptés sont PDF, JPEG, PNG et WebP ; la vérification de format ne constitue pas une analyse antivirus.

## Données de démonstration

Le jeu fictif demandé a été ajouté à l'instance locale. Les noms portent le préfixe `[DEMO]`. Il comprend les équipes A/B/C, neuf postes publiés et leurs affectations, les habilitations, une absence, une consigne reçue, quatre localisations, la pompe `POMPE-001`, une gamme publiée, un plan mensuel, un préventif validé et un correctif avec achats/réceptions/consommation, une anomalie résolue, une indisponibilité, une prestation externe acceptée et des documents privés.

Les comptes de démonstration utilisent le mot de passe `TestClinique2026!` :

| Rôle | E-mail |
|---|---|
| Responsable technique | responsable@clinique.example |
| Techniciens | ali@clinique.example, sami@clinique.example, nour@clinique.example |
| Administrateur | achats@clinique.example |
| Administrateur | direction@clinique.example |
| Prestataire | externe@clinique.example |

Pour essayer le parcours : se connecter comme Ali, ouvrir « Interventions » puis `[DEMO] Intervention a realiser - surveillance pompe`, démarrer après les contrôles de sécurité, compléter la gamme, enregistrer le temps et soumettre. Deux filtres sont réservés sur cette intervention pour essayer la consommation. Se connecter ensuite comme responsable pour valider. Le compte externe permet de consulter la mission et l'intervention qui lui sont affectées. Les opérations clôturées du jeu fictif servent d'exemples consultables dans les achats, réceptions, mouvements et indicateurs.

Le script utilise l'API métier et conserve les données existantes : `python tools/seed-demo.py`. Il réutilise les identités de démonstration lors d'une relance. Son état de reprise et le récapitulatif sont dans `.cache/demo-seed.json` ; aucun jeton ni mot de passe administrateur n'y est enregistré.

## Travail mobile et hors ligne

Installer la PWA depuis un navigateur compatible, en HTTPS hors `localhost`. Ouvrir les interventions affectées et leurs fiches avant la perte du réseau : seules les pages téléchargées sont disponibles sur l'appareil. Le service worker conserve le shell, IndexedDB conserve les fiches consultées, brouillons, photos et opérations du compte courant.

Les réponses et temps peuvent être saisis sans connexion. Les photos de checklist sont conservées localement avant envoi. Les affectations, clôtures, approbations et mouvements de stock exigent le réseau. L'écran Synchronisation indique les opérations restantes ; un accusé serveur est nécessaire avant de les considérer comme synchronisées.

Au retour du réseau, synchronisation au premier plan et bouton manuel. Un conflit conserve la proposition locale et permet une comparaison avec le serveur avant réémission. Les opérations sont identifiées par UUID, empreinte et version attendue ; un rejeu n'a qu'un effet. Ne pas effacer les données du navigateur avec des saisies en attente. Un appareil géré/verrouillé et la procédure de secours restent nécessaires pour le terrain.

## Développement sans Docker

Pré-requis : Java 21, Maven 3.9+, Node 24, PostgreSQL 16 accessible. Utiliser une **base vide dédiée** : le schéma est créé exclusivement par les migrations Flyway dans `backend/src/main/resources/db/migration/`.

Configurer les variables listées dans `.env.example`, ainsi que :

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/gmao
SPRING_DATASOURCE_USERNAME=gmao
SPRING_DATASOURCE_PASSWORD=<secret>
```

Maven ne charge pas automatiquement `.env` : injecter ces variables via le terminal ou la configuration de lancement IDE, sans les copier dans un fichier versionné.

```powershell
cd backend
mvn spring-boot:run
```

Dans un second terminal :

```powershell
cd frontend
npm ci
npm run dev
```

Le proxy Vite écoute sur `http://localhost:3000` et relaie l'API vers le port 8080. `PUBLIC_ORIGIN` doit correspondre exactement à l'origine du navigateur. Pour ce démarrage HTTP local seulement, `SECURE_COOKIE=false`. En production HTTPS : `SECURE_COOKIE=true`.

## API et tests

L'API est sous `/api/v1`. Un corps CRUD est `{ "fields": { ... } }`. Les commandes ont un corps propre (exemple : `{"safetyConfirmed":true}`). Les créations et mutations requièrent `Idempotency-Key: <UUID>`. Les écritures existantes requièrent `If-Match: "<version>"`. Les réponses individuelles portent `ETag`, les créations `Location`.

Les routes réellement exposées sont décrites dans [API-IMPLEMENTEE.md](docs/API-IMPLEMENTEE.md). OpenAPI : `GET /api-docs` avec jeton, en accès backend de développement ; la passerelle de production bloque cette route. La bibliothèque Swagger est intégrée, mais son interface n'est pas exposée publiquement.

```powershell
cd backend
# Définir GMAO_TEST_URL sur une base de test dédiée : les tests y créent des données.
$env:GMAO_TEST_URL='jdbc:postgresql://localhost:5432/gmao_test'
$env:GMAO_TEST_USER='gmao_test'
$env:GMAO_TEST_PASSWORD='<secret-test>'
mvn verify
cd ../frontend
npm test
npm run build
```

Sans `GMAO_TEST_URL`, la suite PostgreSQL est explicitement ignorée ; ne pas confondre ce résultat avec une validation d'intégration. La CI fournie exécute ces tests avec un service PostgreSQL 16. Les rapports Maven se trouvent dans `backend/target/surefire-reports`.

## Exploitation

```powershell
docker compose --profile monitoring up -d
docker compose --profile backup run --rm backup
```

Grafana : `http://localhost:3001`, utilisateur `admin`, mot de passe `GRAFANA_PASSWORD`. Prometheus collecte les métriques Actuator sur le réseau interne ; la datasource et le tableau « GMAO - Exploitation » à huit panneaux sont provisionnés. Quatre règles Prometheus couvrent l'indisponibilité et les erreurs/saturations ; leur routage vers une astreinte reste à configurer. Une sauvegarde PostgreSQL au format custom est créée dans `backups/` par la commande backup.

La recette utilise exclusivement le projet `gmao-verification`, le port 13000 et des volumes distincts :

```powershell
docker compose -p gmao-verification -f docker-compose.yml -f infrastructure/compose.test.yml up --build -d
python tools/smoke.py
python tools/verify-backup.py
python tools/verify-monitoring.py
```

`smoke.py` crée des données de test sur le port 13000. `verify-backup.py` restaure une sauvegarde dans une base temporaire de ce projet puis supprime cette seule base temporaire. `verify-monitoring.py` lit le monitoring principal sur le port 3001. Le contrôle visuel `tools/browser-check.mjs` attend un navigateur Edge/Chromium isolé avec le port de débogage local 9223 ; il produit des captures dans `.cache/screenshots`.

Avant un déploiement réel, appliquer le [guide d'exploitation](docs/EXPLOITATION.md) : HTTPS, SMTP, sauvegardes des objets, copies hors hôte, restauration et validation des seuils. La configuration Compose mono-hôte et le dump manuel ne démontrent ni la disponibilité 99,9 %, ni un RPO de 15 minutes.

## Entretien du code

Depuis `frontend/` :

```powershell
npm run format        # Formater les sources React et Java
npm run format:check  # Vérifier le formatage sans modification
npm run typecheck     # Vérifier les types et les imports inutilisés
npm test
npm run build
```

Chaque ressource dispose de sa page dans `src/features/<domaine>/pages/`, avec ses routes dans `src/features/<domaine>/routes.ts`. `src/app/AppRoutes.tsx` assemble la navigation et charge les pages à la demande ; `src/app/Shell.tsx` porte la structure visuelle. Les tableaux et fiches partagés sont dans `src/components/resources/`. Les profils et rôles sont typés dans `src/types/auth.ts`. Voir l’[architecture du frontend](docs/ARCHITECTURE_FRONTEND.md).

L’entité intervention maintenue est `backend/src/main/java/tn/lamg/gmao/modules/maintenance/WorkOrder.java`. Les migrations Flyway sont la seule source du schéma ; les anciens exemples et le générateur initial ont été supprimés. Les tests backend s’exécutent sur une base dédiée, jamais sur la base de démonstration.
"# gmao" 
