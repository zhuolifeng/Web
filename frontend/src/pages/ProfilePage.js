import { useState, useEffect } from 'react';
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
import { useAuth } from '../context/AuthContext';
import { userService } from '../services/userService';

const { Title, Text, Paragraph } = Typography;

function ProfilePage() {
  const [editing, setEditing] = useState(false);
  const [form] = Form.useForm();
  const { user, fetchProfile } = useAuth();

  const [userInfo, setUserInfo] = useState({
    nickname: '',
    email: '',
    phone: '',
    address: '',
    bio: '',
  });

  useEffect(() => {
    fetchProfile();
  }, [fetchProfile]);

  useEffect(() => {
    if (user) {
      setUserInfo((prev) => ({
        ...prev,
        nickname: user.nickname || '',
        email: user.email || '',
        phone: user.phone || '',
      }));
    }
  }, [user]);

  const handleSave = async (values) => {
    try {
      await userService.updateProfile({
        nickname: values.nickname,
        email: values.email,
        phone: values.phone,
      });
      setUserInfo(values);
      setEditing(false);
      message.success('个人信息保存成功！');
      fetchProfile();
    } catch (err) {
      message.error(err.message || '保存失败');
    }
  };

  const handleEdit = () => {
    form.setFieldsValue(userInfo);
    setEditing(true);
  };

  return (
    <>
      <Title level={3} className="section-title">
        <UserOutlined /> 个人信息
      </Title>

      <Row gutter={24}>
        <Col xs={24} md={8}>
          <Card className="profile-card-center">
            <Avatar
              size={100}
              icon={<UserOutlined />}
              className="profile-avatar"
            />
            <Title level={4} style={{ marginBottom: 4 }}>
              {userInfo.nickname || user?.username}
            </Title>
            <Text type="secondary">{userInfo.email}</Text>
            <Divider />
            <div className="profile-bio">
              <Paragraph>
                <Text type="secondary">个人简介</Text>
                <br />
                {userInfo.bio || '这个人很懒，什么都没写~'}
              </Paragraph>
            </div>
            <Divider />
            <div className="profile-stats">
              <div>
                <Text strong className="profile-stat-num">0</Text>
                <br />
                <Text type="secondary" className="profile-stat-label">订单数</Text>
              </div>
              <div>
                <Text strong className="profile-stat-num">0</Text>
                <br />
                <Text type="secondary" className="profile-stat-label">收藏数</Text>
              </div>
              <div>
                <Text strong className="profile-stat-num">0</Text>
                <br />
                <Text type="secondary" className="profile-stat-label">优惠券</Text>
              </div>
            </div>
          </Card>

          <Card className="account-card" title="账户状态">
            <div className="account-row">
              <Text>用户角色</Text>
              <Tag color={user?.role === 'ADMIN' ? 'red' : 'blue'}>
                {user?.role === 'ADMIN' ? '管理员' : '普通用户'}
              </Tag>
            </div>
            <div className="account-row">
              <Text>账户状态</Text>
              <Tag color="green">正常</Tag>
            </div>
            <div className="account-row">
              <Text>用户名</Text>
              <Text type="secondary">{user?.username}</Text>
            </div>
          </Card>
        </Col>

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
                        { type: 'email', message: '请输入有效邮箱' },
                      ]}
                    >
                      <Input prefix={<MailOutlined />} placeholder="请输入邮箱" />
                    </Form.Item>
                  </Col>
                  <Col span={12}>
                    <Form.Item label="手机号" name="phone">
                      <Input prefix={<PhoneOutlined />} placeholder="请输入手机号" />
                    </Form.Item>
                  </Col>
                  <Col span={12}>
                    <Form.Item label="地址" name="address">
                      <Input prefix={<HomeOutlined />} placeholder="请输入地址" />
                    </Form.Item>
                  </Col>
                  <Col span={24}>
                    <Form.Item label="个人简介" name="bio">
                      <Input.TextArea rows={3} placeholder="介绍一下自己吧" />
                    </Form.Item>
                  </Col>
                </Row>
                <div className="profile-form-actions">
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
                  {userInfo.bio || '这个人很懒，什么都没写~'}
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
