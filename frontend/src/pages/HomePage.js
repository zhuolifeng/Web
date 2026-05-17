import { Button, Typography } from 'antd';
import { RocketOutlined } from '@ant-design/icons';
import BookList from '../components/BookList';
import { books } from '../Data';

const { Title, Paragraph } = Typography;

function HomePage() {
  const scrollToBooks = () => {
    const el = document.getElementById('book-list');
    if (el) el.scrollIntoView({ behavior: 'smooth' });
  };

  return (
    <>
      <div
        style={{
          background: 'linear-gradient(135deg, #4a6cf7 0%, #764ba2 100%)',
          borderRadius: 12,
          padding: '60px 40px',
          textAlign: 'center',
          marginBottom: 40,
        }}
      >
        <Title level={2} style={{ color: '#fff', marginBottom: 12 }}>
          探索知识的海洋
        </Title>
        <Paragraph style={{ color: 'rgba(255,255,255,0.9)', fontSize: 18, marginBottom: 24 }}>
          精选万本好书，为您开启智慧之旅
        </Paragraph>
        <Button
          type="primary"
          size="large"
          icon={<RocketOutlined />}
          onClick={scrollToBooks}
          style={{ height: 48, paddingInline: 32 }}
        >
          浏览书籍
        </Button>
      </div>

      <div id="book-list">
        <BookList books={books} />
      </div>
    </>
  );
}

export default HomePage;
