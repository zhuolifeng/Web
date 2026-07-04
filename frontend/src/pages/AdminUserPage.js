import { useEffect, useState } from 'react';
import { Table, Tag, Switch, Typography, message, Card } from 'antd';
import { TeamOutlined } from '@ant-design/icons';
import { adminService } from '../services/adminService';

const { Title } = Typography;

/**
 * 管理员 - 用户管理页面：展示所有用户，支持禁用/解禁操作。
 */
function AdminUserPage() {
  const [users, setUsers] = useState([]);
  const [loading, setLoading] = useState(true);

  const fetchUsers = () => {
    setLoading(true);
    adminService.listUsers()
      .then((res) => setUsers(res.data || []))
      .catch((err) => message.error(err.message || '获取用户列表失败'))
      .finally(() => setLoading(false));
  };

  useEffect(() => { fetchUsers(); }, []);

  const handleToggle = async (userId, enabled) => {
    try {
      await adminService.toggleUserStatus(userId, enabled);
      message.success(enabled ? '已解禁用户' : '已禁用用户');
      fetchUsers();
    } catch (err) {
      message.error(err.message || '操作失败');
    }
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: '用户名', dataIndex: 'username', key: 'username' },
    { title: '昵称', dataIndex: 'nickname', key: 'nickname' },
    { title: '邮箱', dataIndex: 'email', key: 'email' },
    { title: '手机', dataIndex: 'phone', key: 'phone' },
    {
      title: '角色',
      dataIndex: 'role',
      key: 'role',
      render: (role) => (
        <Tag color={role === 'ADMIN' ? 'red' : 'blue'}>
          {role === 'ADMIN' ? '管理员' : '顾客'}
        </Tag>
      ),
    },
    {
      title: '状态',
      dataIndex: 'enabled',
      key: 'enabled',
      render: (enabled, record) => (
        <Switch
          checked={enabled}
          onChange={(checked) => handleToggle(record.id, checked)}
          checkedChildren="启用"
          unCheckedChildren="禁用"
          disabled={record.role === 'ADMIN'}
        />
      ),
    },
    {
      title: '注册时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (t) => formatDate(t),
    },
  ];

  return (
    <>
      <Title level={3} className="section-title">
        <TeamOutlined /> 用户管理
      </Title>
      <Card>
        <Table
          columns={columns}
          dataSource={users}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 10 }}
        />
      </Card>
    </>
  );
}

function formatDate(iso) {
  if (!iso) return '';
  const d = new Date(iso);
  if (isNaN(d.getTime())) return iso;
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`;
}

export default AdminUserPage;
