from sqlalchemy import create_engine, text
import os

DATABASE_URL = os.getenv("CONNECTION_STRING") or os.getenv("DATABASE_URL")
engine = create_engine(DATABASE_URL)