import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useCart } from '../context/CartContext';

function OrderPage() {
  const { cartItems, cartTotal, cartOriginalTotal, clearCart } = useCart();
  const [submitted, setSubmitted] = useState(false);
  const [payment, setPayment] = useState('alipay');
  const [imgErrors, setImgErrors] = useState({});

  /* ---- 表单受控状态 ---- */
  const [form, setForm] = useState({
    name: '',
    phone: '',
    province: '',
    city: '',
    address: '',
    note: '',
  });

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm(prev => ({ ...prev, [name]: value }));
  };

  const handleImgError = (id) => {
    setImgErrors(prev => ({ ...prev, [id]: true }));
  };

  const shipping = cartTotal >= 99 ? 0 : 10;
  const totalWithShipping = cartTotal + shipping;
  const discount = cartOriginalTotal - cartTotal;

  const handleSubmit = (e) => {
    e.preventDefault();
    setSubmitted(true);
    clearCart();
  };

  /* ---- 空购物车 ---- */
  if (cartItems.length === 0 && !submitted) {
    return (
      <div className="empty-state">
        <div className="empty-icon">📦</div>
        <h2 className="empty-title">暂无可结算的商品</h2>
        <p className="empty-text">请先将书籍加入购物车</p>
        <Link to="/" className="btn btn-primary">去选书</Link>
      </div>
    );
  }

  /* ---- 下单成功 ---- */
  if (submitted) {
    return (
      <div className="empty-state">
        <div className="empty-icon">🎉</div>
        <h2 className="empty-title">下单成功！</h2>
        <p className="empty-text">感谢您的购买，我们将尽快安排发货。</p>
        <Link to="/" className="btn btn-primary">继续逛逛</Link>
      </div>
    );
  }

  return (
    <>
      <h1 className="section-title order-page-title">确认订单</h1>

      <form className="order-page" onSubmit={handleSubmit}>
        {/* ---- 左栏 ---- */}
        <div className="order-main">
          {/* 收货信息 */}
          <section className="order-section">
            <div className="order-section-header">
              <h2 className="order-section-title">收货信息</h2>
            </div>
            <div className="order-section-body">
              <div className="form-grid">
                <div className="form-group">
                  <label className="form-label" htmlFor="name">收货人</label>
                  <input className="form-input" id="name" name="name" placeholder="请输入收货人姓名" value={form.name} onChange={handleChange} required />
                </div>
                <div className="form-group">
                  <label className="form-label" htmlFor="phone">手机号</label>
                  <input className="form-input" id="phone" name="phone" placeholder="请输入手机号码" value={form.phone} onChange={handleChange} required />
                </div>
                <div className="form-group">
                  <label className="form-label" htmlFor="province">省份</label>
                  <input className="form-input" id="province" name="province" placeholder="省" value={form.province} onChange={handleChange} required />
                </div>
                <div className="form-group">
                  <label className="form-label" htmlFor="city">城市</label>
                  <input className="form-input" id="city" name="city" placeholder="市" value={form.city} onChange={handleChange} required />
                </div>
                <div className="form-group full-width">
                  <label className="form-label" htmlFor="address">详细地址</label>
                  <input className="form-input" id="address" name="address" placeholder="请输入详细收货地址" value={form.address} onChange={handleChange} required />
                </div>
                <div className="form-group full-width">
                  <label className="form-label" htmlFor="note">备注</label>
                  <input className="form-input" id="note" name="note" placeholder="选填，有什么要备注的？" value={form.note} onChange={handleChange} />
                </div>
              </div>
            </div>
          </section>

          {/* 商品清单 */}
          <section className="order-section">
            <div className="order-section-header">
              <h2 className="order-section-title">商品清单</h2>
            </div>
            <div className="order-section-body">
              <div className="order-items">
                {cartItems.map(item => {
                  const unitPrice = parseFloat(item.price.replace('¥', ''));
                  const subtotal = (unitPrice * item.quantity).toFixed(2);
                  return (
                    <div className="order-item" key={item.id}>
                      <div className="order-item-cover">
                        {imgErrors[item.id] ? (
                          <span>{item.coverEmoji}</span>
                        ) : (
                          <img
                            src={item.coverImg}
                            alt={item.title}
                            onError={() => handleImgError(item.id)}
                          />
                        )}
                      </div>
                      <div className="order-item-info">
                        <h4>{item.title}</h4>
                        <p>{item.author}</p>
                      </div>
                      <div className="order-item-price-group">
                        <div className="order-item-price">{item.price}</div>
                        <div className="order-item-qty">x{item.quantity}</div>
                      </div>
                      <div className="order-item-subtotal">¥{subtotal}</div>
                    </div>
                  );
                })}
              </div>
            </div>
          </section>
        </div>

        {/* ---- 右栏：订单摘要 ---- */}
        <aside className="order-sidebar">
          <div className="order-summary-card">
            <div className="order-summary-header">
              <h3>订单摘要</h3>
            </div>
            <div className="order-summary-body">
              <div className="summary-row">
                <span className="summary-label">商品金额</span>
                <span className="summary-value">¥{cartTotal.toFixed(2)}</span>
              </div>
              {discount > 0 && (
                <div className="summary-row">
                  <span className="summary-label">优惠</span>
                  <span className="summary-value" style={{ color: 'var(--success)' }}>-¥{discount.toFixed(2)}</span>
                </div>
              )}
              <div className="summary-row">
                <span className="summary-label">运费</span>
                <span className={`summary-value ${shipping === 0 ? 'summary-shipping-free' : ''}`}>
                  {shipping === 0 ? '免运费' : `¥${shipping.toFixed(2)}`}
                </span>
              </div>
              <div className="summary-row total">
                <span className="summary-label">应付总额</span>
                <span className="summary-value">¥{totalWithShipping.toFixed(2)}</span>
              </div>

              {/* 支付方式 */}
              <div className="payment-methods">
                <h4 className="order-summary-title">支付方式</h4>
                <label className={`payment-method ${payment === 'alipay' ? 'selected' : ''}`}>
                  <input type="radio" name="payment" value="alipay" checked={payment === 'alipay'} onChange={(e) => setPayment(e.target.value)} />
                  <span className="payment-icon">💳</span>
                  <div className="payment-info">
                    <h4>支付宝</h4>
                    <p>推荐使用</p>
                  </div>
                </label>
                <label className={`payment-method ${payment === 'wechat' ? 'selected' : ''}`}>
                  <input type="radio" name="payment" value="wechat" checked={payment === 'wechat'} onChange={(e) => setPayment(e.target.value)} />
                  <span className="payment-icon">💬</span>
                  <div className="payment-info">
                    <h4>微信支付</h4>
                    <p>微信扫码支付</p>
                  </div>
                </label>
              </div>

              <button type="submit" className="btn btn-primary btn-block btn-lg place-order-btn">提交订单</button>
            </div>
          </div>
        </aside>
      </form>
    </>
  );
}

export default OrderPage;
