import { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Form,
  Input,
  Button,
  Card,
  Typography,
  Radio,
  Statistic,
  Divider,
  Tag,
  Result,
  Empty,
  Space,
} from 'antd';
import {
  AlipayCircleOutlined,
  WechatOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons';
import { useCart } from '../context/CartContext';

const { Title, Text } = Typography;
const { TextArea } = Input;

function OrderPage() {
  const { cartItems, cartTotal, cartOriginalTotal, clearCart } = useCart();
  const [submitted, setSubmitted] = useState(false);
  const [payment, setPayment] = useState('alipay');
  const [imgErrors, setImgErrors] = useState({});

  const handleImgError = (id) => {
    setImgErrors(prev => ({ ...prev, [id]: true }));
  };

  const shipping = cartTotal >= 99 ? 0 : 10;
  const totalWithShipping = cartTotal + shipping;
  const discount = cartOriginalTotal - cartTotal;

  const onFinish = () => {
    setSubmitted(true);
    clearCart();
  };

  if (cartItems.length === 0 && !submitted) {
    return (
      <div style={{ textAlign: 'center', padding: '80px 0' }}>
        <Empty description="暂无可结算的商品">
          <Link to="/">
            <Button type="primary">去选书</Button>
          </Link>
        </Empty>
      </div>
    );
  }

  if (submitted) {
    return (
      <Result
        status="success"
        title="下单成功！"
        subTitle="感谢您的购买，我们将尽快安排发货。"
        extra={
          <Link to="/">
            <Button type="primary" size="large">继续逛逛</Button>
          </Link>
        }
      />
    );
  }

  return (
    <>
      <Title level={3} style={{ marginBottom: 24 }}>确认订单</Title>

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 380px', gap: 24 }}>
        {/* Left column */}
        <div>
          <Card title="收货信息" style={{ marginBottom: 24 }}>
            <Form
              layout="vertical"
              id="order-form"
              onFinish={onFinish}
            >
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 16px' }}>
                <Form.Item
                  label="收货人"
                  name="name"
                  rules={[{ required: true, message: '请输入收货人姓名' }]}
                >
                  <Input placeholder="请输入收货人姓名" />
                </Form.Item>
                <Form.Item
                  label="手机号"
                  name="phone"
                  rules={[{ required: true, message: '请输入手机号码' }]}
                >
                  <Input placeholder="请输入手机号码" />
                </Form.Item>
                <Form.Item
                  label="省份"
                  name="province"
                  rules={[{ required: true, message: '请输入省份' }]}
                >
                  <Input placeholder="省" />
                </Form.Item>
                <Form.Item
                  label="城市"
                  name="city"
                  rules={[{ required: true, message: '请输入城市' }]}
                >
                  <Input placeholder="市" />
                </Form.Item>
              </div>
              <Form.Item
                label="详细地址"
                name="address"
                rules={[{ required: true, message: '请输入详细地址' }]}
              >
                <Input placeholder="请输入详细收货地址" />
              </Form.Item>
              <Form.Item label="备注" name="note">
                <TextArea placeholder="选填，有什么要备注的？" rows={2} />
              </Form.Item>
            </Form>
          </Card>

          <Card title="商品清单">
            {cartItems.map(item => {
              const unitPrice = parseFloat(item.price.replace('¥', ''));
              const subtotal = (unitPrice * item.quantity).toFixed(2);
              return (
                <div
                  key={item.id}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: 16,
                    padding: 12,
                    background: '#f8f9fa',
                    borderRadius: 8,
                    marginBottom: 12,
                  }}
                >
                  <div
                    style={{
                      width: 50,
                      height: 65,
                      background: 'linear-gradient(135deg, #e9ecef, #dee2e6)',
                      borderRadius: 4,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontSize: 24,
                      flexShrink: 0,
                      overflow: 'hidden',
                    }}
                  >
                    {imgErrors[item.id] ? (
                      <span>{item.coverEmoji}</span>
                    ) : (
                      <img
                        src={item.coverImg}
                        alt={item.title}
                        onError={() => handleImgError(item.id)}
                        style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                      />
                    )}
                  </div>
                  <div style={{ flex: 1 }}>
                    <Text strong>{item.title}</Text>
                    <br />
                    <Text type="secondary" style={{ fontSize: 13 }}>{item.author}</Text>
                  </div>
                  <div style={{ textAlign: 'center' }}>
                    <Text>{item.price}</Text>
                    <br />
                    <Text type="secondary">x{item.quantity}</Text>
                  </div>
                  <Text strong style={{ color: '#f03e3e', minWidth: 80, textAlign: 'right' }}>
                    ¥{subtotal}
                  </Text>
                </div>
              );
            })}
          </Card>
        </div>

        {/* Right column */}
        <div>
          <Card
            title={
              <div
                style={{
                  background: 'linear-gradient(135deg, #4a6cf7 0%, #764ba2 100%)',
                  margin: '-24px -24px 0',
                  padding: '16px 24px',
                  borderRadius: '8px 8px 0 0',
                  color: '#fff',
                  fontWeight: 600,
                  fontSize: 16,
                }}
              >
                订单摘要
              </div>
            }
            headStyle={{ padding: 0, border: 'none' }}
          >
            <Space direction="vertical" style={{ width: '100%' }} size="small">
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <Text type="secondary">商品金额</Text>
                <Text strong>¥{cartTotal.toFixed(2)}</Text>
              </div>
              {discount > 0 && (
                <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                  <Text type="secondary">优惠</Text>
                  <Text style={{ color: '#37b24d' }}>-¥{discount.toFixed(2)}</Text>
                </div>
              )}
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <Text type="secondary">运费</Text>
                {shipping === 0 ? (
                  <Tag color="green">免运费</Tag>
                ) : (
                  <Text strong>¥{shipping.toFixed(2)}</Text>
                )}
              </div>
              <Divider style={{ margin: '12px 0' }} />
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <Text strong style={{ fontSize: 16 }}>应付总额</Text>
                <Statistic
                  value={totalWithShipping}
                  precision={2}
                  prefix="¥"
                  valueStyle={{ color: '#f03e3e', fontSize: 24, fontWeight: 700 }}
                />
              </div>
            </Space>

            <Divider />

            <Title level={5}>支付方式</Title>
            <Radio.Group
              value={payment}
              onChange={(e) => setPayment(e.target.value)}
              style={{ width: '100%' }}
            >
              <Space direction="vertical" style={{ width: '100%' }}>
                <Card
                  size="small"
                  hoverable
                  style={{
                    borderColor: payment === 'alipay' ? '#4a6cf7' : undefined,
                    background: payment === 'alipay' ? 'rgba(74,108,247,0.05)' : undefined,
                  }}
                  onClick={() => setPayment('alipay')}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                    <Radio value="alipay" />
                    <AlipayCircleOutlined style={{ fontSize: 24, color: '#1677ff' }} />
                    <div>
                      <Text strong>支付宝</Text>
                      <br />
                      <Text type="secondary" style={{ fontSize: 12 }}>推荐使用</Text>
                    </div>
                  </div>
                </Card>
                <Card
                  size="small"
                  hoverable
                  style={{
                    borderColor: payment === 'wechat' ? '#4a6cf7' : undefined,
                    background: payment === 'wechat' ? 'rgba(74,108,247,0.05)' : undefined,
                  }}
                  onClick={() => setPayment('wechat')}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                    <Radio value="wechat" />
                    <WechatOutlined style={{ fontSize: 24, color: '#52c41a' }} />
                    <div>
                      <Text strong>微信支付</Text>
                      <br />
                      <Text type="secondary" style={{ fontSize: 12 }}>微信扫码支付</Text>
                    </div>
                  </div>
                </Card>
              </Space>
            </Radio.Group>

            <Button
              type="primary"
              size="large"
              block
              htmlType="submit"
              form="order-form"
              style={{ marginTop: 20, height: 48 }}
              icon={<CheckCircleOutlined />}
            >
              提交订单
            </Button>
          </Card>
        </div>
      </div>
    </>
  );
}

export default OrderPage;
