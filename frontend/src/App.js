import { Routes, Route } from 'react-router-dom';
import { CartProvider } from './context/CartContext';
import AppLayout from './components/Layout';
import HomePage from './pages/HomePage';
import BookDetailPage from './pages/BookDetailPage';
import CartPage from './pages/CartPage';
import LoginPage from './pages/LoginPage';
import OrderPage from './pages/OrderPage';
import ProfilePage from './pages/ProfilePage';

function App() {
  return (
    <CartProvider>
      <Routes>
        <Route path="/" element={<AppLayout />}>
          <Route index element={<HomePage />} />
          <Route path="book/:id" element={<BookDetailPage />} />
          <Route path="cart" element={<CartPage />} />
          <Route path="login" element={<LoginPage />} />
          <Route path="order" element={<OrderPage />} />
          <Route path="profile" element={<ProfilePage />} />
        </Route>
      </Routes>
    </CartProvider>
  );
}

export default App;
