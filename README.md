# Study Group (Spring Boot + React)

## Summary
- Web app for study groups: chat, tasks, file resources, members, realtime notifications.
- Frontend: React (Vite), WebSocket client, production build served by nginx.
- Backend: Spring Boot, JWT, Google OAuth2, WebSocket (no STOMP), PostgreSQL (Cloud SQL), files in Google Cloud Storage.
- Deploy: Cloud Build -> Cloud Run; Cloud SQL for DB.

## Logical architecture
- React/Vite SPA <-> REST API `/api/**` (Spring Boot).
- WebSocket `/ws/chat` for messages/events (`join`, `joinMultiple`, `message`, `typing`, `EVENT`).
- PostgreSQL as primary DB; files in a GCS bucket.

## Key features
- Groups: list, create, settings, member roles.
- Tasks: CRUD, status, deadlines, resource links.
- Resources: upload/download files, link to tasks.
- Chat: realtime messages, links to resources/tasks, online status.
- Auth: Google OAuth2 + email/password with JWT.

## Database (short form)
- USERS(user_id PK, name, email UNIQUE, password_hash, role, created_at)
- GROUPS(group_id PK, name, description, created_by FK USERS, created_at)
- MEMBERSHIPS(membership_id PK, user_id FK, group_id FK, role, joined_at) UNIQUE(user_id, group_id)
- TASKS(task_id PK, group_id FK, created_by FK, title, description, status, deadline, priority, assigned_to FK, created_at, updated_at)
- RESOURCES(resource_id PK, group_id FK, uploaded_by FK, title, type, path_or_url, original_name, file_size, description, uploaded_at)
- TASK_RESOURCES(task_id FK, resource_id FK, PK(task_id, resource_id))
- MESSAGES(message_id PK, group_id FK, user_id FK, content, timestamp, resource_id?)
- ACTIVITY_LOG(log_id PK, user_id FK, action, timestamp, details)

## REST API (main)
- Auth: `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/change-password`, `POST /api/auth/change-email`
- OAuth2: `GET /oauth2/authorization/google` (redirect to Google), callback `/login/oauth2/code/google`; success handler issues JWT and redirects to frontend.
- Groups: `GET /api/groups`, `POST /api/groups`, `GET/PUT/DELETE /api/groups/{id}`
- Members: `GET /api/groups/{groupId}/members`, `POST /api/groups/{groupId}/members` (userId or email), `PATCH /api/groups/{groupId}/members/{membershipId}/role`, `DELETE /api/groups/{groupId}/members/{membershipId}`
- Tasks: `GET /api/tasks?groupId=`, `GET /api/tasks/{id}`, `POST /api/tasks`, `PUT /api/tasks/{id}`, `POST /api/tasks/{id}/status`, `DELETE /api/tasks/{id}`
- Task?Resource: `GET /api/tasks/{taskId}/resources`, `POST /api/tasks/{taskId}/resources`, `DELETE /api/tasks/{taskId}/resources/{resourceId}`
- Resources: `GET /api/resources?groupId=`, `POST /api/resources`, `GET /api/resources/{id}/download`, `DELETE /api/resources/{id}`

## WebSocket
- Endpoint: `/ws/chat`
- Client -> server: `join`, `joinMultiple {userId,userName,groupIds[]}`, `message {groupId,text,resourceId?,taskId?,timestamp}`, `typing`.
- Server -> client: `message`, `typing`, `online`, `EVENT` (TASK_*, RESOURCE_* ...).

## Backend environment variables
- `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`
- `app.frontend.url` (frontend URL for OAuth redirects)
- `DB_URL` (local: `jdbc:postgresql://localhost:5432/appdb`; Cloud SQL via socketFactory: `jdbc:postgresql://google/appdb?socketFactory=com.google.cloud.sql.postgres.SocketFactory&cloudSqlInstance=tsiktapp:europe-central2:studyapp-db`)
- `DB_USER`, `DB_PASSWORD`
- `STORAGE_BUCKET` (GCS bucket name for files)
- `SPRING_PROFILES_ACTIVE` (optional)

## Run locally
1) Prereqs: JDK 21, Maven, Node.js 20+, PostgreSQL (create DB and user).
2) Backend: `cd backend && mvn -DskipTests package && java -jar target/study-group-backend-1.0.0.jar`
   - Set env `DB_URL/DB_USER/DB_PASSWORD`, OAuth vars before start.
3) Frontend: `cd frontend && npm install && npm run dev -- --host`
   - Add `.env.local` with `VITE_API_URL=http://localhost:8080` and `VITE_WS_URL=ws://localhost:8080`.

## Docker
- `cd backend && mvn -DskipTests package`
- `docker-compose up --build` from `APP/` (backend + frontend nginx). Adjust env for Postgres/Cloud SQL if needed.

## Deploy to GCP (Cloud Run)
- Frontend build/push: `gcloud builds submit frontend --config frontend/cloudbuild.yaml --substitutions=_IMAGE=europe-central2-docker.pkg.dev/tsiktapp/app/frontend:latest,_VITE_API_URL=https://backend-821395306768.europe-central2.run.app,_VITE_WS_URL=wss://backend-821395306768.europe-central2.run.app`
- Frontend deploy: `gcloud run deploy frontend --image europe-central2-docker.pkg.dev/tsiktapp/app/frontend:latest --port 80 --allow-unauthenticated`
- Backend deploy (example):
  ```
  gcloud run deploy backend     --image europe-central2-docker.pkg.dev/tsiktapp/app/backend:latest     --port 8080 --allow-unauthenticated     --add-cloudsql-instances tsiktapp:europe-central2:studyapp-db     --set-env-vars "GOOGLE_CLIENT_ID=...,GOOGLE_CLIENT_SECRET=...,app.frontend.url=https://frontend-821395306768.europe-central2.run.app,DB_URL=jdbc:postgresql://google/appdb?socketFactory=com.google.cloud.sql.postgres.SocketFactory&cloudSqlInstance=tsiktapp:europe-central2:studyapp-db,DB_USER=appuser,DB_PASSWORD=...,STORAGE_BUCKET=tsiktapp-resources"
  ```
- OAuth redirect URIs (Google Console): `https://backend-821395306768.europe-central2.run.app/login/oauth2/code/google` and `https://backend-mpiswxpygq-lm.a.run.app/login/oauth2/code/google`; JS origins = frontend URLs.

## UI notes
- Dark neomorphic style; top tabs Dashboard/Tasks/Chat/Resources; left menu: Groups + Game (Snake).
- Snake: keyboard and swipe controls, on-screen buttons for touch.
