from fastapi import FastAPI, Body, Path, Request, status, HTTPException, Header, Depends
from pydantic import BaseModel
import classifier_model
from datetime import datetime, timedelta
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from typing import Optional, Annotated
import os
from jose import jwt, JWTError
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
import uuid

#sql alchemy to make edits to the database
from sqlalchemy import create_engine, text

#Secret Key and algorithm being used to encode our access token
SECRET_KEY = os.getenv("SECRET_KEY")
ACCESS_TOKEN_EXPIRY_MINUTES = 30
ALGORITHM = "HS256"

opt_in = False
#Connecting to spamshiled via connection string
#os.getenv("DATABASE_URL") is specifically for Railway
connection_string = os.getenv("CONNECTION_STRING") or os.getenv("DATABASE_URL")
engine = create_engine(connection_string, echo=True)
connection = engine.connect()

security = HTTPBearer(auto_error=True)

#For reporting misclassified messages
class FeedbackMessage(BaseModel):
    prediction_id: int
    actual: str
    #We do NOT want to store missclassified
    message:str = Optional[None]


app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins = ['*'],
    allow_methods = ['*'],
    allow_headers = ['*']
)

#A universal token (access or refresh) that has 2 parameters a dict and an expires_delta one.
#To distinguish between the two, they can simply be assigned different parameters
def create_token(data:dict, expires_delta: timedelta):
    to_encode = data.copy()
    #Saying that an access token will expire in expire delta minutes from now 
    expire = expires_delta + datetime.now()
    to_encode.update({"exp": expire})
    #Our access token will be a JWT
    encoded_jwt = jwt.encode(to_encode, SECRET_KEY, ALGORITHM)
    return encoded_jwt

# A verify token that will be used to obtain and return the device_id
#The HTTP bearer is a form of the Authorization Header that has "bearer token"
def verify_token(token_type: str):
    #An auxilary verify function into which the token_type is "baked into" via closure
    #Default values are evaluated once by fastAPI on startup so once verify_token is called at startup,
    #All subsequent calls lead directly to auxilary_verify because that is its returned function
    def auxilary_verify (credentials: Annotated[HTTPAuthorizationCredentials, Depends(security)]):
        credential_exception = HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Could not validate user")
        #Could automatically raise an error if the .decode function cannot verify that the token
        #contains the same decoded string as the signature
        try:
            payload = jwt.decode(credentials.credentials, SECRET_KEY, [ALGORITHM])
            device_id = payload.get("device_id")
            actual_token_type = payload.get("type")
            if device_id is None or actual_token_type != token_type:
                raise credential_exception

            #Returning credentials.credentials for the refresh endpoint to verify if the refresh token
            #exists in the first place
            return device_id
        except JWTError:
            raise credential_exception
    
    return auxilary_verify

#New endpoint to allow users to opt into sharing spam messages or not (no need to store ham messages
#unless specified)
@app.post("/allow_messages/{opt_in}")
def allow_message_tracking(opt_in:bool, device_id = Depends(verify_token("access"))):
    with engine.connect() as connection:
        connection.execute(text("INSERT INTO consent(opt_in, device_id) VALUES(:opt_in, :device_id"), 
        {
            "device_id": device_id,
            "opt_in": opt_in
        })

        return {"Opted in": opt_in}

@app.delete("/opt_out")
def opt_out_message_tracking(device_id = Depends(verify_token("access"))):
    with engine.connect() as connection:
        connection.execute(text("UPDATE consent SET opt_in = FALSE WHERE device_id = :device_id"), {
            "device_id": device_id
        })
        connection.commit()
    
    return {"Result": "Opted out of future messages being stored"}

#Separate endpoint that allows users to delete stored spam data
@app.delete("/delete_stored_spam/")
def delete_spam(device_id = Depends(verify_token("access"))):
    with engine.connect() as connection:
        connection.execute(text("DELETE FROM feedback WHERE device_id = :device_id"), {
            "device_id": device_id
        })
    connection.commit()

    return {"Result": "Deleted stored spam data."}

#HTTP endpoint to make a single prediction
@app.post("/predict")
def predict_one(message:str = Body(description="Single message as input into the model. Prediction will be ham or spam. Useful for new incoming messages"), device_id = Depends(verify_token("access"))):
    with engine.connect() as connection:
        data = classifier_model.predict_one(message.message)
        prediction = data["Classification"]
        score = data["Confidence"]
        #Not using formatted strings to prevent sql injections (protection against malicious input)
        connection.execute(text(""" 
        INSERT INTO prediction(device_id, confidence, classification) 
        VALUES(:device_id, :confidence, :classification)"""), {
            "device_id": device_id,
            "confidence": score,
            "classification": prediction
        })
        connection.commit()

        current_time = datetime.now()
        return {
            "Classification" : prediction,
            "Confidence": score,
            "Time": current_time
        }
    #This will also return an HTTP exception automatically from the classifier_model.py script

#HTTP endpoint to make a bulk prediction -> This is only done once
@app.post("/predict-multiple")
def predict_multiple(messages:list[str] = Body(description="Multiple messages as an array input into the model. Predictions will be ham or spam, returned in the same order of messages. Useful for predicting previous messages"), device_id = Depends(verify_token("access"))):
    with engine.connect() as connection:
        #Same logic as predict_one but just in a loop. Instead of calling predict_one endpoint multiple times, 
        #just call the predict_one function in the classifier model multiple times through just one API call.
        #This call will ONLY be made when the app is installed for the first time, and that's it. 
        results = []
        current_time = datetime.now()
        for message in messages:
            result = classifier_model.predict_one(message)
            #Adding current time since it wasn't natively returned
            result["Time"] = current_time
            results.append(result)
            connection.execute(text(""" 
            INSERT INTO prediction(device_id, confidence, classification) 
            VALUES(:device_id, :confidence, :classification)"""), {
                "device_id": device_id,
                "confidence": result["Confidence"],
                "classification": result["Classification"]
            })
            connection.commit()
        

        return {"Batch Predictions": results}

    #This will also return an HTTP exception automatically from the classifier_model.py script


#Just to see if the server is up and running
@app.get("/health")
def health():
    return {"status": "ok", "model_loaded": classifier_model.data is not None}

#A feedback endpoint: users report misclassified predictions here, associated with the device 
@app.post("/feedback")
def give_feedback(feedback: FeedbackMessage, device_id = Depends(verify_token("access"))):
    with engine.connect() as connection:
        consented = connection.execute(text("SELECT opt_in FROM consent WHERE :device_id = device_id"), {
            "device_id":device_id
        }).scalar()

        if not consented:
            connection.execute((text("""
            INSERT INTO feedback(prediction_id, actual) VALUES(:prediction_id, :actual)""")), {
                "prediction_id": feedback.prediction_id,
                "actual": feedback.actual
            })
        else:
            #Don't need to check for ham or spam since sending the message will be handled by the request body
            #(Now feedback.message is optional)
            connection.execute((text("""
            INSERT INTO feedback(message, prediction_id, actual) VALUES(:message, :prediction_id, :actual)""")), {
                "prediction_id": feedback.prediction_id,
                "actual": feedback.actual,
                "message": feedback.message
            })
        connection.commit()
        return {"Feedback": "Feedback received, we apologise for the inconvenience"}

@app.get("/predictions_today/{today}")
#Obtains predictions for either today or previous days
#Query parameter required determining if previous predictions should be returned or just today's
def get_predictions(today: bool = Path(description="A boolean flag to retrieve today's predictions or previous predictions"), device_id = Depends(verify_token("access"))):
    with engine.connect() as connection:
        current_date= datetime.now().date()
        predictions = None
        #Only want predictions of the previous 90 days max (this number can be edited too)
        max_days = 90
        #Each specific phone will only get their own predictions via the device_id filtering

        #Case where today's predictions must be retuned
        if today:
            predictions = connection.execute(text("""SELECT message, classification, confidence, 
            "timestamp" FROM prediction WHERE DATE("timestamp") = :current_date 
            AND device_id = :device_id"""), 
            {
            "current_date": current_date, 
            "device_id":device_id
            })
        #Case where previous predictions must be returned
        else:
            predictions = connection.execute(text("""SELECT message, classification, confidence, 
            "timestamp" FROM prediction WHERE DATE("timestamp") < :current_date 
            AND device_id = :device_id AND DATE("timestamp") > 
            :current_date - :max_days * INTERVAL '1 day' """), 
            {
            "current_date": current_date, 
            "device_id":device_id,
            "max_days": max_days
            })
        results = []
        rows = predictions.fetchall()

        for row in rows:
            results.append({"Classification": row.classification, 
                            "Confidence": row.confidence, 
                            "Timestamp": row.timestamp, 
                            "Message": row.message})


        return {"Predictions": results}    

@app.get("/statistics/")
def statistics(device_id = Depends(verify_token("access"))):
    with engine.connect() as connection:
        spam_count = connection.execute(text("""SELECT COUNT(*) FROM prediction WHERE 
        classification = :classification AND device_id = :device_id"""), 
        {
            "classification": "spam",
            "device_id" : device_id
        }).scalar()
        ham_count = connection.execute(text("""SELECT COUNT(*) FROM prediction WHERE 
        classification = :classification AND device_id = :device_id"""), 
        {
            "classification": "ham",
            "device_id" : device_id
        }).scalar()
        total_messages = ham_count + spam_count
        spam_percentage = (spam_count/total_messages) * 100 if total_messages > 0 else 0

        #Obtains the number of spam today
        today_spam_count = connection.execute(text("""SELECT COUNT(*) FROM prediction 
        WHERE DATE(timestamp) = CURRENT_DATE AND device_id = :device_id 
        AND classification = :classification"""), 
        {
            "device_id": device_id,
            "classification": "spam"

        }).scalar()

        #Obtains the number of spam this week
        week_spam_count = connection.execute(text("""SELECT COUNT(*) FROM prediction 
        WHERE DATE("timestamp") >= DATE_TRUNC('week', CURRENT_DATE) AND device_id = :device_id 
        AND classification = :classification"""), #DATE_TRUNC('week', CURRENT_DATE) is the week's Monday date
        {
            "device_id": device_id,
            "classification": "spam"

        }).scalar()

        #Obtains the number of spam this month
        month_spam_count = connection.execute(text("""SELECT COUNT(*) FROM prediction 
        WHERE DATE(timestamp) >= DATE_TRUNC('month', CURRENT_DATE) AND device_id = :device_id 
        AND classification = :classification"""), #DATE_TRUNC('month', CURRENT_DATE) is the month's beginning day
        {
            "device_id": device_id,
            "classification": "spam"

        }).scalar()

        #Will be used in bar chart showing the spam distribution per day for the week
        weekly_spam_distribution = connection.execute(text("""
        SELECT COUNT(*) AS "Count", DATE("timestamp") as "Date" FROM 
        prediction WHERE DATE("timestamp") >= 
        DATE_TRUNC('week', CURRENT_DATE) AND device_id = :device_id 
        AND classification = :classification GROUP BY DATE("timestamp") 
        ORDER BY DATE("timestamp") DESC
        """),
        {
            "device_id": device_id,
            "classification": "spam"

        }).fetchall()

        weekly_spam_distribution_data = []
        for row in weekly_spam_distribution:
            weekly_spam_distribution_data.append(
                {"Count": row.Count,
                "Date": str(row.Date)
                }
            )

        #Obtaining average confidence score across all predictions 
        avg_confidence_all = connection.execute(text("""
        SELECT AVG(confidence) FROM prediction WHERE device_id = :device_id"""),
        {
            "device_id": device_id
        }).scalar()

        #This is the average confidence score specifically for spam texts
        avg_confidence_spam = connection.execute((text("""
        SELECT AVG(confidence) FROM prediction WHERE classification = :classification
        AND device_id = :device_id""")),
        {
            "device_id": device_id,
            "classification": "spam"
        }).scalar()

        #Retrieves the number of feedback given per week
        num_feedback_given = connection.execute((text("""
        SELECT COUNT(*) FROM feedback JOIN prediction ON prediction.id = feedback.prediction_id
        WHERE DATE(feedback."timestamp") >= DATE_TRUNC('week', CURRENT_DATE) AND device_id = :device_id""")), 
        {
            "device_id": device_id
        }).scalar()

        return {
            "Spam Count": spam_count,
            "Ham Count": ham_count,
            "Total Messages": total_messages,
            "Spam Percentage": spam_percentage,
            "Today Spam Count": today_spam_count,
            "Week Spam Count": week_spam_count,
            "Month Spam Count": month_spam_count,
            "Weekly Spam Distribution": weekly_spam_distribution_data,
            "Average Confidence All": avg_confidence_all,
            "Average Confidence Spam": avg_confidence_spam,
            "Feedback Count": num_feedback_given
        }

#An exception handler that catches anything else that isn't an HTTP exception
@app.exception_handler(Exception)
async def handle_exception(request: Request, exception: Exception):
    print(exception) #Logs it to the server
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={"message": "Oops, something went wrong. Please report to developper"},
    )

#created the access and refresh tokens
@app.post("/register")
def register():
    #device_id is a uuid (unique string)
    device_id = str(uuid.uuid4())
    access_token = create_token(data={"device_id": device_id, "type": "access"}, expires_delta = timedelta(minutes=30))
    refresh_token = create_token(data={"device_id": device_id, "type": "refresh"}, expires_delta = timedelta(days=15))

    with engine.connect() as connection:
        connection.execute(text("""INSERT INTO device_refresh_token(device_id, refresh_token) 
        VALUES(:device_id, :refresh_token)"""), 
        {"device_id": device_id, "refresh_token": refresh_token})

        connection.commit()

    #Returning both access_tokens and refresh_tokens to the client
    return {"refresh_token": refresh_token, "access_token": access_token}

#This is when the access token expires
@app.post("/refresh")
def refresh(device_id = Depends(verify_token("refresh"))):
    credential_exception = HTTPException(status_code=status.HTTP_401_UNAUTHORIZED, detail="Could not validate user")
    with engine.connect() as connection:
        #Deleting the associated refresh token

        exists = connection.execute(text("""
        SELECT refresh_token FROM device_refresh_token where device_id = :device_id
        """), {"device_id": device_id}).scalar() 
        if exists is None:
            raise credential_exception
        
        connection.execute(text("""
        DELETE FROM device_refresh_token WHERE device_id = :device_id
        """), {"device_id": device_id})

        #Not calling register internally since a device_id is also generated, which we don't want
        access_token = create_token(data={"device_id": device_id, "type": "access"}, expires_delta= timedelta(minutes=30))
        refresh_token = create_token(data={"device_id": device_id, "type": "refresh"}, expires_delta = timedelta(days=15))

        connection.execute(text("""INSERT INTO device_refresh_token(device_id, refresh_token) 
        VALUES(:device_id, :refresh_token)"""), 
        {"device_id": device_id, "refresh_token": refresh_token})

        connection.commit()
        #Returning the new access_token to the client
        return {"refresh_token": refresh_token, "access_token": access_token}