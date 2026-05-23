import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Card, Typography, Tag, Spin, Empty, Button, Divider, message,
} from 'antd';
import { FileTextOutlined, ShoppingOutlined } from '@ant-design/icons';
import { orderService } from '../services/orderService';

const { Title, Text } = Typography;

function OrderListPage() {
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    orderService.list()
      .then((res) => { if (mounted) setOrders(res.data || []); })
      .catch((err) => message.error(err.message || '获取订单失败'))
      .finally(() => { if (mounted) setLoading(false); });
    return () => { mounted = false; };
  }, []);

  if (loading) {
    return <div style={{ textAlign: 'center', padding: 80 }}><Spin size="large" /></div>;
  }

  if (orders.length === 0) {
    return (
      <div className="empty-wrapper">
        <Empty description="还没有订单哦">
          <Link to="/">
            <Button type="primary" icon={<ShoppingOutlined />}>去选书</Button>
          </Link>
        </Empty>
      </div>
    );
  }

  return (
    <>
      <Title level={3} className="section-title">
        <FileTextOutlined /> 我的订单
      </Title>

      {orders.map(order => (
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
                <Text type="secondary" className="cart-author">{item.author}</Text>
              </div>
              <div className="order-item-qty">
                <Text>¥{Number(item.price).toFixed(2)}</Text>
                <br />
                <Text type="secondary">x{item.quantity}</Text>
              </div>
              <Text strong className="order-item-subtotal">
                ¥{Number(item.subtotal).toFixed(2)}
              </Text>
            </div>
          ))}

          <Divider style={{ margin: '12px 0' }} />

          <div className="order-list-footer">
            <Text type="secondary">
              收货人：{order.receiver}　电话：{order.phone}　地址：{order.address}
            </Text>
            <Text strong className="order-total">
              合计：¥{Number(order.totalAmount).toFixed(2)}
            </Text>
          </div>
        </Card>
      ))}
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
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} `
       + `${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export default OrderListPage;
