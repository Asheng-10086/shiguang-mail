from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel

from agent import ai_customer_service
from auth import current_token


class ChatRequest(BaseModel):
    userId: str
    question: str
    token: str


app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:8080"],
    allow_methods=["*"],
    allow_headers=["*"],
    allow_credentials=True
)


@app.post("/chat")
def chat(req: ChatRequest):
    print(req.token)
    current_token.set(req.token)

    result = ai_customer_service.invoke(
        {"messages": [{"role": "user", "content": req.question}]},
        config={"configurable": {"thread_id": req.userId}}
    )

    print(result["messages"])
    return {"answer": result["messages"][-1].content}
