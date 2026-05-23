import { Row, Col, Typography } from 'antd';
import BookCard from './BookCard';

const { Title } = Typography;

function BookList({ books }) {
  return (
    <section>
      <Title level={3} className="section-title">热门书籍</Title>
      <Row gutter={[24, 24]}>
        {books.map(book => (
          <Col key={book.id} xs={24} sm={12} md={8} lg={6}>
            <BookCard book={book} />
          </Col>
        ))}
      </Row>
    </section>
  );
}

export default BookList;
