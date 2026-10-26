from langchain_deepseek import ChatDeepSeek

import config

# 实例化大模型客户端
llm = ChatDeepSeek(
    model=config.DEEPSEEK_MODEL,
    api_key=config.DEEPSEEK_API_KEY,
    base_url=config.DEEPSEEK_BASE_URL
)
