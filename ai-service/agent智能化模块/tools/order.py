import httpx
from langchain.tools import tool

from auth import get_auth_headers
from config import BASE_URL


@tool
def get_orders() -> list:
    """
    查询用户的订单列表。
    当用户询问"我的订单""历史订单"等问题时调用。

    参数：无参
    返回类型：订单列表
    """
    resp = httpx.get(f"{BASE_URL}/orders", headers=get_auth_headers(), timeout=10)

    if resp.status_code == 404:
        raise LookupError("未查询到结果")
    if resp.status_code != 200:
        raise LookupError("查询出错")

    data = resp.json()
    return data["data"]


@tool
def get_order_detail(order_id: int) -> dict:
    """
    根据订单ID查询单个订单的明细。
    当用户询问某个订单的具体内容时调用。

    参数：
        order_id: 订单ID
    返回类型：订单明细
    """
    resp = httpx.get(f"{BASE_URL}/order/{order_id}", headers=get_auth_headers(), timeout=10)

    if resp.status_code == 404:
        raise LookupError("未查询到结果")
    if resp.status_code != 200:
        raise LookupError("查询出错")

    data = resp.json()
    return data["data"]


@tool
def cancel_order(order_sn: str) -> str:
    """
    根据订单号取消订单。
    当用户明确要求取消某个订单时调用。

    参数：
        order_sn: 订单号
    返回类型：无返回值
    """
    resp = httpx.post(f"{BASE_URL}/cancelOrderSn", content=order_sn, headers=get_auth_headers(), timeout=10)

    if resp.status_code == 404:
        raise LookupError("未找到该订单")
    if resp.status_code != 200:
        raise LookupError("操作失败")

    return "取消订单成功"


@tool
def confirm_receipt(order_sn: str) -> str:
    """
    根据订单号确认收货。
    当用户表示已收到货、要求确认收货时调用。

    参数：
        order_sn: 订单号
    返回类型：无返回值
    """
    resp = httpx.post(f"{BASE_URL}/order/{order_sn}/confirm-receipt", headers=get_auth_headers(), timeout=10)

    if resp.status_code == 404:
        raise LookupError("未找到该订单")
    if resp.status_code != 200:
        raise LookupError("操作失败")

    return "确认收货成功"
