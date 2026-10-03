# Plateforme d'apprentissage de langues : backend

Un visiteur crée un compte, répond à des questions à choix multiples (texte ou image) dans un temps limité,
et gagne ou perd des points selon la difficulté. L'admin gère les questions, les règles du jeu et les sons.
Première langue : l'anglais.

## Architecture

```
navigateur ──▶ gateway :8080 ──┬──▶ learning :8081 ──▶ postgres (base learning)
                 │             └──▶ mediaService :8082 ──▶ postgres (base mediaservice) + MinIO
                 └──▶ postgres (base gateway)
```

| Dossier | Rôle |
|---|---|
| `gateway/` | Point d'entrée unique. Comptes, connexion, émission du JWT, routage, CORS. |
| `learning/` | Langues, catégories, questions et choix, tentatives chronométrées, points, réglages du jeu. |
| `mediaService/` | Upload des images de choix et des sons, stockés dans MinIO. |
| `frontend/` | Interface de test : une page statique servie par nginx, qui relaie `/api` vers le gateway. |
| `infrastructure/` | `docker-compose.yml` pour le serveur. |

Chaque service a sa propre base et revalide lui-même le JWT émis par le gateway (HS512, secret partagé
`JWT_SECRET`, rôles dans le claim `auth`). Java 17, Spring Boot 3.4.5, Liquibase, PostgreSQL 16.

## Règles du jeu

- Une question a au moins 2 choix et exactement 1 choix correct. Un choix est un texte ou une image.
- Chaque niveau (`EASY`, `MEDIUM`, `HARD`) a ses points, sa pénalité et son temps limite, réglables par l'admin.
  Valeurs de départ : +10 / −5 / 30 s, +20 / −10 / 20 s, +30 / −15 / 15 s.
- Le chrono est tenu par le serveur. Il démarre quand la question est remise à l'apprenant ; redemander
  une question pendant qu'une tentative est en cours renvoie la même question, sans relancer le chrono.
- Bonne réponse dans les temps : les points du niveau, uniquement la première fois que la question est réussie.
- Mauvaise réponse ou temps écoulé : la pénalité du niveau est retirée. Le total ne descend jamais sous 0.
  Une question laissée sans réponse est soldée (pénalité) à l'action suivante de l'apprenant.
- Une question ratée ou expirée peut être reproposée plus tard, avec un nouveau chrono.
- Le résultat d'une réponse contient `soundUrl` : le son de réussite ou d'échec à jouer, s'il est configuré.

## API (tout passe par le gateway)

Public :

| Méthode | Chemin | |
|---|---|---|
| POST | `/api/register` | Créer un compte apprenant |
| POST | `/api/authenticate` | Se connecter (login ou email) → `{ "id_token": "..." }` |
| GET | `/api/languages`, `/api/categories?languageCode=en` | Catalogue |

Apprenant (en-tête `Authorization: Bearer <token>`) :

| Méthode | Chemin | |
|---|---|---|
| GET | `/api/account` | Mon compte |
| GET | `/api/settings/game` | Points, pénalité, temps par niveau, URL des sons |
| POST | `/api/questions/next?categoryId=&difficulty=` | Obtenir une question et démarrer le chrono (204 s'il n'y en a plus) |
| POST | `/api/attempts/{attemptId}/answer` | Répondre : `{ "choiceId": 12 }` → `CORRECT`, `WRONG` ou `TIME_EXPIRED` |
| GET | `/api/scores/me`, `/api/scores/leaderboard?size=10` | Points |

Admin (`ROLE_ADMIN`) :

| Méthode | Chemin | |
|---|---|---|
| GET | `/api/admin/users` | Comptes |
| POST, PUT, DELETE | `/api/admin/languages`, `/api/admin/categories` | Catalogue |
| GET, POST, PUT, DELETE | `/api/admin/questions` | Questions avec leurs choix |
| GET, PUT | `/api/admin/settings/difficulties/{EASY\|MEDIUM\|HARD}` | `{ points, penaltyPoints, timeLimitSeconds }` (5 à 600 s) |
| GET, PUT, DELETE | `/api/admin/settings/sounds/{SUCCESS\|FAILURE}` | `{ audioUrl }` |
| POST | `/api/media/images/upload` | Champ multipart `files` (10 images max, 10 Mo chacune) |
| POST | `/api/media/audio/upload` | Champ multipart `file` (MP3, WAV ou OGG, 5 Mo max) |
| GET, DELETE | `/api/media`, `/api/media/{id}` | Fichiers stockés |

Ajouter un choix image : uploader l'image, puis créer la question avec un choix
`{ "type": "IMAGE", "imageUrl": "<url renvoyée>", "correct": true }`.
Configurer un son : uploader le fichier audio, puis `PUT /api/admin/settings/sounds/SUCCESS` avec son URL.

## Interface de test

`frontend/` est une page unique (HTML, CSS et JavaScript sans dépendance) qui couvre tout le parcours :
inscription et connexion, jeu chronométré avec les sons, classement, et côté admin la création de questions
avec choix texte ou image, les réglages par niveau, les sons et les catégories.

Sur le serveur elle est servie sur `FRONTEND_PORT`. En local, sans nginx :

```bash
cd frontend && python -m http.server 4200
# puis ouvrir http://localhost:4200/?api=http://localhost:8080
```

## Développement local

Prérequis : JDK 17, Maven, PostgreSQL sur `localhost:5432`, et un MinIO sur `localhost:9000` pour les uploads.

```sql
CREATE DATABASE lingua_gateway;
CREATE DATABASE lingua_learning;
CREATE DATABASE lingua_media;
```

Le profil `dev` se connecte avec `postgres` / `root` ; pour d'autres identifiants, définir `DB_USER` et `DB_PASSWORD`.
Dans trois terminaux :

```bash
cd learning      && mvn spring-boot:run
cd mediaService  && mvn spring-boot:run
cd gateway       && mvn spring-boot:run
```

- Swagger des trois services : http://localhost:8080/swagger-ui.html
- Compte admin créé au démarrage : `admin` / `admin`
- Catégories et questions d'exemple chargées automatiquement
- Tests : `mvn test` dans chaque service

## Déploiement sur le VPS

Trois ports sont publiés : l'interface de test (`FRONTEND_PORT`, 8091 par défaut), le gateway (`GATEWAY_PORT`, 8090)
et l'API MinIO (`MINIO_PORT`, 9000).
Les ports 80 et 443 ne sont pas utilisés : le projet cohabite avec un autre déjà en service sur le même serveur.
Les images sont construites sur le serveur, il n'y a besoin que de Docker.

```bash
cd infrastructure
cp .env.example .env
nano .env            # renseigner toutes les valeurs vides
chmod 600 .env
docker compose up -d --build
docker compose ps
docker compose logs -f gateway
```

À renseigner dans `.env` : `DB_PASSWORD`, `JWT_SECRET` (`openssl rand -base64 64 | tr -d '\n'`), `ADMIN_PASSWORD`,
`MINIO_ROOT_PASSWORD`, `MINIO_PUBLIC_URL` (par exemple `http://IP_DU_VPS:9000`) et
`CORS_ALLOWED_ORIGINS` (l'adresse de l'interface de test, par exemple `http://IP_DU_VPS:8091`). Le démarrage est refusé tant qu'une valeur obligatoire manque.

Points d'attention :

- `minio/minio` n'est plus publié sur Docker Hub. Le compose utilise `ghcr.io/bassayiene/minio-ci:latest`, copie
  publique de la dernière image officielle (`RELEASE.2025-09-07T16-13-09Z`, `linux/amd64` uniquement). Cette version
  ne recevra plus de correctifs : ne pas publier sa console d'administration (port 9001).
- Le mot de passe admin n'est lu qu'à la création du compte. Le changer ensuite dans `.env` ne modifie rien.
- Changer `JWT_SECRET` déconnecte tout le monde, sans autre conséquence.
- Ne jamais lancer `docker compose down -v` : cela supprime les bases et les fichiers uploadés.
- L'API est servie en HTTP sur `GATEWAY_PORT`. Pour du HTTPS, placer le gateway derrière le reverse proxy du serveur.
- Compter environ 1 Go de RAM pour les trois services Java.
