import { useState } from 'react';
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
} from 'antd';
import {
  ShoppingCartOutlined,
  ThunderboltOutlined,
  CheckCircleOutlined,
  HomeOutlined,
} from '@ant-design/icons';
import { getBookById } from '../Data';
import { useCart } from '../context/CartContext';

const { Title, Paragraph, Text } = Typography;

function BookDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const book = getBookById(id);
  const { addToCart } = useCart();
  const [quantity, setQuantity] = useState(1);

  if (!book) {
    return (
      <Result
        status="404"
        title="书籍不存在"
        subTitle="抱歉，您查找的书籍不存在"
        extra={
          <Link to="/">
            <Button type="primary">返回首页</Button>
          </Link>
        }
      />
    );
  }

  const handleAddToCart = () => {
    addToCart(book.id, quantity);
    message.success('已加入购物车');
  };

  const handleBuyNow = () => {
    addToCart(book.id, quantity);
    navigate('/order');
  };

  return (
    <>
      <Breadcrumb
        style={{ marginBottom: 24 }}
        items={[
          { title: <Link to="/"><HomeOutlined /> 首页</Link> },
          { title: book.category },
          { title: book.title },
        ]}
      />

      <Card style={{ marginBottom: 24 }}>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1.5fr', gap: 40 }}>
          {/* Cover */}
          <div
            style={{
              height: 400,
              background: 'linear-gradient(135deg, #e9ecef 0%, #dee2e6 100%)',
              borderRadius: 8,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 120,
            }}
          >
            {book.coverEmoji}
          </div>

          {/* Info */}
          <div style={{ display: 'flex', flexDirection: 'column' }}>
            <Title level={2} style={{ marginBottom: 8 }}>{book.title}</Title>
            <Text type="secondary" style={{ fontSize: 16, marginBottom: 16 }}>{book.author}</Text>

            <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 20 }}>
              <Rate disabled defaultValue={parseFloat(book.ratingNum)} allowHalf />
              <Text strong style={{ color: '#faad14' }}>{book.ratingNum}</Text>
              <Text type="secondary">{book.ratingCount}</Text>
            </div>

            <Paragraph type="secondary" style={{ lineHeight: 1.8 }}>{book.description}</Paragraph>

            <div
              style={{
                background: '#f8f9fa',
                borderRadius: 8,
                padding: 20,
                marginBottom: 24,
              }}
            >
              <Text style={{ fontSize: 32, fontWeight: 700, color: '#f03e3e' }}>
                {book.price}
              </Text>
              <br />
              <Text delete type="secondary" style={{ fontSize: 16 }}>
                原价 {book.originalPrice}
              </Text>
              <br />
              <Tag color="green" style={{ marginTop: 8 }}>
                <CheckCircleOutlined /> 库存充足
              </Tag>
            </div>

            <Space size="middle" style={{ marginTop: 'auto' }}>
              <InputNumber
                min={1}
                max={99}
                value={quantity}
                onChange={(val) => val && setQuantity(val)}
                style={{ width: 100 }}
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

      {/* Book Details */}
      <Card title="书籍详情">
        <Title level={4}>内容简介</Title>
        <Paragraph style={{ lineHeight: 1.8 }}>{book.intro}</Paragraph>

        <Title level={4}>作者简介</Title>
        <Paragraph style={{ lineHeight: 1.8 }}>{book.authorBio}</Paragraph>

        <Title level={4}>书籍信息</Title>
        <Descriptions bordered column={2}>
          {Object.entries(book.info).map(([key, value]) => (
            <Descriptions.Item key={key} label={key}>{value}</Descriptions.Item>
          ))}
        </Descriptions>
      </Card>
    </>
  );
}

export default BookDetailPage;
