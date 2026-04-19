import BookList from '../components/BookList';
import { books } from '../Data';

function HomePage() {
  const scrollToBooks = (e) => {
    e.preventDefault();
    const el = document.getElementById('books');
    if (el) el.scrollIntoView({ behavior: 'smooth' });
  };

  return (
    <>
      <section className="hero">
        <h2 className="hero-title">探索知识的海洋</h2>
        <p className="hero-subtitle">精选万本好书，为您开启智慧之旅</p>
        <button className="btn btn-primary btn-lg" onClick={scrollToBooks}>浏览书籍</button>
      </section>

      <BookList books={books} />
    </>
  );
}

export default HomePage;
