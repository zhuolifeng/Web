import { createContext, useContext, useState, useCallback, useEffect } from 'react';
import { cartService } from '../services/cartService';
import { parsePrice } from '../utils/price';
import { useAuth } from './AuthContext';

const CartContext = createContext(null);

/**
 * Cart items in state share this normalized shape regardless of source (backend or local):
 *   { id, bookId, title, author, price, originalPrice, coverImg, coverEmoji, quantity }
 * `id` is the cart_item.id from backend when logged in,
 * and equals bookId when running in offline/local mode.
 */
export function CartProvider({ children }) {
  const { isAuthenticated, token } = useAuth();
  const [cartItems, setCartItems] = useState([]);
  const [loading, setLoading] = useState(false);

  // Load from backend on auth state change
  const refresh = useCallback(async () => {
    if (!isAuthenticated) {
      // logged out: clear in-memory cart (we don't persist guest cart anymore)
      setCartItems([]);
      return;
    }
    setLoading(true);
    try {
      const res = await cartService.list();
      const items = (res.data || []).map(normalize);
      setCartItems(items);
    } catch (err) {
      if (err.code !== 401) console.error('refresh cart failed:', err);
    } finally {
      setLoading(false);
    }
  }, [isAuthenticated]);

  useEffect(() => { refresh(); }, [refresh, token]);

  const addToCart = useCallback(async (bookId, qty = 1) => {
    if (!isAuthenticated) {
      const err = new Error('请先登录后再加入购物车');
      err.code = 401;
      throw err;
    }
    await cartService.add(Number(bookId), qty);
    await refresh();
  }, [isAuthenticated, refresh]);

  const removeFromCart = useCallback(async (cartItemId) => {
    await cartService.remove(cartItemId);
    setCartItems(prev => prev.filter(item => item.id !== cartItemId));
  }, []);

  const updateQuantity = useCallback(async (cartItemId, qty) => {
    if (qty < 1 || qty > 99) return;
    await cartService.update(cartItemId, qty);
    setCartItems(prev =>
      prev.map(item => (item.id === cartItemId ? { ...item, quantity: qty } : item))
    );
  }, []);

  const clearCart = useCallback(async () => {
    await cartService.clear();
    setCartItems([]);
  }, []);

  const cartCount = cartItems.reduce((sum, item) => sum + item.quantity, 0);
  const cartTotal = cartItems.reduce(
    (sum, item) => sum + parsePrice(item.price) * item.quantity, 0,
  );
  const cartOriginalTotal = cartItems.reduce(
    (sum, item) => sum + parsePrice(item.originalPrice || item.price) * item.quantity, 0,
  );

  return (
    <CartContext.Provider value={{
      cartItems,
      loading,
      addToCart,
      removeFromCart,
      updateQuantity,
      clearCart,
      refresh,
      cartCount,
      cartTotal,
      cartOriginalTotal,
    }}>
      {children}
    </CartContext.Provider>
  );
}

function normalize(dto) {
  return {
    id: dto.id,
    bookId: dto.bookId,
    title: dto.title,
    author: dto.author,
    price: dto.price,
    originalPrice: dto.originalPrice,
    coverImg: dto.coverImg,
    coverEmoji: dto.coverEmoji,
    quantity: dto.quantity,
  };
}

export function useCart() {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error('useCart must be used within CartProvider');
  return ctx;
}
