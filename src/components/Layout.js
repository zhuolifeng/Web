import { useState } from 'react';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import { Layout, Menu, Badge, Button, theme } from 'antd';
import {
  HomeOutlined,
  ReadOutlined,
  ShoppingCartOutlined,
  UserOutlined,
  LoginOutlined,
} from '@ant-design/icons';
import { useCart } from '../context/CartContext';

const { Header, Sider, Content, Footer } = Layout;

function AppLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { cartCount } = useCart();
  const { token: { colorBgContainer, borderRadiusLG } } = theme.useToken();

  /* Determine which menu key is selected based on current path */
  const getSelectedKey = () => {
    const path = location.pathname;
    if (path === '/') return 'home';
    if (path.startsWith('/book/')) return 'home';
    if (path === '/cart') return 'cart';
    if (path === '/profile') return 'profile';
    if (path === '/login') return 'login';
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
    {
      key: 'profile',
      icon: <UserOutlined />,
      label: '个人信息',
    },
    {
      key: 'login',
      icon: <LoginOutlined />,
      label: '登录',
    },
  ];

  const handleMenuClick = ({ key }) => {
    const routes = {
      home: '/',
      cart: '/cart',
      profile: '/profile',
      login: '/login',
    };
    if (routes[key]) navigate(routes[key]);
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        theme="light"
        style={{
          boxShadow: '2px 0 8px rgba(0,0,0,0.06)',
          position: 'fixed',
          left: 0,
          top: 0,
          bottom: 0,
          zIndex: 100,
          overflow: 'auto',
        }}
      >
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            borderBottom: '1px solid #f0f0f0',
            cursor: 'pointer',
          }}
          onClick={() => navigate('/')}
        >
          <span style={{ fontSize: 24 }}>📚</span>
          {!collapsed && (
            <span
              style={{
                fontSize: 18,
                fontWeight: 700,
                color: '#4a6cf7',
                marginLeft: 8,
              }}
            >
              猪猪书城
            </span>
          )}
        </div>
        <Menu
          mode="inline"
          selectedKeys={[getSelectedKey()]}
          items={menuItems}
          onClick={handleMenuClick}
          style={{ borderRight: 0, marginTop: 8 }}
        />
      </Sider>

      <Layout style={{ marginLeft: collapsed ? 80 : 200, transition: 'all 0.2s' }}>
        <Header
          style={{
            padding: '0 24px',
            background: colorBgContainer,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            boxShadow: '0 1px 4px rgba(0,0,0,0.06)',
            position: 'sticky',
            top: 0,
            zIndex: 99,
          }}
        >
          <h2 style={{ margin: 0, fontSize: 20, fontWeight: 600 }}>
            猪猪书城 - 在线书店
          </h2>
          <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
            <Badge count={cartCount} size="small">
              <Button
                icon={<ShoppingCartOutlined />}
                onClick={() => navigate('/cart')}
              >
                购物车
              </Button>
            </Badge>
            <Button
              type="primary"
              icon={<UserOutlined />}
              onClick={() => navigate('/profile')}
            >
              个人中心
            </Button>
          </div>
        </Header>

        <Content
          style={{
            margin: 24,
            padding: 24,
            minHeight: 280,
            background: colorBgContainer,
            borderRadius: borderRadiusLG,
          }}
        >
          <Outlet />
        </Content>

        <Footer style={{ textAlign: 'center', color: '#999' }}>
          &copy; 2026 猪猪书城. 探索知识的无限可能
        </Footer>
      </Layout>
    </Layout>
  );
}

export default AppLayout;
