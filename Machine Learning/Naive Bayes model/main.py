from fastapi import FastAPI, Body
#This is the library FastAPI recommends to have to have an option query parameter
from typing import Optional
from pydantic import BaseModel
import classifier_model

#Making sure the input is a string
class SingleMessage(BaseModel):
    message: str

#Making sure the input is an array of strings
class BulkMessages(BaseModel):
    messages: list[str]


app = FastAPI()

#HTTP endpoint to make a single prediction
@app.post("/predict")
def predict_one(message:SingleMessage = Body(description="Single message as input into the model. Prediction will be ham or spam. Useful for new incoming messages")):
    return classifier_model.predict_one(message.message)
    #This will also return an HTTP exception automatically from the classifier_model.py script

#HTTP endpoint to make a bulk prediction
@app.post("/predict-multiple")
def predict_multiple(messages:BulkMessages = Body(description="Multiple messages as an array input into the model. Predictions will be ham or spam, returned in the same order of messages. Useful for predicting previous messages")):
    return classifier_model.predict_multiple(messages.messages)
    #This will also return an HTTP exception automatically from the classifier_model.py script


#Just to see if the server is up and running
@app.get("/health")
def health():
    return {"status": "ok", "model_loaded": classifier_model.data is not None}

