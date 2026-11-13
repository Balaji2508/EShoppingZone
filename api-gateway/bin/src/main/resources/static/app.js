// EShoppingZone Single Page Application (SPA) Controller
const API_BASE = window.location.origin;

// State
let currentUser = null;
let currentToken = null;
let currentProducts = [];
let currentCart = null;
let selectedCategory = '';
let currentRole = 'GUEST';
let currentMerchantOrders = [];
let currentAdminUsers = [];
let currentAdminProducts = [];
let currentAdminOrders = [];
let currentAuditLogs = [];
let currentMerchantRevenue = null;

// Initialization
document.addEventListener('DOMContentLoaded', () => {
    loadSession();
    loadProducts();
    if (currentUser) {
        syncUserData();
    }
    // Poll notifications every 10 seconds
    setInterval(() => {
        if (currentUser) {
            checkNotifications();
        }
    }, 10000);
});

// Toast Utility
function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    let icon = 'fa-info-circle';
    if (type === 'success') icon = 'fa-circle-check';
    if (type === 'error') icon = 'fa-triangle-exclamation';

    toast.innerHTML = `<i class="fa-solid ${icon}"></i> <span>${message}</span>`;
    container.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = '0';
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}

// Session Management
function loadSession() {
    const savedUser = localStorage.getItem('eshop_user');
    const savedToken = localStorage.getItem('eshop_token');
    if (savedUser && savedToken) {
        currentUser = JSON.parse(savedUser);
        currentToken = savedToken;
        currentRole = currentUser.role || 'CUSTOMER';
        renderAuthUI();
    }
}

function saveSession(user, token) {
    currentUser = user;
    currentToken = token;
    currentRole = user.role || 'CUSTOMER';
    localStorage.setItem('eshop_user', JSON.stringify(user));
    localStorage.setItem('eshop_token', token);
    renderAuthUI();
    syncUserData();
}

function logout() {
    currentUser = null;
    currentToken = null;
    currentRole = 'GUEST';
    localStorage.removeItem('eshop_user');
    localStorage.removeItem('eshop_token');
    renderAuthUI();
    switchView('storefront');
    showToast('Logged out successfully', 'info');
}

function renderAuthUI() {
    const authGuest = document.getElementById('authGuest');
    const authUser = document.getElementById('authUser');
    const userNameDisplay = document.getElementById('userNameDisplay');
    const userEmailDisplay = document.getElementById('userEmailDisplay');
    const userRoleBadge = document.getElementById('userRoleBadge');
    const userRoleTag = document.getElementById('userRoleTag');
    const tabMerchant = document.getElementById('tabMerchant');
    const tabAdmin = document.getElementById('tabAdmin');
    const menuMerchantLink = document.getElementById('menuMerchantLink');
    const menuAdminLink = document.getElementById('menuAdminLink');

    if (currentUser) {
        authGuest.style.display = 'none';
        authUser.style.display = 'flex';
        userNameDisplay.innerText = currentUser.name || 'User';
        userEmailDisplay.innerText = currentUser.email || '';
        userRoleBadge.innerText = currentRole;
        userRoleBadge.className = `role-badge ${currentRole}`;
        userRoleTag.innerText = currentRole;

        tabMerchant.style.display = (currentRole === 'MERCHANT' || currentRole === 'ADMIN') ? 'inline-flex' : 'none';
        tabAdmin.style.display = (currentRole === 'ADMIN') ? 'inline-flex' : 'none';
        menuMerchantLink.style.display = (currentRole === 'MERCHANT' || currentRole === 'ADMIN') ? 'flex' : 'none';
        menuAdminLink.style.display = (currentRole === 'ADMIN') ? 'flex' : 'none';
    } else {
        authGuest.style.display = 'flex';
        authUser.style.display = 'none';
        tabMerchant.style.display = 'none';
        tabAdmin.style.display = 'none';
    }
}

function syncUserData() {
    if (!currentUser) return;
    loadWallet();
    loadCart();
    checkNotifications();
}

function toggleUserMenu() {
    const menu = document.getElementById('userMenu');
    menu.classList.toggle('active');
}

document.addEventListener('click', (e) => {
    const menu = document.getElementById('userMenu');
    const btn = document.querySelector('.btn-user');
    if (menu && btn && !btn.contains(e.target) && !menu.contains(e.target)) {
        menu.classList.remove('active');
    }
});

// View Switcher (Storefront / Merchant / Admin)
function switchView(viewName) {
    document.querySelectorAll('.app-view').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('.portal-tab').forEach(el => el.classList.remove('active'));

    if (viewName === 'storefront') {
        document.getElementById('viewStorefront').classList.add('active');
        document.getElementById('tabStorefront').classList.add('active');
        loadProducts();
    } else if (viewName === 'merchant') {
        document.getElementById('viewMerchant').classList.add('active');
        document.getElementById('tabMerchant').classList.add('active');
        loadMerchantDashboard();
    } else if (viewName === 'admin') {
        document.getElementById('viewAdmin').classList.add('active');
        document.getElementById('tabAdmin').classList.add('active');
        loadAdminDashboard();
    }
}

// -----------------------------------------------------------------------------
// 1. STOREFRONT & PRODUCTS
// -----------------------------------------------------------------------------
async function loadProducts(category = '') {
    const grid = document.getElementById('productGrid');
    const badge = document.getElementById('productCount');
    grid.innerHTML = '<p class="text-muted">Loading products catalog...</p>';

    let url = `${API_BASE}/api/products`;
    if (category) {
        url = `${API_BASE}/api/products/category/${encodeURIComponent(category)}`;
    }

    try {
        const res = await fetch(url);
        const products = await res.json();
        currentProducts = products;
        renderProducts(products);
        badge.innerText = `${products.length} Products Available`;
    } catch (err) {
        grid.innerHTML = '<p class="empty-msg text-danger">Failed to load products from Product Service.</p>';
    }
}

function renderProducts(products) {
    const grid = document.getElementById('productGrid');
    if (!products || products.length === 0) {
        grid.innerHTML = '<p class="empty-msg">No products found.</p>';
        return;
    }

    grid.innerHTML = products.map(p => `
        <div class="product-card">
            <img src="${p.imageUrl || 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400'}" alt="${p.name}" class="product-image" onerror="this.src='https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=400'">
            <div class="product-body">
                <div class="product-category">${p.category}</div>
                <h3 class="product-title">${p.name}</h3>
                <div class="product-merchant-tag"><i class="fa-solid fa-store"></i> Sold by: <strong>${p.merchantName || 'Alice Merchant'}</strong></div>
                <p class="product-desc">${p.description || ''}</p>
                <div class="product-footer">
                    <div>
                        <div class="product-price">₹${p.price.toLocaleString('en-IN')}</div>
                        <span class="stock-tag">${p.stockQuantity > 0 ? `${p.stockQuantity} in stock` : '<span class="text-danger">Out of stock</span>'}</span>
                    </div>
                    <button class="btn btn-primary btn-sm" onclick="addToCart(${p.id}, '${escapeQuote(p.name)}', ${p.price}, '${escapeQuote(p.imageUrl || '')}')" ${p.stockQuantity <= 0 ? 'disabled' : ''}>
                        <i class="fa-solid fa-cart-plus"></i> Add
                    </button>
                </div>
            </div>
        </div>
    `).join('');
}

function filterCategory(cat) {
    selectedCategory = cat;
    document.querySelectorAll('.cat-pill').forEach(btn => btn.classList.remove('active'));
    event.target.classList.add('active');
    document.getElementById('sectionTitle').innerText = cat ? `${cat} Catalog` : 'Explore Products';
    loadProducts(cat);
}

async function searchProducts() {
    const query = document.getElementById('searchInput').value.trim();
    if (!query) {
        loadProducts();
        return;
    }
    const grid = document.getElementById('productGrid');
    grid.innerHTML = '<p class="text-muted">Searching...</p>';

    try {
        const res = await fetch(`${API_BASE}/api/products/search?keyword=${encodeURIComponent(query)}`);
        const results = await res.json();
        currentProducts = results;
        renderProducts(results);
        document.getElementById('productCount').innerText = `${results.length} Search Results`;
        document.getElementById('sectionTitle').innerText = `Search: "${query}"`;
    } catch (err) {
        grid.innerHTML = '<p class="empty-msg text-danger">Search request failed.</p>';
    }
}

function escapeQuote(str) {
    return (str || '').replace(/'/g, "\\'");
}

// -----------------------------------------------------------------------------
// 2. CART MANAGEMENT
// -----------------------------------------------------------------------------
async function loadCart() {
    if (!currentUser) {
        document.getElementById('cartCount').innerText = '0';
        return;
    }
    try {
        const res = await fetch(`${API_BASE}/api/cart/${currentUser.id}`);
        const cart = await res.json();
        currentCart = cart;
        const totalItems = cart.items ? cart.items.reduce((acc, i) => acc + i.quantity, 0) : 0;
        document.getElementById('cartCount').innerText = totalItems;
        renderCartModal(cart);
    } catch (err) {
        console.error('Failed to fetch cart:', err);
    }
}

async function addToCart(productId, productName, price, imageUrl) {
    if (!currentUser) {
        openAuthModal('login');
        showToast('Please login to add items to your cart', 'info');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/api/cart/${currentUser.id}/add`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                productId,
                productName,
                price,
                quantity: 1,
                imageUrl
            })
        });

        if (res.ok) {
            showToast(`Added "${productName}" to cart`, 'success');
            loadCart();
        } else {
            const err = await res.json();
            showToast(err.message || 'Failed to add item', 'error');
        }
    } catch (e) {
        showToast('Network error while updating cart', 'error');
    }
}

async function updateCartQty(productId, newQty) {
    if (newQty < 1) {
        removeCartItem(productId);
        return;
    }
    try {
        const res = await fetch(`${API_BASE}/api/cart/${currentUser.id}/item/${productId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ quantity: newQty })
        });
        if (res.ok) {
            loadCart();
        }
    } catch (err) {
        showToast('Failed to update quantity', 'error');
    }
}

async function removeCartItem(productId) {
    try {
        const res = await fetch(`${API_BASE}/api/cart/${currentUser.id}/item/${productId}`, {
            method: 'DELETE'
        });
        if (res.ok) {
            showToast('Item removed from cart', 'info');
            loadCart();
        }
    } catch (err) {
        showToast('Failed to remove item', 'error');
    }
}

async function clearCart() {
    if (!confirm('Are you sure you want to clear your cart?')) return;
    try {
        const res = await fetch(`${API_BASE}/api/cart/${currentUser.id}/clear`, {
            method: 'DELETE'
        });
        if (res.ok) {
            showToast('Cart cleared', 'info');
            loadCart();
        }
    } catch (err) {
        showToast('Failed to clear cart', 'error');
    }
}

function openCartModal() {
    if (!currentUser) {
        openAuthModal('login');
        return;
    }
    loadCart();
    openModal('cartModal');
}

function renderCartModal(cart) {
    const container = document.getElementById('cartItemsContainer');
    const summary = document.getElementById('cartSummary');

    if (!cart || !cart.items || cart.items.length === 0) {
        container.innerHTML = '<p class="empty-msg"><i class="fa-solid fa-cart-arrow-down" style="font-size:32px;display:block;margin-bottom:10px;"></i>Your shopping cart is empty.</p>';
        summary.style.display = 'none';
        return;
    }

    summary.style.display = 'block';
    document.getElementById('cartSummaryCount').innerText = cart.items.reduce((acc, i) => acc + i.quantity, 0);
    document.getElementById('cartGrandTotal').innerText = `₹${cart.totalAmount.toLocaleString('en-IN')}`;

    container.innerHTML = cart.items.map(item => `
        <div class="order-item-row" style="border-bottom:1px solid #e2e8f0; padding:10px 0;">
            <img src="${item.imageUrl || 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400'}" class="order-item-img" onerror="this.src='https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=400'">
            <div style="flex:1;">
                <h4 style="font-size:14px; font-weight:700;">${item.productName}</h4>
                <div style="font-size:12px; color:#64748b;">₹${item.price.toLocaleString('en-IN')} each</div>
            </div>
            <div style="display:flex; align-items:center; gap:8px;">
                <button class="btn btn-outline btn-sm" onclick="updateCartQty(${item.productId}, ${item.quantity - 1})">-</button>
                <span style="font-weight:700; width:20px; text-align:center;">${item.quantity}</span>
                <button class="btn btn-outline btn-sm" onclick="updateCartQty(${item.productId}, ${item.quantity + 1})">+</button>
            </div>
            <div style="font-weight:800; font-size:14px; width:80px; text-align:right;">₹${item.subtotal.toLocaleString('en-IN')}</div>
            <button class="btn btn-danger btn-sm" onclick="removeCartItem(${item.productId})"><i class="fa-solid fa-trash"></i></button>
        </div>
    `).join('');
}

// -----------------------------------------------------------------------------
// 3. CHECKOUT & ORDER PLACEMENT
// -----------------------------------------------------------------------------
async function openCheckoutModal() {
    closeModal('cartModal');
    if (!currentCart || !currentCart.items || currentCart.items.length === 0) {
        showToast('Cart is empty', 'warning');
        return;
    }

    document.getElementById('checkoutAmount').innerText = `₹${currentCart.totalAmount.toLocaleString('en-IN')}`;
    document.getElementById('shippingAddress').value = currentUser.address || '';
    
    // Check wallet balance
    const wallet = await fetchWalletData();
    document.getElementById('checkoutWalletBalance').innerText = `₹${(wallet ? wallet.balance : 0).toLocaleString('en-IN')}`;
    
    updatePaymentMethodInfo();
    openModal('checkoutModal');
}

async function updatePaymentMethodInfo() {
    const paymentMethod = document.querySelector('input[name="paymentMethod"]:checked').value;
    const warning = document.getElementById('walletWarning');
    const placeBtn = document.getElementById('placeOrderBtn');

    if (paymentMethod === 'WALLET') {
        const wallet = await fetchWalletData();
        const balance = wallet ? wallet.balance : 0;
        if (balance < currentCart.totalAmount) {
            warning.style.display = 'block';
            placeBtn.disabled = true;
        } else {
            warning.style.display = 'none';
            placeBtn.disabled = false;
        }
    } else {
        warning.style.display = 'none';
        placeBtn.disabled = false;
    }
}

async function submitOrder() {
    const address = document.getElementById('shippingAddress').value.trim();
    if (!address) {
        showToast('Please provide a delivery address', 'warning');
        return;
    }
    const paymentMethod = document.querySelector('input[name="paymentMethod"]:checked').value;
    const placeBtn = document.getElementById('placeOrderBtn');
    placeBtn.disabled = true;
    placeBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Processing Order...';

    try {
        const res = await fetch(`${API_BASE}/api/orders`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                userId: currentUser.id,
                shippingAddress: address,
                paymentMethod: paymentMethod
            })
        });

        if (res.ok) {
            const placedOrder = await res.json();
            closeModal('checkoutModal');
            showToast(`Order #${placedOrder.id} placed successfully!`, 'success');
            loadCart();
            loadWallet();
            openTrackingModal(placedOrder.id);
        } else {
            const err = await res.json();
            showToast(err.message || 'Failed to place order', 'error');
        }
    } catch (err) {
        showToast('Network error during checkout', 'error');
    } finally {
        placeBtn.disabled = false;
        placeBtn.innerHTML = '<i class="fa-solid fa-check"></i> Confirm & Place Order';
    }
}

// -----------------------------------------------------------------------------
// 4. ORDER TRACKING & ORDER HISTORY
// -----------------------------------------------------------------------------
async function openOrdersModal() {
    if (!currentUser) {
        openAuthModal('login');
        return;
    }
    openModal('ordersModal');
    const container = document.getElementById('ordersContainer');
    container.innerHTML = '<p class="text-muted">Loading your orders...</p>';

    try {
        const res = await fetch(`${API_BASE}/api/orders/user/${currentUser.id}`);
        const orders = await res.json();
        renderOrdersList(orders);
    } catch (err) {
        container.innerHTML = '<p class="empty-msg text-danger">Failed to load order history.</p>';
    }
}

function renderOrdersList(orders) {
    const container = document.getElementById('ordersContainer');
    if (!orders || orders.length === 0) {
        container.innerHTML = '<p class="empty-msg"><i class="fa-solid fa-box-open" style="font-size:32px;display:block;margin-bottom:10px;"></i>No orders found.</p>';
        return;
    }

    container.innerHTML = orders.map(order => `
        <div class="order-card">
            <div class="order-header">
                <div>
                    <strong>Order #${order.id}</strong>
                    <div style="font-size:11px; color:#64748b;">${new Date(order.orderDate).toLocaleString()}</div>
                </div>
                <div style="text-align:right;">
                    <span class="status-badge status-${order.orderStatus}">${order.orderStatus}</span>
                    <div style="font-size:12px; font-weight:700; margin-top:2px;">₹${order.totalAmount.toLocaleString('en-IN')} (${order.paymentMethod})</div>
                </div>
            </div>

            <!-- Visual Progress Bar -->
            ${renderTrackingBar(order.orderStatus)}

            <div class="order-items-list">
                ${order.items.map(item => `
                    <div class="order-item-row">
                        <img src="${item.imageUrl || 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400'}" class="order-item-img" onerror="this.src='https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=400'">
                        <div style="flex:1;">
                            <strong style="font-size:13px;">${item.productName}</strong>
                            <div style="font-size:11px; color:#64748b;">Qty: ${item.quantity} × ₹${item.price.toLocaleString('en-IN')} | Seller: ${item.merchantName || 'Alice Merchant'}</div>
                        </div>
                        <div style="font-weight:700;">₹${item.subtotal.toLocaleString('en-IN')}</div>
                    </div>
                `).join('')}
            </div>

            <div style="display:flex; justify-content:space-between; align-items:center; margin-top:10px; font-size:12px; border-top:1px solid #f1f5f9; padding-top:8px;">
                <div style="color:#64748b;"><i class="fa-solid fa-location-dot"></i> Ship to: ${order.shippingAddress}</div>
                <button class="btn btn-outline btn-sm" onclick="openTrackingModal(${order.id})"><i class="fa-solid fa-location-crosshairs"></i> Live Track</button>
            </div>
        </div>
    `).join('');
}

function renderTrackingBar(status) {
    const steps = [
        { key: 'PLACED', label: 'Placed', icon: 'fa-file-invoice' },
        { key: 'CONFIRMED', label: 'Confirmed', icon: 'fa-circle-check' },
        { key: 'SHIPPED', label: 'Shipped', icon: 'fa-boxes-packing' },
        { key: 'OUT_FOR_DELIVERY', label: 'Out for Delivery', icon: 'fa-truck-fast' },
        { key: 'DELIVERED', label: 'Delivered', icon: 'fa-house-circle-check' }
    ];

    const statusHierarchy = ['PLACED', 'CONFIRMED', 'SHIPPED', 'OUT_FOR_DELIVERY', 'DELIVERED'];
    const currentIdx = statusHierarchy.indexOf(status);

    if (status === 'CANCELLED') {
        return `<div class="alert alert-warning" style="margin:10px 0; font-size:12px;"><i class="fa-solid fa-ban"></i> This order has been CANCELLED.</div>`;
    }

    return `
        <div class="tracking-timeline">
            ${steps.map((step, idx) => {
                let stepClass = '';
                if (idx < currentIdx) stepClass = 'completed';
                else if (idx === currentIdx) stepClass = 'current';
                return `
                    <div class="timeline-step ${stepClass}">
                        <div class="step-icon"><i class="fa-solid ${step.icon}"></i></div>
                        <span class="step-title">${step.label}</span>
                    </div>
                `;
            }).join('')}
        </div>
    `;
}

async function openTrackingModal(orderId) {
    try {
        const res = await fetch(`${API_BASE}/api/orders/${orderId}`);
        const order = await res.json();
        document.getElementById('trackingOrderTitle').innerHTML = `<i class="fa-solid fa-location-dot"></i> Order #${order.id} Tracking`;
        
        const content = document.getElementById('trackingTimelineContent');
        content.innerHTML = `
            <div style="background:#f8fafc; border-radius:10px; padding:16px; margin-bottom:16px;">
                <div style="display:flex; justify-content:space-between; margin-bottom:8px;">
                    <span>Current Status:</span>
                    <span class="status-badge status-${order.orderStatus}">${order.orderStatus}</span>
                </div>
                <div style="display:flex; justify-content:space-between; margin-bottom:8px;">
                    <span>Payment:</span>
                    <strong>${order.paymentMethod} (${order.paymentStatus})</strong>
                </div>
                <div style="display:flex; justify-content:space-between;">
                    <span>Total Amount:</span>
                    <strong style="color:#2563eb; font-size:16px;">₹${order.totalAmount.toLocaleString('en-IN')}</strong>
                </div>
            </div>

            ${renderTrackingBar(order.orderStatus)}

            <div style="margin-top:20px;">
                <h4 style="font-size:13px; font-weight:700; margin-bottom:10px;">Delivery Destination</h4>
                <p style="font-size:12px; color:#475569; background:#fff; border:1px solid #e2e8f0; border-radius:6px; padding:10px;">
                    <i class="fa-solid fa-map-pin" style="color:#2563eb;"></i> ${order.shippingAddress}
                </p>
            </div>
        `;
        openModal('trackingDetailModal');
    } catch (e) {
        showToast('Could not load order tracking details', 'error');
    }
}

// -----------------------------------------------------------------------------
// 5. DIGITAL WALLET
// -----------------------------------------------------------------------------
async function fetchWalletData() {
    if (!currentUser) return null;
    try {
        const res = await fetch(`${API_BASE}/api/wallet/${currentUser.id}`);
        return await res.json();
    } catch (e) {
        return null;
    }
}

async function loadWallet() {
    if (!currentUser) return;
    const wallet = await fetchWalletData();
    if (wallet) {
        const formatted = `₹${wallet.balance.toLocaleString('en-IN')}`;
        document.getElementById('navWallet').innerText = `Wallet: ${formatted}`;
        document.getElementById('walletBalanceBig').innerText = formatted;
    }
}

async function openWalletModal() {
    if (!currentUser) {
        openAuthModal('login');
        return;
    }
    loadWallet();
    loadTransactions();
    openModal('walletModal');
}

async function addMoney() {
    const input = document.getElementById('addMoneyInput');
    const amount = parseFloat(input.value);
    if (!amount || amount <= 0) {
        showToast('Please enter a valid top-up amount', 'warning');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/api/wallet/${currentUser.id}/add-money`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                amount: amount,
                description: 'Wallet Top-up via UPI / Card'
            })
        });

        if (res.ok) {
            showToast(`Successfully added ₹${amount.toLocaleString('en-IN')} to wallet!`, 'success');
            input.value = '';
            loadWallet();
            loadTransactions();
        } else {
            showToast('Top-up failed', 'error');
        }
    } catch (e) {
        showToast('Network error during wallet top-up', 'error');
    }
}

async function loadTransactions() {
    const list = document.getElementById('transactionsList');
    try {
        const res = await fetch(`${API_BASE}/api/wallet/${currentUser.id}/transactions`);
        const transactions = await res.json();

        if (!transactions || transactions.length === 0) {
            list.innerHTML = '<p class="empty-msg">No transactions recorded yet.</p>';
            return;
        }

        list.innerHTML = transactions.map(t => `
            <div style="display:flex; justify-content:space-between; align-items:center; padding:10px 0; border-bottom:1px solid #e2e8f0;">
                <div style="display:flex; align-items:center; gap:10px;">
                    <div style="width:32px; height:32px; border-radius:50%; background:${t.type === 'CREDIT' ? '#dcfce7' : '#fee2e2'}; color:${t.type === 'CREDIT' ? '#15803d' : '#b91c1c'}; display:flex; align-items:center; justify-content:center;">
                        <i class="fa-solid ${t.type === 'CREDIT' ? 'fa-arrow-down-left' : 'fa-arrow-up-right'}"></i>
                    </div>
                    <div>
                        <strong style="font-size:13px;">${t.description}</strong>
                        <div style="font-size:11px; color:#64748b;">${new Date(t.timestamp).toLocaleString()}</div>
                    </div>
                </div>
                <div style="font-weight:800; font-size:14px; color:${t.type === 'CREDIT' ? '#15803d' : '#b91c1c'};">
                    ${t.type === 'CREDIT' ? '+' : '-'}₹${t.amount.toLocaleString('en-IN')}
                </div>
            </div>
        `).join('');
    } catch (e) {
        list.innerHTML = '<p class="empty-msg text-danger">Failed to load transactions.</p>';
    }
}

// -----------------------------------------------------------------------------
// 6. NOTIFICATIONS INBOX
// -----------------------------------------------------------------------------
async function checkNotifications() {
    if (!currentUser) return;
    try {
        const res = await fetch(`${API_BASE}/api/notifications/user/${currentUser.id}`);
        const notifs = await res.json();
        const unreadCount = notifs.filter(n => !n.read).length;
        const badge = document.getElementById('notifBadge');
        if (unreadCount > 0) {
            badge.innerText = unreadCount;
            badge.style.display = 'inline-block';
        } else {
            badge.style.display = 'none';
        }
    } catch (e) {}
}

async function openNotificationsModal() {
    if (!currentUser) {
        openAuthModal('login');
        return;
    }
    openModal('notificationsModal');
    const list = document.getElementById('notificationsList');
    list.innerHTML = '<p class="text-muted">Loading alerts...</p>';

    try {
        const res = await fetch(`${API_BASE}/api/notifications/user/${currentUser.id}`);
        const notifs = await res.json();

        if (!notifs || notifs.length === 0) {
            list.innerHTML = '<p class="empty-msg"><i class="fa-solid fa-bell-slash" style="font-size:32px;display:block;margin-bottom:10px;"></i>No notifications found.</p>';
            return;
        }

        list.innerHTML = notifs.map(n => `
            <div class="notif-card ${n.read ? '' : 'unread'}">
                <i class="fa-solid fa-bell notif-icon"></i>
                <div class="notif-content" style="flex:1;">
                    <h5>${n.title}</h5>
                    <p>${n.message}</p>
                    <time>${new Date(n.timestamp).toLocaleString()}</time>
                </div>
                ${!n.read ? `<button class="btn btn-outline btn-sm" onclick="markNotificationRead(${n.id})"><i class="fa-solid fa-check"></i></button>` : ''}
            </div>
        `).join('');
    } catch (e) {
        list.innerHTML = '<p class="empty-msg text-danger">Failed to load notifications.</p>';
    }
}

async function markNotificationRead(id) {
    try {
        await fetch(`${API_BASE}/api/notifications/${id}/read`, { method: 'PUT' });
        openNotificationsModal();
        checkNotifications();
    } catch (e) {}
}

// -----------------------------------------------------------------------------
// 7. MERCHANT PORTAL
// -----------------------------------------------------------------------------
async function loadMerchantDashboard() {
    if (!currentUser) return;
    const merchantId = currentUser.id;

    // 1. Fetch Revenue & Stats
    try {
        const revRes = await fetch(`${API_BASE}/api/orders/merchant/${merchantId}/revenue`);
        const revData = await revRes.json();
        currentMerchantRevenue = revData;

        document.getElementById('merchantTotalRevenue').innerText = `₹${revData.totalRevenue.toLocaleString('en-IN')}`;
        document.getElementById('merchantPendingRevenue').innerText = `₹${revData.pendingRevenue.toLocaleString('en-IN')}`;
        document.getElementById('merchantUnitsSold').innerText = revData.totalUnitsSold;
        document.getElementById('merchantCompletedOrders').innerText = revData.completedOrders;
        document.getElementById('merchantOrderCount').innerText = revData.totalOrders;

        currentMerchantOrders = revData.orders || [];
        renderMerchantOrdersTable(currentMerchantOrders);
    } catch (e) {
        console.error('Failed to load merchant revenue:', e);
    }

    // 2. Fetch Merchant Products
    try {
        const prodRes = await fetch(`${API_BASE}/api/products/merchant/${merchantId}`);
        const prods = await prodRes.json();
        document.getElementById('merchantProdCount').innerText = prods.length;
        renderMerchantProductsTable(prods);
    } catch (e) {
        console.error('Failed to load merchant products:', e);
    }
}

function switchMerchantTab(tab) {
    document.querySelectorAll('#viewMerchant .sub-tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('#viewMerchant .tab-pane').forEach(p => p.classList.remove('active'));

    if (tab === 'orders') {
        document.getElementById('tabMerchantOrders').classList.add('active');
        document.getElementById('merchantOrdersPanel').classList.add('active');
    } else {
        document.getElementById('tabMerchantProducts').classList.add('active');
        document.getElementById('merchantProductsPanel').classList.add('active');
    }
}

function filterMerchantOrders(category) {
    document.querySelectorAll('#merchantOrdersPanel .filter-pill').forEach(b => b.classList.remove('active'));
    event.target.classList.add('active');

    if (category === 'ALL') {
        renderMerchantOrdersTable(currentMerchantOrders);
    } else if (category === 'PENDING') {
        renderMerchantOrdersTable(currentMerchantOrders.filter(o => o.orderStatus === 'PLACED' || o.paymentStatus === 'COD_PENDING'));
    } else if (category === 'ONGOING') {
        renderMerchantOrdersTable(currentMerchantOrders.filter(o => ['CONFIRMED', 'SHIPPED', 'OUT_FOR_DELIVERY'].includes(o.orderStatus)));
    } else if (category === 'COMPLETED') {
        renderMerchantOrdersTable(currentMerchantOrders.filter(o => o.orderStatus === 'DELIVERED'));
    } else if (category === 'CANCELLED') {
        renderMerchantOrdersTable(currentMerchantOrders.filter(o => o.orderStatus === 'CANCELLED'));
    }
}

function renderMerchantOrdersTable(orders) {
    const container = document.getElementById('merchantOrdersList');
    if (!orders || orders.length === 0) {
        container.innerHTML = '<p class="empty-msg">No orders match this filter.</p>';
        return;
    }

    container.innerHTML = `
        <div class="table-responsive">
            <table class="data-table">
                <thead>
                    <tr>
                        <th>Order ID</th>
                        <th>Date</th>
                        <th>Items Sold</th>
                        <th>Total</th>
                        <th>Payment</th>
                        <th>Current Status</th>
                        <th>Update Status</th>
                    </tr>
                </thead>
                <tbody>
                    ${orders.map(o => `
                        <tr>
                            <td><strong>#${o.id}</strong></td>
                            <td>${new Date(o.orderDate).toLocaleDateString()}</td>
                            <td>
                                ${o.items.map(i => `<div>${i.productName} (×${i.quantity})</div>`).join('')}
                            </td>
                            <td><strong>₹${o.totalAmount.toLocaleString('en-IN')}</strong></td>
                            <td>
                                <span class="status-badge status-${o.paymentStatus}">${o.paymentStatus}</span>
                                <small style="display:block; color:#64748b;">${o.paymentMethod}</small>
                            </td>
                            <td><span class="status-badge status-${o.orderStatus}">${o.orderStatus}</span></td>
                            <td>
                                <select class="form-control form-control-sm" onchange="updateOrderStatus(${o.id}, this.value)" style="width:140px; font-size:12px;">
                                    <option value="">-- Change --</option>
                                    <option value="CONFIRMED" ${o.orderStatus === 'CONFIRMED' ? 'disabled' : ''}>Confirm</option>
                                    <option value="SHIPPED" ${o.orderStatus === 'SHIPPED' ? 'disabled' : ''}>Ship Package</option>
                                    <option value="OUT_FOR_DELIVERY" ${o.orderStatus === 'OUT_FOR_DELIVERY' ? 'disabled' : ''}>Out for Delivery</option>
                                    <option value="DELIVERED" ${o.orderStatus === 'DELIVERED' ? 'disabled' : ''}>Mark Delivered</option>
                                    <option value="CANCELLED" ${o.orderStatus === 'CANCELLED' ? 'disabled' : ''}>Cancel Order</option>
                                </select>
                            </td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        </div>
    `;
}

function renderMerchantProductsTable(products) {
    const container = document.getElementById('merchantProductsList');
    if (!products || products.length === 0) {
        container.innerHTML = '<p class="empty-msg">You have not listed any products yet.</p>';
        return;
    }

    container.innerHTML = `
        <div class="table-responsive">
            <table class="data-table">
                <thead>
                    <tr>
                        <th>Product</th>
                        <th>Category</th>
                        <th>Price</th>
                        <th>Stock</th>
                        <th>Actions</th>
                    </tr>
                </thead>
                <tbody>
                    ${products.map(p => `
                        <tr>
                            <td>
                                <div style="display:flex; align-items:center; gap:10px;">
                                    <img src="${p.imageUrl || 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400'}" style="width:36px; height:36px; object-fit:cover; border-radius:4px;" onerror="this.src='https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=400'">
                                    <strong>${p.name}</strong>
                                </div>
                            </td>
                            <td>${p.category}</td>
                            <td><strong>₹${p.price.toLocaleString('en-IN')}</strong></td>
                            <td>${p.stockQuantity}</td>
                            <td>
                                <button class="btn btn-outline btn-sm" onclick="editProductModal(${p.id})"><i class="fa-solid fa-pen"></i> Edit</button>
                                <button class="btn btn-danger btn-sm" onclick="deleteProduct(${p.id})"><i class="fa-solid fa-trash"></i></button>
                            </td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        </div>
    `;
}

async function updateOrderStatus(orderId, newStatus) {
    if (!newStatus) return;
    try {
        const res = await fetch(`${API_BASE}/api/orders/${orderId}/status`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ orderStatus: newStatus })
        });

        if (res.ok) {
            showToast(`Order #${orderId} status updated to ${newStatus}`, 'success');
            if (currentRole === 'MERCHANT') loadMerchantDashboard();
            if (currentRole === 'ADMIN') loadAdminOrders();
        } else {
            showToast('Failed to update status', 'error');
        }
    } catch (e) {
        showToast('Network error while updating status', 'error');
    }
}

// -----------------------------------------------------------------------------
// 8. ADMIN DASHBOARD & AUDIT LOGS
// -----------------------------------------------------------------------------
async function loadAdminDashboard() {
    loadAdminUsers();
    loadAdminProducts();
    loadAdminOrders();
    loadAuditLogs();
}

function switchAdminTab(tab) {
    document.querySelectorAll('#viewAdmin .sub-tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('#viewAdmin .tab-pane').forEach(p => p.classList.remove('active'));

    if (tab === 'users') {
        document.getElementById('tabAdminUsers').classList.add('active');
        document.getElementById('adminUsersPanel').classList.add('active');
        loadAdminUsers();
    } else if (tab === 'products') {
        document.getElementById('tabAdminProducts').classList.add('active');
        document.getElementById('adminProductsPanel').classList.add('active');
        loadAdminProducts();
    } else if (tab === 'orders') {
        document.getElementById('tabAdminOrders').classList.add('active');
        document.getElementById('adminOrdersPanel').classList.add('active');
        loadAdminOrders();
    } else if (tab === 'audit') {
        document.getElementById('tabAdminAudit').classList.add('active');
        document.getElementById('adminAuditPanel').classList.add('active');
        loadAuditLogs();
    }
}

async function loadAdminUsers() {
    try {
        const res = await fetch(`${API_BASE}/api/profiles`);
        const users = await res.json();
        currentAdminUsers = users;
        
        // Update KPIs
        document.getElementById('adminCustomerCount').innerText = users.filter(u => u.role === 'CUSTOMER').length;
        document.getElementById('adminMerchantCount').innerText = users.filter(u => u.role === 'MERCHANT').length;

        renderAdminUsersTable(users);
    } catch (e) {
        console.error('Failed to load admin users:', e);
    }
}

function filterAdminUsers() {
    const q = document.getElementById('adminUserSearch').value.toLowerCase();
    const role = document.getElementById('adminRoleFilter').value;

    let filtered = currentAdminUsers.filter(u => {
        const matchRole = !role || u.role === role;
        const matchText = !q || (u.name && u.name.toLowerCase().includes(q)) || 
                                (u.email && u.email.toLowerCase().includes(q)) || 
                                (u.phone && u.phone.toLowerCase().includes(q));
        return matchRole && matchText;
    });

    renderAdminUsersTable(filtered);
}

function renderAdminUsersTable(users) {
    const container = document.getElementById('adminUsersList');
    if (!users || users.length === 0) {
        container.innerHTML = '<p class="empty-msg">No users found.</p>';
        return;
    }

    container.innerHTML = `
        <table class="data-table">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>User Details</th>
                    <th>Email</th>
                    <th>Role</th>
                    <th>Phone</th>
                    <th>Address</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                ${users.map(u => `
                    <tr>
                        <td><strong>#${u.id}</strong></td>
                        <td><strong>${u.name}</strong></td>
                        <td>${u.email}</td>
                        <td><span class="role-badge ${u.role}">${u.role}</span></td>
                        <td>${u.phone || '-'}</td>
                        <td>${u.address || '-'}</td>
                        <td>
                            <button class="btn btn-outline btn-sm" onclick="openEditUserModal(${u.id})"><i class="fa-solid fa-pen"></i> Edit</button>
                            <button class="btn btn-danger btn-sm" onclick="deleteUserAccount(${u.id})"><i class="fa-solid fa-trash"></i></button>
                        </td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;
}

function openCreateUserModal() {
    document.getElementById('adminUserModalTitle').innerHTML = '<i class="fa-solid fa-user-plus"></i> Create User Account';
    document.getElementById('adminUserEditId').value = '';
    document.getElementById('adminUserName').value = '';
    document.getElementById('adminUserEmail').value = '';
    document.getElementById('adminUserEmail').disabled = false;
    document.getElementById('adminUserPassword').value = '';
    document.getElementById('adminUserPassword').required = true;
    document.getElementById('adminUserPasswordLabel').innerText = 'Password *';
    document.getElementById('adminUserPhone').value = '';
    document.getElementById('adminUserRole').value = 'CUSTOMER';
    document.getElementById('adminUserAddress').value = '';
    openModal('adminUserModal');
}

async function openEditUserModal(userId) {
    const u = currentAdminUsers.find(x => x.id === userId);
    if (!u) return;

    document.getElementById('adminUserModalTitle').innerHTML = `<i class="fa-solid fa-user-pen"></i> Edit User #${u.id}`;
    document.getElementById('adminUserEditId').value = u.id;
    document.getElementById('adminUserName').value = u.name || '';
    document.getElementById('adminUserEmail').value = u.email || '';
    document.getElementById('adminUserEmail').disabled = true;
    document.getElementById('adminUserPassword').value = '';
    document.getElementById('adminUserPassword').required = false;
    document.getElementById('adminUserPasswordLabel').innerText = 'New Password (leave blank to keep)';
    document.getElementById('adminUserPhone').value = u.phone || '';
    document.getElementById('adminUserRole').value = u.role || 'CUSTOMER';
    document.getElementById('adminUserAddress').value = u.address || '';
    openModal('adminUserModal');
}

async function handleAdminSaveUser(e) {
    e.preventDefault();
    const id = document.getElementById('adminUserEditId').value;
    const name = document.getElementById('adminUserName').value.trim();
    const email = document.getElementById('adminUserEmail').value.trim();
    const password = document.getElementById('adminUserPassword').value;
    const phone = document.getElementById('adminUserPhone').value.trim();
    const role = document.getElementById('adminUserRole').value;
    const address = document.getElementById('adminUserAddress').value.trim();

    try {
        let res;
        if (id) {
            // Update
            res = await fetch(`${API_BASE}/api/profiles/${id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name, phone, address, role, password })
            });
        } else {
            // Create
            res = await fetch(`${API_BASE}/api/profiles/admin/create`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ name, email, password, phone, address, role })
            });
        }

        if (res.ok) {
            closeModal('adminUserModal');
            showToast(`User ${id ? 'updated' : 'created'} successfully`, 'success');
            loadAdminUsers();
            loadAuditLogs();
        } else {
            const err = await res.json();
            showToast(err.message || 'Operation failed', 'error');
        }
    } catch (err) {
        showToast('Network error while saving user', 'error');
    }
}

async function deleteUserAccount(userId) {
    if (!confirm(`Are you sure you want to delete user ID #${userId}?`)) return;
    try {
        const res = await fetch(`${API_BASE}/api/profiles/${userId}`, { method: 'DELETE' });
        if (res.ok) {
            showToast(`User #${userId} deleted successfully`, 'success');
            loadAdminUsers();
            loadAuditLogs();
        } else {
            showToast('Failed to delete user', 'error');
        }
    } catch (e) {
        showToast('Network error while deleting user', 'error');
    }
}

async function loadAdminProducts() {
    try {
        const res = await fetch(`${API_BASE}/api/products`);
        const prods = await res.json();
        currentAdminProducts = prods;
        document.getElementById('adminProductCount').innerText = prods.length;
        renderAdminProductsTable(prods);
    } catch (e) {
        console.error('Failed to load admin products:', e);
    }
}

function filterAdminProducts() {
    const q = document.getElementById('adminProductSearch').value.toLowerCase();
    const filtered = currentAdminProducts.filter(p => 
        (p.name && p.name.toLowerCase().includes(q)) || 
        (p.merchantName && p.merchantName.toLowerCase().includes(q)) ||
        (p.category && p.category.toLowerCase().includes(q))
    );
    renderAdminProductsTable(filtered);
}

function renderAdminProductsTable(prods) {
    const container = document.getElementById('adminProductsList');
    if (!prods || prods.length === 0) {
        container.innerHTML = '<p class="empty-msg">No products found in catalog.</p>';
        return;
    }

    container.innerHTML = `
        <table class="data-table">
            <thead>
                <tr>
                    <th>ID</th>
                    <th>Product</th>
                    <th>Category</th>
                    <th>Merchant Seller</th>
                    <th>Price</th>
                    <th>Stock</th>
                    <th>Actions</th>
                </tr>
            </thead>
            <tbody>
                ${prods.map(p => `
                    <tr>
                        <td><strong>#${p.id}</strong></td>
                        <td>
                            <div style="display:flex; align-items:center; gap:10px;">
                                <img src="${p.imageUrl || 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400'}" style="width:36px; height:36px; object-fit:cover; border-radius:4px;" onerror="this.src='https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=400'">
                                <strong>${p.name}</strong>
                            </div>
                        </td>
                        <td>${p.category}</td>
                        <td><strong>${p.merchantName || 'Merchant'}</strong> (ID: ${p.merchantId || 2})</td>
                        <td><strong>₹${p.price.toLocaleString('en-IN')}</strong></td>
                        <td>${p.stockQuantity}</td>
                        <td>
                            <button class="btn btn-outline btn-sm" onclick="editProductModal(${p.id})"><i class="fa-solid fa-pen"></i> Edit</button>
                            <button class="btn btn-danger btn-sm" onclick="deleteProduct(${p.id})"><i class="fa-solid fa-trash"></i></button>
                        </td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;
}

async function loadAdminOrders() {
    try {
        const res = await fetch(`${API_BASE}/api/orders`);
        const orders = await res.json();
        currentAdminOrders = orders;
        document.getElementById('adminOrderCount').innerText = orders.length;
        const gmv = orders.reduce((sum, o) => sum + (o.totalAmount || 0), 0);
        document.getElementById('adminTotalGMV').innerText = `₹${gmv.toLocaleString('en-IN')}`;
        renderAdminOrdersTable(orders);
    } catch (e) {
        console.error('Failed to load admin orders:', e);
    }
}

function filterAdminOrders() {
    const q = document.getElementById('adminOrderSearch').value.toLowerCase();
    const filtered = currentAdminOrders.filter(o => 
        String(o.id).includes(q) || 
        String(o.userId).includes(q) || 
        (o.orderStatus && o.orderStatus.toLowerCase().includes(q)) || 
        (o.paymentMethod && o.paymentMethod.toLowerCase().includes(q))
    );
    renderAdminOrdersTable(filtered);
}

function renderAdminOrdersTable(orders) {
    const container = document.getElementById('adminOrdersList');
    if (!orders || orders.length === 0) {
        container.innerHTML = '<p class="empty-msg">No orders recorded in system.</p>';
        return;
    }

    container.innerHTML = `
        <table class="data-table">
            <thead>
                <tr>
                    <th>Order</th>
                    <th>Customer</th>
                    <th>Date</th>
                    <th>Total</th>
                    <th>Payment</th>
                    <th>Status</th>
                    <th>Quick Actions</th>
                </tr>
            </thead>
            <tbody>
                ${orders.map(o => `
                    <tr>
                        <td><strong>#${o.id}</strong></td>
                        <td>User #${o.userId}</td>
                        <td>${new Date(o.orderDate).toLocaleDateString()}</td>
                        <td><strong>₹${o.totalAmount.toLocaleString('en-IN')}</strong></td>
                        <td><span class="status-badge status-${o.paymentStatus}">${o.paymentStatus}</span> (${o.paymentMethod})</td>
                        <td><span class="status-badge status-${o.orderStatus}">${o.orderStatus}</span></td>
                        <td>
                            <button class="btn btn-outline btn-sm" onclick="openTrackingModal(${o.id})"><i class="fa-solid fa-location-dot"></i> Track</button>
                        </td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;
}

async function loadAuditLogs() {
    try {
        const res = await fetch(`${API_BASE}/api/profiles/audit`);
        const logs = await res.json();
        currentAuditLogs = logs;
        renderAuditLogsFeed(logs);
    } catch (e) {
        console.error('Failed to load audit logs:', e);
    }
}

function filterAdminAudit() {
    const q = document.getElementById('adminAuditSearch').value.toLowerCase();
    const filtered = currentAuditLogs.filter(l => 
        (l.action && l.action.toLowerCase().includes(q)) || 
        (l.performedBy && l.performedBy.toLowerCase().includes(q)) || 
        (l.details && l.details.toLowerCase().includes(q))
    );
    renderAuditLogsFeed(filtered);
}

function renderAuditLogsFeed(logs) {
    const container = document.getElementById('adminAuditList');
    if (!logs || logs.length === 0) {
        container.innerHTML = '<p class="empty-msg">No audit logs recorded.</p>';
        return;
    }

    container.innerHTML = logs.map(l => `
        <div class="audit-item">
            <div class="audit-icon"><i class="fa-solid fa-clock-rotate-left"></i></div>
            <div class="audit-body">
                <div style="display:flex; justify-content:space-between; align-items:center;">
                    <span class="audit-action">${l.action}</span>
                    <time style="font-size:11px; color:#94a3b8;">${new Date(l.timestamp).toLocaleString()}</time>
                </div>
                <div class="audit-meta">
                    <strong>By:</strong> ${l.performedBy} | <strong>Target:</strong> ${l.entityType} #${l.entityId || ''}
                </div>
                <div class="audit-details">${l.details || ''}</div>
            </div>
        </div>
    `).join('');
}

// -----------------------------------------------------------------------------
// 9. PRODUCT ADD/EDIT/DELETE MODALS
// -----------------------------------------------------------------------------
function openAddProductModal(context = 'merchant') {
    document.getElementById('prodContext').value = context;
    document.getElementById('prodEditId').value = '';
    document.getElementById('productModalTitle').innerHTML = '<i class="fa-solid fa-box"></i> Add New Product';
    document.getElementById('prodName').value = '';
    document.getElementById('prodCategory').value = 'Electronics';
    document.getElementById('prodPrice').value = '';
    document.getElementById('prodStock').value = '10';
    document.getElementById('prodImage').value = '';
    document.getElementById('prodDesc').value = '';

    const adminRow = document.getElementById('adminMerchantSelectRow');
    if (context === 'admin') {
        adminRow.style.display = 'grid';
        document.getElementById('prodMerchantId').value = '2';
        document.getElementById('prodMerchantName').value = 'Alice Merchant';
    } else {
        adminRow.style.display = 'none';
    }

    openModal('productEditModal');
}

async function editProductModal(productId) {
    try {
        const res = await fetch(`${API_BASE}/api/products/${productId}`);
        const p = await res.json();
        document.getElementById('prodEditId').value = p.id;
        document.getElementById('productModalTitle').innerHTML = `<i class="fa-solid fa-pen"></i> Edit Product #${p.id}`;
        document.getElementById('prodName').value = p.name || '';
        document.getElementById('prodCategory').value = p.category || 'Electronics';
        document.getElementById('prodPrice').value = p.price || '';
        document.getElementById('prodStock').value = p.stockQuantity || '0';
        document.getElementById('prodImage').value = p.imageUrl || '';
        document.getElementById('prodDesc').value = p.description || '';

        const adminRow = document.getElementById('adminMerchantSelectRow');
        if (currentRole === 'ADMIN') {
            adminRow.style.display = 'grid';
            document.getElementById('prodMerchantId').value = p.merchantId || '2';
            document.getElementById('prodMerchantName').value = p.merchantName || 'Alice Merchant';
        } else {
            adminRow.style.display = 'none';
        }

        openModal('productEditModal');
    } catch (e) {
        showToast('Failed to fetch product details', 'error');
    }
}

async function handleSaveProduct(e) {
    e.preventDefault();
    const id = document.getElementById('prodEditId').value;
    const name = document.getElementById('prodName').value.trim();
    const category = document.getElementById('prodCategory').value;
    const price = parseFloat(document.getElementById('prodPrice').value);
    const stockQuantity = parseInt(document.getElementById('prodStock').value, 10);
    const imageUrl = document.getElementById('prodImage').value.trim();
    const description = document.getElementById('prodDesc').value.trim();

    let merchantId = currentUser ? currentUser.id : 2;
    let merchantName = currentUser ? currentUser.name : 'Alice Merchant';

    if (currentRole === 'ADMIN') {
        const customMId = document.getElementById('prodMerchantId').value;
        const customMName = document.getElementById('prodMerchantName').value;
        if (customMId) merchantId = parseInt(customMId, 10);
        if (customMName) merchantName = customMName;
    }

    const payload = { name, category, price, stockQuantity, imageUrl, description, merchantId, merchantName };

    try {
        let res;
        if (id) {
            res = await fetch(`${API_BASE}/api/products/${id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
        } else {
            res = await fetch(`${API_BASE}/api/products`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
        }

        if (res.ok) {
            closeModal('productEditModal');
            showToast(`Product ${id ? 'updated' : 'created'} successfully`, 'success');
            loadProducts();
            if (currentRole === 'MERCHANT') loadMerchantDashboard();
            if (currentRole === 'ADMIN') loadAdminProducts();
        } else {
            showToast('Failed to save product', 'error');
        }
    } catch (err) {
        showToast('Network error while saving product', 'error');
    }
}

async function deleteProduct(productId) {
    if (!confirm(`Are you sure you want to delete product #${productId}?`)) return;
    try {
        const res = await fetch(`${API_BASE}/api/products/${productId}`, { method: 'DELETE' });
        if (res.ok) {
            showToast(`Product #${productId} deleted successfully`, 'success');
            loadProducts();
            if (currentRole === 'MERCHANT') loadMerchantDashboard();
            if (currentRole === 'ADMIN') loadAdminProducts();
        } else {
            showToast('Failed to delete product', 'error');
        }
    } catch (e) {
        showToast('Network error while deleting product', 'error');
    }
}

// -----------------------------------------------------------------------------
// 10. AUTH & USER PROFILE MODALS
// -----------------------------------------------------------------------------
function openAuthModal(tab = 'login') {
    switchAuthTab(tab);
    openModal('authModal');
}

function switchAuthTab(tab) {
    const loginForm = document.getElementById('loginForm');
    const regForm = document.getElementById('registerForm');
    const loginBtn = document.getElementById('loginTabBtn');
    const regBtn = document.getElementById('registerTabBtn');
    const title = document.getElementById('authModalTitle');

    if (tab === 'login') {
        loginForm.style.display = 'block';
        regForm.style.display = 'none';
        loginBtn.classList.add('active');
        regBtn.classList.remove('active');
        title.innerText = 'Sign in to EShoppingZone';
    } else {
        loginForm.style.display = 'none';
        regForm.style.display = 'block';
        loginBtn.classList.remove('active');
        regBtn.classList.add('active');
        title.innerText = 'Create your Account';
    }
}

function fillCredentials(email, pass) {
    document.getElementById('loginEmail').value = email;
    document.getElementById('loginPassword').value = pass;
}

async function handleLogin(e) {
    e.preventDefault();
    const email = document.getElementById('loginEmail').value.trim();
    const password = document.getElementById('loginPassword').value;

    try {
        const res = await fetch(`${API_BASE}/api/profiles/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });

        if (res.ok) {
            const data = await res.json();
            saveSession({ id: data.userId, name: data.name, email: data.email, role: data.role }, data.token);
            closeModal('authModal');
            showToast(`Welcome back, ${data.name}!`, 'success');

            if (data.role === 'ADMIN') switchView('admin');
            else if (data.role === 'MERCHANT') switchView('merchant');
            else switchView('storefront');
        } else {
            const err = await res.json();
            showToast(err.message || 'Invalid email or password', 'error');
        }
    } catch (err) {
        showToast('Login request failed', 'error');
    }
}

async function handleRegister(e) {
    e.preventDefault();
    const name = document.getElementById('regName').value.trim();
    const email = document.getElementById('regEmail').value.trim();
    const password = document.getElementById('regPassword').value;
    const phone = document.getElementById('regPhone').value.trim();
    const address = document.getElementById('regAddress').value.trim();
    const role = document.getElementById('regRole').value;

    try {
        const res = await fetch(`${API_BASE}/api/profiles/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, email, password, phone, address, role })
        });

        if (res.ok) {
            const data = await res.json();
            saveSession({ id: data.userId, name: data.name, email: data.email, role: data.role }, data.token);
            closeModal('authModal');
            showToast('Account created successfully!', 'success');
            if (data.role === 'MERCHANT') switchView('merchant');
            else switchView('storefront');
        } else {
            const err = await res.json();
            showToast(err.message || 'Registration failed', 'error');
        }
    } catch (err) {
        showToast('Registration request failed', 'error');
    }
}

function openProfileModal() {
    if (!currentUser) return;
    document.getElementById('profName').value = currentUser.name || '';
    document.getElementById('profEmail').value = currentUser.email || '';
    document.getElementById('profPhone').value = currentUser.phone || '';
    document.getElementById('profAddress').value = currentUser.address || '';
    openModal('profileModal');
}

async function handleUpdateProfile(e) {
    e.preventDefault();
    const name = document.getElementById('profName').value.trim();
    const phone = document.getElementById('profPhone').value.trim();
    const address = document.getElementById('profAddress').value.trim();

    try {
        const res = await fetch(`${API_BASE}/api/profiles/${currentUser.id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, phone, address })
        });

        if (res.ok) {
            const updated = await res.json();
            currentUser.name = updated.name;
            currentUser.phone = updated.phone;
            currentUser.address = updated.address;
            localStorage.setItem('eshop_user', JSON.stringify(currentUser));
            renderAuthUI();
            closeModal('profileModal');
            showToast('Profile updated successfully', 'success');
        } else {
            showToast('Failed to update profile', 'error');
        }
    } catch (err) {
        showToast('Network error while updating profile', 'error');
    }
}

// Modal Helpers
function openModal(id) {
    document.getElementById(id).classList.add('active');
}

function closeModal(id) {
    document.getElementById(id).classList.remove('active');
}

window.onclick = function(event) {
    if (event.target.classList.contains('modal')) {
        event.target.classList.remove('active');
    }
};
