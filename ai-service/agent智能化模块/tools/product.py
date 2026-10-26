import httpx
from langchain.tools import tool

from config import BASE_URL


@tool
def get_products_list() -> list:
    """
    get_products_list 的 Docstring
    查询订单列表，返回商品列表给用户
    当用户需要你提供商品列表时候，调用这个工具

    参数：无参
    返回类型：商品列表
    """
    resp = httpx.get(f"{BASE_URL}/products", timeout=10)

    if resp.status_code == 404:
        raise LookupError("未查询到结果")

    if resp.status_code != 200:
        raise LookupError("查询出错")

    data = resp.json()
    return data["data"]


@tool
def get_product_by_id(product_id: int) -> dict:
    """
    根据商品ID查询单个商品的详细信息。
    当用户询问某个具体商品的详情（价格、库存、规格等）时调用。

    参数：
        product_id: 商品ID
    返回类型：单个商品
    """
    resp = httpx.get(f"{BASE_URL}/products/{product_id}", timeout=10)

    if resp.status_code == 404:
        raise LookupError("未查询到结果")
    if resp.status_code != 200:
        raise LookupError("查询出错")

    data = resp.json()
    return data["data"]


@tool
def get_products_by_category(category_id: int) -> list:
    """
    根据商品分类ID查询该分类下的商品列表。
    当用户询问某个分类下有哪些商品时调用。

    参数：
        category_id: 商品分类ID
    返回类型：商品列表
    """
    resp = httpx.get(f"{BASE_URL}/category/{category_id}", timeout=10)

    if resp.status_code == 404:
        raise LookupError("未查询到结果")
    if resp.status_code != 200:
        raise LookupError("查询出错")

    data = resp.json()
    return data["data"]


@tool
def search_products(key_string: str) -> list:
    """
    根据关键字搜索商品，返回匹配的商品列表。
    当用户输入关键字想要查找商品时调用。

    参数：
        key_string: 搜索关键字
    返回类型：商品列表
    """
    resp = httpx.post(f"{BASE_URL}/searchProduct/{key_string}", timeout=10)

    if resp.status_code == 404:
        raise LookupError("未查询到结果")
    if resp.status_code != 200:
        raise LookupError("查询出错")

    data = resp.json()
    return data["data"]


@tool
def get_seckill_goods() -> list:
    """
    查询秒杀活动中的商品列表。
    当用户询问秒杀商品、限时抢购活动时调用。

    参数：无参
    返回类型：秒杀商品列表
    """
    resp = httpx.get(f"{BASE_URL}/seckillGoods", timeout=10)

    if resp.status_code == 404:
        raise LookupError("未查询到结果")
    if resp.status_code != 200:
        raise LookupError("查询出错")

    data = resp.json()
    return data["data"]
