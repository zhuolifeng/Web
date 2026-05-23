import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Card, Tag, Rate } from 'antd';

const { Meta } = Card;

function BookCard({ book }) {
  const [imgError, setImgError] = useState(false);

  return (
    <Link to={`/book/${book.id}`} className="book-card-link">
      <Card
        hoverable
        cover={
          <div className="book-cover">
            {book.badge && (
              <Tag color="gold" className="book-badge">
                {book.badge}
              </Tag>
            )}
            {imgError ? (
              <span className="book-cover-emoji">{book.coverEmoji}</span>
            ) : (
              <img
                src={book.coverImg}
                alt={`${book.title}封面`}
                onError={() => setImgError(true)}
                className="book-cover-img"
              />
            )}
          </div>
        }
        className="book-card"
        bodyStyle={{ padding: 16 }}
      >
        <Meta
          title={<span className="book-meta-title">{book.title}</span>}
          description={book.author}
        />
        <div className="book-meta-row">
          <span className="book-price">{book.price}</span>
          <Rate
            disabled
            defaultValue={parseFloat(book.ratingNum)}
            allowHalf
            className="book-rate"
          />
        </div>
      </Card>
    </Link>
  );
}

export default BookCard;
