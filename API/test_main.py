import sys
import os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from main import app
from fastapi.testclient import TestClient
import auth
client = TestClient(app)
from datetime import timedelta

# ── Helpers ──────────────────────────────────────────────────────────────────

def register():
    """Register a new device and return access + refresh tokens."""
    response = client.post("/register")
    assert response.status_code == 200
    data = response.json()
    return data["access_token"], data["refresh_token"]

def auth_headers(access_token):
    return {"Authorization": f"Bearer {access_token}"}

SPAM_MESSAGE = "Congratulations! You've won a free iPhone. Click here: bit.ly/free-iph0ne"
HAM_MESSAGE  = "Hey, are you coming to the study group tonight at the library?"

# ── 1. Every protected endpoint without any token ─────────────────────────────

def test_predict_no_token():
    r = client.post("/predict", json=SPAM_MESSAGE)
    assert r.status_code == 401  


def test_predict_multiple_no_token():
    r = client.post("/predict-multiple", json=[SPAM_MESSAGE, HAM_MESSAGE])
    assert r.status_code == 401  


def test_feedback_no_token():
    r = client.post("/feedback", json={"prediction_id": 1, "actual": "ham"})
    assert r.status_code == 401  


def test_allow_messages_no_token():
    r = client.post("/allow_messages/true")
    assert r.status_code == 401  


def test_opt_out_no_token():
    r = client.delete("/opt_out")
    assert r.status_code == 401  


def test_delete_stored_spam_no_token():
    r = client.delete("/delete_stored_spam/")
    assert r.status_code == 401  


def test_predictions_today_no_token():
    r = client.get("/predictions_today/true")
    assert r.status_code == 401  


def test_statistics_no_token():
    r = client.get("/statistics/")
    assert r.status_code == 401  


# ── 2. Every protected endpoint with a fake/invalid token ────────────────────

FAKE_HEADERS = {"Authorization": "Bearer this.is.fake"}

def test_predict_invalid_token():
    r = client.post("/predict", content=SPAM_MESSAGE,
                    headers=FAKE_HEADERS)
    assert r.status_code == 401

def test_predict_multiple_invalid_token():
    r = client.post("/predict-multiple", json=[SPAM_MESSAGE],
                    headers=FAKE_HEADERS)
    assert r.status_code == 401

def test_feedback_invalid_token():
    r = client.post("/feedback",
                    json={"prediction_id": 1, "actual": "ham"},
                    headers=FAKE_HEADERS)
    assert r.status_code == 401

def test_statistics_invalid_token():
    r = client.get("/statistics/", headers=FAKE_HEADERS)
    assert r.status_code == 401

# ── 3. Predict one + predict multiple (happy path) ───────────────────────────

def test_predict_one():
    access, _ = register()
    r = client.post("/predict", content=SPAM_MESSAGE,
                    headers=auth_headers(access))
    assert r.status_code == 200
    data = r.json()
    assert "Classification" in data
    assert "Confidence" in data
    assert "Prediction ID" in data
    assert data["Classification"] in ("spam", "ham")
    assert 0.0 <= data["Confidence"] <= 1.0

def test_predict_multiple():
    access, _ = register()
    r = client.post("/predict-multiple",
                    json=[SPAM_MESSAGE, HAM_MESSAGE],
                    headers=auth_headers(access))
    assert r.status_code == 200
    results = r.json()["Batch Predictions"]
    assert len(results) == 2
    for result in results:
        assert "Classification" in result
        assert "Prediction ID" in result

# ── 4. False positive feedback then delete stored spam (nothing to delete) ───

def test_false_positive_feedback_then_delete():
    access, _ = register()
    # Make a prediction first
    pred = client.post("/predict", content=SPAM_MESSAGE,
                       headers=auth_headers(access)).json()
    prediction_id = pred["Prediction ID"]

    # Report as false positive (model said spam, actually ham)
    r = client.post("/feedback",
                    json={"prediction_id": prediction_id, "actual": "ham"},
                    headers=auth_headers(access))
    assert r.status_code == 200

    # Delete stored spam — nothing stored because actual == "ham"
    r = client.delete("/delete_stored_spam/", headers=auth_headers(access))
    assert r.status_code == 200
    assert r.json()["Result"] == "No data to delete"

# ── 5. False negative feedback then delete stored spam (nothing to delete) ───

def test_false_negative_feedback_no_consent_then_delete():
    access, _ = register()
    pred = client.post("/predict", content=HAM_MESSAGE,
                       headers=auth_headers(access)).json()
    prediction_id = pred["Prediction ID"]

    # Report as false negative (model said ham, actually spam)
    # No consent registered so message should NOT be stored
    r = client.post("/feedback",
                    json={"prediction_id": prediction_id, "actual": "spam"},
                    headers=auth_headers(access))
    assert r.status_code == 200

    # Nothing stored because user never opted in
    r = client.delete("/delete_stored_spam/", headers=auth_headers(access))
    assert r.status_code == 200
    assert r.json()["Result"] == "No data to delete"

# ── 6. Opt in first then report false negative — message should be stored ────

def test_false_negative_with_consent_then_delete():
    access, _ = register()

    # Opt in BEFORE making any prediction
    r = client.post("/allow_messages/true", headers=auth_headers(access))
    assert r.status_code == 200

    pred = client.post("/predict", content=HAM_MESSAGE,
                       headers=auth_headers(access)).json()
    prediction_id = pred["Prediction ID"]

    # Report false negative with message included
    r = client.post("/feedback",
                    json={"prediction_id": prediction_id,
                          "actual": "spam",
                          "message": HAM_MESSAGE},
                    headers=auth_headers(access))
    assert r.status_code == 200

    # Now there IS stored spam to delete
    r = client.delete("/delete_stored_spam/", headers=auth_headers(access))
    assert r.status_code == 200
    assert r.json()["Result"] == "Deleted stored spam data."

# ── 7. Opt out without having opted in ───────────────────────────────────────

def test_opt_out_without_opting_in():
    access, _ = register()
    # Register consent as false first
    client.post("/allow_messages/false", headers=auth_headers(access))
    r = client.delete("/opt_out", headers=auth_headers(access))
    assert r.status_code == 200
    assert r.json()["Result"] == "Already opted out"

# ── 8. Opt in then opt out ────────────────────────────────────────────────────

def test_opt_in_then_opt_out():
    access, _ = register()
    client.post("/allow_messages/true", headers=auth_headers(access))
    r = client.delete("/opt_out", headers=auth_headers(access))
    assert r.status_code == 200
    assert r.json()["Result"] == "Opted out of future messages being stored"

# ── 9. Delete all stored spam since the beginning ────────────────────────────

def test_delete_all_stored_spam():
    access, _ = register()
    client.post("/allow_messages/true", headers=auth_headers(access))

    # Make multiple predictions and report false negatives
    for _ in range(3):
        pred = client.post("/predict", content=HAM_MESSAGE,
                           headers=auth_headers(access)).json()
        client.post("/feedback",
                    json={"prediction_id": pred["Prediction ID"],
                          "actual": "spam",
                          "message": HAM_MESSAGE},
                    headers=auth_headers(access))

    # Delete all stored spam
    r = client.delete("/delete_stored_spam/", headers=auth_headers(access))
    assert r.status_code == 200
    assert r.json()["Result"] == "Deleted stored spam data."

    # Second delete should find nothing
    r = client.delete("/delete_stored_spam/", headers=auth_headers(access))
    assert r.json()["Result"] == "No data to delete"

# ── 10. Statistics after a few predictions ───────────────────────────────────

def test_statistics_after_predictions():
    access, _ = register()
    client.post("/predict", content=SPAM_MESSAGE, headers=auth_headers(access))
    client.post("/predict", content=HAM_MESSAGE,  headers=auth_headers(access))
    client.post("/predict", content=SPAM_MESSAGE, headers=auth_headers(access))

    r = client.get("/statistics/", headers=auth_headers(access))
    assert r.status_code == 200
    data = r.json()
    assert data["Total Messages"] == 3
    assert data["Spam Count"] + data["Ham Count"] == data["Total Messages"]
    assert "Weekly Spam Distribution" in data
    assert "Average Confidence All" in data

def test_statistics_zero_predictions():
    access, _ = register()
    r = client.get("/statistics/", headers=auth_headers(access))
    assert r.status_code == 200
    data = r.json()
    assert data["Total Messages"] == 0
    assert data["Spam Percentage"] == 0

# ── 11. Refresh endpoint — new access token should work ──────────────────────

def test_refresh_gives_working_access_token():
    _, refresh = register()
    r = client.post("/refresh",
                    headers={"Authorization": f"Bearer {refresh}"})
    assert r.status_code == 200
    new_access = r.json()["access_token"]

    # New access token should authorize predict
    r = client.post("/predict", content=SPAM_MESSAGE,
                    headers=auth_headers(new_access))
    assert r.status_code == 200

# ── 12. Refresh token should NOT work on protected endpoints ─────────────────

def test_refresh_token_rejected_on_predict():
    _, refresh = register()
    r = client.post("/predict", content=SPAM_MESSAGE,
                    headers={"Authorization": f"Bearer {refresh}"})
    assert r.status_code == 401

# ── 13. Refresh token rejected on register ───────────────────────────────────

def test_refresh_token_rejected_on_register():
    # /register has no auth — but if someone passes a refresh token
    # it should still just generate a fresh device (register ignores auth)
    _, refresh = register()
    r = client.post("/register",
                    headers={"Authorization": f"Bearer {refresh}"})
    # Register is public so it should succeed and return a NEW device
    assert r.status_code == 200
    data = r.json()
    assert "access_token" in data
    assert "refresh_token" in data

# ── 14. Two registrations produce different device IDs ───────────────────────

def test_two_registrations_different_devices():
    access1, _ = register()
    access2, _ = register()

    from jose import jwt
    import os
    SECRET_KEY = os.getenv("SECRET_KEY")
    ALGORITHM  = "HS256"

    payload1 = jwt.decode(access1, SECRET_KEY, algorithms=[ALGORITHM])
    payload2 = jwt.decode(access2, SECRET_KEY, algorithms=[ALGORITHM])

    assert payload1["device_id"] != payload2["device_id"]

# ── 15. Expired access token is rejected ─────────────────────────────────────

def test_expired_access_token_rejected():
    expired_token = auth.create_token(
        data={"device_id": "test-device", "type": "access"},
        expires_delta=timedelta(seconds=-1)
    )
    r = client.post("/predict", content=SPAM_MESSAGE,
                    headers={"Authorization": f"Bearer {expired_token}"})
    assert r.status_code == 401

# ── 16. Predict with empty string ────────────────────────────────────────────

def test_predict_empty_string():
    access, _ = register()
    r = client.post("/predict", content="",
                    headers=auth_headers(access))
    # Should either return a valid classification or a 422 validation error
    assert r.status_code in (200, 422, 400)

# ── 17. Predict with very long message ───────────────────────────────────────

def test_predict_very_long_message():
    access, _ = register()
    long_message = "spam " * 1000
    r = client.post("/predict", content=long_message,
                    headers=auth_headers(access))
    assert r.status_code == 200

# ── 18. Feedback with invalid prediction_id ──────────────────────────────────

def test_feedback_invalid_prediction_id():
    access, _ = register()
    r = client.post("/feedback",
                    json={"prediction_id": 999999, "actual": "spam"},
                    headers=auth_headers(access))
    # Should fail due to foreign key constraint
    assert r.status_code in (422, 500, 400)

# ── 19. Old refresh token rejected after rotation ────────────────────────────

def test_refresh_token_rotation_invalidates_old_token():
    _, refresh = register()

    # Use refresh token once — get new pair
    r = client.post("/refresh",
                    headers={"Authorization": f"Bearer {refresh}"})
    assert r.status_code == 200

    # Old refresh token should now be rejected
    r = client.post("/refresh",
                    headers={"Authorization": f"Bearer {refresh}"})
    assert r.status_code == 401

# ── 20. Health check ─────────────────────────────────────────────────────────

def test_health():
    r = client.get("/health")
    assert r.status_code == 200
    assert r.json()["status"] == "ok"
    assert r.json()["model_loaded"] is True