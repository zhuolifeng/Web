import { Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { CartProvider } from './context/CartContext';
import AppLayout from './components/Layout';
import RequireAuth from './components/RequireAuth';
import HomePage from './pages/HomePage';
import BookDetailPage from './pages/BookDetailPage';
import CartPage from './pages/CartPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import OrderPage from './pages/OrderPage';
import OrderListPage from './pages/OrderListPage';
import ProfilePage from './pages/ProfilePage';

function App() {
  return (
    <AuthProvider>
      <CartProvider>
        <Routes>
          <Route path="/" element={<AppLayout />}>
            <Route index element={<HomePage />} />
            <Route path="book/:id" element={<BookDetailPage />} />
            <Route path="login" element={<LoginPage />} />
            <Route path="register" element={<RegisterPage />} />
            <Route path="cart" element={
              <RequireAuth><CartPage /></RequireAuth>
            } />
            <Route path="order" element={
              <RequireAuth><OrderPage /></RequireAuth>
            } />
            <Route path="orders" element={
              <RequireAuth><OrderListPage /></RequireAuth>
            } />
            <Route path="profile" element={
              <RequireAuth><ProfilePage /></RequireAuth>
            } />
          </Route>
        </Routes>
      </CartProvider>
    </AuthProvider>
  );
}

export default App;
