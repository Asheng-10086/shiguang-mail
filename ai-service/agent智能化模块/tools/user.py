import httpx
from langchain.tools import tool

from auth import get_auth_headers
from config import BASE_URL


@tool
def get_cart() -> list:
    """
    查询当前用户的购物车列表。
    当用户询问购物车里有什么商品时调用。

    参数：无参
    返回类型：购物车列表
    """
    resp = httpx.get(f"{BASE_URL}/cart", headers=get_auth_headers(), timeout=10)

    if resp.status_code == 404:
        raise LookupError("未查询到结果")
    if resp.status_code != 200:
        raise LookupError("查询出错")

    data = resp.json()
    return data["data"]


@tool
def get_addresses() -> list:
    """
    查询当前用户的收货地址列表。
    当用户询问地址信息时调用。

    参数：无参
    返回类型：地址列表
    """
    resp = httpx.get(f"{BASE_URL}/addresses", headers=get_auth_headers(), timeout=10)

    if resp.status_code == 404:
        raise LookupError("未查询到结果")
    if resp.status_code != 200:
        raise LookupError("查询出错")

    data = resp.json()
    return data["data"]


@tool
def get_address_by_id(address_id: int) -> dict:
    """
    根据地址ID查询单个收货地址的详细信息。
    当用户询问某个具体地址时调用。

    参数：
        address_id: 地址ID
    返回类型：单个地址
    """
    resp = httpx.get(f"{BASE_URL}/addresses/{address_id}", headers=get_auth_headers(), timeout=10)

    if resp.status_code == 404:
        raise LookupError("未查询到结果")
    if resp.status_code != 200:
        raise LookupError("查询出错")

    data = resp.json()
    return data["data"]


@tool
def delete_address(address_id: int) -> str:
    """
    根据地址ID删除收货地址。
    当用户要求删除某个地址时调用。

    参数：
        address_id: 地址ID
    返回类型：无返回值
    """
    resp = httpx.delete(f"{BASE_URL}/addresses/{address_id}", headers=get_auth_headers(), timeout=10)

    if resp.status_code == 404:
        raise LookupError("未找到该地址")
    if resp.status_code != 200:
        raise LookupError("操作失败")

    return "删除地址成功"


@tool
def get_user_by_id(user_id: int) -> dict:
    """
    根据用户ID查询用户的详细信息。
    当用户询问个人信息（昵称、会员等级等）时调用。

    参数：
        user_id: 用户ID
    返回类型：单个用户
    """
    resp = httpx.get(f"{BASE_URL}/users/{user_id}", headers=get_auth_headers(), timeout=10)

    if resp.status_code == 404:
        raise LookupError("未查询到结果")
    if resp.status_code != 200:
        raise LookupError("查询出错")

    data = resp.json()
    return data["data"]


@tool
def update_by_id(record_id: int) -> str:
    """
    根据ID更新记录。
    当用户要求修改某个记录的信息时调用。

    参数：
        record_id: 记录ID
    返回类型：无返回值
    """
    resp = httpx.put(f"{BASE_URL}/update/{record_id}", headers=get_auth_headers(), timeout=10)

    if resp.status_code == 404:
        raise LookupError("未找到该记录")
    if resp.status_code != 200:
        raise LookupError("操作失败")

    return "更新成功"
