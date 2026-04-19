import { Link, NavLink } from 'react-router-dom';

function Header() {
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
            <li><NavLink to="#" className="nav-link">分类</NavLink></li>
            <li><NavLink to="#" className="nav-link">排行</NavLink></li>
            <li><NavLink to="#" className="nav-link">书单</NavLink></li>
          </ul>
        </nav>

        <div className="header-actions">
          <Link to="/cart" className="btn btn-secondary btn-sm">🛒 购物车</Link>
          <Link to="/login" className="btn btn-primary btn-sm">登录</Link>
        </div>
      </div>
    </header>
  );
}

export default Header;
