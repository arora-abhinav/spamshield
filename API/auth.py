from fastapi import APIRouter, status, HTTPException, Request, Depends
from datetime import timedelta, datetime
from typing import Annotated
import os
from jose import jwt, JWTError
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
import uuid
from sqlalchemy import text
from database import engine
from rate_limiter import limiter


#Secret Key and algorithm being used to encode our access token
SECRET_KEY = os.getenv("SECRET_KEY")
ACCESS_TOKEN_EXPIRY_MINUTES = 30
ALGORITHM = "HS256"

router = APIRouter()
security = HTTPBearer(auto_error=True)

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


#created the access and refresh tokens
@router.get("/register")
@limiter.limit("5/hour")
def register(request: Request):
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
@router.get("/refresh")
@limiter.limit("10/hour")
def refresh(request: Request, device_id = Depends(verify_token("refresh"))):
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