import { useState } from 'react';
import { Link } from 'react-router-dom';

function LoginPage() {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [remember, setRemember] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = (e) => {
    e.preventDefault();
    if (!username.trim() || !password.trim()) {
      setError('请输入用户名和密码');
      return;
    }
    setError('');
    alert(`登录成功！欢迎回来，${username}`);
  };

  return (
    <div className="login-page">
      <div className="login-card">
        <div className="login-header">
          <div className="login-logo">📚</div>
          <h1 className="login-title">登录猪猪书城</h1>
          <p className="login-subtitle">欢迎回来，请登录你的账号</p>
        </div>

        <form onSubmit={handleSubmit}>
          {error && <p className="form-error">{error}</p>}

          <div className="form-group">
            <label className="form-label" htmlFor="username">用户名 / 邮箱</label>
            <input
              className="form-input"
              type="text"
              id="username"
              name="username"
              placeholder="请输入用户名或邮箱"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="password">密码</label>
            <input
              className="form-input"
              type="password"
              id="password"
              name="password"
              placeholder="请输入密码"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
            />
          </div>

          <div className="remember-row">
            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={remember}
                onChange={(e) => setRemember(e.target.checked)}
              />
              记住我
            </label>
            <Link to="#" className="forgot-link">忘记密码？</Link>
          </div>

          <button type="submit" className="btn btn-primary btn-block">登录</button>

          <p className="login-footer">
            还没有账号？<Link to="#">立即注册</Link>
          </p>
        </form>
      </div>
    </div>
  );
}

export default LoginPage;
