--Table for all predictions containng message, prediction and timestamp
CREATE TABLE IF NOT EXISTS prediction(
    id SERIAL PRIMARY KEY,
    --Ham or spam are the only 2 predictions, char count is 4
    classification VARCHAR(4),
    confidence FLOAT,
    "timestamp" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    device_id TEXT
);

--Table for containing feedback -> will be useful for retraining model
CREATE TABLE IF NOT EXISTS feedback(
    id SERIAL PRIMARY KEY,
    prediction_id INT,
    message TEXT DEFAULT NULL,
    actual varchar(4),
    --Creating the foreign key
    CONSTRAINT fk_prediction FOREIGN KEY(prediction_id) REFERENCES prediction(id),
    "timestamp" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--Table for checking if user consent allows us to store messages for spam retraining and prediction
CREATE TABLE IF NOT EXISTS consent(
    id SERIAL PRIMARY KEY,
    device_id TEXT NOT NULL,
    opt_in BOOLEAN DEFAULT FALSE
);

--Table for storing refresh tokens
CREATE TABLE IF NOT EXISTS device_refresh_token(
    id SERIAL PRIMARY KEY,
    device_id TEXT NOT NULL,
    refresh_token TEXT NOT NULL,
    "timestamp" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);