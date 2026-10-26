import contextvars

current_token = contextvars.ContextVar("current_token", default="")


def get_auth_headers() -> dict:
    token = current_token.get()
    return {"token": token} if token else {}
