import os
from dotenv import load_dotenv

load_dotenv()

# 大模型配置（DeepSeek）
DEEPSEEK_API_KEY = os.getenv("DEEPSEEK_API_KEY")
DEEPSEEK_MODEL = "deepseek-chat"
DEEPSEEK_BASE_URL = "https://api.deepseek.com/v1"

# 嵌入模型配置（SiliconFlow）
SILICONFLOW_API_KEY = os.getenv("SILICONFLOW_API_KEY")
SILICONFLOW_BASE_URL = "https://api.siliconflow.cn/v1"
EMBEDDING_MODEL = "Pro/BAAI/bge-m3"

# 电商 Java 后端地址
BASE_URL = "http://localhost:8080"

# 知识库文档路径
KNOWLEDGE_BASE_PATH = "D:/电商项目文件夹/ai-service/agent智能化模块\store_knowledge_base.txt"
