import { useEffect, useState } from 'react';
import { Button, Typography, Spin, Empty, message } from 'antd';
import { RocketOutlined } from '@ant-design/icons';
import BookList from '../components/BookList';
import { bookService } from '../services/bookService';

const { Title, Paragraph } = Typography;

function HomePage() {
  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    bookService.list()
      .then((res) => { if (mounted) setBooks(res.data || []); })
      .catch((err) => message.error(err.message || '获取书籍失败'))
      .finally(() => { if (mounted) setLoading(false); });
    return () => { mounted = false; };
  }, []);

  const scrollToBooks = () => {
    const el = document.getElementById('book-list');
    if (el) el.scrollIntoView({ behavior: 'smooth' });
  };

  return (
    <>
      <div className="hero">
        <Title level={2} className="hero-title">探索知识的海洋</Title>
        <Paragraph className="hero-text">
          精选万本好书，为您开启智慧之旅
        </Paragraph>
        <Button
          type="primary"
          size="large"
          icon={<RocketOutlined />}
          onClick={scrollToBooks}
          className="hero-button"
        >
          浏览书籍
        </Button>
      </div>

      <div id="book-list">
        {loading ? (
          <div style={{ textAlign: 'center', padding: 60 }}><Spin size="large" /></div>
        ) : books.length === 0 ? (
          <Empty description="书库暂时空空如也" />
        ) : (
          <BookList books={books} />
        )}
      </div>
    </>
  );
}

export default HomePage;
