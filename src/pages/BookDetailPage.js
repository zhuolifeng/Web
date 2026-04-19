import { useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { getBookById } from '../Data';

function BookDetailPage() {
  const { id } = useParams();
  const book = getBookById(id);
  const [quantity, setQuantity] = useState(1);
  const [imgError, setImgError] = useState(false);

  if (!book) {
    return (
      <div className="container" style={{ textAlign: 'center', padding: '100px 0' }}>
        <h2>书籍不存在</h2>
        <Link to="/" className="btn btn-primary">返回首页</Link>
      </div>
    );
  }

  const decreaseQuantity = () => {
    if (quantity > 1) setQuantity(quantity - 1);
  };

  const increaseQuantity = () => {
    if (quantity < 99) setQuantity(quantity + 1);
  };

  const handleQuantityChange = (e) => {
    const val = parseInt(e.target.value, 10);
    if (!isNaN(val) && val >= 1 && val <= 99) {
      setQuantity(val);
    } else if (e.target.value === '') {
      setQuantity(1);
    }
  };

  return (
    <>
      <nav className="breadcrumb" aria-label="面包屑导航">
        <ol className="breadcrumb-list">
          <li className="breadcrumb-item">
            <Link to="/" className="breadcrumb-link">首页</Link>
            <span className="breadcrumb-sep">/</span>
          </li>
          <li className="breadcrumb-item">
            <span className="breadcrumb-link">{book.category}</span>
            <span className="breadcrumb-sep">/</span>
          </li>
          <li className="breadcrumb-item">
            <span className="breadcrumb-current">{book.title}</span>
          </li>
        </ol>
      </nav>

      <div className="detail-wrapper">
        <figure className="detail-cover">
          {imgError ? (
            <span className="detail-cover-ph">{book.coverEmoji}</span>
          ) : (
            <img
              id="detail-cover-img"
              src={book.coverImg}
              alt={`${book.title}封面`}
              className="detail-cover-img"
              onError={() => setImgError(true)}
            />
          )}
        </figure>

        <div className="detail-info">
          <h1 className="detail-title">{book.title}</h1>
          <p className="detail-author">{book.author}</p>

          <div className="detail-rating">
            <span className="detail-stars">{book.stars}</span>
            <span className="detail-price-num">{book.ratingNum}</span>
            <span className="rating-count">{book.ratingCount}</span>
          </div>

          <p className="detail-description">{book.description}</p>

          <div className="detail-price-box">
            <div className="detail-price">{book.price}</div>
            <div className="detail-original-price">原价 {book.originalPrice}</div>
            <div className="detail-stock">✓ 库存充足</div>
          </div>

          <div className="detail-actions">
            <div className="quantity-control">
              <button
                className="quantity-btn"
                onClick={decreaseQuantity}
                aria-label="减少数量"
              >−</button>
              <input
                type="number"
                className="quantity-input"
                value={quantity}
                onChange={handleQuantityChange}
                min="1"
                max="99"
                aria-label="购买数量"
              />
              <button
                className="quantity-btn"
                onClick={increaseQuantity}
                aria-label="增加数量"
              >+</button>
            </div>
            <Link to="/cart" className="btn btn-primary btn-lg">加入购物车</Link>
            <Link to="/order" className="btn btn-secondary btn-lg">立即购买</Link>
          </div>
        </div>
      </div>

      <section className="book-detail-section">
        <div className="section-header">
          <h2 className="section-title">书籍详情</h2>
        </div>
        <div className="card book-detail-card">
          <article>
            <h3 className="book-detail-subtitle">内容简介</h3>
            <p className="book-detail-text">{book.intro}</p>

            <h3 className="book-detail-subtitle">作者简介</h3>
            <p className="book-detail-text">{book.authorBio}</p>

            <h3 className="book-detail-subtitle">书籍信息</h3>
            <dl className="book-detail-info">
              {Object.entries(book.info).map(([key, value]) => (
                <div key={key} className="info-item">
                  <dt>{key}</dt>
                  <dd>{value}</dd>
                </div>
              ))}
            </dl>
          </article>
        </div>
      </section>
    </>
  );
}

export default BookDetailPage;
