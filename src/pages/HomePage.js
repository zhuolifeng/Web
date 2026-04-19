import { Link } from 'react-router-dom';
import BookList from '../components/BookList';
import { books } from '../Data';

function HomePage() {
  return (
    <>
      <section className="hero">
        <h2 className="hero-title">探索知识的海洋</h2>
        <p className="hero-subtitle">精选万本好书，为您开启智慧之旅</p>
        <Link to="/#books" className="btn btn-primary btn-lg">浏览书籍</Link>
      </section>

      <BookList books={books} />
    </>
  );
}

export default HomePage;
