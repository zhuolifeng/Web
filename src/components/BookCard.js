import { useState } from 'react';
import { Link } from 'react-router-dom';

function BookCard({ book }) {
  const [imgError, setImgError] = useState(false);

  return (
    <article className="book-card">
      <Link to={`/book/${book.id}`} className="book-card-link">
        <figure className="book-cover">
          {book.badge && <span className="book-badge">{book.badge}</span>}
          {imgError ? (
            <span className="book-cover-ph">{book.coverEmoji}</span>
          ) : (
            <img
              className="book-cover-img"
              src={book.coverImg}
              alt={`${book.title}封面`}
              onError={() => setImgError(true)}
            />
          )}
        </figure>
        <div className="book-info">
          <h3 className="book-title">{book.title}</h3>
          <p className="book-author">{book.author}</p>
          <div className="book-meta">
            <span className="book-price">{book.price}</span>
            <span className="book-rating">{book.stars} {book.ratingNum}</span>
          </div>
        </div>
      </Link>
    </article>
  );
}

export default BookCard;
