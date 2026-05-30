from typing import Annotated

from fastapi import Depends
from slowapi.util import get_remote_address
from slowapi import Limiter
from jose import jwt

import auth

#Simply returns the device_id from the header. If device_id isn't present
#Then falls back to the ip_address
def get_device_header(header: Annotated[auth.HTTPAuthorizationCredentials, Depends(security_optional)], request: Request):
    try:
        payload = jwt.decode(header.credentials, auth.SECRET_KEY, auth.ALGORITHM)
        device_id = payload.get("device_id")
        if device_id is None:
            return get_remote_address(request)
    except:
        return get_remote_address(request)
    return device_id


#limiter wil be imported into both auth.py and main.py
limiter = Limiter(key_func=get_device_header)
