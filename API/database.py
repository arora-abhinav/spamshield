from sqlalchemy import create_engine, text
import os

DATABASE_URL = os.getenv("CONNECTION_STRING") or os.getenv("DATABASE_URL")
engine = create_engine(DATABASE_URL)

import os
print(f"CONNECTION_STRING: {os.getenv('CONNECTION_STRING')}")
print(f"DATABASE_URL: {os.getenv('DATABASE_URL')}")
connection_string = os.getenv("CONNECTION_STRING") or os.getenv("DATABASE_URL")
print(f"Using: {connection_string}")