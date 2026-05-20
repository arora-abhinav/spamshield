# SpamShield — SMS Spam Detector

An end-to-end Android application that classifies incoming SMS messages as spam or ham in real time using a from-scratch Multinomial Naive Bayes classifier served via a containerized FastAPI backend.

---

## Project Status

- ✅ **Complete:** Dataset combination + synthetic augmentation
- ✅ **Complete:** Multinomial Naive Bayes — trained, evaluated, serialized to JSON
- ✅ **Complete:** FastAPI backend with `/predict`, `/predict-multiple`, `/health` endpoints
- ✅ **Complete:** Docker containerization
- ✅ **Complete:** Database schema design + ERD
- 🟡 **In Progress:** Docker Compose + PostgreSQL integration
- 🔲 **Planned:** Redis caching
- 🔲 **Planned:** Railway deployment
- 🔲 **Planned:** Android app

---

## Model Performance

| Metric | Score |
|---|---|
| Accuracy | 94.8% |
| Spam Precision | 0.93 |
| Spam Recall | 0.97 |
| Ham Precision | 0.97 |
| Ham Recall | 0.92 |
| F1 Score | 0.95 |

Evaluated on 3,360 held-out test examples (80/20 train/test split).

---

## Architecture

```
Android App (Kotlin + Jetpack Compose)
        ↓
FastAPI (Railway)
        ↓              ↓
Redis Cache     PostgreSQL (prediction history + feedback)
        ↓
Multinomial Naive Bayes (from scratch)
```

---

## ML Model

**Algorithm:** Multinomial Naive Bayes — implemented from scratch without any ML libraries

**Training Data — 16,796 examples across 4 sources:**
- UCI SMS Spam Collection
- SMS Smishing Collection (Kaggle)
- SMS Spam Dataset 10,286 rows (Kaggle)
- Synthetic augmentation via Claude API — delivery scams, bank alerts, OTP scams, prize scams, government impersonation, phishing links, job scams, crypto scams

**Preprocessing Pipeline:**
- Lowercase normalization
- Punctuation and special character removal
- Top 10,000 token vocabulary by frequency
- Word count vectorization (Multinomial)
- Laplace smoothing (+1 numerator, +vocab_size denominator)
- Rare token filtering — tokens appearing fewer than 3 times removed
- Stopwords preserved — "you", "your", "now" are informative spam signals
- Log space evaluation throughout to prevent numerical underflow

---

## Database Schema

See [`docs/erd.png`](docs/erd.png) for the full Entity Relationship Diagram.

**predictions**
| Column | Type | Notes |
|---|---|---|
| id | SERIAL | Primary key |
| message | TEXT | Raw SMS text |
| classification | VARCHAR(4) | spam or ham |
| confidence | FLOAT | Model confidence score |
| device_id | VARCHAR(255) | Unique per device |
| timestamp | TIMESTAMP | Auto set on insert |

**feedback**
| Column | Type | Notes |
|---|---|---|
| id | SERIAL | Primary key |
| classification | VARCHAR(4) | What model predicted |
| actual | VARCHAR(4) | What user reported |
| message_id | INT | Foreign key → predictions.id |
| timestamp | TIMESTAMP | Auto set on insert |

---

## Backend Stack

| Technology | Purpose |
|---|---|
| FastAPI | REST API framework |
| Pydantic | Request/response validation |
| Redis | Prediction caching (24hr TTL) |
| PostgreSQL | Prediction history + user feedback |
| Docker | Containerization |
| Docker Compose | Local orchestration |

**API Endpoints:**
```
POST /predict          — classify a single message
POST /predict-multiple — classify multiple messages
GET  /predictions      — prediction history
GET  /health           — health check
```

---

## Infrastructure

| Service | Technology |
|---|---|
| Deployment | Railway |
| Database | PostgreSQL (Docker / Railway) |
| Cache | Redis (Docker / Railway) |

---

## Android App

**Tech Stack:**
| Technology | Purpose |
|---|---|
| Kotlin | Primary language |
| Jetpack Compose | Declarative UI |
| Retrofit | HTTP client |
| Room | Local SQLite cache |
| ViewModel | UI state management |
| Hilt | Dependency injection |

**Features:**
- Intercepts incoming SMS via Android background service
- Real time spam classification
- Silent for ham, "⚠️ Spam detected" notification for spam
- Prediction history with confidence scores
- User feedback — mark incorrect classifications
- Works offline for previously classified messages

---

## Hybrid Detection Flow

```
Message arrives
        ↓
Check Redis cache (hash of message)
        ↓ cache miss
Run Multinomial Naive Bayes
        ↓
Store result in PostgreSQL
        ↓
Cache result in Redis (24hr TTL)
        ↓
Return prediction + confidence score
```

---

## Implementation Roadmap

- ✅ Week 1 — Dataset combination + synthetic augmentation + model training
- 🟡 Week 2 — Docker Compose + PostgreSQL + Redis integration
- 🔲 Week 3 — Railway deployment
- 🔲 Week 4-5 — Kotlin + Jetpack Compose basics
- 🔲 Week 6-7 — Android app development
- 🔲 Week 8 — Polish + Google Play publish

---

## Related Projects

- **[bare-metal-ml](link)** — classical ML algorithms implemented from scratch in Python and C++

---

## Author

University of Maryland, Computer Science — Freshman
