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
} from 'antd';
import {
  DeleteOutlined,
  ShoppingCartOutlined,
  ClearOutlined,
} from '@ant-design/icons';
import { useCart } from '../context/CartContext';

const { Title, Text } = Typography;

function CartPage() {
  const {
    cartItems,
    removeFromCart,
    updateQuantity,
    clearCart,
    cartTotal,
    cartOriginalTotal,
  } = useCart();

  if (cartItems.length === 0) {
    return (
      <div style={{ textAlign: 'center', padding: '80px 0' }}>
        <Empty
          image={Empty.PRESENTED_IMAGE_SIMPLE}
          description="购物车空空如也"
        >
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
        <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
          <div
            style={{
              width: 60,
              height: 80,
              background: 'linear-gradient(135deg, #e9ecef 0%, #dee2e6 100%)',
              borderRadius: 4,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              fontSize: 28,
              flexShrink: 0,
            }}
          >
            {record.coverEmoji}
          </div>
          <div>
            <Link to={`/book/${record.id}`}>
              <Text strong>{record.title}</Text>
            </Link>
            <br />
            <Text type="secondary" style={{ fontSize: 13 }}>{record.author}</Text>
          </div>
        </div>
      ),
    },
    {
      title: '单价',
      dataIndex: 'price',
      key: 'price',
      width: 120,
      render: (price) => <Text strong style={{ color: '#f03e3e' }}>{price}</Text>,
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
          onChange={(val) => val && updateQuantity(record.id, val)}
        />
      ),
    },
    {
      title: '小计',
      key: 'subtotal',
      width: 120,
      render: (_, record) => {
        const unitPrice = parseFloat(record.price.replace('¥', ''));
        return (
          <Text strong>¥{(unitPrice * record.quantity).toFixed(2)}</Text>
        );
      },
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_, record) => (
        <Popconfirm
          title="确定删除该商品？"
          onConfirm={() => removeFromCart(record.id)}
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
      <Title level={3} style={{ marginBottom: 24 }}>
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

      <Card style={{ marginTop: 24 }}>
        <div
          style={{
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
          }}
        >
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
              onConfirm={clearCart}
              okText="确定"
              cancelText="取消"
            >
              <Button icon={<ClearOutlined />}>清空购物车</Button>
            </Popconfirm>
            <Text style={{ fontSize: 24, fontWeight: 700, color: '#f03e3e' }}>
              合计：¥{cartTotal.toFixed(2)}
            </Text>
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
