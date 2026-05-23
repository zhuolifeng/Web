import { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Table,
  Button,
  InputNumber,
  Typography,
  Card,
  Space,
  Statistic,
  Empty,
  Popconfirm,
  Spin,
  message,
} from 'antd';
import {
  DeleteOutlined,
  ShoppingCartOutlined,
  ClearOutlined,
} from '@ant-design/icons';
import { useCart } from '../context/CartContext';
import { parsePrice } from '../utils/price';

const { Title, Text } = Typography;

function CartPage() {
  const {
    cartItems,
    removeFromCart,
    updateQuantity,
    clearCart,
    cartTotal,
    cartOriginalTotal,
    loading,
  } = useCart();

  const [imgErrors, setImgErrors] = useState({});
  const handleImgError = (id) => setImgErrors(prev => ({ ...prev, [id]: true }));

  if (loading) {
    return <div style={{ textAlign: 'center', padding: 80 }}><Spin size="large" /></div>;
  }

  if (cartItems.length === 0) {
    return (
      <div className="empty-wrapper">
        <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="购物车空空如也">
          <Link to="/">
            <Button type="primary" icon={<ShoppingCartOutlined />}>去逛逛</Button>
          </Link>
        </Empty>
      </div>
    );
  }

  const discount = cartOriginalTotal - cartTotal;

  const columns = [
    {
      title: '商品信息',
      dataIndex: 'title',
      key: 'title',
      render: (_, record) => (
        <div className="cart-row-cell">
          <div className="cart-thumb">
            {imgErrors[record.id] ? (
              <span>{record.coverEmoji}</span>
            ) : (
              <img
                src={record.coverImg}
                alt={record.title}
                onError={() => handleImgError(record.id)}
                className="cart-thumb-img"
              />
            )}
          </div>
          <div>
            <Link to={`/book/${record.bookId}`}>
              <Text strong>{record.title}</Text>
            </Link>
            <br />
            <Text type="secondary" className="cart-author">{record.author}</Text>
          </div>
        </div>
      ),
    },
    {
      title: '单价',
      dataIndex: 'price',
      key: 'price',
      width: 120,
      render: (price) => <Text strong className="cart-price-cell">{price}</Text>,
    },
    {
      title: '数量',
      dataIndex: 'quantity',
      key: 'quantity',
      width: 150,
      render: (qty, record) => (
        <InputNumber
          min={1}
          max={99}
          value={qty}
          onChange={(val) => {
            if (!val) return;
            updateQuantity(record.id, val).catch(err =>
              message.error(err.message || '更新数量失败'));
          }}
        />
      ),
    },
    {
      title: '小计',
      key: 'subtotal',
      width: 120,
      render: (_, record) => (
        <Text strong>¥{(parsePrice(record.price) * record.quantity).toFixed(2)}</Text>
      ),
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_, record) => (
        <Popconfirm
          title="确定删除该商品？"
          onConfirm={() => removeFromCart(record.id).catch(err =>
            message.error(err.message || '删除失败'))}
          okText="确定"
          cancelText="取消"
        >
          <Button type="link" danger icon={<DeleteOutlined />}>删除</Button>
        </Popconfirm>
      ),
    },
  ];

  return (
    <>
      <Title level={3} className="section-title">
        <ShoppingCartOutlined /> 购物车（{cartItems.length} 件商品）
      </Title>

      <Card bodyStyle={{ padding: 0 }}>
        <Table
          dataSource={cartItems}
          columns={columns}
          rowKey="id"
          pagination={false}
        />
      </Card>

      <Card className="cart-summary-card">
        <div className="cart-summary-row">
          <Space size="large">
            <Statistic
              title="商品件数"
              value={cartItems.reduce((s, i) => s + i.quantity, 0)}
              suffix="件"
            />
            {discount > 0 && (
              <Statistic
                title="已优惠"
                value={discount}
                precision={2}
                prefix="¥"
                valueStyle={{ color: '#37b24d' }}
              />
            )}
          </Space>

          <Space size="middle">
            <Popconfirm
              title="确定清空购物车？"
              onConfirm={() => clearCart().catch(err =>
                message.error(err.message || '清空失败'))}
              okText="确定"
              cancelText="取消"
            >
              <Button icon={<ClearOutlined />}>清空购物车</Button>
            </Popconfirm>
            <Text className="cart-total-text">合计：¥{cartTotal.toFixed(2)}</Text>
            <Link to="/order">
              <Button type="primary" size="large">去结算</Button>
            </Link>
          </Space>
        </div>
      </Card>
    </>
  );
}

export default CartPage;
