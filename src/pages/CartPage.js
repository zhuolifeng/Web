import { Link } from 'react-router-dom';

function CartPage() {
  return (
    <>
      <h1 style={{ textAlign: 'center', padding: '50px 0' }}>
        购物车页面（建设中...）
      </h1>
      <div style={{ textAlign: 'center' }}>
        <Link to="/" className="btn btn-primary">返回首页</Link>
      </div>
    </>
  );
}

export default CartPage;
