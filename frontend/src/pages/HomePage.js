import { useEffect, useState, useCallback } from 'react';
import { Button, Typography, Spin, Empty, Input, message } from 'antd';
import { RocketOutlined, SearchOutlined } from '@ant-design/icons';
import BookList from '../components/BookList';
import { bookService } from '../services/bookService';

const { Title, Paragraph } = Typography;
const { Search } = Input;

/**
 * 首页：展示书籍列表，支持按书名搜索。
 */
function HomePage() {
  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [keyword, setKeyword] = useState('');

  const fetchBooks = useCallback((kw) => {
    setLoading(true);
    bookService.list(kw || undefined)
      .then((res) => setBooks(res.data || []))
      .catch((err) => message.error(err.message || '获取书籍失败'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    fetchBooks();
  }, [fetchBooks]);

  const handleSearch = (value) => {
    setKeyword(value);
    fetchBooks(value);
  };

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
        {/* 搜索栏 */}
        <div style={{ maxWidth: 480, margin: '0 auto 24px', padding: '0 16px' }}>
          <Search
            placeholder="搜索书名、作者或分类..."
            allowClear
            enterButton={<><SearchOutlined /> 搜索</>}
            size="large"
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onSearch={handleSearch}
          />
        </div>

        {loading ? (
          <div style={{ textAlign: 'center', padding: 60 }}><Spin size="large" /></div>
        ) : books.length === 0 ? (
          <Empty description={keyword ? '没有找到匹配的书籍' : '书库暂时空空如也'} />
        ) : (
          <BookList books={books} />
        )}
      </div>
    </>
  );
}

export default HomePage;
