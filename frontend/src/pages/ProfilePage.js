import { useState } from 'react';
import {
  Form,
  Input,
  Button,
  Card,
  Typography,
  Avatar,
  Divider,
  Row,
  Col,
  message,
  Descriptions,
  Tag,
} from 'antd';
import {
  UserOutlined,
  MailOutlined,
  PhoneOutlined,
  HomeOutlined,
  SaveOutlined,
  EditOutlined,
} from '@ant-design/icons';

const { Title, Text, Paragraph } = Typography;

function ProfilePage() {
  const [editing, setEditing] = useState(false);
  const [form] = Form.useForm();

  /* Hard-coded user data */
  const [userInfo, setUserInfo] = useState({
    nickname: '猪猪侠',
    email: 'zhuzhu@bookstore.com',
    phone: '138-0000-1234',
    address: '上海市浦东新区张江高科技园区',
    bio: '爱读书，爱生活。希望在书海中找到人生的方向。',
  });

  const handleSave = (values) => {
    setUserInfo(values);
    setEditing(false);
    message.success('个人信息保存成功！');
  };

  const handleEdit = () => {
    form.setFieldsValue(userInfo);
    setEditing(true);
  };

  return (
    <>
      <Title level={3} style={{ marginBottom: 24 }}>
        <UserOutlined /> 个人信息
      </Title>

      <Row gutter={24}>
        {/* Left: Avatar Card */}
        <Col xs={24} md={8}>
          <Card style={{ textAlign: 'center' }}>
            <Avatar
              size={100}
              icon={<UserOutlined />}
              style={{
                backgroundColor: '#4a6cf7',
                marginBottom: 16,
                fontSize: 48,
              }}
            />
            <Title level={4} style={{ marginBottom: 4 }}>{userInfo.nickname}</Title>
            <Text type="secondary">{userInfo.email}</Text>
            <Divider />
            <div style={{ textAlign: 'left' }}>
              <Paragraph>
                <Text type="secondary">个人简介</Text>
                <br />
                {userInfo.bio}
              </Paragraph>
            </div>
            <Divider />
            <div style={{ display: 'flex', justifyContent: 'space-around' }}>
              <div>
                <Text strong style={{ fontSize: 20, color: '#4a6cf7' }}>12</Text>
                <br />
                <Text type="secondary" style={{ fontSize: 12 }}>订单数</Text>
              </div>
              <div>
                <Text strong style={{ fontSize: 20, color: '#4a6cf7' }}>5</Text>
                <br />
                <Text type="secondary" style={{ fontSize: 12 }}>收藏数</Text>
              </div>
              <div>
                <Text strong style={{ fontSize: 20, color: '#4a6cf7' }}>3</Text>
                <br />
                <Text type="secondary" style={{ fontSize: 12 }}>优惠券</Text>
              </div>
            </div>
          </Card>

          <Card style={{ marginTop: 16 }} title="账户状态">
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
              <Text>会员等级</Text>
              <Tag color="gold">黄金会员</Tag>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 8 }}>
              <Text>账户状态</Text>
              <Tag color="green">正常</Tag>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between' }}>
              <Text>注册时间</Text>
              <Text type="secondary">2025-01-15</Text>
            </div>
          </Card>
        </Col>

        {/* Right: Info / Edit Form */}
        <Col xs={24} md={16}>
          <Card
            title="基本信息"
            extra={
              !editing && (
                <Button icon={<EditOutlined />} onClick={handleEdit}>
                  编辑资料
                </Button>
              )
            }
          >
            {editing ? (
              <Form
                form={form}
                layout="vertical"
                onFinish={handleSave}
                initialValues={userInfo}
              >
                <Row gutter={16}>
                  <Col span={12}>
                    <Form.Item
                      label="昵称"
                      name="nickname"
                      rules={[{ required: true, message: '请输入昵称' }]}
                    >
                      <Input prefix={<UserOutlined />} placeholder="请输入昵称" />
                    </Form.Item>
                  </Col>
                  <Col span={12}>
                    <Form.Item
                      label="邮箱"
                      name="email"
                      rules={[
                        { required: true, message: '请输入邮箱' },
                        { type: 'email', message: '请输入有效邮箱' },
                      ]}
                    >
                      <Input prefix={<MailOutlined />} placeholder="请输入邮箱" />
                    </Form.Item>
                  </Col>
                  <Col span={12}>
                    <Form.Item
                      label="手机号"
                      name="phone"
                      rules={[{ required: true, message: '请输入手机号' }]}
                    >
                      <Input prefix={<PhoneOutlined />} placeholder="请输入手机号" />
                    </Form.Item>
                  </Col>
                  <Col span={12}>
                    <Form.Item
                      label="地址"
                      name="address"
                      rules={[{ required: true, message: '请输入地址' }]}
                    >
                      <Input prefix={<HomeOutlined />} placeholder="请输入地址" />
                    </Form.Item>
                  </Col>
                  <Col span={24}>
                    <Form.Item label="个人简介" name="bio">
                      <Input.TextArea rows={3} placeholder="介绍一下自己吧" />
                    </Form.Item>
                  </Col>
                </Row>
                <div style={{ display: 'flex', gap: 12, justifyContent: 'flex-end' }}>
                  <Button onClick={() => setEditing(false)}>取消</Button>
                  <Button type="primary" htmlType="submit" icon={<SaveOutlined />}>
                    保存
                  </Button>
                </div>
              </Form>
            ) : (
              <Descriptions column={2} bordered>
                <Descriptions.Item label={<><UserOutlined /> 昵称</>}>
                  {userInfo.nickname}
                </Descriptions.Item>
                <Descriptions.Item label={<><MailOutlined /> 邮箱</>}>
                  {userInfo.email}
                </Descriptions.Item>
                <Descriptions.Item label={<><PhoneOutlined /> 手机号</>}>
                  {userInfo.phone}
                </Descriptions.Item>
                <Descriptions.Item label={<><HomeOutlined /> 地址</>}>
                  {userInfo.address}
                </Descriptions.Item>
                <Descriptions.Item label="个人简介" span={2}>
                  {userInfo.bio}
                </Descriptions.Item>
              </Descriptions>
            )}
          </Card>
        </Col>
      </Row>
    </>
  );
}

export default ProfilePage;
