from fastapi import Request
from slowapi.util import get_remote_address
from slowapi import Limiter
from jose import jwt

import auth

#Simply returns the device_id from the header. If device_id isn't present
#Then falls back to the ip_address
def get_device_header(request: Request):
    auth_header = request.headers.get("Authorization")
    if auth_header and auth_header.startswith("Bearer "):
        token = auth_header[7:]
        try:
            payload = jwt.decode(token, auth.SECRET_KEY, [auth.ALGORITHM])
            device_id = payload.get("device_id")
            if device_id:
                return device_id
        except Exception:
            pass
    return get_remote_address(request)


#limiter wil be imported into both auth.py and main.py
limiter = Limiter(key_func=get_device_header)
