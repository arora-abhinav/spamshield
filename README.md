# SpamShield — SMS Spam Detector

An end-to-end Android application that classifies incoming SMS messages as spam or ham in real time using a from-scratch Multinomial Naive Bayes classifier served via a containerized FastAPI backend deployed on Railway.

---

## Project Status

- ✅ **Complete:** Dataset curation + synthetic augmentation
- ✅ **Complete:** Multinomial Naive Bayes — trained, evaluated, serialized to JSON
- ✅ **Complete:** FastAPI backend with full REST API
- ✅ **Complete:** JWT authentication with access/refresh token rotation
- ✅ **Complete:** Privacy-first database schema with user consent system
- ✅ **Complete:** Docker containerization + Docker Compose local orchestration
- ✅ **Complete:** Railway deployment (FastAPI + PostgreSQL)
- 🔲 **Planned:** Android app (Kotlin + Jetpack Compose)
- 🔲 **Planned:** Google Play Store submission

---

## Model Performance

| Metric | Score |
|---|---|
| Training Accuracy | 94.32% |
| F1 Score (real-world) | 0.918 |
| Vocabulary Size | 20,000 |
| Classification Threshold | 0.88 |

Evaluated on a 97-message real-world test set. Training set: 18,392 examples (9,662 spam / 8,730 ham).

---

## Architecture

```
Android App (Kotlin + Jetpack Compose)
        ↓  JWT Bearer Token
FastAPI (Railway)
        ↓
PostgreSQL (Railway)
        ↓
Multinomial Naive Bayes (from scratch, TF-IDF)
```

---

## ML Model

**Algorithm:** Multinomial Naive Bayes — implemented from scratch without any ML libraries

**Training Data — 18,392 examples across multiple sources:**
- UCI SMS Spam Collection
- SMS Smishing Collection (Kaggle)
- SMS Spam Dataset (Kaggle)
- Targeted synthetic ham: delivery notifications, OTPs, flight alerts, academic messages, account alerts (Gemini)
- Targeted synthetic spam: phishing, prize scams, KYC fraud, job scams, crypto scams (Gemini, GPT, DeepSeek)

**Preprocessing Pipeline:**
- Lowercase normalization
- Punctuation and special character removal
- Top 20,000 token vocabulary by frequency (grid searched: 10k → 25k)
- TF-IDF feature weighting
- Laplace smoothing (+1 numerator, +vocab_size denominator)
- Stopwords preserved — "you", "your", "now" are informative spam signals
- Log space evaluation throughout to prevent numerical underflow

---

## Privacy Architecture

SpamShield is built with a privacy-first approach:

- **No message content is stored server-side** without explicit user consent
- The `prediction` table stores only classification metadata — never the SMS text
- Message text stays on the device, stored locally in Android Room database
- Users can opt in to share confirmed spam examples for model retraining
- Users can opt out at any time and request deletion of stored data
- Compliant with DPDPA 2023 (India's Digital Personal Data Protection Act)

---

## Database Schema

**prediction**
| Column | Type | Notes |
|---|---|---|
| id | SERIAL | Primary key |
| classification | VARCHAR(4) | `spam` or `ham` |
| confidence | FLOAT | Model confidence score |
| device_id | TEXT | Unique per device |
| timestamp | TIMESTAMP | Auto set on insert |

**feedback**
| Column | Type | Notes |
|---|---|---|
| id | SERIAL | Primary key |
| prediction_id | INT | Foreign key → prediction.id |
| message | TEXT | NULL unless user opted in AND actual == spam |
| actual | VARCHAR(4) | User-reported correct label |
| timestamp | TIMESTAMP | Auto set on insert |

**consent**
| Column | Type | Notes |
|---|---|---|
| id | SERIAL | Primary key |
| device_id | TEXT | Unique per device |
| opt_in | BOOLEAN | Default false |

**device_refresh_token**
| Column | Type | Notes |
|---|---|---|
| id | SERIAL | Primary key |
| device_id | TEXT | Unique per device |
| refresh_token | TEXT | JWT refresh token |
| timestamp | TIMESTAMP | Auto set on insert |

---

## Authentication

SpamShield uses **JWT-based anonymous device authentication** — no username or password required.

- On first launch, the app calls `POST /register` to receive an access token and refresh token
- All protected endpoints require `Authorization: Bearer <access_token>` header
- Access tokens expire after 30 minutes; refresh tokens expire after 15 days
- Refresh token rotation on every `/refresh` call — old tokens are invalidated immediately
- Device IDs are server-generated UUIDs — clients cannot spoof another device's identity

---

## Backend Stack

| Technology | Purpose |
|---|---|
| FastAPI | REST API framework |
| Pydantic | Request/response validation |
| SQLAlchemy | Database ORM |
| PostgreSQL | Prediction history, feedback, consent |
| python-jose | JWT token generation and verification |
| Docker | Containerization |
| Docker Compose | Local orchestration |
| Railway | Cloud deployment |

---

## API Endpoints

**Auth (no token required)**
```
POST /register          — generate device ID, return access + refresh tokens
POST /refresh           — exchange refresh token for new token pair
```

**Predictions (access token required)**
```
POST /predict           — classify a single message
POST /predict-multiple  — classify multiple messages (used on first app install)
GET  /predictions_today/{today} — retrieve today's or previous predictions
GET  /statistics        — spam counts, percentages, weekly distribution, confidence averages
```

**Feedback & Consent (access token required)**
```
POST /feedback                  — report a misclassified message
POST /allow_messages/{opt_in}   — register consent preference
DELETE /opt_out                 — opt out of future message storage
DELETE /delete_stored_spam      — delete all stored spam messages (GDPR/DPDPA)
```

**Public**
```
GET /health             — server status and model load check
```

---

## Infrastructure

| Service | Technology |
|---|---|
| Deployment | Railway (Hobby plan) |
| Database | PostgreSQL (Railway managed) |
| Containerization | Docker |

---

## Android App (Planned)

**Tech Stack:**
| Technology | Purpose |
|---|---|
| Kotlin | Primary language |
| Jetpack Compose | Declarative UI |
| Retrofit | HTTP client + JWT interceptor |
| Room | Local SQLite — stores message text + prediction IDs |
| ViewModel | UI state management |
| Hilt | Dependency injection |
| EncryptedSharedPreferences | Secure token storage |

**Planned Features:**
- Intercepts incoming SMS via Android BroadcastReceiver
- Real-time spam classification with confidence-based colour coding (green/yellow/red)
- Silent for ham, notification for spam
- Prediction history with local message text
- User feedback — mark incorrect classifications
- Consent management UI
- Works offline using local Room database

---

## Local Development

```bash
# Clone the repo
git clone https://github.com/arora-abhinav/spamshield

# Start services
docker compose up --build

# API available at http://localhost:8000
# Swagger UI at http://localhost:8000/docs
```

---

## Known Limitations

- Test set (97 messages) is too small for statistically significant accuracy claims — cross-validation on full dataset is planned
- Synthetic training data may not fully capture real-world Indian SMS spam patterns
- Jio/Airtel transactional messages occasionally misclassified as spam (false positives)
- No rate limiting implemented yet
- No model versioning or automated retraining pipeline

---

## Related Projects

- **[bare-metal-ml](link)** — classical ML algorithms implemented from scratch in Python

---

## Author

University of Maryland, Computer Science — Freshman
