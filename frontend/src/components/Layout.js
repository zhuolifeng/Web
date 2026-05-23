import { useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { Layout, Menu, Badge, Button, theme } from 'antd';
import {
  HomeOutlined,
  ShoppingCartOutlined,
  UserOutlined,
  LoginOutlined,
  LogoutOutlined,
  FileTextOutlined,
} from '@ant-design/icons';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';

const { Header, Sider, Content, Footer } = Layout;

function AppLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { cartCount } = useCart();
  const { user, isAuthenticated, logout } = useAuth();
  const { token: { colorBgContainer, borderRadiusLG } } = theme.useToken();

  const getSelectedKey = () => {
    const path = location.pathname;
    if (path === '/') return 'home';
    if (path.startsWith('/book/')) return 'home';
    if (path === '/cart') return 'cart';
    if (path === '/orders') return 'orders';
    if (path === '/profile') return 'profile';
    if (path === '/login') return 'login';
    if (path === '/register') return 'login';
    if (path === '/order') return 'order';
    return 'home';
  };

  const menuItems = [
    {
      key: 'home',
      icon: <HomeOutlined />,
      label: '首页',
    },
    {
      key: 'cart',
      icon: (
        <Badge count={cartCount} size="small" offset={[4, 0]}>
          <ShoppingCartOutlined style={{ fontSize: 16 }} />
        </Badge>
      ),
      label: '购物车',
    },
    ...(isAuthenticated
      ? [
          {
            key: 'orders',
            icon: <FileTextOutlined />,
            label: '我的订单',
          },
          {
            key: 'profile',
            icon: <UserOutlined />,
            label: user?.nickname || user?.username || '个人信息',
          },
          {
            key: 'logout',
            icon: <LogoutOutlined />,
            label: '退出登录',
          },
        ]
      : [
          {
            key: 'login',
            icon: <LoginOutlined />,
            label: '登录',
          },
        ]
    ),
  ];

  const handleMenuClick = ({ key }) => {
    if (key === 'logout') {
      logout();
      navigate('/');
      return;
    }
    const routes = {
      home: '/',
      cart: '/cart',
      orders: '/orders',
      profile: '/profile',
      login: '/login',
    };
    if (routes[key]) navigate(routes[key]);
  };

  return (
    <Layout className="app-layout">
      <Sider
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        theme="light"
        className="app-sider"
      >
        <div className="app-logo" onClick={() => navigate('/')}>
          <span className="app-logo-emoji">📚</span>
          {!collapsed && <span className="app-logo-text">猪猪书城</span>}
        </div>
        <Menu
          mode="inline"
          selectedKeys={[getSelectedKey()]}
          items={menuItems}
          onClick={handleMenuClick}
          className="app-menu"
        />
      </Sider>

      <Layout className={`app-main ${collapsed ? 'app-main--collapsed' : 'app-main--expanded'}`}>
        <Header
          className="app-header"
          style={{ background: colorBgContainer }}
        >
          <h2 className="app-header-title">猪猪书城 - 在线书店</h2>
          <div className="app-header-actions">
            <Badge count={cartCount} size="small">
              <Button
                icon={<ShoppingCartOutlined />}
                onClick={() => navigate('/cart')}
              >
                购物车
              </Button>
            </Badge>
            {isAuthenticated ? (
              <>
                <Button
                  icon={<UserOutlined />}
                  onClick={() => navigate('/profile')}
                >
                  {user?.nickname || user?.username}
                </Button>
                <Button onClick={() => { logout(); navigate('/'); }}>
                  退出
                </Button>
              </>
            ) : (
              <Button
                type="primary"
                icon={<LoginOutlined />}
                onClick={() => navigate('/login')}
              >
                登录
              </Button>
            )}
          </div>
        </Header>

        <Content
          className="app-content"
          style={{ background: colorBgContainer, borderRadius: borderRadiusLG }}
        >
          <Outlet />
        </Content>

        <Footer className="app-footer">
          &copy; 2026 猪猪书城. 探索知识的无限可能
        </Footer>
      </Layout>
    </Layout>
  );
}

export default AppLayout;
