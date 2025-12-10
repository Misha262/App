# Study Group (Spring Boot + React)

## Stručný popis projektu a cieľov aplikácie
- Webová aplikácia pre študijné skupiny: chat, úlohy, zdroje (súbory), členovia.
- Cieľ: zjednotiť komunikáciu, správu úloh a zdrojov v jednej aplikácii s realtime upozorneniami.
- Používatelia: študenti/mentori v menších tímoch; rieši problém roztrieštených nástrojov (chat, úlohy, dokumenty).

## Architektúra systému
```
[ React / Vite frontend ]
   └── REST API (HTTP) -> Spring Boot (controller/service/repository)
        └── SQLite databáza (JDBC, pripravené SQL)
   └── WebSocket (ws://.../ws/chat) -> ChatWebSocketHandler (notifikácie, chat)
```
- Frontend: Vite + React, custom UI, WS klient na realtime udalosti.
- Backend: Spring Boot, vlastný JWT filter, WebSocket handler, SQLite (JDBC).
- Databáza: súbor SQLite (studyplatform.db) alebo volume v Dockeri.

## Databázový model (ER)
- USERS (user_id PK, name, email UNIQUE, password_hash, avatar_path, bio, role, created_at)
- GROUPS (group_id PK, name, description, created_by FK USERS, created_at, visibility)
- MEMBERSHIPS (membership_id PK, user_id FK, group_id FK, role, joined_at) – UNIQUE(user_id, group_id)
- TASKS (task_id PK, group_id FK, created_by FK, title, description, status, deadline, priority, assigned_to FK, created_at, updated_at)
- RESOURCES (resource_id PK, group_id FK, uploaded_by FK, title, type, path_or_url, original_name, file_size, description, uploaded_at)
- TASK_RESOURCES (task_id FK, resource_id FK, PK(task_id, resource_id))
- ACTIVITY_LOG (log_id PK, user_id FK, action, timestamp, details JSON/Text)
- MESSAGES (message_id PK, group_id FK, user_id FK, content, timestamp)

## REST API (hlavné endpointy)
- Auth: `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/change-password`, `POST /api/auth/change-email`
- OAuth2: `GET /oauth2/authorization/google` (redirect), `GET /oauth2/success` (uloží token do localStorage cez HTML)
- Groups: `GET /api/groups`, `GET /api/groups/{id}`, `POST /api/groups`, `PUT /api/groups/{id}`, `DELETE /api/groups/{id}`
- Members: `GET /api/groups/{groupId}/members`, `POST /api/groups/{groupId}/members`, `PATCH /api/groups/{groupId}/members/{membershipId}/role`, `DELETE /api/groups/{groupId}/members/{membershipId}`
- Tasks: `GET /api/tasks?groupId=`, `GET /api/tasks/{id}`, `POST /api/tasks`, `PUT /api/tasks/{id}`, `POST /api/tasks/{id}/status`, `DELETE /api/tasks/{id}`
- Task–Resource: `GET /api/tasks/{taskId}/resources`, `POST /api/tasks/{taskId}/resources`, `DELETE /api/tasks/{taskId}/resources/{resourceId}`
- Resources: `GET /api/resources?groupId=`, `POST /api/resources`, `GET /api/resources/{id}/download`, `DELETE /api/resources/{id}`
- Activity: `GET /api/activity/user`, `GET /api/activity/group/{id}` (ak sú prítomné v projekte)

## WebSocket endpoint
- `ws://localhost:8080/ws/chat`
  - Typy: `join` / `joinMultiple {userId, userName, groupIds[]}`, `message {groupId?, text, resourceId?, taskId?, timestamp}`, `typing`.
  - Server posiela: `message`, `typing`, `online`, `EVENT` (TASK_* , RESOURCE_* ...).

## Ukážky používateľského rozhrania
- Dashboard (groups + tasks overview) – tmavý neomorfický dizajn.
- Chat – realtime správy, priloženie zdrojov.
- Tasks – vytváranie úloh, stav, termín, prílohy zo „Resources“.
- Members – zoznam členov, role, pridanie/odobratie.

## Výzvy a riešenia
- Validácia vstupov: kontrola null/blank na backend; PreparedStatement pre všetky SQL; úprava status/priority enum-like kontrolou.
- Autentifikácia: vlastný JWT filter pre `/api/**`; heslá cez BCrypt; CORS povolené len pre frontend origin. OAuth2 Google pridané cez Spring OAuth2 Client, úspech handler generuje JWT a ukladá ho na fronte.
- Realtime: WebSocket handler drží mapu skupín na sedenia; `joinMultiple` pre paralelné notifikácie; frontend subscribuje všetky skupiny používateľa.

## Návod na spustenie
### Lokálne
1. Backend: `cd backend && mvn -DskipTests package && java -jar target/study-group-backend-1.0.0.jar`
2. Frontend: `cd frontend && npm install && npm run dev -- --host`
3. OAuth2 Google: nastav env `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`; callback je `{backend}/login/oauth2/code/google`, úspech na `/oauth2/success` presmeruje na frontend.

### Docker
1. Vygeneruj jar: `cd backend && mvn -DskipTests package`
2. `docker-compose up --build` z koreňa `APP/`
   - Backend na `8080`, frontend na `5173` (nginx build).
   - SQLite súbory v `./data` (volume).

## Analytika a bezpečnosť (plán / čiastočne hotové)
- Vizualizácia: pripraviť endpoint `/api/analytics/summary` (agregáty úloh/členov/aktivít) + frontend grafy (recharts).
- Bezpečnosť: vstupné kontroly, BCrypt, JWT, prepared statements. Pre produkciu: HTTPS, kratší JWT TTL + refresh, CSP hlavičky, rate-limit.
- CI/CD: jednoduchý GitHub Actions job (mvn test/build, npm build) + docker build/push (nepriložené, ale kompatibilné).
