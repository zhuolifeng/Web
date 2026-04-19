import { useState } from 'react';
import { Link } from 'react-router-dom';
import { useCart } from '../context/CartContext';

function CartPage() {
  const {
    cartItems,
    removeFromCart,
    updateQuantity,
    clearCart,
    cartTotal,
    cartOriginalTotal,
  } = useCart();

  const [imgErrors, setImgErrors] = useState({});

  const handleImgError = (id) => {
    setImgErrors(prev => ({ ...prev, [id]: true }));
  };

  if (cartItems.length === 0) {
    return (
      <div className="empty-state">
        <div className="empty-icon">🛒</div>
        <h2 className="empty-title">购物车空空如也</h2>
        <p className="empty-text">快去挑选你喜欢的书籍吧！</p>
        <Link to="/" className="btn btn-primary">去逛逛</Link>
      </div>
    );
  }

  const discount = cartOriginalTotal - cartTotal;

  return (
    <div className="cart-page">
      <div className="cart-header">
        <h1 className="cart-title">🛒 购物车（{cartItems.length} 件商品）</h1>
      </div>

      <table className="cart-table">
        <thead>
          <tr>
            <th className="cart-col-product">商品信息</th>
            <th className="cart-col-price">单价</th>
            <th className="cart-col-qty">数量</th>
            <th className="cart-col-subtotal">小计</th>
            <th className="cart-col-action">操作</th>
          </tr>
        </thead>
        <tbody>
          {cartItems.map(item => {
            const unitPrice = parseFloat(item.price.replace('¥', ''));
            const subtotal = (unitPrice * item.quantity).toFixed(2);

            return (
              <tr key={item.id}>
                <td>
                  <div className="cart-product">
                    <div className="cart-product-cover">
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
                    <div className="cart-product-info">
                      <h4><Link to={`/book/${item.id}`}>{item.title}</Link></h4>
                      <p>{item.author}</p>
                    </div>
                  </div>
                </td>
                <td className="cart-price">{item.price}</td>
                <td>
                  <div className="quantity-control">
                    <button
                      className="quantity-btn"
                      onClick={() => updateQuantity(item.id, item.quantity - 1)}
                      disabled={item.quantity <= 1}
                      aria-label="减少数量"
                    >−</button>
                    <input
                      type="number"
                      className="quantity-input"
                      value={item.quantity}
                      onChange={(e) => {
                        const val = parseInt(e.target.value, 10);
                        if (!isNaN(val) && val >= 1 && val <= 99) {
                          updateQuantity(item.id, val);
                        }
                      }}
                      min="1"
                      max="99"
                      aria-label="购买数量"
                    />
                    <button
                      className="quantity-btn"
                      onClick={() => updateQuantity(item.id, item.quantity + 1)}
                      disabled={item.quantity >= 99}
                      aria-label="增加数量"
                    >+</button>
                  </div>
                </td>
                <td className="cart-subtotal">¥{subtotal}</td>
                <td>
                  <button
                    className="cart-remove-btn"
                    onClick={() => removeFromCart(item.id)}
                    aria-label={`删除${item.title}`}
                  >✕ 删除</button>
                </td>
              </tr>
            );
          })}
        </tbody>
      </table>

      <div className="cart-summary">
        <div className="cart-summary-info">
          <h4>共 {cartItems.reduce((s, i) => s + i.quantity, 0)} 件商品</h4>
          {discount > 0 && (
            <p className="cart-shipping-note">已优惠 ¥{discount.toFixed(2)}</p>
          )}
        </div>
        <div className="cart-summary-actions">
          <button className="btn btn-secondary" onClick={clearCart}>清空购物车</button>
          <div>
            <span className="cart-summary-total">合计：¥{cartTotal.toFixed(2)}</span>
            {' '}
            <Link to="/order" className="btn btn-primary btn-lg">去结算</Link>
          </div>
        </div>
      </div>
    </div>
  );
}

export default CartPage;
