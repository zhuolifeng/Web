import { Link } from 'react-router-dom';

function LoginPage() {
  return (
    <>
      <div className="login-container">
        <h1 className="login-title">登录猪猪书城</h1>
        <form className="login-form">
          <div className="form-group">
            <label htmlFor="username">用户名/邮箱</label>
            <input type="text" id="username" name="username" placeholder="请输入用户名或邮箱" />
          </div>
          <div className="form-group">
            <label htmlFor="password">密码</label>
            <input type="password" id="password" name="password" placeholder="请输入密码" />
          </div>
          <div className="form-actions">
            <button type="submit" className="btn btn-primary btn-block">登录</button>
          </div>
          <p className="login-footer">
            还没有账号？<Link to="#">立即注册</Link>
          </p>
        </form>
      </div>
    </>
  );
}

export default LoginPage;
