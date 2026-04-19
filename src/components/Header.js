import { Link, NavLink } from 'react-router-dom';
import { useCart } from '../context/CartContext';

function Header() {
  const { cartCount } = useCart();

  return (
    <header className="header">
      <div className="header-inner">
        <Link to="/" className="logo">
          <span className="logo-icon">📚</span>
          <span>猪猪书城</span>
        </Link>

        <nav>
          <ul className="nav-list">
            <li><NavLink to="/" className="nav-link" end>首页</NavLink></li>
            <li><span className="nav-link" style={{ cursor: 'default' }}>分类</span></li>
            <li><span className="nav-link" style={{ cursor: 'default' }}>排行</span></li>
            <li><span className="nav-link" style={{ cursor: 'default' }}>书单</span></li>
          </ul>
        </nav>

        <div className="header-actions">
          <Link to="/cart" className="btn btn-secondary btn-sm">
            🛒 购物车{cartCount > 0 && `(${cartCount})`}
          </Link>
          <Link to="/login" className="btn btn-primary btn-sm">登录</Link>
        </div>
      </div>
    </header>
  );
}

export default Header;
