# Leave Management System — How to access the frontend locally

This project includes two ways to run and access the frontend locally:

- Option A (simple): Use the Spring Boot app that serves the static frontend from src/main/resources/static. Best for local development without Docker.
- Option B (Docker): Run the full stack (PostgreSQL + backend services + Nginx frontend) with docker-compose.

If you just want the Docker answer, see the quickstart below.

## Quickstart — Run with Docker (recommended)

Prerequisites: Docker Desktop (with Docker Compose v2)

1) Create an .env file (only once):
   - PowerShell: Copy-Item .env.example .env

2) Start everything (Postgres + backends + Nginx frontend):
   - docker compose up -d --build

3) Open the app:
   - Frontend (via Nginx): http://localhost:${env:FRONTEND_PORT -as [int] -ne $null ? $env:FRONTEND_PORT : 8088}
   - Leave service Swagger: http://localhost:${env:LEAVE_SERVICE_HOST_PORT -as [int] -ne $null ? $env:LEAVE_SERVICE_HOST_PORT : 8080}/swagger-ui/index.html

4) See what’s running / logs:
   - docker compose ps
   - docker compose logs -f frontend leave-service auth-service postgres

5) Stop or clean up:
   - Stop (keep containers/images/volumes): docker compose stop
   - Down (remove containers, keep images/volumes): docker compose down
   - Full clean (remove containers, networks, images not used): docker system prune -f

Notes:
- If port 8088 shows “site can’t be reached”, ensure the frontend container is up (docker compose ps). If you only run the Spring Boot app (Option A), use http://localhost:8080 instead.
- You can change ports by editing .env (FRONTEND_PORT, LEAVE_SERVICE_HOST_PORT, AUTH_SERVICE_HOST_PORT) or rely on defaults in docker-compose.yml.

## Run only the leave-service container (without Compose)
If you prefer to run just the Spring Boot backend container while using a Postgres running on your host (or via Compose), use host.docker.internal to reach the host DB on Windows/macOS.

1) Ensure Postgres is available on your host at 5433 (e.g., docker compose up -d postgres).
2) Build the leave-service image:
   - docker build -t leave-service:local ./leavemanagementsystem
3) Run the container, pointing it to the host DB and providing JWT settings:
   - PowerShell command (one line):
     docker run --name leave-service --rm -p 8080:8080 \
       -e DB_URL=jdbc:postgresql://host.docker.internal:5433/shecancode \
       -e DB_USER=postgres -e DB_PASSWORD=123456 \
       -e JWT_SECRET=ZmFrZVN1cGVyU2VjcmV0S2V5Rm9yRGVtby0xMjM0NTY3ODkw \
       -e JWT_EXP_MINUTES=240 \
       -e LEAVE_SERVICE_PORT=8080 \
       leave-service:local
4) Open Swagger: http://localhost:8080/swagger-ui/index.html

Pick one option below.

---

## Option A — Run Spring Boot locally (serves the frontend at http://localhost:8080)

Prerequisites:
- JDK 21+
- Maven 3.9+
- PostgreSQL reachable at localhost:5433 (the project is preconfigured for this) — easiest is to run the DB using the Docker Compose service shown in Option B, or change application.properties to point to your local DB.

Steps (Windows PowerShell):
1) (If you don’t already have a Postgres running) start Postgres via Docker only:
   - Create an .env file in the project root with at least:
     POSTGRES_DB=shecancode
     POSTGRES_USER=postgres
     POSTGRES_PASSWORD=123456
   - Then start only the database service:
     docker compose up -d postgres

   This exposes Postgres on localhost:5433, which matches leavemanagementsystem/src/main/resources/application.properties.

2) Start the Spring Boot app:
   - In a new terminal at the project root:
     cd leavemanagementsystem
     ./mvnw spring-boot:run
   - The app runs on port 8080 by default (overridable with LEAVE_SERVICE_PORT env var).

3) Open the frontend in your browser:
   http://localhost:8080

Notes:
- The static frontend lives in leavemanagementsystem/src/main/resources/static (index.html, app.js, styles.css).
- API base in app.js is set to the same origin (apiBase = ''), so requests go to the same server at http://localhost:8080.
- SecurityConfig already permits static files and public endpoints. You can hit Swagger UI at http://localhost:8080/swagger-ui/index.html.

Troubleshooting:
- If port 8080 is in use, set a different port:
  PowerShell example:
    $env:LEAVE_SERVICE_PORT=9090; ./mvnw spring-boot:run
  Then browse http://localhost:9090
- DB connection error: ensure Postgres is running on localhost:5433 with the credentials in application.properties or adjust those values. For Docker DB, use docker compose up -d postgres as above.

---

## Option B — Run everything with Docker Compose (serves the frontend via Nginx)

Prerequisites:
- Docker Desktop (Docker Compose v2)

1) Create a .env file in the project root (same folder as docker-compose.yml):
   - Quick way: copy .env.example to .env
     PowerShell: Copy-Item .env.example .env
   - Or create it manually with content like:

POSTGRES_DB=shecancode
POSTGRES_USER=postgres
POSTGRES_PASSWORD=123456

# Backend service ports
LEAVE_SERVICE_PORT=8080
LEAVE_SERVICE_HOST_PORT=8080
AUTH_SERVICE_PORT=8081
AUTH_SERVICE_HOST_PORT=8081

# Frontend port on your machine
FRONTEND_PORT=8088

# DB URL that services see inside Docker (matches docker-compose networks)
DB_URL=jdbc:postgresql://postgres:5432/${POSTGRES_DB}

# JWT settings
JWT_SECRET=ZmFrZVN1cGVyU2VjcmV0S2V5Rm9yRGVtby0xMjM0NTY3ODkw
JWT_EXP_MINUTES=240

2) Start the stack:
   docker compose up -d --build

   This will bring up:
   - Postgres (exposed on localhost:5433)
   - leave-service (Spring Boot backend)
   - auth-service (Node-based auth service)
   - frontend (Nginx serving the static app and proxying /api/* calls)

3) Access the frontend in your browser:
   http://localhost:8088
   Note: Port 8088 works only when the Docker 'frontend' container is running. If you only run the Spring Boot app locally, use http://localhost:8080 instead (see Option A).

   Nginx routes:
   - /api/auth/* -> auth-service:8081
   - /api/* -> leave-service:8080

4) Optional: Access the backend directly:
   - Leave service API/Swagger: http://localhost:8080/swagger-ui/index.html
   - Auth service (if it exposes docs): http://localhost:8081

Troubleshooting:
- Port conflicts: change the *_HOST_PORT or FRONTEND_PORT values in .env, or rely on defaults set in docker-compose.yml. For example, set FRONTEND_PORT=3000 to use http://localhost:3000 instead.
- Check services: docker compose ps
- View logs: docker compose logs -f frontend leave-service auth-service postgres
- Rebuild after changes: docker compose up -d --build

---

## Seeded demo users and passwords

On first run, the backend seeds three demo users (see leavemanagementsystem/src/main/java/com/shecancode/leavemanagementsystem/config/DataInitializer.java):

- Role: ADMIN — Email: admin@demo.com — Password: Admin123!
- Role: MANAGER — Email: manager@demo.com — Password: Manager123!
- Role: STAFF — Email: staff@demo.com — Password: Staff123!

How to log in:
- Using the built-in UI (Option A at http://localhost:8080 or Option B at http://localhost:8088):
  1) Open the app in your browser.
  2) In the Login tab, enter one of the emails above and its password.
  3) After login, a JWT is stored in localStorage and used for API calls automatically.

- Using Swagger UI (leave-service):
  1) Open Swagger UI: http://localhost:8080/swagger-ui/index.html (or your mapped port).
  2) Click the Authorize button (bearer-jwt). If you already have a token, paste it without the "Bearer " prefix.
  3) To obtain a token, call POST /api/auth/login with body:
     {"email":"staff@demo.com", "password":"Staff123!"}
  4) Copy the token field from the response and Authorize with it. Swagger will send Authorization: Bearer <token> for protected endpoints.

Notes:
- If you enabled 2FA on a user (not enabled by default in seed data), the login API requires twoFactorCode.
- The UI's login form is pre-filled with staff@demo.com for convenience.

## FAQ

- Q: I just want to open the HTML without running the backend. Will it work?
  A: You can open frontend/index.html or the Spring Boot static index.html, but API calls will fail without the backend. Use Option A or Option B to run the backend.

- Q: Where do I change the frontend’s API base URL?
  A: For the Spring Boot static app (leavemanagementsystem/src/main/resources/static/app.js), apiBase is ''. It should remain that way when served via Spring Boot or Nginx in Docker (Nginx proxies /api). If you host the frontend elsewhere, you could set apiBase to that backend’s base URL.

- Q: How do I change the Spring Boot server port?
  A: Set LEAVE_SERVICE_PORT env var when running, or edit leavemanagementsystem/src/main/resources/application.properties.

- Q: Default demo login?
  A: On first run, DataInitializer may seed demo users (e.g., staff@demo.com). Try the prefilled email and create/register if needed.

---

Happy hacking! If something doesn’t start, run docker compose ps and docker compose logs -f to diagnose, or check the Spring Boot console for stack traces.
