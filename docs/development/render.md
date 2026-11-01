# Deploy to Render

Use one Git repository and two Render services defined by the root [`render.yaml`](../../render.yaml):

| Service | Render type | Root directory | Purpose |
| --- | --- | --- | --- |
| `universal-erp-api` | Docker web service | `backend` | Spring Boot API and ERP-owned login |
| `universal-erp-web` | Static site | `frontend` | React/Vite application |

The database remains on Supabase PostgreSQL. The frontend never connects to PostgreSQL directly. The Blueprint links the two public Render URLs: it sets the API's `CORS_ALLOWED_ORIGIN` to the static site's URL and sets the frontend's `VITE_API_ORIGIN` to the API's URL. The frontend appends `/api/v1` to that origin. Render builds the API with `backend/Dockerfile`, publishes `frontend/dist`, and rewrites frontend routes to `index.html`.

## Before creating the Blueprint

1. Put the project in a Git repository connected to Render. Commit `render.yaml`, `backend`, and `frontend`. Keep `.env` and `frontend/.env.local` out of Git; they are ignored. Do not put database passwords in `render.yaml` or any `VITE_` variable.
2. In the Supabase project, open **Connect** and select the **Session pooler** on port `5432` for the existing IPv4-compatible connection. Copy its host and username. Use the JDBC URL `jdbc:postgresql://<pooler-host>:5432/postgres?sslmode=require` for `DB_URL`. Set `DB_USERNAME` to the session-pooler username and `DB_PASSWORD` to the database password. The password is a separate variable; do not put it in the JDBC URL.
3. Because a database password was previously shared in chat, rotate it in Supabase before using this as a public deployment. Update the local `.env` and the Render `DB_PASSWORD` value after rotating it. Do not send the new password in chat.
4. Change the first administrator's temporary password through the local ERP before putting the API on the public internet. That temporary password was previously shared in chat. The ERP requires a new password at first sign-in; complete that step before deploying.
5. Make a backup of any database that will hold real ERP data. Flyway applies pending migrations when the API starts. The approved ERP test project already has V1–V13 and a first administrator. A new database needs the [first-administrator bootstrap](setup.md#first-administrator) after its schema is initialized.

## Create the services

1. In Render, choose **New → Blueprint**, connect the repository and select its deployment branch. Render reads the root `render.yaml`.
2. Enter the three requested backend values: `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`. Render generates and retains a Base64 `AUTH_JWT_SECRET`; keep that value stable. The Blueprint sets the production Spring profile, routes, health check, and service URLs.
3. Apply the Blueprint and wait for both services to build. The API health check is `GET /actuator/health`. The frontend URL is the `universal-erp-web` static site's `onrender.com` URL.
4. Open the frontend and sign in with an ERP account. Open a direct frontend route such as `/inventory` to confirm the single-page app rewrite works. Check the browser network tab if login cannot reach the API.

The Blueprint sets the API to Render's **Free** compute plan for initial evaluation. Render's Free web services can spin down when idle, so the first API request may be slow. Move the API to a suitable paid plan before real business use. The database is still your Supabase project; no Render database is created.

## If you use custom domains

The automatic URL references use each service's `onrender.com` address. If the frontend moves to a custom domain, set `CORS_ALLOWED_ORIGIN` on the API to the exact frontend origin, including `https://` and without a trailing slash. If the API moves to a custom domain, set `VITE_API_BASE_URL` on the static site to `https://<api-domain>/api` and rebuild the static site. `VITE_API_BASE_URL` is public build-time configuration, never a secret.

## Troubleshooting

- API fails to start: inspect Render logs for missing database variables, JWT-secret validation, Flyway migration errors, or database connection failures. Render supplies `PORT`; Spring Boot binds to that port and to all interfaces.
- API is healthy, but login fails in the browser: check that the frontend bundle calls the API's public HTTPS URL and that `CORS_ALLOWED_ORIGIN` exactly matches the browser's frontend origin.
- Direct frontend routes show 404: confirm the `/*` → `/index.html` rewrite exists on the static site.
- First request is slow: the Free API service may be waking from idle. Use a paid plan for sustained ERP usage.

This Blueprint prepares deployment; it does not complete the security, backup/restore, and end-to-end checks listed in the [completion checklist](completion-checklist.md).
