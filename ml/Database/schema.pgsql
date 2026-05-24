--Table for all predictions containng message, prediction and timestamp
CREATE TABLE IF NOT EXISTS prediction(
    id SERIAL PRIMARY KEY,
    message TEXT,
    --Ham or spam are the only 2 predictions, char count is 4
    classification VARCHAR(4),
    confidence FLOAT,
    "timestamp" TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    device_id VARCHAR(255)
);

--Table for containing feedback -> will be useful for retraining model
CREATE TABLE IF NOT EXISTS feedback(
    id SERIAL PRIMARY KEY,
    prediction_id INT,
    actual varchar(4),
    --Creating the foreign key
    CONSTRAINT fk_prediction FOREIGN KEY(prediction_id) REFERENCES prediction(id),
    "timestamp" TIMESTAMP DEFAULT CURRENT_TIMESTAMP
)


