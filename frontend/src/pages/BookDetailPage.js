import { useEffect, useState } from 'react';
import { Link, useParams, useNavigate } from 'react-router-dom';
import {
  Button,
  InputNumber,
  Breadcrumb,
  Rate,
  Tag,
  Descriptions,
  Typography,
  Card,
  Space,
  message,
  Result,
  Spin,
} from 'antd';
import {
  ShoppingCartOutlined,
  ThunderboltOutlined,
  CheckCircleOutlined,
  HomeOutlined,
} from '@ant-design/icons';
import { bookService } from '../services/bookService';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';

const { Title, Paragraph, Text } = Typography;

function BookDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addToCart } = useCart();
  const { isAuthenticated } = useAuth();
  const [book, setBook] = useState(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [quantity, setQuantity] = useState(1);
  const [imgError, setImgError] = useState(false);

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    setNotFound(false);
    bookService.getById(id)
      .then((res) => { if (mounted) setBook(res.data); })
      .catch((err) => {
        if (mounted) {
          if (err.code === 404) setNotFound(true);
          else message.error(err.message || '获取书籍详情失败');
        }
      })
      .finally(() => { if (mounted) setLoading(false); });
    return () => { mounted = false; };
  }, [id]);

  if (loading) {
    return <div style={{ textAlign: 'center', padding: 80 }}><Spin size="large" /></div>;
  }

  if (notFound || !book) {
    return (
      <Result
        status="404"
        title="书籍不存在"
        subTitle="抱歉，您查找的书籍不存在"
        extra={<Link to="/"><Button type="primary">返回首页</Button></Link>}
      />
    );
  }

  const handleAddToCart = async () => {
    if (!isAuthenticated) {
      message.warning('请先登录后再加入购物车');
      navigate('/login', { state: { from: { pathname: `/book/${id}` } } });
      return;
    }
    try {
      await addToCart(book.id, quantity);
      message.success('已加入购物车');
    } catch (err) {
      message.error(err.message || '加入购物车失败');
    }
  };

  const handleBuyNow = async () => {
    if (!isAuthenticated) {
      message.warning('请先登录');
      navigate('/login', { state: { from: { pathname: `/book/${id}` } } });
      return;
    }
    try {
      await addToCart(book.id, quantity);
      navigate('/order');
    } catch (err) {
      message.error(err.message || '操作失败');
    }
  };

  return (
    <>
      <Breadcrumb
        className="detail-breadcrumb"
        items={[
          { title: <Link to="/"><HomeOutlined /> 首页</Link> },
          { title: book.category },
          { title: book.title },
        ]}
      />

      <Card className="detail-card">
        <div className="detail-grid book-detail-grid">
          <div className="detail-cover">
            {imgError ? (
              <span className="detail-cover-emoji">{book.coverEmoji}</span>
            ) : (
              <img
                src={book.coverImg}
                alt={`${book.title}封面`}
                onError={() => setImgError(true)}
                className="detail-cover-img"
              />
            )}
          </div>

          <div className="detail-info">
            <Title level={2} style={{ marginBottom: 8 }}>{book.title}</Title>
            <Text type="secondary" className="detail-author">{book.author}</Text>

            <div className="detail-rating-row">
              <Rate disabled defaultValue={parseFloat(book.ratingNum) || 0} allowHalf />
              <Text strong className="detail-rating-num">{book.ratingNum}</Text>
              <Text type="secondary">{book.ratingCount}</Text>
            </div>

            <Paragraph type="secondary" className="detail-desc">{book.description}</Paragraph>

            <div className="detail-price-box">
              <Text className="detail-price">{book.price}</Text>
              <br />
              {book.originalPrice && (
                <>
                  <Text delete type="secondary" className="detail-original-price">
                    原价 {book.originalPrice}
                  </Text>
                  <br />
                </>
              )}
              <Tag color="green" className="detail-stock-tag">
                <CheckCircleOutlined /> 库存充足
              </Tag>
            </div>

            <Space size="middle" className="detail-actions">
              <InputNumber
                min={1}
                max={99}
                value={quantity}
                onChange={(val) => val && setQuantity(val)}
                className="detail-qty-input"
                size="large"
              />
              <Button
                type="primary"
                size="large"
                icon={<ShoppingCartOutlined />}
                onClick={handleAddToCart}
              >
                加入购物车
              </Button>
              <Button
                size="large"
                icon={<ThunderboltOutlined />}
                onClick={handleBuyNow}
              >
                立即购买
              </Button>
            </Space>
          </div>
        </div>
      </Card>

      <Card title="书籍详情">
        <Title level={4}>内容简介</Title>
        <Paragraph className="detail-body-paragraph">{book.intro}</Paragraph>

        <Title level={4}>作者简介</Title>
        <Paragraph className="detail-body-paragraph">{book.authorBio}</Paragraph>

        <Title level={4}>书籍信息</Title>
        <Descriptions bordered column={2}>
          {book.publisher && <Descriptions.Item label="出版社">{book.publisher}</Descriptions.Item>}
          {book.publishDate && <Descriptions.Item label="出版日期">{book.publishDate}</Descriptions.Item>}
          {book.pages && <Descriptions.Item label="页数">{book.pages}</Descriptions.Item>}
          {book.isbn && <Descriptions.Item label="ISBN">{book.isbn}</Descriptions.Item>}
          {book.binding && <Descriptions.Item label="装帧">{book.binding}</Descriptions.Item>}
          {book.category && <Descriptions.Item label="分类">{book.category}</Descriptions.Item>}
        </Descriptions>
      </Card>
    </>
  );
}

export default BookDetailPage;
