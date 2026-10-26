from langchain.tools import tool

from rag import retriever


@tool
def search_knowledge(query: str) -> str:
    """
    从平台知识库中检索相关政策、规则、使用说明等文档内容。
    当用户询问退货政策、运费规则、使用说明等静态知识时调用。

    参数：
        query: 检索的问题或关键词
    返回类型：知识库检索到的相关内容
    """
    docs = retriever.invoke(query)
    if not docs:
        return "知识库中未找到相关内容"
    return "\n\n".join(doc.page_content for doc in docs)
