import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
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
  Empty,
  Space,
  message,
} from 'antd';
import {
  AlipayCircleOutlined,
  WechatOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons';
import { useCart } from '../context/CartContext';
import { orderService } from '../services/orderService';
import { parsePrice } from '../utils/price';

const { Title, Text } = Typography;
const { TextArea } = Input;

function OrderPage() {
  const { cartItems, cartTotal, cartOriginalTotal, refresh } = useCart();
  const [payment, setPayment] = useState('alipay');
  const [submitting, setSubmitting] = useState(false);
  const [imgErrors, setImgErrors] = useState({});
  const navigate = useNavigate();

  const handleImgError = (id) => setImgErrors(prev => ({ ...prev, [id]: true }));

  const shipping = cartTotal >= 99 ? 0 : 10;
  const totalWithShipping = cartTotal + shipping;
  const discount = cartOriginalTotal - cartTotal;

  const onFinish = async (values) => {
    setSubmitting(true);
    try {
      const res = await orderService.create({
        receiver: values.name,
        phone: values.phone,
        province: values.province,
        city: values.city,
        address: values.address,
        note: values.note,
        payment,
      });
      message.success('下单成功！');
      await refresh();
      navigate(`/orders`);
      return res;
    } catch (err) {
      message.error(err.message || '下单失败');
    } finally {
      setSubmitting(false);
    }
  };

  if (cartItems.length === 0) {
    return (
      <div className="empty-wrapper">
        <Empty description="暂无可结算的商品">
          <Link to="/">
            <Button type="primary">去选书</Button>
          </Link>
        </Empty>
      </div>
    );
  }

  return (
    <>
      <Title level={3} className="section-title">确认订单</Title>

      <div className="order-grid">
        <div>
          <Card title="收货信息" className="order-card-spaced">
            <Form layout="vertical" id="order-form" onFinish={onFinish}>
              <div className="order-form-grid">
                <Form.Item label="收货人" name="name"
                  rules={[{ required: true, message: '请输入收货人姓名' }]}>
                  <Input placeholder="请输入收货人姓名" />
                </Form.Item>
                <Form.Item label="手机号" name="phone"
                  rules={[{ required: true, message: '请输入手机号码' }]}>
                  <Input placeholder="请输入手机号码" />
                </Form.Item>
                <Form.Item label="省份" name="province"
                  rules={[{ required: true, message: '请输入省份' }]}>
                  <Input placeholder="省" />
                </Form.Item>
                <Form.Item label="城市" name="city"
                  rules={[{ required: true, message: '请输入城市' }]}>
                  <Input placeholder="市" />
                </Form.Item>
              </div>
              <Form.Item label="详细地址" name="address"
                rules={[{ required: true, message: '请输入详细地址' }]}>
                <Input placeholder="请输入详细收货地址" />
              </Form.Item>
              <Form.Item label="备注" name="note">
                <TextArea placeholder="选填，有什么要备注的？" rows={2} />
              </Form.Item>
            </Form>
          </Card>

          <Card title="商品清单">
            {cartItems.map(item => {
              const unitPrice = parsePrice(item.price);
              const subtotal = (unitPrice * item.quantity).toFixed(2);
              return (
                <div key={item.id} className="order-item">
                  <div className="order-item-thumb">
                    {imgErrors[item.id] ? (
                      <span>{item.coverEmoji}</span>
                    ) : (
                      <img
                        src={item.coverImg}
                        alt={item.title}
                        onError={() => handleImgError(item.id)}
                        className="order-item-thumb-img"
                      />
                    )}
                  </div>
                  <div className="order-item-info">
                    <Text strong>{item.title}</Text>
                    <br />
                    <Text type="secondary" className="cart-author">{item.author}</Text>
                  </div>
                  <div className="order-item-qty">
                    <Text>{item.price}</Text>
                    <br />
                    <Text type="secondary">x{item.quantity}</Text>
                  </div>
                  <Text strong className="order-item-subtotal">¥{subtotal}</Text>
                </div>
              );
            })}
          </Card>
        </div>

        <div>
          <Card
            title={<div className="order-summary-head">订单摘要</div>}
            headStyle={{ padding: 0, border: 'none' }}
          >
            <Space direction="vertical" style={{ width: '100%' }} size="small">
              <div className="summary-row">
                <Text type="secondary">商品金额</Text>
                <Text strong>¥{cartTotal.toFixed(2)}</Text>
              </div>
              {discount > 0 && (
                <div className="summary-row">
                  <Text type="secondary">优惠</Text>
                  <Text className="summary-discount">-¥{discount.toFixed(2)}</Text>
                </div>
              )}
              <div className="summary-row">
                <Text type="secondary">运费</Text>
                {shipping === 0
                  ? <Tag color="green">免运费</Tag>
                  : <Text strong>¥{shipping.toFixed(2)}</Text>}
              </div>
              <Divider style={{ margin: '12px 0' }} />
              <div className="summary-row summary-row--center">
                <Text strong className="summary-total-label">应付总额</Text>
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
                <Card size="small" hoverable className="pay-option"
                  style={{
                    borderColor: payment === 'alipay' ? '#4a6cf7' : undefined,
                    background: payment === 'alipay' ? 'rgba(74,108,247,0.05)' : undefined,
                  }}
                  onClick={() => setPayment('alipay')}>
                  <div className="pay-option-row">
                    <Radio value="alipay" />
                    <AlipayCircleOutlined className="pay-icon-alipay" />
                    <div>
                      <Text strong>支付宝</Text><br />
                      <Text type="secondary" className="pay-hint">推荐使用</Text>
                    </div>
                  </div>
                </Card>
                <Card size="small" hoverable className="pay-option"
                  style={{
                    borderColor: payment === 'wechat' ? '#4a6cf7' : undefined,
                    background: payment === 'wechat' ? 'rgba(74,108,247,0.05)' : undefined,
                  }}
                  onClick={() => setPayment('wechat')}>
                  <div className="pay-option-row">
                    <Radio value="wechat" />
                    <WechatOutlined className="pay-icon-wechat" />
                    <div>
                      <Text strong>微信支付</Text><br />
                      <Text type="secondary" className="pay-hint">微信扫码支付</Text>
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
              loading={submitting}
              className="order-submit-btn"
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
