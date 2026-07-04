import { Form, Input, Button, Checkbox, Card, Typography, message } from 'antd';
import { UserOutlined, LockOutlined, LoginOutlined } from '@ant-design/icons';
import { Link, useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const { Title, Paragraph, Text } = Typography;

/**
 * 登录页面。
 * 校验用户名和密码是否输入，对被禁用用户给出明确提示。
 */
function LoginPage() {
  const { login, loading } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const onFinish = async (values) => {
    const result = await login(values.username, values.password);
    if (result.success) {
      message.success(`登录成功！欢迎回来，${values.username}`);
      const from = location.state?.from?.pathname || '/';
      navigate(from, { replace: true });
    } else {
      message.error(result.message);
    }
  };

  return (
    <div className="auth-page">
      <Card className="auth-card">
        <div className="auth-header">
          <div className="auth-logo">📚</div>
          <Title level={3} style={{ marginBottom: 8 }}>登录猪猪书城</Title>
          <Paragraph type="secondary">欢迎回来，请登录你的账号</Paragraph>
        </div>

        <Form
          name="login"
          onFinish={onFinish}
          autoComplete="off"
          size="large"
        >
          <Form.Item
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input prefix={<UserOutlined />} placeholder="请输入用户名" />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="请输入密码" />
          </Form.Item>

          <Form.Item>
            <div className="auth-row">
              <Form.Item name="remember" valuePropName="checked" noStyle>
                <Checkbox>记住我</Checkbox>
              </Form.Item>
              <Link to="#">忘记密码？</Link>
            </div>
          </Form.Item>

          <Form.Item>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              icon={<LoginOutlined />}
              block
            >
              登录
            </Button>
          </Form.Item>

          <div className="auth-footer">
            <Text type="secondary">还没有账号？</Text>
            <Link to="/register">立即注册</Link>
          </div>
        </Form>
      </Card>
    </div>
  );
}

export default LoginPage;
