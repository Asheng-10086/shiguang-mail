from langchain_core.documents import Document
from langchain_openai import OpenAIEmbeddings
from langchain_text_splitters import RecursiveCharacterTextSplitter
from langchain_chroma import Chroma

import config


# 加载文档，使其成为智能客服的知识库
def load_documents(path) -> Document:
    if path is None:
        raise ValueError("文件路径不能为空")

    if not path.endswith(".txt"):
        raise ValueError("文件格式不正确，请提供txt文件")

    with open(path, "r", encoding="utf-8") as file:
        content = file.read()

    return Document(page_content=content, metadata={"source": path})


# 建立向量数据库，并将文档加载到数据库中
document = load_documents(config.KNOWLEDGE_BASE_PATH)

# 对文档再进行细分
splitter = RecursiveCharacterTextSplitter(chunk_size=1000, chunk_overlap=200)
chunks = splitter.split_documents([document])

# 配置嵌入模型
embeddings = OpenAIEmbeddings(
    model=config.EMBEDDING_MODEL,
    api_key=config.SILICONFLOW_API_KEY,
    base_url=config.SILICONFLOW_BASE_URL,
    dimensions=None
)

vector_store = Chroma(
    embedding_function=embeddings,
    collection_name="ai_customer_service"
)

vector_store.add_documents(documents=chunks)

retriever = vector_store.as_retriever(
    search_type="similarity",
    search_kwargs={"k": 1}
)
