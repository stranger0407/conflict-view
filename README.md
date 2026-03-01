# ConflictView

**Live global conflict tracking and news aggregation platform for researchers, journalists, and news followers.**

A full-stack web application that tracks active conflicts worldwide on an interactive map, aggregates news from multiple sources, and provides per-source reliability scoring, sentiment analysis, timeline views, and analytics dashboards.

---

## Features

- **Interactive world map** (Mapbox GL, dark globe projection) with pulsing severity markers
- **Conflict detail dashboards** — news feed, event timeline, source charts, analytics
- **Multi-source news aggregation** — GDELT, NewsAPI, BBC/Reuters/Al Jazeera/AP RSS feeds
- **Source reliability scoring** — curated scores (Reuters=92, BBC=88, unknown=40) with color-coded badges
- **Sentiment analysis** — keyword-based classification per article
- **Live WebSocket updates** — new articles pushed to subscribers in real time
- **Search & filter** — by region, severity, conflict type, status
- **18 pre-seeded real conflicts** — data available immediately on first run

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | Angular 17, Mapbox GL JS, Angular Material, ngx-charts |
| Backend | Spring Boot 3.2, Java 21, Maven |
| Database | PostgreSQL |
| Cache | Caffeine (in-memory) |
| Real-time | WebSocket (STOMP over SockJS) |
| Deployment | Railway |

---

## Prerequisites

- Java 21 JDK
- Node.js 20+ and npm
- PostgreSQL 15+
- Maven 3.9+
- Mapbox account (free token)
- NewsAPI key (free at newsapi.org — 100 req/day)

---

## Local Development Setup

### 1. Clone the repository

```bash
git clone https://github.com/YOUR_ORG/conflict-view.git
cd conflict-view
```

### 2. Set up PostgreSQL

```sql
CREATE DATABASE conflictview;
CREATE USER conflictview_user WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE conflictview TO conflictview_user;
```

### 3. Configure the backend

Edit `conflict-view-backend/src/main/resources/application.yml` and update:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/conflictview
    username: conflictview_user
    password: your_password

app:
  news-api:
    key: YOUR_NEWSAPI_KEY        # from newsapi.org
  cors:
    allowed-origins: http://localhost:4200
```

### 4. Start the backend

```bash
cd conflict-view-backend
mvn spring-boot:run
```

The backend starts at `http://localhost:8080`.
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Health check: `http://localhost:8080/actuator/health`

Flyway will automatically run all migrations and seed 18 conflicts + 35 news sources.

### 5. Configure the frontend

Edit `conflict-view-frontend/src/environments/environment.ts`:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080/api',
  wsUrl: 'http://localhost:8080/ws',
  mapboxToken: 'pk.eyJ1...'   // ← your Mapbox public token
};
```

### 6. Start the frontend

```bash
cd conflict-view-frontend
npm install
npm start
```

App runs at `http://localhost:4200`.

---

## Project Structure

```
conflict-view/
├── conflict-view-backend/           Spring Boot 3 / Java 21
│   ├── src/main/java/com/conflictview/
│   │   ├── config/                  CORS, WebSocket, Cache, RestTemplate
│   │   ├── model/                   JPA entities + enums
│   │   ├── repository/              Spring Data JPA repositories
│   │   ├── dto/                     API response objects
│   │   ├── service/                 Business logic + data fetchers
│   │   │   ├── GdeltService         GDELT API integration
│   │   │   ├── NewsApiService       NewsAPI.org integration
│   │   │   ├── RssFeedService       RSS feed parser (Rome)
│   │   │   ├── ReliabilityService   Source trust scoring
│   │   │   └── SentimentService     Keyword-based sentiment
│   │   ├── scheduler/               @Scheduled news refresh (every 15min)
│   │   ├── controller/              REST API endpoints
│   │   └── websocket/               STOMP publisher
│   └── src/main/resources/
│       └── db/migration/
│           ├── V1__init_schema.sql  Database schema
│           ├── V2__seed_news_sources.sql  35 sources with reliability scores
│           └── V3__seed_conflicts.sql     18 real active conflicts
│
└── conflict-view-frontend/          Angular 17
    └── src/app/
        ├── core/                    Models, services, interceptors
        ├── layout/                  Header navigation
        ├── shared/                  reliability-badge, severity-badge, news-card, sentiment-indicator
        └── features/
            ├── map/                 Full-screen Mapbox globe map
            ├── dashboard/           Global stats, charts, latest news
            ├── conflict-detail/     Per-conflict news feed, timeline, analytics
            └── search/              Search & filter conflicts
```

---

## API Reference

| Method | Endpoint | Description |
|---|---|---|
| GET | `/api/conflicts` | All active conflicts (map data) |
| GET | `/api/conflicts/{id}` | Full conflict detail |
| GET | `/api/conflicts/{id}/news` | Paginated news `?page=0&size=20&sentiment=&source=` |
| GET | `/api/conflicts/{id}/events` | Timeline events |
| GET | `/api/conflicts/{id}/stats` | Chart and statistics data |
| GET | `/api/conflicts/search` | Search `?q=&region=&severity=&type=&status=` |
| GET | `/api/dashboard/stats` | Global dashboard statistics |
| GET | `/api/sources` | All news sources with reliability scores |

WebSocket topics:
- `SUBSCRIBE /topic/conflicts/updates` — conflict severity/status changes
- `SUBSCRIBE /topic/news/{conflictId}` — new articles for a conflict

---

## Railway Deployment

### Step 1 — Create a Railway project

1. Go to [railway.app](https://railway.app) and create a new project
2. Add a **PostgreSQL** plugin — Railway auto-provides `DATABASE_URL`

### Step 2 — Deploy the backend

1. Add a new service → "Deploy from GitHub repo" → select `conflict-view-backend/`
2. Set environment variables in Railway dashboard:

```
NEWS_API_KEY=your_key
CORS_ALLOWED_ORIGINS=https://your-frontend.railway.app
GDELT_ENABLED=true
RSS_ENABLED=true
NEWS_API_ENABLED=true
```

3. Railway uses `nixpacks.toml` — it will build the Maven JAR and start it.

### Step 3 — Deploy the frontend

1. Add another service → select `conflict-view-frontend/`
2. **Update** `src/environments/environment.prod.ts` before deploying:

```typescript
export const environment = {
  production: true,
  apiUrl: 'https://YOUR_BACKEND_SERVICE.railway.app/api',
  wsUrl: 'https://YOUR_BACKEND_SERVICE.railway.app/ws',
  mapboxToken: 'pk.eyJ1...'
};
```

3. No additional environment variables needed — Mapbox token is baked into the build.

### Step 4 — Verify

- Backend health: `https://your-backend.railway.app/actuator/health`
- Swagger docs: `https://your-backend.railway.app/swagger-ui.html`
- Frontend: `https://your-frontend.railway.app`

---

## Extending the Platform

### Adding a new conflict

Run raw SQL or add to `V3__seed_conflicts.sql`:

```sql
INSERT INTO conflicts (name, region, country_codes, latitude, longitude, severity, conflict_type, status, start_date, summary, involved_parties)
VALUES ('New Conflict Name', 'Region', 'XX', 12.3, 45.6, 'HIGH', 'CIVIL_UNREST', 'ACTIVE', '2024-01-01', 'Summary...', 'Party A, Party B');
```

### Adding a new news source

```sql
INSERT INTO news_sources (domain, name, reliability_score, bias_rating, category, country)
VALUES ('example.com', 'Example News', 75, 'Center', 'MAINSTREAM', 'United States');
```

### Adjusting reliability scores

Edit `V2__seed_news_sources.sql` or update directly in the `news_sources` table. Scores range 0–100:
- **80–100**: High reliability (green badge)
- **60–79**: Medium reliability (yellow badge)
- **0–59**: Low reliability (red badge)

---

## Environment Variables Reference

### Backend

| Variable | Default | Description |
|---|---|---|
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/conflictview` | PostgreSQL connection URL |
| `DATABASE_USERNAME` | `postgres` | DB username |
| `DATABASE_PASSWORD` | `postgres` | DB password |
| `NEWS_API_KEY` | _(empty)_ | newsapi.org API key |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:4200` | Allowed CORS origins |
| `GDELT_ENABLED` | `true` | Enable GDELT sync |
| `RSS_ENABLED` | `true` | Enable RSS feed fetching |
| `NEWS_API_ENABLED` | `true` | Enable NewsAPI fetching |

---

## License

MIT
