import { useState } from 'react';
import { Link } from 'react-router-dom';
import { Card, Tag, Rate } from 'antd';

const { Meta } = Card;

function BookCard({ book }) {
  const [imgError, setImgError] = useState(false);

  return (
    <Link to={`/book/${book.id}`} style={{ textDecoration: 'none' }}>
      <Card
        hoverable
        cover={
          <div
            style={{
              height: 220,
              background: 'linear-gradient(135deg, #e9ecef 0%, #dee2e6 100%)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              position: 'relative',
              overflow: 'hidden',
            }}
          >
            {book.badge && (
              <Tag
                color="gold"
                style={{ position: 'absolute', top: 12, left: 12, zIndex: 1 }}
              >
                {book.badge}
              </Tag>
            )}
            {imgError ? (
              <span style={{ fontSize: 64 }}>{book.coverEmoji}</span>
            ) : (
              <img
                src={book.coverImg}
                alt={`${book.title}封面`}
                onError={() => setImgError(true)}
                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
              />
            )}
          </div>
        }
        style={{ borderRadius: 12 }}
        bodyStyle={{ padding: 16 }}
      >
        <Meta
          title={<span style={{ fontSize: 15, fontWeight: 600 }}>{book.title}</span>}
          description={book.author}
        />
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginTop: 12,
          }}
        >
          <span style={{ fontSize: 20, fontWeight: 700, color: '#f03e3e' }}>
            {book.price}
          </span>
          <Rate disabled defaultValue={parseFloat(book.ratingNum)} allowHalf style={{ fontSize: 14 }} />
        </div>
      </Card>
    </Link>
  );
}

export default BookCard;
