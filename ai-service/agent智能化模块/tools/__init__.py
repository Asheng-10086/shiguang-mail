from .product import (
    get_products_list,
    get_product_by_id,
    get_products_by_category,
    search_products,
    get_seckill_goods,
)
from .order import (
    get_orders,
    get_order_detail,
    cancel_order,
    confirm_receipt,
)
from .user import (
    get_cart,
    get_addresses,
    get_address_by_id,
    delete_address,
    get_user_by_id,
    update_by_id,
)
from .knowledge import search_knowledge

tools_list = [
    get_products_list,
    get_product_by_id,
    get_products_by_category,
    search_products,
    get_seckill_goods,
    get_orders,
    get_order_detail,
    cancel_order,
    confirm_receipt,
    get_cart,
    get_addresses,
    get_address_by_id,
    delete_address,
    get_user_by_id,
    update_by_id,
    search_knowledge,
]
