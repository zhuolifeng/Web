import { createContext, useContext, useState, useCallback } from 'react';
import { books } from '../Data';

const CartContext = createContext(null);

export function CartProvider({ children }) {
  const [cartItems, setCartItems] = useState([]);

  const addToCart = useCallback((bookId, qty = 1) => {
    setCartItems(prev => {
      const existing = prev.find(item => item.id === bookId);
      if (existing) {
        return prev.map(item =>
          item.id === bookId
            ? { ...item, quantity: Math.min(item.quantity + qty, 99) }
            : item
        );
      }
      const book = books.find(b => b.id === bookId);
      if (!book) return prev;
      return [...prev, { ...book, quantity: qty }];
    });
  }, []);

  const removeFromCart = useCallback((bookId) => {
    setCartItems(prev => prev.filter(item => item.id !== bookId));
  }, []);

  const updateQuantity = useCallback((bookId, qty) => {
    if (qty < 1 || qty > 99) return;
    setCartItems(prev =>
      prev.map(item =>
        item.id === bookId ? { ...item, quantity: qty } : item
      )
    );
  }, []);

  const clearCart = useCallback(() => {
    setCartItems([]);
  }, []);

  const cartCount = cartItems.reduce((sum, item) => sum + item.quantity, 0);
  const cartTotal = cartItems.reduce(
    (sum, item) => sum + parseFloat(item.price.replace('¥', '')) * item.quantity,
    0
  );
  const cartOriginalTotal = cartItems.reduce(
    (sum, item) => sum + parseFloat(item.originalPrice.replace('¥', '')) * item.quantity,
    0
  );

  return (
    <CartContext.Provider value={{
      cartItems,
      addToCart,
      removeFromCart,
      updateQuantity,
      clearCart,
      cartCount,
      cartTotal,
      cartOriginalTotal,
    }}>
      {children}
    </CartContext.Provider>
  );
}

export function useCart() {
  const ctx = useContext(CartContext);
  if (!ctx) throw new Error('useCart must be used within CartProvider');
  return ctx;
}
