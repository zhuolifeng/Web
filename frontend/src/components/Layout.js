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
  TeamOutlined,
  BookOutlined,
  BarChartOutlined,
  UnorderedListOutlined,
} from '@ant-design/icons';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';

const { Header, Sider, Content, Footer } = Layout;

/**
 * 全局布局组件。
 * 根据用户角色（ADMIN/USER）动态渲染不同的侧边栏菜单项。
 */
function AppLayout() {
  const [collapsed, setCollapsed] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();
  const { cartCount } = useCart();
  const { user, isAuthenticated, logout } = useAuth();
  const { token: { colorBgContainer, borderRadiusLG } } = theme.useToken();

  const isAdmin = user?.role === 'ADMIN';

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
    if (path === '/admin/users') return 'admin-users';
    if (path === '/admin/books') return 'admin-books';
    if (path === '/admin/orders') return 'admin-orders';
    if (path === '/admin/statistics') return 'admin-statistics';
    if (path === '/statistics') return 'my-statistics';
    return 'home';
  };

  // 管理员专用菜单
  const adminMenuItems = isAdmin ? [
    { type: 'divider' },
    {
      key: 'admin-group',
      label: '管理后台',
      type: 'group',
      children: [
        { key: 'admin-users', icon: <TeamOutlined />, label: '用户管理' },
        { key: 'admin-books', icon: <BookOutlined />, label: '书籍管理' },
        { key: 'admin-orders', icon: <UnorderedListOutlined />, label: '订单管理' },
        { key: 'admin-statistics', icon: <BarChartOutlined />, label: '数据统计' },
      ],
    },
  ] : [];

  const menuItems = [
    { key: 'home', icon: <HomeOutlined />, label: '首页' },
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
          { key: 'orders', icon: <FileTextOutlined />, label: '我的订单' },
          { key: 'my-statistics', icon: <BarChartOutlined />, label: '购书统计' },
          { key: 'profile', icon: <UserOutlined />, label: user?.nickname || user?.username || '个人信息' },
          ...adminMenuItems,
          { key: 'logout', icon: <LogoutOutlined />, label: '退出登录' },
        ]
      : [
          { key: 'login', icon: <LoginOutlined />, label: '登录' },
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
      'home': '/',
      'cart': '/cart',
      'orders': '/orders',
      'profile': '/profile',
      'login': '/login',
      'my-statistics': '/statistics',
      'admin-users': '/admin/users',
      'admin-books': '/admin/books',
      'admin-orders': '/admin/orders',
      'admin-statistics': '/admin/statistics',
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
                {isAdmin && (
                  <Button onClick={() => navigate('/admin/statistics')}>
                    管理后台
                  </Button>
                )}
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
