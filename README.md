# SMS Spam Detector

An end-to-end Android application that classifies SMS messages as spam or ham using a hybrid detection system combining a from-scratch Multinomial Naive Bayes classifier with a known phishing patterns database.

---

## Project Status

- 🟡 **In Progress:** ML model training and preprocessing pipeline
- 🔲 **Planned:** FastAPI backend
- 🔲 **Planned:** Docker containerization
- 🔲 **Planned:** AWS deployment
- 🔲 **Planned:** Android app

---

## Architecture

```
Android App (Kotlin + Jetpack Compose)
        ↓
AWS API Gateway
        ↓
FastAPI (AWS EC2)
        ↓              ↓
Redis Cache     PostgreSQL (PhishTank patterns)
        ↓
Multinomial Naive Bayes (from scratch)
```

---

## ML Model

**Algorithm:** Multinomial Naive Bayes — implemented from scratch without any ML libraries

**Dataset:**
- UCI SMS Spam Collection
- SMS Spam Dataset v2
- Synthetic augmentation covering modern spam patterns:
    - Delivery scams
    - Bank alerts
    - OTP verification scams
    - Prize scams
    - Government impersonation

**Preprocessing Pipeline:**
- Lowercase normalization
- Special token replacement — URLs → `<URL>`, phone numbers → `<PHONE>`, money → `<MONEY>`, codes → `<CODE>`
- Word unigrams and bigrams
- Character n-grams (length 3-5)
- Stopwords preserved — "you", "your", "now" are informative in spam context
- Rare token filtering — tokens appearing fewer than 3 times removed
- Light stemming

---

## Backend Stack

| Technology | Purpose |
|---|---|
| FastAPI | REST API framework |
| Pydantic | Request/response validation |
| Redis | Prediction caching |
| PostgreSQL | Known phishing patterns database |
| Docker | Containerization |
| Docker Compose | Local orchestration |

**API Endpoints:**
```
POST /predict        — classify a single message
POST /predict/batch  — classify multiple messages
GET  /predictions    — prediction history
GET  /health         — health check
```

---

## Infrastructure

| Service | Technology |
|---|---|
| Server | AWS EC2 |
| Database | AWS RDS (PostgreSQL) |
| Cache | AWS ElastiCache (Redis) |
| Image Registry | AWS ECR |
| CI/CD | GitHub Actions |

**Deployment Pipeline:**
```
Push to main
        ↓
Run Pytest
        ↓
Build Docker image
        ↓
Push to AWS ECR
        ↓
Deploy to EC2
        ↓
Health check
```

---

## Android App

**Tech Stack:**
| Technology | Purpose |
|---|---|
| Kotlin | Primary language |
| Jetpack Compose | Declarative UI |
| Retrofit | HTTP client |
| Room | Local database |
| ViewModel | UI state management |
| Hilt | Dependency injection |

**Features:**
- Read SMS inbox via Android SMS API
- Real time spam classification
- Color coded results — red for spam, green for ham
- Confidence score display
- Local caching via Room database
- Works offline for previously classified messages

---

## Hybrid Detection System

```
Message arrives
        ↓
Check Redis cache
        ↓ cache miss
Check PostgreSQL known phishing patterns (PhishTank)
        ↓ no match
Run Multinomial Naive Bayes
        ↓
Cache result in Redis
        ↓
Return prediction + confidence score
```

---

## Implementation Roadmap

- 🟡 Week 1 — Dataset combination + synthetic augmentation + model training
- 🔲 Week 2 — FastAPI backend + Redis + PostgreSQL
- 🔲 Week 3 — Docker + Docker Compose
- 🔲 Week 4 — AWS deployment
- 🔲 Week 5 — GitHub Actions CI/CD
- 🔲 Week 6-7 — Kotlin + Jetpack Compose basics
- 🔲 Week 8-9 — Android app development
- 🔲 Week 10 — Polish + Google Play publish

---

## Related Projects

This project is part of a broader ML portfolio:
- **[bare-metal-ml](link)** — classical ML algorithms implemented from scratch in Python and C++

---

## Author
University of Maryland, Computer Science — Freshman
