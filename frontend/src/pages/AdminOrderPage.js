import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Card, Typography, Tag, Spin, Empty, Divider, DatePicker, Input, Space, Button, message,
} from 'antd';
import { UnorderedListOutlined, SearchOutlined } from '@ant-design/icons';
import { adminService } from '../services/adminService';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

/**
 * 管理员 - 订单管理页面：展示所有用户的订单，支持按日期和书名搜索。
 */
function AdminOrderPage() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [dateRange, setDateRange] = useState(null);
  const [bookTitle, setBookTitle] = useState('');

  const fetchOrders = (params = {}) => {
    setLoading(true);
    adminService.listAllOrders(params)
      .then((res) => setOrders(res.data || []))
      .catch((err) => message.error(err.message || '获取订单失败'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchOrders(); }, []);

  const handleSearch = () => {
    const params = {};
    if (dateRange && dateRange[0]) params.startDate = dateRange[0].format('YYYY-MM-DD');
    if (dateRange && dateRange[1]) params.endDate = dateRange[1].format('YYYY-MM-DD');
    if (bookTitle.trim()) params.bookTitle = bookTitle.trim();
    fetchOrders(params);
  };

  const handleReset = () => {
    setDateRange(null);
    setBookTitle('');
    fetchOrders();
  };

  if (loading) {
    return <div style={{ textAlign: 'center', padding: 80 }}><Spin size="large" /></div>;
  }

  return (
    <>
      <Title level={3} className="section-title">
        <UnorderedListOutlined /> 全部订单
      </Title>

      <Card style={{ marginBottom: 16 }} size="small">
        <Space wrap>
          <RangePicker
            value={dateRange}
            onChange={setDateRange}
            placeholder={['开始日期', '结束日期']}
          />
          <Input
            placeholder="按书名搜索"
            value={bookTitle}
            onChange={(e) => setBookTitle(e.target.value)}
            style={{ width: 200 }}
            onPressEnter={handleSearch}
          />
          <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>搜索</Button>
          <Button onClick={handleReset}>重置</Button>
        </Space>
      </Card>

      {orders.length === 0 ? (
        <Empty description="没有找到订单" />
      ) : (
        orders.map(order => (
          <Card key={order.id} className="order-card-spaced">
            <div className="order-list-header">
              <div>
                <Text strong>订单号：</Text>
                <Text copyable>{order.orderNo}</Text>
                <Divider type="vertical" />
                <Text type="secondary">{formatDate(order.createdAt)}</Text>
              </div>
              <Tag color={statusColor(order.status)}>{statusLabel(order.status)}</Tag>
            </div>

            <Divider style={{ margin: '12px 0' }} />

            {(order.items || []).map(item => (
              <div key={item.id} className="order-item">
                <div className="order-item-thumb">
                  {item.coverImg ? (
                    <img src={item.coverImg} alt={item.title} className="order-item-thumb-img" />
                  ) : (
                    <span>{item.coverEmoji}</span>
                  )}
                </div>
                <div className="order-item-info">
                  <Link to={`/book/${item.bookId}`}>
                    <Text strong>{item.title}</Text>
                  </Link>
                  <br />
                  <Text type="secondary">{item.author}</Text>
                </div>
                <div className="order-item-qty">
                  <Text>&yen;{Number(item.price).toFixed(2)}</Text>
                  <br />
                  <Text type="secondary">x{item.quantity}</Text>
                </div>
                <Text strong className="order-item-subtotal">
                  &yen;{Number(item.subtotal).toFixed(2)}
                </Text>
              </div>
            ))}

            <Divider style={{ margin: '12px 0' }} />

            <div className="order-list-footer">
              <Text type="secondary">
                收货人：{order.receiver}　电话：{order.phone}　地址：{order.address}
              </Text>
              <Text strong className="order-total">
                合计：&yen;{Number(order.totalAmount).toFixed(2)}
              </Text>
            </div>
          </Card>
        ))
      )}
    </>
  );
}

function statusLabel(status) {
  switch (status) {
    case 'PAID':     return '已支付';
    case 'PENDING':  return '待支付';
    case 'SHIPPED':  return '已发货';
    case 'COMPLETED':return '已完成';
    case 'CANCELED': return '已取消';
    default:         return status || '未知';
  }
}

function statusColor(status) {
  switch (status) {
    case 'PAID':      return 'green';
    case 'PENDING':   return 'orange';
    case 'SHIPPED':   return 'blue';
    case 'COMPLETED': return 'cyan';
    case 'CANCELED':  return 'red';
    default:          return 'default';
  }
}

function formatDate(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  if (isNaN(d.getTime())) return iso;
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export default AdminOrderPage;
