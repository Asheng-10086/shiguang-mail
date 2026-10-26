(() => {
  const TOKEN_KEY = "shiguang-token";
  const PAYMENT_RESULT_KEY = "shiguang-payment-result";
  const CHAT_URL = "http://localhost:8000/chat";
  const state = {
    products: [],
    seckillGoods: [],
    currentProduct: null,
    cart: [],
    addresses: [],
    selectedAddressId: "",
    editingAddressId: null,
    orders: [],
    orderFilter: "all",
    paymentOrderSnapshot: null,
    paymentWindow: null,
    paymentSyncPromise: null,
    toastTimer: null
  };

  const elements = {
    productGrid: document.querySelector("#product-grid"),
    resultsCaption: document.querySelector("#results-caption"),
    searchInput: document.querySelector("#search-input"),
    categoryId: document.querySelector("#category-id"),
    cartCount: document.querySelector("#cart-count"),
    accountButton: document.querySelector("#account-button"),
    profileButton: document.querySelector("#profile-button"),
    adminButton: document.querySelector("#admin-button"),
    addressButton: document.querySelector("#address-button"),
    logoutButton: document.querySelector("#logout-button"),
    productDialog: document.querySelector("#product-dialog"),
    loginDialog: document.querySelector("#login-dialog"),
    cartDialog: document.querySelector("#cart-dialog"),
    ordersDialog: document.querySelector("#orders-dialog"),
    profileDialog: document.querySelector("#profile-dialog"),
    adminDialog: document.querySelector("#admin-dialog"),
    addressesDialog: document.querySelector("#addresses-dialog"),
    toast: document.querySelector("#toast")
  };

  const statusLabels = {
    "-1": "已取消",
    "0": "待付款",
    "1": "已付款 · 待发货",
    "2": "待收货",
    "3": "已完成"
  };

  async function request(path, options = {}) {
    const headers = new Headers(options.headers || {});
    const token = localStorage.getItem(TOKEN_KEY);
    if (token) headers.set("token", token);
    if (options.body && !(options.body instanceof URLSearchParams) && typeof options.body !== "string") {
      headers.set("Content-Type", "application/json");
      options.body = JSON.stringify(options.body);
    }
    const response = await fetch(path, { ...options, headers });
    const text = await response.text();
    let result;
    try {
      result = text ? JSON.parse(text) : null;
    } catch {
      throw new Error("服务器返回了无法识别的数据");
    }
    const resultCode = result?.Num ?? result?.num;
    if (!response.ok) {
      if (response.status === 401) {
        localStorage.removeItem(TOKEN_KEY);
        updateAccountButton();
      }
      throw new Error(result?.msg || `请求失败（${response.status}）`);
    }
    if (resultCode != null && Number(resultCode) !== 0) {
      if (result.msg === "NOT_LOGIN") {
        localStorage.removeItem(TOKEN_KEY);
        updateAccountButton();
      }
      throw new Error(result.msg || "操作失败");
    }
    return result ? result.data : null;
  }

  function tokenClaims() {
    const token = localStorage.getItem(TOKEN_KEY);
    if (!token) return null;
    try {
      const payload = token.split(".")[1].replace(/-/g, "+").replace(/_/g, "/");
      return JSON.parse(atob(payload));
    } catch {
      localStorage.removeItem(TOKEN_KEY);
      return null;
    }
  }

  function currentUserId() {
    const claims = tokenClaims();
    return claims?.id ?? claims?.sub ?? null;
  }

  function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, (char) => ({
      "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    })[char]);
  }

  function formatPrice(value) {
    const amount = Number(value);
    return Number.isFinite(amount) ? amount.toFixed(2) : "0.00";
  }

  function productSymbol(product) {
    const text = `${product?.name || ""} ${product?.detail || ""}`.toLowerCase();
    if (/手机|数码|耳机|电脑|电子|phone|digital/.test(text)) return "⌁";
    if (/杯|壶|水具|咖啡|茶/.test(text)) return "♨";
    if (/衣|服|包|鞋|穿搭/.test(text)) return "◒";
    if (/花|绿植|家居|摆件|装饰/.test(text)) return "✿";
    if (/护肤|美妆|香水|面膜/.test(text)) return "✧";
    if (/书|文具|笔记/.test(text)) return "▤";
    return "✳";
  }

  function toneClass(index) {
    return `tone-${index % 5}`;
  }

  function productCard(product, index) {
    const symbol = productSymbol(product);
    const description = product.detail || "认真挑选，为日常添一点好心情。";
    return `<article class="product-card" tabindex="0" data-product-id="${escapeHtml(product.id)}" aria-label="查看商品 ${escapeHtml(product.name || "商品")}">
      <div class="product-art ${toneClass(index)}"><span class="product-symbol" aria-hidden="true">${symbol}</span><span class="view-label">查看详情 ↗</span></div>
      <div class="product-meta">
        <h3>${escapeHtml(product.name || "拾光好物")}</h3>
        <p>${escapeHtml(description)}</p>
        <div class="product-bottom"><span class="product-price"><small>¥</small>${formatPrice(product.price)}</span>
          <button class="add-button" data-add-id="${escapeHtml(product.id)}" aria-label="将${escapeHtml(product.name || "商品")}加入购物车">+</button>
        </div>
      </div>
    </article>`;
  }

  function renderProducts(products, caption) {
    state.products = Array.isArray(products)
      ? products.filter((product) => Number(product.status) === 1)
      : [];
    elements.resultsCaption.textContent = caption;
    if (!state.products.length) {
      elements.productGrid.innerHTML = `<div class="empty-state"><span aria-hidden="true">✳</span><strong>还没有找到合适的好物</strong><span>试试其他关键词，或查看全部商品。</span></div>`;
      return;
    }
    elements.productGrid.innerHTML = state.products.map(productCard).join("");
  }

  function renderSeckillGoods(goodsList) {
    state.seckillGoods = Array.isArray(goodsList) ? goodsList : [];
    const grid = document.querySelector("#seckill-grid");
    const caption = document.querySelector("#seckill-caption");

    if (!state.seckillGoods.length) {
      caption.textContent = "暂时没有进行中或即将开始的活动。";
      grid.innerHTML = `<div class="empty-state"><strong>暂无秒杀活动</strong><span>有新活动时会在这里展示。</span></div>`;
      return;
    }

    caption.textContent = "限时限量，活动结束后恢复日常价格。";
    const now = Date.now();
    grid.innerHTML = state.seckillGoods.map((item) => {
      const startTime = new Date(item.startTime);
      const endTime = new Date(item.endTime);
      const validTimes = Number.isFinite(startTime.getTime()) && Number.isFinite(endTime.getTime());
      const upcoming = validTimes && now < startTime.getTime();
      const ended = validTimes && now > endTime.getTime();
      const stock = Number(item.stock);
      const soldOut = !Number.isFinite(stock) || stock <= 0;
      const status = !validTimes ? "时间信息无效" : ended ? "已结束" : soldOut ? "已抢光" : upcoming ? "即将开始" : "抢购中";
      const disabled = !validTimes || ended || soldOut || upcoming;
      const timeText = validTimes
        ? `${upcoming ? "开始" : "结束"}：${(upcoming ? startTime : endTime).toLocaleString()}`
        : "活动时间暂不可用";
      const productName = item.productName || `商品 ${item.goodsId}`;
      return `<article class="seckill-card">
        <div class="seckill-card-art"><span aria-hidden="true">${productSymbol({ name: productName, detail: item.productDetail })}</span><strong>限时秒杀</strong></div>
        <div class="seckill-card-content">
          <span class="seckill-state">${escapeHtml(status)}</span>
          <h3>${escapeHtml(productName)}</h3>
          <p>${escapeHtml(item.productDetail || "限时特惠，数量有限。")}</p>
          <div class="seckill-prices"><strong>¥${formatPrice(item.seckillPrice)}</strong><del>¥${formatPrice(item.productPrice)}</del></div>
          <small class="seckill-time">${escapeHtml(timeText)}</small>
          <small class="seckill-stock">秒杀库存：${escapeHtml(Number.isFinite(stock) ? stock : "—")}</small>
          <button class="primary-button seckill-buy-button" type="button" data-seckill-id="${escapeHtml(item.id)}" ${disabled ? "disabled" : ""}>${upcoming ? "未开始" : soldOut ? "已抢光" : ended ? "已结束" : "立即秒杀"}</button>
        </div>
      </article>`;
    }).join("");
  }

  async function loadSeckillGoods() {
    const grid = document.querySelector("#seckill-grid");
    grid.innerHTML = `<div class="loading-state"><span class="loader"></span>正在加载秒杀活动…</div>`;
    try {
      renderSeckillGoods(await request("/seckillGoods"));
    } catch (error) {
      state.seckillGoods = [];
      document.querySelector("#seckill-caption").textContent = "秒杀活动暂时无法加载。";
      grid.innerHTML = `<div class="empty-state"><strong>加载失败</strong><span>${escapeHtml(error.message)}</span><button class="secondary-button" data-action="retry-seckill">重新加载</button></div>`;
    }
  }

  async function loadProducts() {
    elements.productGrid.innerHTML = `<div class="loading-state"><span class="loader"></span>正在为你挑选好物…</div>`;
    try {
      const products = await request("/products");
      renderProducts(products, "为日常，挑一件喜欢的。");
    } catch (error) {
      renderProducts([], "商品暂时无法加载。");
      elements.productGrid.innerHTML = `<div class="empty-state"><strong>商品加载失败</strong><span>${escapeHtml(error.message)}，请检查后端服务后重试。</span><button class="secondary-button" data-action="retry-products">重新加载</button></div>`;
    }
  }

  async function searchProducts(query) {
    const value = query.trim();
    if (!value) return loadProducts();
    elements.productGrid.innerHTML = `<div class="loading-state"><span class="loader"></span>正在搜索「${escapeHtml(value)}」…</div>`;
    try {
      const products = await request(`/searchProduct/${encodeURIComponent(value)}`, { method: "POST" });
      renderProducts(products, `搜索「${value}」的结果`);
    } catch (error) {
      renderProducts([], `搜索「${value}」失败`);
      showToast(error.message);
    }
  }

  async function filterCategory(categoryId) {
    const id = categoryId.trim();
    if (!id) return loadProducts();
    elements.productGrid.innerHTML = `<div class="loading-state"><span class="loader"></span>正在筛选分类…</div>`;
    try {
      const products = await request(`/category/${encodeURIComponent(id)}`);
      renderProducts(products, `分类 ${id} 的好物`);
    } catch (error) {
      renderProducts([], "分类筛选暂时不可用。");
      showToast(error.message);
    }
  }

  function openDialog(element) {
    element.hidden = false;
    document.body.classList.add("dialog-open");
    const focusTarget = element.querySelector("input, button:not(.modal-close)");
    if (focusTarget) focusTarget.focus({ preventScroll: true });
  }

  function closeDialog(element) {
    element.hidden = true;
    if (![elements.productDialog, elements.loginDialog, elements.cartDialog, elements.ordersDialog, elements.profileDialog, elements.adminDialog, elements.addressesDialog].some((dialog) => !dialog.hidden)) {
      document.body.classList.remove("dialog-open");
    }
  }

  function showToast(message) {
    elements.toast.textContent = message;
    elements.toast.classList.add("visible");
    clearTimeout(state.toastTimer);
    state.toastTimer = setTimeout(() => elements.toast.classList.remove("visible"), 2800);
  }

  function requireLogin() {
    if (localStorage.getItem(TOKEN_KEY)) return true;
    openDialog(elements.loginDialog);
    showToast("请先登录后继续操作");
    return false;
  }

  function updateAccountButton() {
    const claims = tokenClaims();
    const loggedIn = Boolean(claims);
    const isAdmin = claims?.role === "ADMIN";
    elements.accountButton.hidden = loggedIn;
    elements.profileButton.hidden = !loggedIn;
    elements.adminButton.hidden = !isAdmin;
    elements.addressButton.hidden = !loggedIn;
    elements.logoutButton.hidden = !loggedIn;
  }

  async function openProduct(productId) {
    try {
      const product = await request(`/products/${encodeURIComponent(productId)}`);
      if (!product) throw new Error("商品不存在");
      state.currentProduct = product;
      document.querySelector("#detail-name").textContent = product.name || "拾光好物";
      document.querySelector("#detail-description").textContent = product.detail || "认真挑选，为日常添一点好心情。";
      document.querySelector("#detail-price").textContent = `¥${formatPrice(product.price)}`;
      document.querySelector("#detail-stock").textContent = `库存 ${product.stock ?? "—"} 件 · 已售 ${product.sale ?? "—"} 件`;
      document.querySelector("#detail-art").innerHTML = `<span class="product-symbol" aria-hidden="true">${productSymbol(product)}</span>`;
      openDialog(elements.productDialog);
    } catch (error) {
      showToast(error.message);
    }
  }

  async function addToCart(product) {
    if (!requireLogin()) return;
    try {
      await request("/addToCart", {
        method: "POST",
        body: new URLSearchParams({
          productId: String(product.id),
          productName: String(product.name || "拾光好物"),
          productPrice: String(product.price ?? 0),
          quantity: "1",
          checked: "0"
        })
      });
      showToast("已放入购物车，喜欢的好物先收好");
      await loadCart(false);
    } catch (error) {
      showToast(error.message);
    }
  }

  function renderAddressOptions() {
    const selects = [
      document.querySelector("#checkout-address-select"),
      document.querySelector("#seckill-address-select")
    ].filter(Boolean);
    if (!state.addresses.length) {
      selects.forEach((select) => {
        select.innerHTML = `<option value="">请先添加收货地址</option>`;
      });
      state.selectedAddressId = "";
    } else {
      const selectedAddress = state.addresses.find((address) => String(address.id) === String(state.selectedAddressId));
      if (!selectedAddress) {
        const defaultAddress = state.addresses.find((address) => Number(address.isDefault) === 1);
        state.selectedAddressId = String((defaultAddress || state.addresses[0]).id);
      }
      const options = state.addresses.map((address) => {
        const location = [address.province, address.city, address.district, address.detail].filter(Boolean).join("");
        const label = `${address.recipient} · ${address.phone} · ${location}${Number(address.isDefault) === 1 ? "（默认）" : ""}`;
        return `<option value="${escapeHtml(address.id)}" ${String(address.id) === String(state.selectedAddressId) ? "selected" : ""}>${escapeHtml(label)}</option>`;
      }).join("");
      selects.forEach((select) => {
        select.innerHTML = options;
        select.value = state.selectedAddressId;
      });
    }
    const hasSelectedItems = state.cart.some((item) => Number(item.checked) === 0);
    const checkoutButton = document.querySelector("#checkout-button");
    if (checkoutButton) checkoutButton.disabled = !hasSelectedItems || !state.selectedAddressId;
  }

  function renderCart() {
    const totalQuantity = state.cart.reduce((total, item) => total + (Number(item.quantity) || 0), 0);
    elements.cartCount.textContent = String(totalQuantity);
    const content = document.querySelector("#cart-content");
    const selectedItems = state.cart.filter((item) => Number(item.checked) === 0);
    const total = selectedItems.reduce((sum, item) => sum + (Number(item.productPrice) || 0) * (Number(item.quantity) || 0), 0);
    document.querySelector("#cart-total").textContent = `¥${formatPrice(total)}`;
    document.querySelector("#clear-cart").disabled = state.cart.length === 0;
    document.querySelector("#checkout-button").disabled = selectedItems.length === 0 || !state.selectedAddressId;
    if (!state.cart.length) {
      content.innerHTML = `<div class="empty-state"><span aria-hidden="true">♧</span><strong>购物车还是空的</strong><span>去发现一件让你心动的好物吧。</span></div>`;
      return;
    }
    content.innerHTML = state.cart.map((item) => `<article class="cart-row">
      <label class="cart-select"><input type="checkbox" data-cart-select="${escapeHtml(item.id)}" ${Number(item.checked) === 0 ? "checked" : ""} aria-label="选择${escapeHtml(item.productName || "商品")}"></label>
      <div class="cart-thumb" aria-hidden="true">${productSymbol({ name: item.productName })}</div>
      <div><h3>${escapeHtml(item.productName || "拾光好物")}</h3><p>¥${formatPrice(item.productPrice)}</p>
        <div class="quantity-control"><button data-quantity="-1" data-cart-id="${escapeHtml(item.id)}" aria-label="减少数量">−</button><span>${Number(item.quantity) || 0}</span><button data-quantity="1" data-cart-id="${escapeHtml(item.id)}" aria-label="增加数量">+</button></div>
      </div>
      <strong class="cart-line-total">¥${formatPrice((Number(item.productPrice) || 0) * (Number(item.quantity) || 0))}</strong>
    </article>`).join("");
  }

  async function loadCart(openAfterLoad = true) {
    if (!requireLogin()) return;
    try {
      state.cart = await request("/cart") || [];
      renderCart();
      if (openAfterLoad) openDialog(elements.cartDialog);
      try {
        const addresses = await request("/addresses");
        state.addresses = Array.isArray(addresses) ? addresses : [];
        renderAddressOptions();
      } catch (error) {
        showToast(error.message);
      }
    } catch (error) {
      showToast(error.message);
    }
  }

  function renderAddresses() {
    const content = document.querySelector("#addresses-content");
    if (!state.addresses.length) {
      content.innerHTML = `<div class="empty-state"><span aria-hidden="true">⌖</span><strong>还没有收货地址</strong><span>添加地址后即可在结算时选择。</span><button class="secondary-button" type="button" data-action="new-address">新增收货地址</button></div>`;
      return;
    }
    content.innerHTML = state.addresses.map((address) => `<article class="address-card">
      <div class="address-card-main">
        <div class="address-card-recipient">
          <strong>${escapeHtml(address.recipient)}</strong>
          <span>${escapeHtml(address.phone)}</span>
          ${Number(address.isDefault) === 1 ? '<span class="address-default-badge">默认</span>' : ""}
        </div>
        <p>${escapeHtml([address.province, address.city, address.district, address.detail].filter(Boolean).join(""))}</p>
      </div>
      <div class="address-card-actions">
        ${Number(address.isDefault) !== 1 ? `<button type="button" data-address-default="${escapeHtml(address.id)}">设为默认</button>` : ""}
        <button type="button" data-address-edit="${escapeHtml(address.id)}">编辑</button>
        <button class="address-delete" type="button" data-address-delete="${escapeHtml(address.id)}">删除</button>
      </div>
    </article>`).join("");
  }

  async function loadAddresses(openAfterLoad = true) {
    if (!requireLogin()) return;
    try {
      state.addresses = await request("/addresses") || [];
      if (!Array.isArray(state.addresses)) state.addresses = [];
      renderAddresses();
      renderAddressOptions();
      if (openAfterLoad) openDialog(elements.addressesDialog);
    } catch (error) {
      showToast(error.message);
    }
  }

  function startAddressForm(address = null) {
    const form = document.querySelector("#address-form");
    state.editingAddressId = address?.id ?? null;
    form.reset();
    document.querySelector("#address-form-title").textContent = address ? "编辑收货地址" : "新增收货地址";
    document.querySelector("#address-recipient").value = address?.recipient || "";
    document.querySelector("#address-phone").value = address?.phone || "";
    document.querySelector("#address-province").value = address?.province || "";
    document.querySelector("#address-city").value = address?.city || "";
    document.querySelector("#address-district").value = address?.district || "";
    document.querySelector("#address-detail").value = address?.detail || "";
    document.querySelector("#address-is-default").checked = Number(address?.isDefault) === 1;
    form.hidden = false;
    document.querySelector("#address-recipient").focus({ preventScroll: true });
  }

  function cancelAddressForm() {
    document.querySelector("#address-form").reset();
    document.querySelector("#address-form").hidden = true;
    state.editingAddressId = null;
  }

  async function saveAddress(event) {
    event.preventDefault();
    if (!requireLogin()) return;
    const address = {
      recipient: document.querySelector("#address-recipient").value.trim(),
      phone: document.querySelector("#address-phone").value.trim(),
      province: document.querySelector("#address-province").value.trim(),
      city: document.querySelector("#address-city").value.trim(),
      district: document.querySelector("#address-district").value.trim(),
      detail: document.querySelector("#address-detail").value.trim(),
      isDefault: document.querySelector("#address-is-default").checked ? 1 : 0
    };
    if (!/^1\d{10}$/.test(address.phone)) {
      showToast("请输入有效的 11 位手机号");
      return;
    }
    try {
      if (state.editingAddressId == null) {
        await request("/addresses", { method: "POST", body: address });
        showToast("收货地址已添加");
      } else {
        await request(`/addresses/${encodeURIComponent(state.editingAddressId)}`, { method: "PUT", body: address });
        showToast("收货地址已更新");
      }
      cancelAddressForm();
      await loadAddresses(false);
    } catch (error) {
      showToast(error.message);
    }
  }

  async function deleteAddress(addressId) {
    const address = state.addresses.find((item) => String(item.id) === String(addressId));
    if (!address || !window.confirm(`确定删除 ${address.recipient} 的收货地址吗？`)) return;
    try {
      await request(`/addresses/${encodeURIComponent(addressId)}`, { method: "DELETE" });
      if (String(state.selectedAddressId) === String(addressId)) state.selectedAddressId = "";
      showToast("收货地址已删除");
      await loadAddresses(false);
    } catch (error) {
      showToast(error.message);
    }
  }

  async function setDefaultAddress(addressId) {
    try {
      await request(`/addresses/${encodeURIComponent(addressId)}/default`, { method: "PUT" });
      showToast("默认地址已更新");
      await loadAddresses(false);
    } catch (error) {
      showToast(error.message);
    }
  }

  async function changeQuantity(cartId, difference) {
    const item = state.cart.find((cartItem) => String(cartItem.id) === String(cartId));
    if (!item) return;
    const quantity = (Number(item.quantity) || 0) + difference;
    if (quantity <= 0) {
      showToast("数量不能少于 1 件");
      return;
    }
    try {
      await request("/cart", { method: "PUT", body: new URLSearchParams({ id: String(item.id), quantity: String(quantity) }) });
      state.cart = state.cart.map((cartItem) => String(cartItem.id) === String(cartId) ? { ...cartItem, quantity } : cartItem);
      renderCart();
    } catch (error) {
      showToast(error.message);
    }
  }

  async function setCartSelection(cartId, selected) {
    const item = state.cart.find((cartItem) => String(cartItem.id) === String(cartId));
    if (!item) return;
    try {
      await request("/cart", {
        method: "PUT",
        body: new URLSearchParams({
          id: String(item.id),
          checked: selected ? "0" : "1"
        })
      });
      state.cart = state.cart.map((cartItem) => String(cartItem.id) === String(cartId)
        ? { ...cartItem, checked: selected ? 0 : 1 }
        : cartItem);
      renderCart();
    } catch (error) {
      showToast(error.message);
      await loadCart(false);
    }
  }

  async function clearCart() {
    if (!state.cart.length || !requireLogin()) return;
    try {
      await request("/delcart", { method: "DELETE" });
      state.cart = [];
      renderCart();
      showToast("购物车已清空");
    } catch (error) {
      showToast(error.message);
    }
  }

  async function loadOrders() {
    if (!requireLogin()) return;
    try {
      const orders = await request("/orders");
      state.orders = Array.isArray(orders) ? orders : [];
      renderOrders();
      openDialog(elements.ordersDialog);
    } catch (error) {
      showToast(error.message);
    }
  }

  async function captureOrderStatuses() {
    const orders = await request("/orders");
    return new Map((Array.isArray(orders) ? orders : []).map((order) => [
      String(order.orderSn || order.id),
      Number(order.status)
    ]));
  }

  async function refreshOrdersAfterPayment() {
    if (!requireLogin()) return;
    if (state.paymentSyncPromise) return state.paymentSyncPromise;
    const previousStatuses = state.paymentOrderSnapshot || new Map();
    state.orderFilter = "all";
    openDialog(elements.ordersDialog);

    state.paymentSyncPromise = (async () => {
      for (let attempt = 0; attempt < 60; attempt += 1) {
        try {
          const orders = await request(`/orders?_=${Date.now()}`, { cache: "no-store" });
          state.orders = Array.isArray(orders) ? orders : [];
          renderOrders();
          const paymentConfirmed = state.orders.some((order) => {
            if (Number(order.status) !== 1) return false;
            const key = String(order.orderSn || order.id);
            return !previousStatuses.has(key) || previousStatuses.get(key) === 0;
          });
          if (paymentConfirmed) {
            state.paymentOrderSnapshot = null;
            showToast("支付已确认，订单状态已更新为已付款 · 待发货");
            return;
          }
        } catch (error) {
          showToast(`订单状态同步失败：${error.message}`);
          return;
        }

        if (attempt < 59) {
          await new Promise((resolve) => window.setTimeout(resolve, 1000));
        }
      }
      state.paymentOrderSnapshot = null;
      showToast("支付已返回，但服务器订单状态尚未更新；请稍后刷新订单");
    })().finally(() => {
      state.paymentSyncPromise = null;
    });
    return state.paymentSyncPromise;
  }

  function renderOrders() {
    const content = document.querySelector("#orders-content");
    const pendingOrders = state.orders.filter((order) => Number(order.status) === 0);
    const visibleOrders = state.orderFilter === "pending" ? pendingOrders : state.orders;
    document.querySelector("#all-orders-count").textContent = String(state.orders.length);
    document.querySelector("#pending-orders-count").textContent = String(pendingOrders.length);
    document.querySelectorAll("[data-filter]").forEach((button) => {
      const active = button.dataset.filter === state.orderFilter;
      button.classList.toggle("active", active);
      button.setAttribute("aria-pressed", String(active));
    });

    if (!visibleOrders.length) {
      content.innerHTML = state.orderFilter === "pending"
        ? `<div class="empty-state"><span aria-hidden="true">◷</span><strong>暂无待付款订单</strong><span>待付款订单会显示在这里；秒杀订单可能需要稍后刷新。</span></div>`
        : `<div class="empty-state"><span aria-hidden="true">▤</span><strong>还没有订单</strong><span>喜欢的好物，正在等你发现。</span></div>`;
      return;
    }

    content.innerHTML = visibleOrders.map((order) => {
      const isPending = Number(order.status) === 0;
      const address = [order.shippingProvince, order.shippingCity, order.shippingDistrict, order.shippingDetail].filter(Boolean).join("");
      return `<article class="order-row${isPending ? " order-row-pending" : ""}">
        <div class="order-main">
          <div class="order-title-line"><h3>订单号：${escapeHtml(order.orderSn || order.id)}</h3><span class="order-status${isPending ? " order-status-pending" : ""}">${escapeHtml(statusLabels[String(order.status)] || "订单处理中")}</span></div>
          <p>${escapeHtml(order.createTime || "下单时间未知")}<br>订单编号 ${escapeHtml(order.id)}${order.shippingRecipient ? `<br>收件人：${escapeHtml(order.shippingRecipient)} · ${escapeHtml(order.shippingPhone || "")}${address ? `<br>${escapeHtml(address)}` : ""}` : ""}</p>
        </div>
        <div class="order-aside"><span class="order-total-label">实付款</span><strong class="order-total">¥${formatPrice(order.payAmount ?? order.totalAmount)}</strong>
        ${isPending ? `<button class="pay-button" data-pay-order="${escapeHtml(order.orderSn || "")}" ${order.orderSn ? "" : "disabled"}>立即支付</button>` : ""}
        ${Number(order.status) === 2 ? `<button class="pay-button" data-confirm-order="${escapeHtml(order.orderSn || "")}">确认收货</button>` : ""}</div>
      </article>`;
    }).join("");
  }

  async function payOrder(orderSn) {
    if (!requireLogin()) return;
    const paymentWindow = window.open("", "_blank");
    try {
      state.paymentWindow = paymentWindow;
      state.paymentOrderSnapshot = await captureOrderStatuses();
      const html = await request(`/order/pay/${encodeURIComponent(orderSn)}`, { method: "POST" });
      if (!paymentWindow) {
        showToast("浏览器阻止了支付窗口，请允许弹出窗口后重试");
        return;
      }
      paymentWindow.document.open();
      paymentWindow.document.write(html);
      paymentWindow.document.close();
    } catch (error) {
      if (paymentWindow) paymentWindow.close();
      showToast(error.message);
    }
  }

  async function confirmReceipt(orderSn) {
    if (!requireLogin()) return;
    if (!window.confirm("确认已经收到这笔订单的商品吗？")) return;
    try {
      await request(`/order/${encodeURIComponent(orderSn)}/confirm-receipt`, { method: "POST" });
      showToast("确认收货成功");
      await loadOrders();
    } catch (error) {
      showToast(error.message);
    }
  }

  async function checkout() {
    if (!requireLogin()) return;
    const addressId = state.selectedAddressId;
    if (!addressId) {
      showToast("请先添加并选择收货地址");
      await loadAddresses(true);
      return;
    }

    const paymentWindow = window.open("", "_blank");
    if (!paymentWindow) {
      showToast("浏览器阻止了支付窗口，请允许弹出窗口后重试");
      return;
    }
    try {
      state.paymentWindow = paymentWindow;
      state.paymentOrderSnapshot = await captureOrderStatuses();
      const paymentHtml = await request(`/order/checkout?addressId=${encodeURIComponent(addressId)}`, { method: "POST" });
      if (typeof paymentHtml !== "string" || !paymentHtml) {
        throw new Error("结算接口未返回支付页面");
      }
      paymentWindow.document.open();
      paymentWindow.document.write(paymentHtml);
      paymentWindow.document.close();
      await loadCart(false);
      closeDialog(elements.cartDialog);
    } catch (error) {
      paymentWindow.close();
      showToast(error.message);
    }
  }

  async function openProfile() {
    if (!requireLogin()) return;
    const userId = currentUserId();
    if (userId == null) {
      localStorage.removeItem(TOKEN_KEY);
      updateAccountButton();
      showToast("登录凭证缺少用户信息，请重新登录");
      return;
    }
    try {
      const user = await request(`/users/${encodeURIComponent(userId)}`);
      document.querySelector("#profile-summary").textContent = `账号状态：${Number(user.status) === 1 ? "正常" : "禁用"} · 角色：${user.role === "ADMIN" ? "管理员" : "普通用户"}`;
      document.querySelector("#profile-username").value = user.username || "";
      document.querySelector("#profile-nickname").value = user.nickname || "";
      document.querySelector("#profile-phone").value = user.phone || "";
      document.querySelector("#old-password").value = "";
      document.querySelector("#new-password").value = "";
      openDialog(elements.profileDialog);
    } catch (error) {
      showToast(error.message);
    }
  }

  async function saveProfile(event) {
    event.preventDefault();
    if (!requireLogin()) return;
    const userId = currentUserId();
    const profile = {
      username: document.querySelector("#profile-username").value.trim() || null,
      nickname: document.querySelector("#profile-nickname").value.trim() || null,
      phone: document.querySelector("#profile-phone").value.trim() || null
    };
    if (!profile.username && !profile.nickname && !profile.phone) {
      showToast("请至少填写一项资料");
      return;
    }
    if (profile.phone && !/^1\d{10}$/.test(profile.phone)) {
      showToast("请输入有效的 11 位手机号");
      return;
    }
    try {
      await request(`/update/${encodeURIComponent(userId)}`, { method: "PUT", body: profile });
      showToast("资料已保存");
      await openProfile();
    } catch (error) {
      showToast(error.message);
    }
  }

  async function changePassword(event) {
    event.preventDefault();
    if (!requireLogin()) return;
    const oldPassword = document.querySelector("#old-password").value;
    const newPassword = document.querySelector("#new-password").value;
    if (newPassword.length < 8) {
      showToast("新密码至少需要 8 位");
      return;
    }
    try {
      await request("/users/change-password", {
        method: "PUT",
        body: { oldPassword, newPassword }
      });
      document.querySelector("#change-password-form").reset();
      showToast("密码修改成功");
    } catch (error) {
      showToast(error.message);
    }
  }

  async function loadAdminUsers() {
    if (tokenClaims()?.role !== "ADMIN") {
      showToast("只有管理员可以管理用户账号");
      return;
    }
    try {
      const users = await request("/users");
      const content = document.querySelector("#admin-users-content");
      if (!Array.isArray(users) || users.length === 0) {
        content.innerHTML = `<div class="empty-state"><strong>暂无用户</strong></div>`;
      } else {
        content.innerHTML = users.map((user) => {
          const enabled = Number(user.status) === 1;
          const isSelf = String(user.id) === String(currentUserId());
          return `<article class="admin-user-row">
            <div><strong>${escapeHtml(user.nickname || user.username || `用户 ${user.id}`)}</strong><small>ID ${escapeHtml(user.id)} · ${escapeHtml(user.phone || "无手机号")} · ${user.role === "ADMIN" ? "管理员" : "普通用户"}</small></div>
            <div><span class="order-status">${enabled ? "正常" : "已禁用"}</span>
              <button class="pay-button" data-user-status="${escapeHtml(user.id)}" data-next-status="${enabled ? "0" : "1"}" ${isSelf && enabled ? "disabled" : ""}>${enabled ? "禁用" : "启用"}</button>
            </div>
          </article>`;
        }).join("");
      }
      openDialog(elements.adminDialog);
    } catch (error) {
      showToast(error.message);
    }
  }

  async function updateUserStatus(userId, status) {
    if (tokenClaims()?.role !== "ADMIN") {
      showToast("只有管理员可以管理用户账号");
      return;
    }
    const action = status === 0 ? "禁用" : "启用";
    if (!window.confirm(`确定要${action}用户 ${userId} 吗？`)) return;
    try {
      await request(`/admin/users/${encodeURIComponent(userId)}/status?status=${status}`, { method: "PUT" });
      showToast(`账号已${action}`);
      await loadAdminUsers();
    } catch (error) {
      showToast(error.message);
    }
  }

  async function addProduct(event) {
    event.preventDefault();
    if (tokenClaims()?.role !== "ADMIN") {
      showToast("只有管理员可以添加商品");
      return;
    }

    const form = event.currentTarget;
    const formData = new FormData(form);
    const submitButton = form.querySelector('[type="submit"]');
    const product = {
      name: String(formData.get("name")).trim(),
      brandId: Number(formData.get("brandId")),
      categoryId: Number(formData.get("categoryId")),
      price: Number(formData.get("price")),
      stock: Number(formData.get("stock")),
      sale: 0,
      detail: String(formData.get("detail")).trim(),
      status: Number(formData.get("status"))
    };

    if (!product.name || !product.detail
      || !Number.isInteger(product.brandId) || product.brandId <= 0
      || !Number.isInteger(product.categoryId) || product.categoryId <= 0
      || !Number.isFinite(product.price) || product.price <= 0
      || !Number.isInteger(product.stock) || product.stock < 0
      || ![0, 1].includes(product.status)) {
      showToast("请检查商品信息后再提交");
      return;
    }

    submitButton.disabled = true;
    try {
      await request("/addProduct", { method: "POST", body: product });
      form.reset();
      form.hidden = true;
      showToast("商品添加成功");
      await loadProducts();
    } catch (error) {
      showToast(error.message);
    } finally {
      submitButton.disabled = false;
    }
  }

  async function loadSeckillProductOptions() {
    const select = document.querySelector("#seckill-goods-id");
    select.disabled = true;
    select.innerHTML = `<option value="">正在加载商品…</option>`;
    try {
      const products = await request("/products");
      const activeProducts = Array.isArray(products)
        ? products.filter((product) => Number(product.status) === 1)
        : [];
      if (activeProducts.length === 0) {
        select.innerHTML = `<option value="">没有可参加秒杀的上架商品</option>`;
        return false;
      }
      select.innerHTML = `<option value="">请选择商品</option>${activeProducts.map((product) =>
        `<option value="${escapeHtml(product.id)}">${escapeHtml(product.name || `商品 ${product.id}`)} · ID ${escapeHtml(product.id)}</option>`
      ).join("")}`;
      return true;
    } catch (error) {
      select.innerHTML = `<option value="">商品列表加载失败</option>`;
      showToast(error.message);
      return false;
    } finally {
      select.disabled = false;
    }
  }

  async function addSeckillGoods(event) {
    event.preventDefault();
    if (tokenClaims()?.role !== "ADMIN") {
      showToast("只有管理员可以添加秒杀活动");
      return;
    }

    const form = event.currentTarget;
    const formData = new FormData(form);
    const submitButton = form.querySelector('[type="submit"]');
    const goodsId = Number(formData.get("goodsId"));
    const seckillPrice = Number(formData.get("seckillPrice"));
    const stock = Number(formData.get("stock"));
    const startTime = String(formData.get("startTime"));
    const endTime = String(formData.get("endTime"));
    const startDate = new Date(startTime);
    const endDate = new Date(endTime);

    if (!Number.isInteger(goodsId) || goodsId <= 0
      || !Number.isFinite(seckillPrice) || seckillPrice <= 0
      || !Number.isInteger(stock) || stock <= 0
      || !startTime || !endTime
      || !Number.isFinite(startDate.getTime()) || !Number.isFinite(endDate.getTime())
      || endDate <= startDate || endDate <= new Date()) {
      showToast("请检查商品、价格、库存和活动时间");
      return;
    }

    submitButton.disabled = true;
    try {
      await request("/addSeckillGoods", {
        method: "POST",
        body: { goodsId, seckillPrice, startTime, endTime, stock }
      });
      form.reset();
      form.hidden = true;
      showToast("秒杀活动已创建");
      await loadSeckillGoods();
    } catch (error) {
      showToast(error.message);
    } finally {
      submitButton.disabled = false;
    }
  }

  async function purchaseSeckill(button) {
    if (!requireLogin()) return;
    if (!state.addresses.length) {
      await loadAddresses(false);
    }
    if (!state.addresses.length) {
      showToast("秒杀前请先添加收货地址");
      openDialog(elements.addressesDialog);
      return;
    }
    const addressId = document.querySelector("#seckill-address-select").value || state.selectedAddressId;
    if (!addressId) {
      showToast("请先选择秒杀订单的收货地址");
      return;
    }
    button.disabled = true;
    try {
      await request(`/seckillProduct?productId=${encodeURIComponent(button.dataset.seckillId)}&addressId=${encodeURIComponent(addressId)}`, { method: "POST" });
      showToast("秒杀请求已提交，订单正在处理中");
      await loadSeckillGoods();
    } catch (error) {
      showToast(error.message);
      await loadSeckillGoods();
    } finally {
      button.disabled = false;
    }
  }

  async function sendCode(button) {
    const phone = document.querySelector("#phone-input").value.trim();
    if (!/^1\d{10}$/.test(phone)) {
      showToast("请输入正确的 11 位手机号");
      return;
    }
    button.disabled = true;
    try {
      const code = await request(`/sendCode?Phone=${encodeURIComponent(phone)}`);
      if (typeof code === "string" && code) {
        document.querySelector("#code-input").value = code;
      }
      let seconds = 60;
      button.textContent = `${seconds}s 后重试`;
      const timer = setInterval(() => {
        seconds -= 1;
        if (seconds <= 0) {
          clearInterval(timer);
          button.disabled = false;
          button.textContent = "获取验证码";
        } else {
          button.textContent = `${seconds}s 后重试`;
        }
      }, 1000);
      showToast(typeof code === "string" && code ? "开发环境验证码已填入，请点击登录" : "验证码已发送，请在两分钟内完成登录");
    } catch (error) {
      button.disabled = false;
      showToast(error.message);
    }
  }

  async function login(event) {
    event.preventDefault();
    const phone = document.querySelector("#phone-input").value.trim();
    const code = document.querySelector("#code-input").value.trim();
    if (!/^1\d{10}$/.test(phone) || !code) {
      showToast("请填写正确的手机号和验证码");
      return;
    }
    const button = document.querySelector("#login-submit");
    button.disabled = true;
    try {
      const token = await request(`/login?Phone=${encodeURIComponent(phone)}&code=${encodeURIComponent(code)}`, {
        method: "POST",
        body: {}
      });
      if (typeof token !== "string" || !token) throw new Error("登录接口未返回有效凭证");
      localStorage.setItem(TOKEN_KEY, token);
      updateAccountButton();
      closeDialog(elements.loginDialog);
      showToast("登录成功，欢迎来到拾光优选");
      await loadAddresses(false);
      if (!elements.cartDialog.hidden) await loadCart(false);
    } catch (error) {
      showToast(error.message);
    } finally {
      button.disabled = false;
    }
  }

  async function adminLogin(event) {
    event.preventDefault();
    const username = document.querySelector("#admin-username").value.trim();
    const password = document.querySelector("#admin-password").value;
    const button = document.querySelector("#admin-login-submit");
    if (!username || !password) {
      showToast("请输入管理员用户名和密码");
      return;
    }
    button.disabled = true;
    try {
      const token = await request("/admin/login", {
        method: "POST",
        body: { username, password }
      });
      if (typeof token !== "string" || !token) throw new Error("管理员登录接口未返回有效凭证");
      localStorage.setItem(TOKEN_KEY, token);
      updateAccountButton();
      closeDialog(elements.loginDialog);
      document.querySelector("#admin-login-form").reset();
      showToast("管理员登录成功");
    } catch (error) {
      showToast(error.message);
    } finally {
      button.disabled = false;
    }
  }

  document.querySelector("#search-form").addEventListener("submit", (event) => {
    event.preventDefault();
    searchProducts(elements.searchInput.value);
  });
  document.querySelector("#category-form").addEventListener("submit", (event) => {
    event.preventDefault();
    filterCategory(elements.categoryId.value);
  });
  document.querySelector("#clear-category").addEventListener("click", () => {
    elements.categoryId.value = "";
    elements.searchInput.value = "";
    loadProducts();
  });
  document.querySelector("#login-form").addEventListener("submit", login);
  document.querySelector("#admin-login-form").addEventListener("submit", adminLogin);
  document.querySelector("#profile-form").addEventListener("submit", saveProfile);
  document.querySelector("#change-password-form").addEventListener("submit", changePassword);
  document.querySelector("#address-form").addEventListener("submit", saveAddress);
  document.querySelector("#admin-product-form").addEventListener("submit", addProduct);
  document.querySelector("#admin-seckill-form").addEventListener("submit", addSeckillGoods);
  [document.querySelector("#checkout-address-select"), document.querySelector("#seckill-address-select")]
    .filter(Boolean)
    .forEach((select) => select.addEventListener("change", (event) => {
      state.selectedAddressId = event.currentTarget.value;
      renderAddressOptions();
    }));
  document.querySelector("#send-code").addEventListener("click", (event) => sendCode(event.currentTarget));

  document.addEventListener("click", (event) => {
    const actionElement = event.target.closest("[data-action]");
    if (actionElement) {
      switch (actionElement.dataset.action) {
        case "browse":
          document.querySelector("#products-section").scrollIntoView({ behavior: "smooth" });
          break;
        case "login":
          openDialog(elements.loginDialog);
          break;
        case "profile":
          openProfile();
          break;
        case "addresses":
          loadAddresses();
          break;
        case "admin-panel":
          loadAdminUsers();
          break;
        case "new-seckill":
          if (tokenClaims()?.role !== "ADMIN") {
            showToast("只有管理员可以添加秒杀活动");
            break;
          }
          loadSeckillProductOptions().then((loaded) => {
            if (loaded) {
              document.querySelector("#admin-seckill-form").hidden = false;
              document.querySelector("#seckill-goods-id").focus();
            }
          });
          break;
        case "cancel-new-seckill":
          document.querySelector("#admin-seckill-form").reset();
          document.querySelector("#admin-seckill-form").hidden = true;
          break;
        case "retry-seckill":
          loadSeckillGoods();
          break;
        case "new-product":
          if (tokenClaims()?.role !== "ADMIN") {
            showToast("只有管理员可以添加商品");
            break;
          }
          document.querySelector("#admin-product-form").hidden = false;
          document.querySelector("#product-name").focus();
          break;
        case "cancel-new-product":
          document.querySelector("#admin-product-form").reset();
          document.querySelector("#admin-product-form").hidden = true;
          break;
        case "show-admin-login":
          document.querySelector("#login-form").hidden = true;
          document.querySelector(".login-hint").hidden = true;
          document.querySelector("#admin-login-form").hidden = false;
          document.querySelector("#admin-username").focus();
          break;
        case "show-user-login":
          document.querySelector("#login-form").hidden = false;
          document.querySelector(".login-hint").hidden = false;
          document.querySelector("#admin-login-form").hidden = true;
          document.querySelector("#phone-input").focus();
          break;
        case "logout":
          localStorage.removeItem(TOKEN_KEY);
          state.cart = [];
          state.addresses = [];
          state.selectedAddressId = "";
          renderCart();
          renderAddressOptions();
          updateAccountButton();
          showToast("已退出登录");
          break;
        case "cart":
          loadCart();
          break;
        case "orders":
          state.orderFilter = "all";
          loadOrders();
          break;
        case "refresh-orders":
          loadOrders();
          break;
        case "filter-orders":
          state.orderFilter = actionElement.dataset.filter === "pending" ? "pending" : "all";
          renderOrders();
          break;
        case "close-product":
          closeDialog(elements.productDialog);
          break;
        case "close-login":
          closeDialog(elements.loginDialog);
          break;
        case "close-cart":
          closeDialog(elements.cartDialog);
          break;
        case "close-orders":
          closeDialog(elements.ordersDialog);
          break;
        case "close-profile":
          closeDialog(elements.profileDialog);
          break;
        case "close-admin":
          closeDialog(elements.adminDialog);
          break;
        case "close-addresses":
          closeDialog(elements.addressesDialog);
          break;
        case "new-address":
          startAddressForm();
          break;
        case "cancel-address-edit":
          cancelAddressForm();
          break;
        case "add-detail":
          if (state.currentProduct) addToCart(state.currentProduct);
          break;
        case "clear-cart":
          clearCart();
          break;
        case "checkout":
          checkout();
          break;
        case "retry-products":
          loadProducts();
          break;
      }
    }

    const addButton = event.target.closest("[data-add-id]");
    if (addButton) {
      event.stopPropagation();
      const product = state.products.find((item) => String(item.id) === String(addButton.dataset.addId));
      if (product) addToCart(product);
      return;
    }
    const quantityButton = event.target.closest("[data-quantity]");
    if (quantityButton) {
      changeQuantity(quantityButton.dataset.cartId, Number(quantityButton.dataset.quantity));
      return;
    }
    const selectionInput = event.target.closest("[data-cart-select]");
    if (selectionInput) {
      setCartSelection(selectionInput.dataset.cartSelect, selectionInput.checked);
      return;
    }
    const payButton = event.target.closest("[data-pay-order]");
    if (payButton) payOrder(payButton.dataset.payOrder);
    const confirmButton = event.target.closest("[data-confirm-order]");
    if (confirmButton) confirmReceipt(confirmButton.dataset.confirmOrder);
    const statusButton = event.target.closest("[data-user-status]");
    if (statusButton) updateUserStatus(statusButton.dataset.userStatus, Number(statusButton.dataset.nextStatus));
    const seckillButton = event.target.closest("[data-seckill-id]");
    if (seckillButton) purchaseSeckill(seckillButton);
    const editAddressButton = event.target.closest("[data-address-edit]");
    if (editAddressButton) {
      const address = state.addresses.find((item) => String(item.id) === String(editAddressButton.dataset.addressEdit));
      if (address) startAddressForm(address);
    }
    const deleteAddressButton = event.target.closest("[data-address-delete]");
    if (deleteAddressButton) deleteAddress(deleteAddressButton.dataset.addressDelete);
    const defaultAddressButton = event.target.closest("[data-address-default]");
    if (defaultAddressButton) setDefaultAddress(defaultAddressButton.dataset.addressDefault);
  });

  window.addEventListener("message", (event) => {
    if (event.origin !== window.location.origin
        || event.source !== state.paymentWindow
        || event.data?.type !== "shiguang-payment-result") {
      return;
    }
    if (event.data.result === "success") {
      refreshOrdersAfterPayment();
    } else if (event.data.result === "failed") {
      showToast("支付验证未通过，订单仍为待付款");
      if (!elements.ordersDialog.hidden) loadOrders();
    }
    state.paymentWindow = null;
  });

  window.addEventListener("storage", (event) => {
    if (event.key !== PAYMENT_RESULT_KEY || !event.newValue || !state.paymentWindow) return;
    let result;
    try {
      result = JSON.parse(event.newValue);
    } catch {
      return;
    }
    if (result?.result === "success") {
      refreshOrdersAfterPayment();
    } else if (result?.result === "failed") {
      showToast("支付验证未通过，订单仍为待付款");
      if (!elements.ordersDialog.hidden) loadOrders();
    }
  });

  document.querySelector("#product-grid").addEventListener("click", (event) => {
    const card = event.target.closest("[data-product-id]");
    if (card && !event.target.closest("[data-add-id]")) openProduct(card.dataset.productId);
  });
  document.querySelector("#product-grid").addEventListener("keydown", (event) => {
    const card = event.target.closest("[data-product-id]");
    if (card && (event.key === "Enter" || event.key === " ")) {
      event.preventDefault();
      openProduct(card.dataset.productId);
    }
  });
  document.querySelectorAll(".modal-backdrop").forEach((backdrop) => {
    backdrop.addEventListener("click", (event) => {
      if (event.target === backdrop) closeDialog(backdrop);
    });
  });
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
      [elements.productDialog, elements.loginDialog, elements.cartDialog, elements.ordersDialog, elements.profileDialog, elements.adminDialog, elements.addressesDialog].forEach((dialog) => {
        if (!dialog.hidden) closeDialog(dialog);
      });
    }
  });

  const chat = {
    panel: document.querySelector("#chat-panel"),
    launcher: document.querySelector("#chat-launcher"),
    messages: document.querySelector("#chat-messages"),
    form: document.querySelector("#chat-form"),
    input: document.querySelector("#chat-input"),
    quick: document.querySelector("#chat-quick"),
    sending: false
  };

  function openChat() {
    chat.panel.hidden = false;
    if (!chat.messages.childElementCount) {
      appendChatMessage("bot", "你好呀，我是拾光优选的智能客服。可以问我商品推荐、秒杀活动、订单、购物车、收货地址等问题～");
    }
    chat.input.focus({ preventScroll: true });
  }

  function closeChat() {
    chat.panel.hidden = true;
  }

  function appendChatMessage(role, text) {
    const bubble = document.createElement("div");
    bubble.className = `chat-bubble chat-${role}`;
    const body = document.createElement("span");
    body.textContent = text || "（未返回内容）";
    bubble.appendChild(body);
    chat.messages.appendChild(bubble);
    chat.messages.scrollTop = chat.messages.scrollHeight;
    return bubble;
  }

  async function sendChatMessage(message) {
    const text = String(message || "").trim();
    if (!text || chat.sending) return;
    appendChatMessage("user", text);
    chat.input.value = "";
    chat.sending = true;

    try {
      const response = await fetch(CHAT_URL, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          userId: currentUserId(),
          question: text,
          token: localStorage.getItem(TOKEN_KEY) || ""
        })
      });
      if (!response.ok) {
        throw new Error(`客服服务返回 ${response.status}`);
      }
      const data = await response.json();
      const reply = data?.reply ?? data?.answer ?? data?.content;
      appendChatMessage("bot", reply || "抱歉，我暂时没有理解你的问题。");
    } catch (error) {
      appendChatMessage("bot", `抱歉，客服暂时不可用（${error.message}）。请稍后再试。`);
    } finally {
      chat.sending = false;
      chat.input.focus({ preventScroll: true });
    }
  }

  chat.launcher.addEventListener("click", openChat);
  document.querySelector("#chat-close").addEventListener("click", closeChat);
  chat.form.addEventListener("submit", (event) => {
    event.preventDefault();
    sendChatMessage(chat.input.value);
  });
  chat.quick.addEventListener("click", (event) => {
    const button = event.target.closest("[data-quick]");
    if (button) sendChatMessage(button.dataset.quick);
  });
  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape" && !chat.panel.hidden) closeChat();
  });

  updateAccountButton();
  renderCart();
  if (tokenClaims()) loadAddresses(false);
  loadProducts();
  loadSeckillGoods();
  window.setInterval(() => {
    if (state.seckillGoods.length) renderSeckillGoods(state.seckillGoods);
  }, 30000);
  const paymentResult = new URLSearchParams(window.location.search).get("payment");
  if (paymentResult === "success") {
    showToast("支付结果已返回，正在同步订单状态");
    localStorage.setItem(PAYMENT_RESULT_KEY, JSON.stringify({
      result: "success",
      timestamp: Date.now()
    }));
    refreshOrdersAfterPayment();
    if (window.opener && !window.opener.closed) {
      window.opener.postMessage({ type: "shiguang-payment-result", result: "success" }, window.location.origin);
    }
  }
  if (paymentResult === "failed") {
    showToast("支付验证未通过，请稍后查看订单状态");
    localStorage.setItem(PAYMENT_RESULT_KEY, JSON.stringify({
      result: "failed",
      timestamp: Date.now()
    }));
    if (!window.opener || window.opener.closed) {
      loadOrders();
    }
    if (window.opener && !window.opener.closed) {
      window.opener.postMessage({ type: "shiguang-payment-result", result: "failed" }, window.location.origin);
    }
  }
})();
