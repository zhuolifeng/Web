import { Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { CartProvider } from './context/CartContext';
import AppLayout from './components/Layout';
import RequireAuth from './components/RequireAuth';
import RequireAdmin from './components/RequireAdmin';
import HomePage from './pages/HomePage';
import BookDetailPage from './pages/BookDetailPage';
import CartPage from './pages/CartPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import OrderPage from './pages/OrderPage';
import OrderListPage from './pages/OrderListPage';
import ProfilePage from './pages/ProfilePage';
import MyStatisticsPage from './pages/MyStatisticsPage';
import AdminUserPage from './pages/AdminUserPage';
import AdminBookPage from './pages/AdminBookPage';
import AdminOrderPage from './pages/AdminOrderPage';
import AdminStatisticsPage from './pages/AdminStatisticsPage';

/**
 * 应用根组件：定义所有路由及其权限控制。
 * 公开路由：首页、书籍详情、登录、注册
 * 需登录：购物车、下单、我的订单、个人信息、购书统计
 * 管理员：用户管理、书籍管理、订单管理、数据统计
 */
function App() {
  return (
    <AuthProvider>
      <CartProvider>
        <Routes>
          <Route path="/" element={<AppLayout />}>
            {/* 公开页面 */}
            <Route index element={<HomePage />} />
            <Route path="book/:id" element={<BookDetailPage />} />
            <Route path="login" element={<LoginPage />} />
            <Route path="register" element={<RegisterPage />} />

            {/* 需要登录的页面 */}
            <Route path="cart" element={<RequireAuth><CartPage /></RequireAuth>} />
            <Route path="order" element={<RequireAuth><OrderPage /></RequireAuth>} />
            <Route path="orders" element={<RequireAuth><OrderListPage /></RequireAuth>} />
            <Route path="profile" element={<RequireAuth><ProfilePage /></RequireAuth>} />
            <Route path="statistics" element={<RequireAuth><MyStatisticsPage /></RequireAuth>} />

            {/* 管理员页面 */}
            <Route path="admin/users" element={<RequireAdmin><AdminUserPage /></RequireAdmin>} />
            <Route path="admin/books" element={<RequireAdmin><AdminBookPage /></RequireAdmin>} />
            <Route path="admin/orders" element={<RequireAdmin><AdminOrderPage /></RequireAdmin>} />
            <Route path="admin/statistics" element={<RequireAdmin><AdminStatisticsPage /></RequireAdmin>} />
          </Route>
        </Routes>
      </CartProvider>
    </AuthProvider>
  );
}

export default App;
