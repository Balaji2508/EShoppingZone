/* ==========================================================================
   EShoppingZone - Modern Microservices Frontend SPA Engine (app.js)
   ========================================================================== */

const API_BASE = (typeof window !== 'undefined' && window.location && window.location.origin && window.location.origin.startsWith('http'))
    ? `${window.location.origin}/api`
    : 'http://localhost:8080/api';

// Application State
let state = {
    user: null,
    token: null,
    products: [],
    wishlist: [],
    cart: null,
    orders: [],
    merchantOrders: [],
    deliveryOrders: [],
    notifications: [],
    currentProduct: null,
    activeView: 'storefront',
    activeMerchantTab: 'orders',
    activeAdminTab: 'users'
};

try {
    const rawUser = localStorage.getItem('esz_user');
    if (rawUser) state.user = JSON.parse(rawUser);
    state.token = localStorage.getItem('esz_token') || null;
} catch (e) {
    console.warn('Could not parse stored session:', e);
    state.user = null;
    state.token = null;
    localStorage.removeItem('esz_user');
    localStorage.removeItem('esz_token');
}

// ==========================================================================
// Initialization & Lifecycle
// ==========================================================================
function bootApp() {
    try {
        initAuth();
    } catch (e) {
        console.warn('initAuth error:', e);
    }
    loadProducts();
    if (state.user) {
        syncUserData();
    }
}

if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', bootApp);
} else {
    bootApp();
}

function initAuth() {
    try {
        if (state.user && state.token) {
            const guestEl = document.getElementById('authGuest');
            const userEl = document.getElementById('authUser');
            if (guestEl) guestEl.style.display = 'none';
            if (userEl) userEl.style.display = 'block';

            const nameEl = document.getElementById('userNameDisplay');
            if (nameEl) nameEl.textContent = state.user.fullName || (state.user.name ? state.user.name : (state.user.email ? state.user.email.split('@')[0] : 'User'));

            const badgeEl = document.getElementById('userRoleBadge');
            if (badgeEl) badgeEl.textContent = state.user.role || 'CUSTOMER';

            const emailEl = document.getElementById('userEmailDisplay');
            if (emailEl) emailEl.textContent = state.user.email || '';

            const tagEl = document.getElementById('userRoleTag');
            if (tagEl) tagEl.textContent = state.user.role || 'CUSTOMER';

            // Role-based navigation visibility
            const role = state.user.role || 'CUSTOMER';
            const tabM = document.getElementById('tabMerchant');
            if (tabM) tabM.style.display = (role === 'MERCHANT' || role === 'ADMIN') ? 'inline-flex' : 'none';

            const tabD = document.getElementById('tabDelivery');
            if (tabD) tabD.style.display = (role === 'DELIVERY_MAN' || role === 'ADMIN') ? 'inline-flex' : 'none';

            const tabA = document.getElementById('tabAdmin');
            if (tabA) tabA.style.display = (role === 'ADMIN') ? 'inline-flex' : 'none';

            const linkM = document.getElementById('menuMerchantLink');
            if (linkM) linkM.style.display = (role === 'MERCHANT' || role === 'ADMIN') ? 'flex' : 'none';

            const linkD = document.getElementById('menuDeliveryLink');
            if (linkD) linkD.style.display = (role === 'DELIVERY_MAN' || role === 'ADMIN') ? 'flex' : 'none';

            const linkA = document.getElementById('menuAdminLink');
            if (linkA) linkA.style.display = (role === 'ADMIN') ? 'flex' : 'none';
        } else {
            const guestEl = document.getElementById('authGuest');
            const userEl = document.getElementById('authUser');
            if (guestEl) guestEl.style.display = 'block';
            if (userEl) userEl.style.display = 'none';

            const tabM = document.getElementById('tabMerchant');
            if (tabM) tabM.style.display = 'none';
            const tabD = document.getElementById('tabDelivery');
            if (tabD) tabD.style.display = 'none';
            const tabA = document.getElementById('tabAdmin');
            if (tabA) tabA.style.display = 'none';
        }
    } catch (err) {
        console.error('initAuth error, resetting user state:', err);
        state.user = null;
        state.token = null;
        localStorage.removeItem('esz_user');
        localStorage.removeItem('esz_token');
    }
}

async function syncUserData() {
    if (!state.user) return;
    try {
        await Promise.allSettled([
            loadWalletBalance(),
            loadWishlist(),
            loadCart(),
            loadNotifications()
        ]);
    } catch (err) {
        console.warn('syncUserData warning:', err);
    }
}

// ==========================================================================
// View Switching & Portal Routing
// ==========================================================================
function switchView(viewName) {
    state.activeView = viewName;

    // Update portal tabs
    document.querySelectorAll('.portal-tab').forEach(t => t.classList.remove('active'));
    document.querySelectorAll('.portal-view').forEach(v => v.classList.remove('active'));

    const tabId = 'tab' + viewName.charAt(0).toUpperCase() + viewName.slice(1);
    const viewId = 'view' + viewName.charAt(0).toUpperCase() + viewName.slice(1);

    const tabEl = document.getElementById(tabId);
    if (tabEl) tabEl.classList.add('active');

    const viewEl = document.getElementById(viewId);
    if (viewEl) {
        viewEl.classList.add('active');
        viewEl.style.display = 'block';
    }

    // Hide others
    ['storefront', 'wishlist', 'merchant', 'delivery', 'admin'].forEach(v => {
        if (v !== viewName) {
            const el = document.getElementById('view' + v.charAt(0).toUpperCase() + v.slice(1));
            if (el) el.style.display = 'none';
        }
    });

    // Close user menu
    const um = document.getElementById('userMenu');
    if (um) um.classList.remove('show');

    // Trigger view-specific loads
    if (viewName === 'storefront') loadProducts();
    if (viewName === 'wishlist') loadWishlist();
    if (viewName === 'merchant') loadMerchantPortal();
    if (viewName === 'delivery') loadDeliveryPortal();
    if (viewName === 'admin') loadAdminPortal();
}

// ==========================================================================
// Storefront & Products Catalog
// ==========================================================================
async function loadProducts() {
    const grid = document.getElementById('productsGrid');
    try {
        const res = await fetch(`${API_BASE}/products`);
        if (!res.ok) throw new Error('Failed to load products');
        state.products = await res.json();
        renderProductsGrid(state.products);
    } catch (err) {
        showToast(err.message, 'error');
        if (grid) {
            grid.innerHTML = `
                <div class="empty-state" style="grid-column: 1 / -1; padding: 40px; text-align: center;">
                    <i class="fa-solid fa-triangle-exclamation" style="font-size:48px; color:#f44336; margin-bottom:12px;"></i>
                    <h3>Unable to Connect to Backend Services</h3>
                    <p class="section-sub">Make sure all microservices are started (Run <code>start_all.bat</code>).</p>
                    <button class="btn btn-primary" style="margin-top:16px;" onclick="loadProducts()"><i class="fa-solid fa-rotate"></i> Retry Connection</button>
                </div>
            `;
        }
    }
}

function renderProductsGrid(products) {
    const grid = document.getElementById('productsGrid');
    if (!products || products.length === 0) {
        grid.innerHTML = `<div class="empty-state"><i class="fa-solid fa-box-open"></i><p>No products found in this category.</p></div>`;
        return;
    }

    grid.innerHTML = products.map(p => {
        const isWishlisted = state.wishlist.some(w => w.productId === p.id);
        const offerActive = p.offerActive && p.offerPercentage > 0;
        const discountBadge = offerActive ? `<span class="pc-discount-pct">${p.offerPercentage}% off</span>` : '';
        const originalPrice = offerActive ? `<span class="pc-original-price">₹${p.price.toFixed(2)}</span>` : '';
        const finalPrice = offerActive ? (p.finalPrice || (p.price * (1 - p.offerPercentage / 100))) : p.price;

        return `
            <div class="product-card" onclick="openProductDetailModal(${p.id})">
                <div class="pc-img-wrap">
                    <img src="${p.imageUrl || 'https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=300'}" alt="${p.name}" loading="lazy">
                    <button class="btn-wishlist-card ${isWishlisted ? 'active' : ''}" onclick="event.stopPropagation(); toggleWishlist(${p.id})">
                        <i class="fa-${isWishlisted ? 'solid' : 'regular'} fa-heart"></i>
                    </button>
                </div>
                <span class="pc-brand">${p.brand || 'GENERIC'}</span>
                <h3 class="pc-title" title="${p.name}">${p.name}</h3>
                <div class="pc-rating-row">
                    <span class="rating-badge">${(p.averageRating || 4.5).toFixed(1)} <i class="fa-solid fa-star"></i></span>
                    <span class="pc-reviews-count">(${p.totalReviews || 0})</span>
                </div>
                <div class="pc-price-row">
                    <span class="pc-final-price">₹${finalPrice.toFixed(2)}</span>
                    ${originalPrice}
                    ${discountBadge}
                </div>
                <div class="pc-card-footer">
                    <button class="btn-card-cart" onclick="event.stopPropagation(); quickAddToCart(${p.id}, '${escapeHtml(p.name)}', ${finalPrice}, '${p.imageUrl}')">
                        <i class="fa-solid fa-cart-shopping"></i> Add to Cart
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

async function filterByCategory(category, evt) {
    document.querySelectorAll('.cat-item').forEach(c => c.classList.remove('active'));
    const target = evt ? evt.currentTarget : (typeof event !== 'undefined' ? event.currentTarget : null);
    if (target) target.classList.add('active');

    document.getElementById('productsSectionTitle').textContent = category === 'All' ? 'Featured Products & Best Deals' : `${category} Products`;

    if (category === 'All') {
        loadProducts();
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/products/category/${encodeURIComponent(category)}`);
        if (!res.ok) throw new Error('Failed to filter products');
        state.products = await res.json();
        renderProductsGrid(state.products);
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function loadActiveOffers() {
    try {
        const res = await fetch(`${API_BASE}/products/offers/active`);
        if (!res.ok) throw new Error('Failed to load active offers');
        const offers = await res.json();
        document.getElementById('productsSectionTitle').textContent = '🔥 Super Saver Deals & Offers';
        document.getElementById('productsSectionSubtitle').textContent = 'Limited time merchant discount offers';
        state.products = offers;
        renderProductsGrid(offers);
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function searchProducts() {
    const keyword = document.getElementById('searchInput').value.trim();
    if (!keyword) {
        loadProducts();
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/products/search?name=${encodeURIComponent(keyword)}`);
        if (!res.ok) throw new Error('Search failed');
        state.products = await res.json();
        document.getElementById('productsSectionTitle').textContent = `Search Results for "${keyword}"`;
        renderProductsGrid(state.products);
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function sortProducts(criteria) {
    let sorted = [...state.products];
    if (criteria === 'price-low') {
        sorted.sort((a, b) => (a.finalPrice || a.price) - (b.finalPrice || b.price));
    } else if (criteria === 'price-high') {
        sorted.sort((a, b) => (b.finalPrice || b.price) - (a.finalPrice || a.price));
    } else if (criteria === 'rating') {
        sorted.sort((a, b) => (b.averageRating || 0) - (a.averageRating || 0));
    } else if (criteria === 'discount') {
        sorted.sort((a, b) => (b.offerPercentage || 0) - (a.offerPercentage || 0));
    }
    renderProductsGrid(sorted);
}

// ==========================================================================
// Product Details & Reviews (Flipkart-Style Two-Column Modal)
// ==========================================================================
async function openProductDetailModal(productId) {
    try {
        const res = await fetch(`${API_BASE}/products/${productId}`);
        if (!res.ok) throw new Error('Failed to load product details');
        const p = await res.json();
        state.currentProduct = p;

        document.getElementById('pdImage').src = p.imageUrl || 'https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=400';
        document.getElementById('pdBrand').textContent = p.brand || 'GENERIC';
        document.getElementById('pdTitle').textContent = p.name;
        document.getElementById('pdRatingBadge').innerHTML = `${(p.averageRating || 4.5).toFixed(1)} <i class="fa-solid fa-star"></i>`;
        document.getElementById('pdReviewCount').textContent = `(${p.totalReviews || 0} Ratings & ${p.totalReviews || 0} Reviews)`;
        document.getElementById('pdSeller').textContent = `${p.merchantName || 'Alice Merchant'} (4.8 ★)`;
        document.getElementById('pdStock').textContent = (p.stockQuantity > 0) ? `In Stock (${p.stockQuantity} available)` : 'Out of Stock';
        document.getElementById('pdCategory').textContent = p.category;
        document.getElementById('pdDescription').textContent = p.description || 'No description provided.';

        const offerActive = p.offerActive && p.offerPercentage > 0;
        const finalPrice = offerActive ? (p.finalPrice || (p.price * (1 - p.offerPercentage / 100))) : p.price;

        document.getElementById('pdFinalPrice').textContent = `₹${finalPrice.toFixed(2)}`;
        document.getElementById('pdOriginalPrice').textContent = offerActive ? `₹${p.price.toFixed(2)}` : '';
        document.getElementById('pdDiscountPercent').textContent = offerActive ? `${p.offerPercentage}% OFF` : '';

        // Wishlist button state
        const isWishlisted = state.wishlist.some(w => w.productId === p.id);
        const wb = document.getElementById('pdWishlistBtn');
        if (isWishlisted) {
            wb.classList.add('active');
            wb.innerHTML = '<i class="fa-solid fa-heart"></i>';
        } else {
            wb.classList.remove('active');
            wb.innerHTML = '<i class="fa-regular fa-heart"></i>';
        }

        // Render Reviews
        renderProductReviews(p.reviews || []);

        openModal('productDetailModal');
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function renderProductReviews(reviews) {
    const list = document.getElementById('pdReviewsList');
    if (!reviews || reviews.length === 0) {
        list.innerHTML = `<div class="empty-reviews"><p>No customer reviews yet. Be the first to review this product!</p></div>`;
        return;
    }

    list.innerHTML = reviews.map(r => {
        const isOwner = state.user && state.user.id === r.userId;
        const isAdmin = state.user && state.user.role === 'ADMIN';
        const deleteBtn = (isOwner || isAdmin) ? `<button class="btn btn-sm btn-outline text-danger" onclick="deleteReview(${r.id})"><i class="fa-solid fa-trash"></i></button>` : '';

        return `
            <div class="review-item-card">
                <div class="ric-top">
                    <div>
                        <span class="rating-badge">${r.rating} <i class="fa-solid fa-star"></i></span>
                        <strong class="ric-user" style="margin-left:8px;">${r.userName || 'Verified Buyer'}</strong>
                    </div>
                    <div style="display:flex; align-items:center; gap:8px;">
                        <span class="ric-date">${formatDate(r.createdAt)}</span>
                        ${deleteBtn}
                    </div>
                </div>
                <p class="ric-comment">${escapeHtml(r.comment)}</p>
            </div>
        `;
    }).join('');
}

function openWriteReviewModal() {
    if (!state.user) {
        showToast('Please login to write a product review', 'info');
        openAuthModal('login');
        return;
    }
    if (!state.currentProduct) return;

    document.getElementById('reviewProductId').value = state.currentProduct.id;
    document.getElementById('reviewComment').value = '';
    openModal('reviewModal');
}

async function handleReviewSubmit(e) {
    e.preventDefault();
    if (!state.user || !state.currentProduct) return;

    const req = {
        productId: state.currentProduct.id,
        userId: state.user.id,
        userName: state.user.fullName || state.user.email.split('@')[0],
        rating: parseInt(document.getElementById('reviewRating').value),
        comment: document.getElementById('reviewComment').value.trim()
    };

    try {
        const res = await fetch(`${API_BASE}/reviews`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(req)
        });

        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || 'Failed to submit review');
        }

        showToast('Review submitted successfully!', 'success');
        closeModal('reviewModal');
        // Refresh product details
        openProductDetailModal(state.currentProduct.id);
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function deleteReview(reviewId) {
    if (!confirm('Are you sure you want to delete this review?')) return;
    try {
        const url = `${API_BASE}/reviews/${reviewId}?userId=${state.user.id}&userRole=${state.user.role}`;
        const res = await fetch(url, { method: 'DELETE' });
        if (!res.ok) throw new Error('Failed to delete review');

        showToast('Review deleted successfully', 'success');
        if (state.currentProduct) {
            openProductDetailModal(state.currentProduct.id);
        }
        if (state.activeView === 'admin') {
            loadAdminReviews();
        }
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ==========================================================================
// Wishlist Engine
// ==========================================================================
async function loadWishlist() {
    if (!state.user) return;
    try {
        const res = await fetch(`${API_BASE}/wishlist/${state.user.id}`);
        if (!res.ok) throw new Error('Failed to load wishlist');
        state.wishlist = await res.json();

        // Update counts
        const count = state.wishlist.length;
        document.getElementById('wishlistCount').textContent = count;
        document.getElementById('wishlistTotalHeader').textContent = count;

        const badge = document.getElementById('wishlistTabBadge');
        if (count > 0) {
            badge.textContent = count;
            badge.style.display = 'inline-block';
        } else {
            badge.style.display = 'none';
        }

        renderWishlistGrid(state.wishlist);
    } catch (err) {
        console.error('Wishlist error:', err);
    }
}

function renderWishlistGrid(items) {
    const grid = document.getElementById('wishlistGrid');
    if (!items || items.length === 0) {
        grid.innerHTML = `
            <div class="empty-state" style="grid-column: 1 / -1; padding: 40px; text-align: center;">
                <i class="fa-solid fa-heart" style="font-size:48px; color:#ddd; margin-bottom:12px;"></i>
                <h3>Your Wishlist is Empty</h3>
                <p class="section-sub">Explore our catalog and add items you want to buy later.</p>
                <button class="btn btn-primary" style="margin-top:16px;" onclick="switchView('storefront')">Explore Products</button>
            </div>
        `;
        return;
    }

    grid.innerHTML = items.map(w => {
        const offerActive = w.discountPercentage > 0;
        return `
            <div class="wishlist-card">
                <div class="wc-img-wrap">
                    <img src="${w.imageUrl || 'https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=300'}" alt="${w.productName}">
                </div>
                <span class="pc-brand">${w.brand || 'GENERIC'}</span>
                <h4 class="pc-title">${w.productName}</h4>
                <div class="pc-price-row">
                    <span class="pc-final-price">₹${(w.offerPrice || w.originalPrice).toFixed(2)}</span>
                    ${offerActive ? `<span class="pc-original-price">₹${w.originalPrice.toFixed(2)}</span>` : ''}
                    ${offerActive ? `<span class="pc-discount-pct">${w.discountPercentage}% OFF</span>` : ''}
                </div>
                <span class="stock-badge ${w.stockStatus === 'In Stock' ? 'text-success' : 'text-danger'}" style="font-size:12px; font-weight:600; margin-bottom:8px;">
                    ${w.stockStatus || 'In Stock'}
                </span>
                <div class="wc-actions">
                    <button class="btn btn-primary btn-sm" style="flex:1;" onclick="moveWishlistToCart(${w.productId})">
                        <i class="fa-solid fa-cart-arrow-down"></i> Move to Cart
                    </button>
                    <button class="btn btn-outline btn-sm text-danger" onclick="removeFromWishlist(${w.productId})">
                        <i class="fa-solid fa-trash"></i>
                    </button>
                </div>
            </div>
        `;
    }).join('');
}

async function toggleWishlist(productId) {
    if (!state.user) {
        showToast('Please login to use Wishlist', 'info');
        openAuthModal('login');
        return;
    }

    const exists = state.wishlist.some(w => w.productId === productId);
    if (exists) {
        await removeFromWishlist(productId);
    } else {
        await addToWishlist(productId);
    }
}

async function togglePdWishlist() {
    if (!state.currentProduct) return;
    await toggleWishlist(state.currentProduct.id);
    const isWishlisted = state.wishlist.some(w => w.productId === state.currentProduct.id);
    const wb = document.getElementById('pdWishlistBtn');
    if (isWishlisted) {
        wb.classList.add('active');
        wb.innerHTML = '<i class="fa-solid fa-heart"></i>';
    } else {
        wb.classList.remove('active');
        wb.innerHTML = '<i class="fa-regular fa-heart"></i>';
    }
}

async function addToWishlist(productId) {
    try {
        const res = await fetch(`${API_BASE}/wishlist/${state.user.id}/add/${productId}`, { method: 'POST' });
        if (!res.ok) throw new Error('Failed to add to wishlist');
        showToast('Added to Wishlist!', 'success');
        await loadWishlist();
        renderProductsGrid(state.products);
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function removeFromWishlist(productId) {
    try {
        const res = await fetch(`${API_BASE}/wishlist/${state.user.id}/remove/${productId}`, { method: 'DELETE' });
        if (!res.ok) throw new Error('Failed to remove from wishlist');
        showToast('Removed from Wishlist', 'info');
        await loadWishlist();
        renderProductsGrid(state.products);
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function moveWishlistToCart(productId) {
    try {
        const res = await fetch(`${API_BASE}/wishlist/${state.user.id}/move-to-cart/${productId}`, { method: 'POST' });
        if (!res.ok) throw new Error('Failed to move item to cart');
        showToast('Moved to Cart successfully!', 'success');
        await Promise.all([loadWishlist(), loadCart()]);
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function clearWishlist() {
    if (!state.user) return;
    if (!confirm('Clear all items from your wishlist?')) return;
    try {
        const res = await fetch(`${API_BASE}/wishlist/${state.user.id}/clear`, { method: 'DELETE' });
        if (!res.ok) throw new Error('Failed to clear wishlist');
        showToast('Wishlist cleared', 'info');
        await loadWishlist();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ==========================================================================
// Cart Engine (Two-Column Flipkart Breakdown)
// ==========================================================================
async function loadCart() {
    if (!state.user) return;
    try {
        const res = await fetch(`${API_BASE}/cart/${state.user.id}`);
        if (!res.ok) throw new Error('Failed to load cart');
        state.cart = await res.json();

        const count = state.cart.items ? state.cart.items.reduce((sum, i) => sum + i.quantity, 0) : 0;
        document.getElementById('cartCount').textContent = count;
        document.getElementById('cartModalCount').textContent = count;
    } catch (err) {
        console.error('Cart error:', err);
    }
}

async function quickAddToCart(productId, name, price, imageUrl) {
    if (!state.user) {
        showToast('Please login to add items to cart', 'info');
        openAuthModal('login');
        return;
    }

    try {
        const req = { productId, productName: name, price, quantity: 1, imageUrl };
        const res = await fetch(`${API_BASE}/cart/${state.user.id}/items`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(req)
        });

        if (!res.ok) throw new Error('Failed to add item to cart');
        showToast(`Added ${name} to Cart!`, 'success');
        await loadCart();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function addCurrentPdToCart() {
    if (!state.currentProduct) return;
    const p = state.currentProduct;
    const offerActive = p.offerActive && p.offerPercentage > 0;
    const finalPrice = offerActive ? (p.finalPrice || (p.price * (1 - p.offerPercentage / 100))) : p.price;
    quickAddToCart(p.id, p.name, finalPrice, p.imageUrl);
}

function buyCurrentPdNow() {
    addCurrentPdToCart();
    closeModal('productDetailModal');
    setTimeout(() => openCartModal(), 200);
}

async function openCartModal() {
    if (!state.user) {
        showToast('Please login to view your cart', 'info');
        openAuthModal('login');
        return;
    }
    await loadCart();
    renderCartModal();
    openModal('cartModal');
}

function renderCartModal() {
    const list = document.getElementById('cartItemsList');
    const items = (state.cart && state.cart.items) ? state.cart.items : [];

    if (items.length === 0) {
        list.innerHTML = `
            <div class="empty-state" style="padding: 40px; text-align: center;">
                <i class="fa-solid fa-cart-shopping" style="font-size:48px; color:#ddd; margin-bottom:12px;"></i>
                <h3>Your Cart is Empty</h3>
                <p class="section-sub">Add items from your wishlist or products catalog</p>
            </div>
        `;
        document.getElementById('cartSubtotal').textContent = '₹0.00';
        document.getElementById('cartDiscount').textContent = '- ₹0.00';
        document.getElementById('cartGrandTotal').textContent = '₹0.00';
        document.getElementById('cartItemsTotalQty').textContent = '0';
        document.getElementById('cartSaveAlert').textContent = 'Your cart is empty';
        return;
    }

    const totalQty = items.reduce((sum, i) => sum + i.quantity, 0);
    const subtotal = state.cart.totalAmount || 0;

    document.getElementById('cartSubtotal').textContent = `₹${subtotal.toFixed(2)}`;
    document.getElementById('cartDiscount').textContent = `- ₹0.00`;
    document.getElementById('cartGrandTotal').textContent = `₹${subtotal.toFixed(2)}`;
    document.getElementById('cartItemsTotalQty').textContent = totalQty;
    document.getElementById('cartSaveAlert').textContent = `Free Express Delivery applied on your order!`;

    list.innerHTML = items.map(item => {
        const pId = item.productId || item.id;
        return `
            <div class="cart-item-row">
                <img src="${item.imageUrl || 'https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?w=200'}" class="ci-img" alt="${escapeHtml(item.productName)}">
                <div class="ci-info">
                    <h4 class="ci-title">${escapeHtml(item.productName)}</h4>
                    <div class="ci-price">₹${item.price.toFixed(2)} × ${item.quantity} = <strong>₹${item.subtotal.toFixed(2)}</strong></div>
                    <div class="ci-qty-stepper">
                        <button class="qty-btn" type="button" onclick="updateCartItemQty(${pId}, ${item.quantity - 1})">-</button>
                        <span><strong>${item.quantity}</strong></span>
                        <button class="qty-btn" type="button" onclick="updateCartItemQty(${pId}, ${item.quantity + 1})">+</button>
                        <button class="btn btn-sm btn-outline text-danger" type="button" style="margin-left:16px;" onclick="removeCartItem(${pId})">
                            <i class="fa-solid fa-trash"></i> Remove
                        </button>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

async function updateCartItemQty(productId, newQty) {
    if (newQty <= 0) {
        removeCartItem(productId);
        return;
    }
    try {
        const res = await fetch(`${API_BASE}/cart/${state.user.id}/items/${productId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ quantity: newQty })
        });
        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || 'Failed to update quantity');
        }
        state.cart = await res.json();
        const count = state.cart.items ? state.cart.items.reduce((sum, i) => sum + i.quantity, 0) : 0;
        document.getElementById('cartCount').textContent = count;
        document.getElementById('cartModalCount').textContent = count;
        renderCartModal();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function removeCartItem(productId) {
    try {
        const res = await fetch(`${API_BASE}/cart/${state.user.id}/items/${productId}`, { method: 'DELETE' });
        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || 'Failed to remove item');
        }
        state.cart = await res.json();
        const count = state.cart.items ? state.cart.items.reduce((sum, i) => sum + i.quantity, 0) : 0;
        document.getElementById('cartCount').textContent = count;
        document.getElementById('cartModalCount').textContent = count;
        showToast('Item removed from cart', 'info');
        renderCartModal();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function clearUserCart() {
    if (!state.user || !state.cart || !state.cart.items || state.cart.items.length === 0) return;
    if (!confirm('Are you sure you want to clear all items from your cart?')) return;
    try {
        const res = await fetch(`${API_BASE}/cart/${state.user.id}/clear`, { method: 'DELETE' });
        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || 'Failed to clear cart');
        }
        state.cart = { userId: state.user.id, totalAmount: 0.0, items: [] };
        document.getElementById('cartCount').textContent = '0';
        document.getElementById('cartModalCount').textContent = '0';
        showToast('Cart cleared', 'info');
        renderCartModal();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ==========================================================================
// Multi-Step Checkout & Payment Service Integration (8087)
// ==========================================================================
function openCheckoutModal() {
    if (!state.cart || !state.cart.items || state.cart.items.length === 0) {
        showToast('Your cart is empty', 'warning');
        return;
    }

    closeModal('cartModal');

    // Pre-fill address
    document.getElementById('checkoutAddress').value = (state.user && state.user.address) ? state.user.address : 'Express Hub 4, Indiranagar, Bangalore 560038';
    renderCheckoutItems();
    openModal('checkoutModal');
}

function renderCheckoutItems() {
    const list = document.getElementById('checkoutItemsList');
    if (!list) return;

    if (!state.cart || !state.cart.items || state.cart.items.length === 0) {
        list.innerHTML = `<div style="text-align:center; padding:12px; color:#888;">Cart is empty.</div>`;
        document.getElementById('coItemsQty').textContent = '0';
        document.getElementById('coFinalTotal').textContent = '₹0.00';
        return;
    }

    const totalQty = state.cart.items.reduce((s, i) => s + i.quantity, 0);
    const totalAmt = state.cart.totalAmount || 0;

    document.getElementById('coItemsQty').textContent = totalQty;
    document.getElementById('coFinalTotal').textContent = `₹${totalAmt.toFixed(2)}`;

    list.innerHTML = state.cart.items.map(item => {
        const pId = item.productId || item.id;
        return `
            <div style="display:flex; justify-content:space-between; align-items:center; background:#f9f9fa; border:1px solid #e0e0e0; border-radius:6px; padding:8px 12px;">
                <div style="display:flex; align-items:center; gap:10px;">
                    <img src="${item.imageUrl || 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=100'}" style="width:40px; height:40px; object-fit:contain; border-radius:4px; background:#fff;">
                    <div>
                        <strong style="font-size:13px; display:block; max-width:200px; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">${escapeHtml(item.productName)}</strong>
                        <span style="font-size:12px; color:#666;">₹${item.price.toFixed(2)} each</span>
                    </div>
                </div>
                <div style="display:flex; align-items:center; gap:8px;">
                    <div style="display:flex; align-items:center; border:1px solid #ccc; border-radius:4px; overflow:hidden; background:#fff;">
                        <button type="button" style="border:none; background:#eee; padding:3px 8px; cursor:pointer; font-weight:700;" onclick="updateCheckoutItemQty(${pId}, ${item.quantity - 1})">-</button>
                        <span style="padding:0 8px; font-size:12px; font-weight:700;">${item.quantity}</span>
                        <button type="button" style="border:none; background:#eee; padding:3px 8px; cursor:pointer; font-weight:700;" onclick="updateCheckoutItemQty(${pId}, ${item.quantity + 1})">+</button>
                    </div>
                    <strong style="font-size:13px; min-width:65px; text-align:right;">₹${item.subtotal.toFixed(2)}</strong>
                    <button type="button" style="border:none; background:transparent; color:#d32f2f; cursor:pointer; font-size:14px; margin-left:4px;" onclick="removeCheckoutItem(${pId})"><i class="fa-solid fa-trash"></i></button>
                </div>
            </div>
        `;
    }).join('');
}

async function updateCheckoutItemQty(productId, newQty) {
    if (newQty <= 0) {
        removeCheckoutItem(productId);
        return;
    }
    try {
        const res = await fetch(`${API_BASE}/cart/${state.user.id}/items/${productId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ quantity: newQty })
        });
        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || 'Failed to update quantity');
        }
        state.cart = await res.json();
        const count = state.cart.items ? state.cart.items.reduce((sum, i) => sum + i.quantity, 0) : 0;
        document.getElementById('cartCount').textContent = count;
        document.getElementById('cartModalCount').textContent = count;
        renderCheckoutItems();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function removeCheckoutItem(productId) {
    try {
        const res = await fetch(`${API_BASE}/cart/${state.user.id}/items/${productId}`, { method: 'DELETE' });
        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || 'Failed to remove item');
        }
        state.cart = await res.json();
        const count = state.cart.items ? state.cart.items.reduce((sum, i) => sum + i.quantity, 0) : 0;
        document.getElementById('cartCount').textContent = count;
        document.getElementById('cartModalCount').textContent = count;
        showToast('Item removed from cart', 'info');
        renderCheckoutItems();
        if (!state.cart || !state.cart.items || state.cart.items.length === 0) {
            closeModal('checkoutModal');
        }
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function togglePaymentInputs(method) {
    document.getElementById('upiInputBox').style.display = (method === 'UPI') ? 'block' : 'none';
    document.getElementById('cardInputBox').style.display = (method === 'CARD') ? 'block' : 'none';
}

async function submitCheckoutOrder() {
    const address = document.getElementById('checkoutAddress').value.trim();
    if (!address) {
        showToast('Please enter shipping address', 'warning');
        return;
    }

    const methodRadio = document.querySelector('input[name="paymentMethodRadio"]:checked');
    const paymentMethod = methodRadio ? methodRadio.value : 'WALLET';

    const req = {
        userId: state.user.id,
        paymentMethod: paymentMethod,
        shippingAddress: address
    };

    if (paymentMethod === 'UPI') {
        const upiId = document.getElementById('checkoutUpiId').value.trim();
        req.upiId = upiId || 'customer@okaxis';
    } else if (paymentMethod === 'CARD') {
        req.cardHolderName = document.getElementById('checkoutCardHolder').value.trim() || 'John Customer';
        req.cardNumber = document.getElementById('checkoutCardNumber').value.trim() || '4111111111111234';
        req.cardExpiry = document.getElementById('checkoutCardExpiry').value.trim() || '12/28';
        req.cardCvv = document.getElementById('checkoutCardCvv').value.trim() || '123';
    }

    try {
        const res = await fetch(`${API_BASE}/orders`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(req)
        });

        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || 'Order checkout failed');
        }

        const order = await res.json();
        showToast(`🎉 Order #${order.id} placed successfully!`, 'success');
        closeModal('checkoutModal');

        // Refresh Cart, Wallet and open Tracking Modal
        await Promise.all([loadCart(), loadWalletBalance(), loadNotifications()]);
        setTimeout(() => openOrderTrackingModal(order.id), 300);
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ==========================================================================
// Customer Orders & 8-Stage Visual Tracking Timeline
// ==========================================================================
async function openOrdersModal() {
    if (!state.user) {
        showToast('Please login to view orders', 'info');
        openAuthModal('login');
        return;
    }
    await loadCustomerOrders();
    openModal('ordersModal');
}

async function loadCustomerOrders() {
    try {
        const res = await fetch(`${API_BASE}/orders/user/${state.user.id}`);
        if (!res.ok) throw new Error('Failed to load orders');
        state.orders = await res.json();
        renderCustomerOrders(state.orders);
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function filterCustomerOrders(filter, evt) {
    document.querySelectorAll('.ot-btn').forEach(b => b.classList.remove('active'));
    const target = evt ? evt.currentTarget : (typeof event !== 'undefined' ? event.currentTarget : null);
    if (target) target.classList.add('active');

    let filtered = state.orders;
    if (filter === 'ACTIVE') {
        filtered = state.orders.filter(o => !['DELIVERED', 'CANCELLED', 'REJECTED'].includes(o.orderStatus));
    } else if (filter === 'DELIVERED') {
        filtered = state.orders.filter(o => o.orderStatus === 'DELIVERED');
    } else if (filter === 'REJECTED') {
        filtered = state.orders.filter(o => o.orderStatus === 'REJECTED');
    }
    renderCustomerOrders(filtered);
}

function renderCustomerOrders(orders) {
    const container = document.getElementById('customerOrdersList');
    if (!orders || orders.length === 0) {
        container.innerHTML = `
            <div class="empty-state" style="padding: 40px; text-align: center;">
                <i class="fa-solid fa-box-open" style="font-size:48px; color:#ddd; margin-bottom:12px;"></i>
                <h3>No Orders Placed Yet</h3>
                <p class="section-sub">Start shopping to see your ordered items here.</p>
            </div>
        `;
        return;
    }

    container.innerHTML = orders.map(o => {
        const isDelivered = o.orderStatus === 'DELIVERED';
        const isRejected = o.orderStatus === 'REJECTED';
        const isCancelled = o.orderStatus === 'CANCELLED';
        const canCancel = !['DELIVERED', 'CANCELLED', 'REJECTED', 'OUT_FOR_DELIVERY', 'PICKED_UP'].includes(o.orderStatus);

        const rejectionBanner = isRejected ? `
            <div style="background:#ffebee; color:#c62828; padding:8px 12px; border-radius:4px; font-size:12px; margin-top:8px;">
                <strong><i class="fa-solid fa-triangle-exclamation"></i> Rejection Reason:</strong> ${escapeHtml(o.rejectionReason || 'Seller was unable to fulfill this order.')}
            </div>
        ` : '';

        return `
            <div class="order-card" style="background:#fff; border:1px solid #e0e0e0; border-radius:8px; padding:16px; margin-bottom:12px;">
                <div style="display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid #eee; padding-bottom:10px; margin-bottom:12px;">
                    <div>
                        <strong>Order #${o.id}</strong>
                        <span style="color:#888; font-size:12px; margin-left:10px;">${formatDate(o.orderDate)}</span>
                    </div>
                    <div>
                        <span class="badge ${getStatusBadgeClass(o.orderStatus)}">${o.orderStatus}</span>
                    </div>
                </div>
                <div style="display:flex; justify-content:space-between; align-items:center;">
                    <div>
                        <p style="font-size:14px; font-weight:600;">Total: ₹${o.totalAmount.toFixed(2)} (${o.items ? o.items.length : 0} items)</p>
                        <p style="font-size:12px; color:#666;">Payment: <strong>${o.paymentMethod}</strong> (${o.paymentStatus})</p>
                        <p style="font-size:12px; color:#888;">Delivery To: ${o.shippingAddress}</p>
                    </div>
                    <div style="display:flex; flex-direction:column; gap:6px;">
                        <button class="btn btn-primary btn-sm" onclick="openOrderTrackingModal(${o.id})">
                            <i class="fa-solid fa-truck-fast"></i> Track Order
                        </button>
                        ${canCancel ? `
                            <button class="btn btn-outline btn-sm text-danger" style="border-color:#d32f2f; color:#d32f2f;" onclick="cancelCustomerOrder(${o.id})">
                                <i class="fa-solid fa-ban"></i> Cancel Order
                            </button>
                        ` : ''}
                        ${isDelivered ? `
                            <button class="btn btn-outline btn-sm text-success" onclick="openReviewDeliveredOrder(${o.id})">
                                <i class="fa-solid fa-star"></i> Rate & Review
                            </button>
                        ` : ''}
                    </div>
                </div>
                ${rejectionBanner}
            </div>
        `;
    }).join('');
}

async function cancelCustomerOrder(orderId) {
    if (!state.user || !state.user.id) {
        showToast('Please login to cancel order', 'warning');
        return;
    }

    if (!confirm(`Are you sure you want to cancel Order #${orderId}? If paid with wallet, the refund will be credited back immediately.`)) {
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/orders/${orderId}/cancel?userId=${state.user.id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ reason: 'Cancelled by customer from My Orders' })
        });

        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || 'Failed to cancel order');
        }

        showToast(`Order #${orderId} cancelled successfully!`, 'info');
        await Promise.all([loadCustomerOrders(), loadWalletBalance(), loadNotifications()]);
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function openOrderTrackingModal(orderId) {
    try {
        const res = await fetch(`${API_BASE}/orders/${orderId}`);
        if (!res.ok) throw new Error('Failed to load tracking info');
        const o = await res.json();

        document.getElementById('trackOrderId').textContent = `#${o.id}`;
        document.getElementById('trackOrderAmount').textContent = `₹${o.totalAmount.toFixed(2)}`;
        document.getElementById('trackPaymentStatus').textContent = `${o.paymentMethod} (${o.paymentStatus})`;
        document.getElementById('trackDeliveryPartner').textContent = o.deliveryPartnerName ? `${o.deliveryPartnerName} (${o.deliveryPartnerPhone || 'N/A'})` : 'Awaiting Assignment';
        document.getElementById('trackShippingAddress').textContent = o.shippingAddress;
        document.getElementById('trackOrderBadge').textContent = o.orderStatus;
        document.getElementById('trackOrderBadge').className = `badge badge-lg ${getStatusBadgeClass(o.orderStatus)}`;

        renderTrackingStepper(o.orderStatus, o.rejectionReason);
        openModal('orderTrackingModal');
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function renderTrackingStepper(status, rejectionReason) {
    const stepper = document.getElementById('trackingStepper');

    if (status === 'REJECTED') {
        stepper.innerHTML = `
            <div class="stepper-stage rejected">
                <div class="stage-icon"><i class="fa-solid fa-ban"></i></div>
                <div class="stage-content">
                    <div class="stage-title text-danger">Order Rejected by Merchant</div>
                    <div class="stage-desc">${rejectionReason || 'Seller was unable to fulfill this order.'}</div>
                </div>
            </div>
        `;
        return;
    }

    const stages = [
        { key: 'PLACED', title: 'Order Placed', desc: 'We have received your order' },
        { key: 'ACCEPTED', title: 'Order Accepted', desc: 'Seller confirmed order fulfillment' },
        { key: 'PACKING', title: 'Packing Items', desc: 'Item is being safely packed' },
        { key: 'READY_FOR_DELIVERY', title: 'Ready for Delivery', desc: 'Package ready at seller warehouse' },
        { key: 'ASSIGNED_TO_DELIVERY', title: 'Assigned to Partner', desc: 'Assigned to verified delivery personnel' },
        { key: 'PICKED_UP', title: 'Picked Up by Partner', desc: 'Delivery partner has received parcel' },
        { key: 'OUT_FOR_DELIVERY', title: 'Out for Delivery', desc: 'Courier is on the way to your doorstep' },
        { key: 'DELIVERED', title: 'Delivered', desc: 'Package handed over to customer' }
    ];

    const stageHierarchy = ['PLACED', 'ACCEPTED', 'PACKING', 'READY_FOR_DELIVERY', 'ASSIGNED_TO_DELIVERY', 'DELIVERY_ACCEPTED', 'PICKED_UP', 'OUT_FOR_DELIVERY', 'DELIVERED'];
    const currentIdx = stageHierarchy.indexOf(status);

    stepper.innerHTML = stages.map((s, idx) => {
        let stateClass = '';
        let iconHtml = '<i class="fa-solid fa-circle"></i>';

        const stageIdx = stageHierarchy.indexOf(s.key);
        if (stageIdx <= currentIdx && currentIdx !== -1) {
            stateClass = 'completed';
            iconHtml = '<i class="fa-solid fa-check"></i>';
        } else if (stageIdx === currentIdx + 1) {
            stateClass = 'active';
            iconHtml = '<i class="fa-solid fa-arrow-right"></i>';
        }

        return `
            <div class="stepper-stage ${stateClass}">
                <div class="stage-icon">${iconHtml}</div>
                <div class="stage-content">
                    <div class="stage-title">${s.title}</div>
                    <div class="stage-desc">${s.desc}</div>
                </div>
            </div>
        `;
    }).join('');
}

function openReviewDeliveredOrder(orderId) {
    const order = state.orders.find(o => o.id === orderId);
    if (order && order.items && order.items.length > 0) {
        openProductDetailModal(order.items[0].productId);
        setTimeout(() => openWriteReviewModal(), 300);
    }
}

// ==========================================================================
// Merchant Seller Hub & Orders Fulfiller
// ==========================================================================
async function loadMerchantPortal() {
    const merchantId = (state.user && state.user.id) ? state.user.id : 2;

    try {
        const [revRes, prodRes, ordRes] = await Promise.all([
            fetch(`${API_BASE}/orders/merchant/${merchantId}/revenue`),
            fetch(`${API_BASE}/products/merchant/${merchantId}`),
            fetch(`${API_BASE}/orders/merchant/${merchantId}`)
        ]);

        if (revRes.ok) {
            const rev = await revRes.json();
            document.getElementById('kpiTotalRevenue').textContent = `₹${rev.totalRevenue.toFixed(2)}`;
            document.getElementById('kpiCompletedOrders').textContent = `${rev.completedOrdersCount} Completed Orders`;
            document.getElementById('kpiPendingRevenue').textContent = `₹${rev.pendingRevenue.toFixed(2)}`;
            document.getElementById('kpiPendingOrders').textContent = `${rev.pendingOrdersCount} In-Progress Orders`;
            document.getElementById('kpiUnitsSold').textContent = rev.totalUnitsSold;
        }

        if (prodRes.ok) {
            const prods = await prodRes.json();
            document.getElementById('kpiMerchantProducts').textContent = prods.length;
            renderMerchantProducts(prods);
            renderMerchantOffers(prods);
        }

        if (ordRes.ok) {
            state.merchantOrders = await ordRes.json();
            document.getElementById('merchantOrdersBadge').textContent = state.merchantOrders.length;
            renderMerchantOrders(state.merchantOrders);
        }
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function switchMerchantTab(tab, evt) {
    state.activeMerchantTab = tab;
    document.querySelectorAll('.m-tab').forEach(b => b.classList.remove('active'));
    const target = evt ? evt.currentTarget : (typeof event !== 'undefined' ? event.currentTarget : null);
    if (target) target.classList.add('active');

    document.getElementById('merchantTabOrders').style.display = (tab === 'orders') ? 'block' : 'none';
    document.getElementById('merchantTabProducts').style.display = (tab === 'products') ? 'block' : 'none';
    document.getElementById('merchantTabOffers').style.display = (tab === 'offers') ? 'block' : 'none';
}

function filterMerchantOrders(status, evt) {
    document.querySelectorAll('.of-btn').forEach(b => b.classList.remove('active'));
    const target = evt ? evt.currentTarget : (typeof event !== 'undefined' ? event.currentTarget : null);
    if (target) target.classList.add('active');

    let filtered = state.merchantOrders;
    if (status !== 'ALL') {
        if (status === 'NEW') {
            filtered = state.merchantOrders.filter(o => ['PLACED', 'CONFIRMED'].includes(o.orderStatus));
        } else {
            filtered = state.merchantOrders.filter(o => o.orderStatus === status);
        }
    }
    renderMerchantOrders(filtered);
}

function renderMerchantOrders(orders) {
    const container = document.getElementById('merchantOrdersList');
    if (!orders || orders.length === 0) {
        container.innerHTML = `<div class="empty-state" style="padding:30px; text-align:center;"><p>No merchant orders matching criteria.</p></div>`;
        return;
    }

    container.innerHTML = orders.map(o => {
        let actionButtons = '';
        const st = o.orderStatus;

        if (st === 'PLACED' || st === 'CONFIRMED') {
            actionButtons = `
                <button class="btn btn-success btn-sm" onclick="merchantAcceptOrder(${o.id})"><i class="fa-solid fa-check"></i> Accept</button>
                <button class="btn btn-danger btn-sm" onclick="openMerchantRejectModal(${o.id})"><i class="fa-solid fa-ban"></i> Reject</button>
            `;
        } else if (st === 'ACCEPTED') {
            actionButtons = `
                <button class="btn btn-primary btn-sm" onclick="merchantPackOrder(${o.id})"><i class="fa-solid fa-box"></i> Mark Packing</button>
            `;
        } else if (st === 'PACKING') {
            actionButtons = `
                <button class="btn btn-primary btn-sm" onclick="merchantReadyForDelivery(${o.id})"><i class="fa-solid fa-clipboard-check"></i> Ready for Delivery</button>
            `;
        } else if (st === 'READY_FOR_DELIVERY') {
            actionButtons = `
                <button class="btn btn-primary btn-sm" onclick="openAssignDeliveryModal(${o.id})"><i class="fa-solid fa-motorcycle"></i> Assign Partner</button>
            `;
        }

        return `
            <div class="order-card" style="background:#fff; border:1px solid #e0e0e0; border-radius:8px; padding:14px; margin-bottom:10px;">
                <div style="display:flex; justify-content:space-between; align-items:center;">
                    <div>
                        <strong>Order #${o.id}</strong> | <span style="color:#666; font-size:12px;">Customer #${o.userId}</span>
                        <p style="font-size:12px; color:#888; margin-top:2px;">${o.shippingAddress}</p>
                    </div>
                    <div style="text-align:right;">
                        <span class="badge ${getStatusBadgeClass(o.orderStatus)}">${o.orderStatus}</span>
                        <div style="font-size:14px; font-weight:700; margin-top:4px;">₹${o.totalAmount.toFixed(2)}</div>
                    </div>
                </div>
                <div style="display:flex; justify-content:space-between; align-items:center; margin-top:10px; border-top:1px dashed #eee; padding-top:8px;">
                    <div style="font-size:12px; color:#555;">
                        Partner: <strong>${o.deliveryPartnerName || 'Unassigned'}</strong>
                    </div>
                    <div style="display:flex; gap:6px;">
                        ${actionButtons}
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

async function merchantAcceptOrder(orderId) {
    try {
        const res = await fetch(`${API_BASE}/orders/${orderId}/accept?merchantId=${state.user.id}`, { method: 'PUT' });
        if (!res.ok) throw new Error('Failed to accept order');
        showToast(`Order #${orderId} Accepted!`, 'success');
        loadMerchantPortal();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function openMerchantRejectModal(orderId) {
    document.getElementById('rejectOrderId').value = orderId;
    document.getElementById('rejectReason').value = document.getElementById('rejectPreset').value;
    openModal('merchantRejectModal');
}

async function handleMerchantRejectSubmit(e) {
    e.preventDefault();
    const orderId = document.getElementById('rejectOrderId').value;
    const reason = document.getElementById('rejectReason').value.trim();

    try {
        const res = await fetch(`${API_BASE}/orders/${orderId}/reject?merchantId=${state.user.id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ reason })
        });
        if (!res.ok) throw new Error('Failed to reject order');
        showToast(`Order #${orderId} Rejected`, 'info');
        closeModal('merchantRejectModal');
        loadMerchantPortal();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function merchantPackOrder(orderId) {
    try {
        const res = await fetch(`${API_BASE}/orders/${orderId}/pack?merchantId=${state.user.id}`, { method: 'PUT' });
        if (!res.ok) throw new Error('Failed to update status');
        showToast(`Order #${orderId} is now Packing`, 'success');
        loadMerchantPortal();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function merchantReadyForDelivery(orderId) {
    try {
        const res = await fetch(`${API_BASE}/orders/${orderId}/ready-for-delivery?merchantId=${state.user.id}`, { method: 'PUT' });
        if (!res.ok) throw new Error('Failed to update status');
        showToast(`Order #${orderId} is Ready for Delivery`, 'success');
        loadMerchantPortal();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function openAssignDeliveryModal(orderId) {
    document.getElementById('assignOrderId').value = orderId;
    try {
        const res = await fetch(`${API_BASE}/profiles/delivery-partners`);
        const partners = res.ok ? await res.json() : [];
        const select = document.getElementById('deliveryPartnerSelect');
        if (partners && partners.length > 0) {
            select.innerHTML = partners.map(p => {
                const pName = p.name || p.fullName || (p.email ? p.email.split('@')[0] : ('Partner #' + p.id));
                const pPhone = p.phone || '9870001122';
                return `<option value="${p.id}" data-name="${escapeHtml(pName)}" data-phone="${escapeHtml(pPhone)}">${escapeHtml(pName)} (${escapeHtml(pPhone)})</option>`;
            }).join('');
        } else {
            select.innerHTML = `<option value="3" data-name="David Delivery" data-phone="9870001122">David Delivery (9870001122)</option>`;
        }
        openModal('assignDeliveryModal');
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function handleAssignDeliverySubmit(e) {
    e.preventDefault();
    const orderId = document.getElementById('assignOrderId').value;
    const select = document.getElementById('deliveryPartnerSelect');
    const opt = select.options[select.selectedIndex];

    const partnerId = opt.value ? parseInt(opt.value) : 3;
    const partnerName = opt.getAttribute('data-name') || (opt.text ? opt.text.split('(')[0].trim() : 'David Delivery');
    const partnerPhone = opt.getAttribute('data-phone') || '9870001122';
    const mId = (state.user && state.user.id) ? state.user.id : 2;

    try {
        const res = await fetch(`${API_BASE}/orders/${orderId}/assign-delivery?merchantId=${mId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ deliveryPartnerId: partnerId, deliveryPartnerName: partnerName, deliveryPartnerPhone: partnerPhone })
        });
        if (!res.ok) throw new Error('Failed to assign partner');
        showToast(`Order #${orderId} assigned to ${partnerName}!`, 'success');
        closeModal('assignDeliveryModal');
        loadMerchantPortal();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function renderMerchantProducts(products) {
    const tbody = document.getElementById('merchantProductsTableBody');
    if (!products || products.length === 0) {
        tbody.innerHTML = `<tr><td colspan="8" style="text-align:center; padding:20px;">No products listed.</td></tr>`;
        return;
    }

    tbody.innerHTML = products.map(p => {
        const offerActive = p.offerActive && p.offerPercentage > 0;
        return `
            <tr>
                <td><strong>${p.name}</strong><br><small style="color:#888;">${p.brand || 'GENERIC'}</small></td>
                <td>${p.category}</td>
                <td>₹${p.price.toFixed(2)}</td>
                <td>${offerActive ? `<span class="badge badge-success">${p.offerPercentage}% OFF</span>` : '<span style="color:#888;">None</span>'}</td>
                <td><strong>₹${(p.finalPrice || p.price).toFixed(2)}</strong></td>
                <td>${p.stockQuantity}</td>
                <td>${(p.averageRating || 4.5).toFixed(1)} ★</td>
                <td>
                    <button class="btn btn-sm btn-outline" onclick="openEditProductModal(${p.id})"><i class="fa-solid fa-pen"></i></button>
                    <button class="btn btn-sm btn-outline text-danger" onclick="deleteProduct(${p.id})"><i class="fa-solid fa-trash"></i></button>
                </td>
            </tr>
        `;
    }).join('');
}

function renderMerchantOffers(products) {
    const grid = document.getElementById('merchantOffersList');
    if (!products || products.length === 0) {
        grid.innerHTML = `<p>No products available to set offers on.</p>`;
        return;
    }

    grid.innerHTML = products.map(p => {
        const offerActive = p.offerActive && p.offerPercentage > 0;
        return `
            <div class="kpi-card ${offerActive ? 'kpi-green' : 'kpi-blue'}" style="flex-direction:column; align-items:flex-start; gap:8px;">
                <div style="display:flex; justify-content:space-between; width:100%;">
                    <strong>${p.name}</strong>
                    <span class="badge ${offerActive ? 'badge-success' : 'badge-secondary'}">${offerActive ? `${p.offerPercentage}% OFF` : 'No Offer'}</span>
                </div>
                <div style="font-size:13px; color:#555;">Original: ₹${p.price.toFixed(2)} | Final: <strong>₹${(p.finalPrice || p.price).toFixed(2)}</strong></div>
                <button class="btn btn-primary btn-sm" style="margin-top:6px;" onclick="openMerchantOfferModal(${p.id}, '${escapeHtml(p.name)}', ${p.price}, ${p.offerPercentage || 0})">
                    <i class="fa-solid fa-tag"></i> ${offerActive ? 'Update Offer' : 'Set Offer'}
                </button>
            </div>
        `;
    }).join('');
}

function openMerchantOfferModal(productId, name, price, currentOffer) {
    document.getElementById('offerProductId').value = productId;
    document.getElementById('offerProductName').value = name;
    document.getElementById('offerProductPrice').value = `₹${price.toFixed(2)}`;
    document.getElementById('offerPercentage').value = currentOffer > 0 ? currentOffer : 10;
    openModal('merchantOfferModal');
}

async function handleOfferSubmit(e) {
    e.preventDefault();
    const productId = document.getElementById('offerProductId').value;
    const offerPct = parseFloat(document.getElementById('offerPercentage').value);

    try {
        const res = await fetch(`${API_BASE}/products/${productId}/offer?merchantId=${state.user.id}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ offerPercentage: offerPct, offerActive: true })
        });
        if (!res.ok) throw new Error('Failed to set offer');
        showToast('Special Offer applied successfully!', 'success');
        closeModal('merchantOfferModal');
        loadMerchantPortal();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function removeOfferFromProduct() {
    const productId = document.getElementById('offerProductId').value;
    try {
        const res = await fetch(`${API_BASE}/products/${productId}/offer?merchantId=${state.user.id}`, { method: 'DELETE' });
        if (!res.ok) throw new Error('Failed to remove offer');
        showToast('Offer removed', 'info');
        closeModal('merchantOfferModal');
        loadMerchantPortal();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ==========================================================================
// Delivery Partner Portal (Role: DELIVERY_MAN)
// ==========================================================================
async function loadDeliveryPortal() {
    const dpId = (state.user && state.user.id) ? state.user.id : 3;
    try {
        const res = await fetch(`${API_BASE}/orders/delivery/${dpId}`);
        if (!res.ok) throw new Error('Failed to load delivery orders');
        state.deliveryOrders = await res.json();

        // Calculate KPI
        const newCount = state.deliveryOrders.filter(o => ['ASSIGNED_TO_DELIVERY', 'READY_FOR_DELIVERY'].includes(o.orderStatus)).length;
        const activeCount = state.deliveryOrders.filter(o => ['DELIVERY_ACCEPTED', 'PICKED_UP', 'OUT_FOR_DELIVERY'].includes(o.orderStatus)).length;
        const completedCount = state.deliveryOrders.filter(o => o.orderStatus === 'DELIVERED').length;

        document.getElementById('dpKpiNew').textContent = newCount;
        document.getElementById('dpKpiActive').textContent = activeCount;
        document.getElementById('dpKpiCompleted').textContent = completedCount;

        renderDeliveryOrders(state.deliveryOrders);
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function renderDeliveryOrders(orders) {
    const container = document.getElementById('deliveryOrdersList');
    if (!orders || orders.length === 0) {
        container.innerHTML = `<div class="empty-state" style="padding:40px; text-align:center;"><p>No deliveries currently assigned.</p></div>`;
        return;
    }

    container.innerHTML = orders.map(o => {
        let actionBtn = '';
        const st = o.orderStatus;

        if (st === 'ASSIGNED_TO_DELIVERY' || st === 'READY_FOR_DELIVERY') {
            actionBtn = `<button class="btn btn-primary" onclick="updateDeliveryMilestone(${o.id}, 'DELIVERY_ACCEPTED')"><i class="fa-solid fa-check"></i> Accept Delivery</button>`;
        } else if (st === 'DELIVERY_ACCEPTED') {
            actionBtn = `<button class="btn btn-primary" onclick="updateDeliveryMilestone(${o.id}, 'PICKED_UP')"><i class="fa-solid fa-box"></i> Picked Up from Seller</button>`;
        } else if (st === 'PICKED_UP') {
            actionBtn = `<button class="btn btn-primary" onclick="updateDeliveryMilestone(${o.id}, 'OUT_FOR_DELIVERY')"><i class="fa-solid fa-motorcycle"></i> Mark Out for Delivery</button>`;
        } else if (st === 'OUT_FOR_DELIVERY') {
            actionBtn = `<button class="btn btn-success" onclick="updateDeliveryMilestone(${o.id}, 'DELIVERED')"><i class="fa-solid fa-house-chimney-check"></i> Mark Delivered</button>`;
        }

        return `
            <div class="order-card" style="background:#fff; border:1px solid #e0e0e0; border-radius:8px; padding:16px; margin-bottom:12px;">
                <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:8px;">
                    <strong>Order #${o.id}</strong>
                    <span class="badge ${getStatusBadgeClass(o.orderStatus)}">${o.orderStatus}</span>
                </div>
                <div style="font-size:13px; margin-bottom:10px;">
                    <p><i class="fa-solid fa-location-dot text-danger"></i> <strong>Deliver to:</strong> ${o.shippingAddress}</p>
                    <p><i class="fa-solid fa-indian-rupee-sign"></i> <strong>Amount:</strong> ₹${o.totalAmount.toFixed(2)} (${o.paymentMethod} - ${o.paymentStatus})</p>
                </div>
                <div style="display:flex; justify-content:flex-end;">
                    ${actionBtn}
                </div>
            </div>
        `;
    }).join('');
}

async function updateDeliveryMilestone(orderId, status) {
    try {
        const res = await fetch(`${API_BASE}/orders/${orderId}/delivery-status?deliveryPartnerId=${state.user.id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status })
        });
        if (!res.ok) throw new Error('Failed to update delivery milestone');
        showToast(`Milestone updated to ${status}!`, 'success');
        loadDeliveryPortal();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ==========================================================================
// Super Admin Control Center
// ==========================================================================
async function loadAdminPortal() {
    if (!state.user || state.user.role !== 'ADMIN') return;
    loadAdminUsers();
    loadAdminProducts();
    loadAdminOrders();
    loadAdminReviews();
    loadAdminPayments();
    loadAdminAudit();
}

function switchAdminTab(tab, evt) {
    state.activeAdminTab = tab;
    document.querySelectorAll('.a-tab').forEach(b => b.classList.remove('active'));
    const target = evt ? evt.currentTarget : (typeof event !== 'undefined' ? event.currentTarget : null);
    if (target) target.classList.add('active');

    ['users', 'products', 'orders', 'reviews', 'payments', 'audit'].forEach(t => {
        document.getElementById('adminTab' + t.charAt(0).toUpperCase() + t.slice(1)).style.display = (t === tab) ? 'block' : 'none';
    });
}

async function loadAdminUsers() {
    try {
        const res = await fetch(`${API_BASE}/profiles`);
        if (!res.ok) throw new Error('Failed to load users');
        const users = await res.json();
        document.getElementById('adminUsersCount').textContent = users.length;
        document.getElementById('adminUsersTableBody').innerHTML = users.map(u => `
            <tr>
                <td>#${u.id}</td>
                <td><strong>${u.fullName}</strong></td>
                <td>${u.email}</td>
                <td>${u.phone || 'N/A'}</td>
                <td><span class="badge ${u.role === 'ADMIN' ? 'badge-danger' : (u.role === 'MERCHANT' ? 'badge-primary' : (u.role === 'DELIVERY_MAN' ? 'badge-purple' : 'badge-secondary'))}">${u.role}</span></td>
                <td>${u.address || 'N/A'}</td>
                <td>${formatDate(u.createdAt)}</td>
                <td>
                    <button class="btn btn-sm btn-outline text-danger" onclick="deleteUser(${u.id})"><i class="fa-solid fa-trash"></i></button>
                </td>
            </tr>
        `).join('');
    } catch (err) {
        console.error(err);
    }
}

async function loadAdminProducts() {
    try {
        const res = await fetch(`${API_BASE}/products`);
        if (!res.ok) throw new Error('Failed to load products');
        const prods = await res.json();
        document.getElementById('adminProductsCount').textContent = prods.length;
        document.getElementById('adminProductsTableBody').innerHTML = prods.map(p => `
            <tr>
                <td><strong>${p.name}</strong></td>
                <td>${p.brand || 'GENERIC'}</td>
                <td>${p.category}</td>
                <td>₹${p.price.toFixed(2)}</td>
                <td>${p.offerActive ? `${p.offerPercentage}%` : '0%'}</td>
                <td>₹${(p.finalPrice || p.price).toFixed(2)}</td>
                <td>${p.stockQuantity}</td>
                <td>${p.merchantName || 'Alice'}</td>
                <td>
                    <button class="btn btn-sm btn-outline text-danger" onclick="deleteProduct(${p.id})"><i class="fa-solid fa-trash"></i></button>
                </td>
            </tr>
        `).join('');
    } catch (err) {
        console.error(err);
    }
}

async function loadAdminOrders() {
    try {
        const res = await fetch(`${API_BASE}/orders`);
        if (!res.ok) throw new Error('Failed to load orders');
        const orders = await res.json();
        document.getElementById('adminOrdersCount').textContent = orders.length;
        document.getElementById('adminOrdersTableBody').innerHTML = orders.map(o => `
            <tr>
                <td>#${o.id}</td>
                <td>${formatDate(o.orderDate)}</td>
                <td>#${o.userId}</td>
                <td>${o.items ? o.items.length : 0}</td>
                <td><strong>₹${o.totalAmount.toFixed(2)}</strong></td>
                <td>${o.paymentMethod}</td>
                <td>${o.paymentStatus}</td>
                <td><span class="badge ${getStatusBadgeClass(o.orderStatus)}">${o.orderStatus}</span></td>
                <td>${o.deliveryPartnerName || 'Unassigned'}</td>
                <td>
                    <button class="btn btn-sm btn-primary" onclick="openOrderTrackingModal(${o.id})">Track</button>
                </td>
            </tr>
        `).join('');
    } catch (err) {
        console.error(err);
    }
}

async function loadAdminReviews() {
    try {
        const res = await fetch(`${API_BASE}/reviews`);
        if (!res.ok) return;
        const reviews = await res.json();
        document.getElementById('adminReviewsCount').textContent = reviews.length;
        document.getElementById('adminReviewsTableBody').innerHTML = reviews.map(r => `
            <tr>
                <td>#${r.id}</td>
                <td>#${r.productId}</td>
                <td><strong>${r.userName}</strong></td>
                <td>${r.rating} ★</td>
                <td>${escapeHtml(r.comment)}</td>
                <td>${formatDate(r.createdAt)}</td>
                <td>
                    <button class="btn btn-sm btn-outline text-danger" onclick="deleteReview(${r.id})"><i class="fa-solid fa-trash"></i> Moderate</button>
                </td>
            </tr>
        `).join('');
    } catch (err) {
        console.error(err);
    }
}

async function loadAdminPayments() {
    try {
        const res = await fetch(`${API_BASE}/payments`);
        if (!res.ok) return;
        const payments = await res.json();
        document.getElementById('adminPaymentsTableBody').innerHTML = payments.map(p => `
            <tr>
                <td>${p.paymentId}</td>
                <td>#${p.orderId || 'N/A'}</td>
                <td>#${p.userId}</td>
                <td>₹${p.amount.toFixed(2)}</td>
                <td>${p.paymentMethod}</td>
                <td><span class="badge badge-success">${p.paymentStatus}</span></td>
                <td>${p.transactionRef || 'N/A'}</td>
                <td>${formatDate(p.createdAt)}</td>
            </tr>
        `).join('');
    } catch (err) {
        console.error(err);
    }
}

async function loadAdminAudit() {
    try {
        const res = await fetch(`${API_BASE}/notifications/logs`);
        if (!res.ok) return;
        const logs = await res.json();
        document.getElementById('adminAuditTableBody').innerHTML = logs.map(l => `
            <tr>
                <td colspan="5">${escapeHtml(l)}</td>
            </tr>
        `).join('');
    } catch (err) {
        console.error(err);
    }
}

function openAdminCreateUserModal() {
    openModal('adminUserModal');
}

async function handleAdminCreateUser(e) {
    e.preventDefault();
    const nameVal = document.getElementById('adminNewName').value.trim();
    const req = {
        name: nameVal,
        fullName: nameVal,
        email: document.getElementById('adminNewEmail').value.trim(),
        password: document.getElementById('adminNewPassword').value.trim(),
        phone: document.getElementById('adminNewPhone').value.trim(),
        role: document.getElementById('adminNewRole').value,
        address: document.getElementById('adminNewAddress').value.trim()
    };

    try {
        const res = await fetch(`${API_BASE}/profiles/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(req)
        });
        if (!res.ok) throw new Error('Failed to create account');
        showToast('Account created successfully!', 'success');
        closeModal('adminUserModal');
        loadAdminUsers();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function deleteUser(userId) {
    if (!confirm('Are you sure you want to delete this user?')) return;
    try {
        const res = await fetch(`${API_BASE}/profiles/${userId}`, { method: 'DELETE' });
        if (!res.ok) throw new Error('Failed to delete user');
        showToast('User deleted', 'info');
        loadAdminUsers();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function deleteProduct(productId) {
    if (!confirm('Are you sure you want to delete this product?')) return;
    try {
        const res = await fetch(`${API_BASE}/products/${productId}`, { method: 'DELETE' });
        if (!res.ok) throw new Error('Failed to delete product');
        showToast('Product deleted', 'info');
        loadProducts();
        if (state.activeView === 'merchant') loadMerchantPortal();
        if (state.activeView === 'admin') loadAdminProducts();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ==========================================================================
// Digital Wallet & Top-up
// ==========================================================================
async function loadWalletBalance() {
    if (!state.user) return;
    try {
        const res = await fetch(`${API_BASE}/wallet/${state.user.id}`);
        if (!res.ok) return;
        const w = await res.json();
        const balStr = `₹${(w.balance || 0).toFixed(2)}`;
        document.getElementById('navWallet').textContent = `Wallet: ${balStr}`;
        document.getElementById('walletModalBalance').textContent = balStr;
        document.getElementById('checkoutWalletBal').textContent = `Available Balance: ${balStr}`;

        if (w.transactions) {
            document.getElementById('walletTxnsList').innerHTML = w.transactions.map(t => `
                <div style="display:flex; justify-content:space-between; padding:8px 0; border-bottom:1px solid #eee; font-size:12px;">
                    <div><strong>${t.description}</strong><br><span style="color:#888;">${formatDate(t.transactionDate)}</span></div>
                    <div style="font-weight:700; color:${t.amount > 0 ? '#388e3c' : '#d32f2f'};">${t.amount > 0 ? '+' : ''}₹${Math.abs(t.amount).toFixed(2)}</div>
                </div>
            `).join('');
        }
    } catch (err) {
        console.error(err);
    }
}

function openWalletModal() {
    if (!state.user) {
        showToast('Please login to access wallet', 'info');
        openAuthModal('login');
        return;
    }
    loadWalletBalance();
    openModal('walletModal');
}

async function handleWalletTopup(e) {
    e.preventDefault();
    const amt = parseFloat(document.getElementById('topupAmount').value);
    if (!amt || amt <= 0) return;

    try {
        const res = await fetch(`${API_BASE}/wallet/${state.user.id}/topup`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ amount: amt, description: 'Wallet Top-up via Card/UPI' })
        });
        if (!res.ok) throw new Error('Topup failed');
        showToast(`₹${amt} added to wallet!`, 'success');
        document.getElementById('topupAmount').value = '';
        await loadWalletBalance();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ==========================================================================
// Notifications & Alerts Drawer
// ==========================================================================
async function loadNotifications() {
    if (!state.user) return;
    try {
        const res = await fetch(`${API_BASE}/notifications/user/${state.user.id}`);
        if (!res.ok) return;
        state.notifications = await res.json();

        const unread = state.notifications.filter(n => !n.read).length;
        const badge = document.getElementById('notifBadge');
        if (unread > 0) {
            badge.textContent = unread;
            badge.style.display = 'inline-block';
        } else {
            badge.style.display = 'none';
        }

        renderNotifications(state.notifications);
    } catch (err) {
        console.error(err);
    }
}

function openNotificationsModal() {
    if (!state.user) {
        showToast('Please login to view alerts', 'info');
        openAuthModal('login');
        return;
    }
    loadNotifications();
    openModal('notificationsModal');
}

function renderNotifications(notifs) {
    const list = document.getElementById('notificationsList');
    if (!notifs || notifs.length === 0) {
        list.innerHTML = `<div class="empty-state" style="padding:30px; text-align:center;"><p>No notifications yet.</p></div>`;
        return;
    }

    list.innerHTML = notifs.map(n => `
        <div style="background:#fff; border:1px solid #e0e0e0; border-left:4px solid var(--primary); border-radius:6px; padding:12px; margin-bottom:10px;">
            <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:4px;">
                <strong>${n.title}</strong>
                <span style="font-size:11px; color:#888;">${formatDate(n.timestamp)}</span>
            </div>
            <p style="font-size:13px; color:#444;">${n.message}</p>
        </div>
    `).join('');
}

async function clearAllNotifications() {
    if (!state.user) return;
    try {
        await fetch(`${API_BASE}/notifications/user/${state.user.id}/clear`, { method: 'DELETE' });
        showToast('Notifications cleared', 'info');
        loadNotifications();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ==========================================================================
// Authentication Engine (JWT + Role)
// ==========================================================================
function openAuthModal(mode) {
    switchAuthTab(mode);
    openModal('authModal');
}

function switchAuthTab(mode) {
    document.getElementById('authTabLogin').classList.toggle('active', mode === 'login');
    document.getElementById('authTabRegister').classList.toggle('active', mode === 'register');
    document.getElementById('loginForm').style.display = (mode === 'login') ? 'block' : 'none';
    document.getElementById('registerForm').style.display = (mode === 'register') ? 'block' : 'none';
}

function quickLogin(email, pass) {
    document.getElementById('loginEmail').value = email;
    document.getElementById('loginPassword').value = pass;
    document.getElementById('loginForm').requestSubmit();
}

async function handleLoginSubmit(e) {
    e.preventDefault();
    const email = document.getElementById('loginEmail').value.trim();
    const password = document.getElementById('loginPassword').value.trim();

    try {
        const res = await fetch(`${API_BASE}/profiles/login`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email, password })
        });

        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || 'Login failed');
        }

        const data = await res.json();
        state.token = data.token;
        const uid = data.userId || data.id;
        const uName = data.name || data.fullName || (data.email ? data.email.split('@')[0] : 'User');
        state.user = {
            id: uid,
            userId: uid,
            email: data.email,
            name: uName,
            fullName: uName,
            role: data.role,
            phone: data.phone || '',
            address: data.address || ''
        };

        localStorage.setItem('esz_token', state.token);
        localStorage.setItem('esz_user', JSON.stringify(state.user));

        showToast(`Welcome back, ${state.user.fullName}!`, 'success');
        closeModal('authModal');
        initAuth();
        syncUserData();

        // Switch to corresponding role dashboard if merchant or delivery
        if (state.user.role === 'MERCHANT') switchView('merchant');
        else if (state.user.role === 'DELIVERY_MAN') switchView('delivery');
        else if (state.user.role === 'ADMIN') switchView('admin');
        else switchView('storefront');
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function handleRegisterSubmit(e) {
    e.preventDefault();
    const nameVal = document.getElementById('regName').value.trim();
    const req = {
        name: nameVal,
        fullName: nameVal,
        email: document.getElementById('regEmail').value.trim(),
        password: document.getElementById('regPassword').value.trim(),
        phone: document.getElementById('regPhone').value.trim(),
        role: document.getElementById('regRole').value,
        address: document.getElementById('regAddress').value.trim()
    };

    try {
        const res = await fetch(`${API_BASE}/profiles/register`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(req)
        });

        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || 'Registration failed');
        }

        showToast('Registration successful! Please sign in.', 'success');
        switchAuthTab('login');
        document.getElementById('loginEmail').value = req.email;
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function logout() {
    state.user = null;
    state.token = null;
    state.wishlist = [];
    state.cart = null;
    state.orders = [];
    localStorage.removeItem('esz_token');
    localStorage.removeItem('esz_user');

    showToast('Logged out successfully', 'info');
    initAuth();
    switchView('storefront');
}

function toggleUserMenu() {
    document.getElementById('userMenu').classList.toggle('show');
}

// ==========================================================================
// Modal Helpers & Utilities
// ==========================================================================
function openModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.add('show');
}

function closeModal(id) {
    const el = document.getElementById(id);
    if (el) el.classList.remove('show');
}

function openAddProductModal() {
    document.getElementById('editProductId').value = '';
    document.getElementById('productModalTitle').innerHTML = '<i class="fa-solid fa-box-open"></i> Add New Product';
    document.getElementById('productForm').reset();
    openModal('productModal');
}

function openEditProductModal(productId) {
    const p = state.products.find(x => x.id === productId);
    if (!p) return;

    document.getElementById('editProductId').value = p.id;
    document.getElementById('productModalTitle').innerHTML = '<i class="fa-solid fa-pen-to-square"></i> Edit Product';
    document.getElementById('prodName').value = p.name;
    document.getElementById('prodBrand').value = p.brand || '';
    document.getElementById('prodCategory').value = p.category;
    document.getElementById('prodPrice').value = p.price;
    document.getElementById('prodStock').value = p.stockQuantity;
    document.getElementById('prodImage').value = p.imageUrl || '';
    document.getElementById('prodDesc').value = p.description || '';

    openModal('productModal');
}

async function handleProductSubmit(e) {
    e.preventDefault();
    const editId = document.getElementById('editProductId').value;
    const isEdit = Boolean(editId);

    const req = {
        name: document.getElementById('prodName').value.trim(),
        brand: document.getElementById('prodBrand').value.trim(),
        category: document.getElementById('prodCategory').value,
        price: parseFloat(document.getElementById('prodPrice').value),
        stockQuantity: parseInt(document.getElementById('prodStock').value),
        imageUrl: document.getElementById('prodImage').value.trim(),
        description: document.getElementById('prodDesc').value.trim(),
        merchantId: state.user ? state.user.id : 2,
        merchantName: state.user ? state.user.fullName : 'Alice Merchant'
    };

    try {
        const url = isEdit ? `${API_BASE}/products/${editId}` : `${API_BASE}/products`;
        const method = isEdit ? 'PUT' : 'POST';

        const res = await fetch(url, {
            method,
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(req)
        });

        if (!res.ok) throw new Error('Failed to save product');
        showToast(`Product ${isEdit ? 'updated' : 'created'} successfully!`, 'success');
        closeModal('productModal');
        loadProducts();
        if (state.activeView === 'merchant') loadMerchantPortal();
        if (state.activeView === 'admin') loadAdminProducts();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function openProfileModal() {
    if (!state.user) return;
    document.getElementById('profName').value = state.user.fullName || '';
    document.getElementById('profEmail').value = state.user.email || '';
    document.getElementById('profPhone').value = state.user.phone || '';
    document.getElementById('profRole').value = state.user.role || '';
    document.getElementById('profAddress').value = state.user.address || '';
    switchProfileTab('info');
    openModal('profileModal');
}

function switchProfileTab(tab) {
    const btnInfo = document.getElementById('profTabBtnInfo');
    const btnPass = document.getElementById('profTabBtnPass');
    const formInfo = document.getElementById('profileForm');
    const formPass = document.getElementById('changePasswordForm');

    if (btnInfo) btnInfo.classList.toggle('active', tab === 'info');
    if (btnPass) btnPass.classList.toggle('active', tab === 'password');
    if (formInfo) formInfo.style.display = (tab === 'info') ? 'block' : 'none';
    if (formPass) formPass.style.display = (tab === 'password') ? 'block' : 'none';
}

async function handleChangePassword(e) {
    e.preventDefault();
    if (!state.user || !state.user.id) {
        showToast('Please login to change password', 'warning');
        return;
    }

    const currentPassword = document.getElementById('changeCurrentPass').value.trim();
    const newPassword = document.getElementById('changeNewPass').value.trim();
    const confirmPassword = document.getElementById('changeConfirmPass').value.trim();

    if (newPassword !== confirmPassword) {
        showToast('New passwords do not match', 'error');
        return;
    }

    try {
        const res = await fetch(`${API_BASE}/profiles/${state.user.id}/change-password`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ currentPassword, newPassword })
        });

        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || 'Failed to update password');
        }

        showToast('Password updated successfully!', 'success');
        document.getElementById('changePasswordForm').reset();
        switchProfileTab('info');
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function handleProfileUpdate(e) {
    e.preventDefault();
    if (!state.user || !state.user.id) {
        showToast('Please login to update profile', 'warning');
        return;
    }

    const nameVal = document.getElementById('profName').value.trim();
    const phoneVal = document.getElementById('profPhone').value.trim();
    const addressVal = document.getElementById('profAddress').value.trim();

    if (!nameVal) {
        showToast('Name cannot be empty', 'error');
        return;
    }

    const req = {
        name: nameVal,
        fullName: nameVal,
        phone: phoneVal,
        address: addressVal
    };

    try {
        const res = await fetch(`${API_BASE}/profiles/${state.user.id}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(req)
        });

        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.message || 'Profile update failed');
        }

        const updated = await res.json();
        const finalName = updated.name || updated.fullName || nameVal;
        state.user.name = finalName;
        state.user.fullName = finalName;
        state.user.phone = updated.phone || phoneVal;
        state.user.address = updated.address || addressVal;
        localStorage.setItem('esz_user', JSON.stringify(state.user));

        showToast('Profile updated successfully!', 'success');
        closeModal('profileModal');
        initAuth();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function showToast(msg, type = 'info') {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.innerHTML = `<i class="fa-solid ${type === 'success' ? 'fa-circle-check' : (type === 'error' ? 'fa-circle-exclamation' : 'fa-circle-info')}"></i> ${msg}`;
    container.appendChild(toast);
    setTimeout(() => toast.remove(), 4000);
}

function formatDate(isoString) {
    if (!isoString) return 'Just now';
    try {
        const d = new Date(isoString);
        return d.toLocaleDateString('en-IN', { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
    } catch {
        return isoString;
    }
}

function getStatusBadgeClass(status) {
    if (['DELIVERED'].includes(status)) return 'badge-success';
    if (['REJECTED', 'CANCELLED'].includes(status)) return 'badge-danger';
    if (['ASSIGNED_TO_DELIVERY', 'OUT_FOR_DELIVERY', 'PICKED_UP'].includes(status)) return 'badge-purple';
    if (['ACCEPTED', 'PACKING', 'READY_FOR_DELIVERY'].includes(status)) return 'badge-warning';
    return 'badge-primary';
}

function escapeHtml(str) {
    if (!str) return '';
    return str.replace(/[&<>'"]/g, tag => ({
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        "'": '&#39;',
        '"': '&quot;'
    }[tag] || tag));
}
