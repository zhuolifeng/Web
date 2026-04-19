import { Link } from 'react-router-dom';
import BookCard from './BookCard';

function BookList({ books }) {
  return (
    <section id="books">
      <div className="section-header">
        <h2 className="section-title">热门书籍</h2>
        <Link to="#" className="btn btn-secondary btn-sm">查看更多</Link>
      </div>

      <div className="book-grid">
        {books.map(book => (
          <BookCard key={book.id} book={book} />
        ))}
      </div>
    </section>
  );
}

export default BookList;
