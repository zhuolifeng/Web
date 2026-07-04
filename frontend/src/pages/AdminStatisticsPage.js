import { useState } from 'react';
import {
  Card, Typography, DatePicker, Button, Space, Table, Spin, Tabs, message,
} from 'antd';
import { BarChartOutlined, SearchOutlined } from '@ant-design/icons';
import { adminService } from '../services/adminService';

const { Title } = Typography;
const { RangePicker } = DatePicker;

/**
 * 管理员 - 数据统计页面。
 * 包含两个Tab：书籍销量排行 和 用户消费排行。
 * 支持按日期范围过滤，以表格形式展示排行数据。
 */
function AdminStatisticsPage() {
  const [dateRange, setDateRange] = useState(null);
  const [bookSales, setBookSales] = useState([]);
  const [userSpending, setUserSpending] = useState([]);
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState('bookSales');

  const getParams = () => {
    const params = {};
    if (dateRange && dateRange[0]) params.startDate = dateRange[0].format('YYYY-MM-DD');
    if (dateRange && dateRange[1]) params.endDate = dateRange[1].format('YYYY-MM-DD');
    return params;
  };

  const fetchBookSales = async () => {
    setLoading(true);
    try {
      const res = await adminService.bookSalesRanking(getParams());
      setBookSales(res.data || []);
    } catch (err) {
      message.error(err.message || '获取销量数据失败');
    } finally {
      setLoading(false);
    }
  };

  const fetchUserSpending = async () => {
    setLoading(true);
    try {
      const res = await adminService.userSpendingRanking(getParams());
      setUserSpending(res.data || []);
    } catch (err) {
      message.error(err.message || '获取消费数据失败');
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = () => {
    if (activeTab === 'bookSales') fetchBookSales();
    else fetchUserSpending();
  };

  const handleTabChange = (key) => {
    setActiveTab(key);
    if (key === 'bookSales') fetchBookSales();
    else fetchUserSpending();
  };

  const bookSalesColumns = [
    { title: '排名', key: 'rank', render: (_, __, idx) => idx + 1, width: 60 },
    { title: '书名', dataIndex: 'title', key: 'title' },
    { title: '销量（本）', dataIndex: 'quantity', key: 'quantity', sorter: (a, b) => a.quantity - b.quantity },
    {
      title: '销售额（元）', dataIndex: 'amount', key: 'amount',
      render: (v) => `¥${Number(v).toFixed(2)}`,
      sorter: (a, b) => Number(a.amount) - Number(b.amount),
    },
  ];

  const userSpendingColumns = [
    { title: '排名', key: 'rank', render: (_, __, idx) => idx + 1, width: 60 },
    { title: '用户名', dataIndex: 'username', key: 'username' },
    {
      title: '消费总额（元）', dataIndex: 'totalAmount', key: 'totalAmount',
      render: (v) => `¥${Number(v).toFixed(2)}`,
      sorter: (a, b) => Number(a.totalAmount) - Number(b.totalAmount),
    },
  ];

  const tabItems = [
    {
      key: 'bookSales',
      label: '📚 书籍销量排行',
      children: (
        <Spin spinning={loading}>
          {bookSales.length > 0 && (
            <div style={{ marginBottom: 16 }}>
              <SalesBarChart data={bookSales} />
            </div>
          )}
          <Table
            columns={bookSalesColumns}
            dataSource={bookSales}
            rowKey="title"
            pagination={false}
          />
        </Spin>
      ),
    },
    {
      key: 'userSpending',
      label: '👤 用户消费排行',
      children: (
        <Spin spinning={loading}>
          {userSpending.length > 0 && (
            <div style={{ marginBottom: 16 }}>
              <SpendingBarChart data={userSpending} />
            </div>
          )}
          <Table
            columns={userSpendingColumns}
            dataSource={userSpending}
            rowKey="username"
            pagination={false}
          />
        </Spin>
      ),
    },
  ];

  return (
    <>
      <Title level={3} className="section-title">
        <BarChartOutlined /> 数据统计
      </Title>

      <Card style={{ marginBottom: 16 }} size="small">
        <Space>
          <RangePicker
            value={dateRange}
            onChange={setDateRange}
            placeholder={['开始日期', '结束日期']}
          />
          <Button type="primary" icon={<SearchOutlined />} onClick={handleSearch}>查询</Button>
        </Space>
      </Card>

      <Card>
        <Tabs activeKey={activeTab} onChange={handleTabChange} items={tabItems} />
      </Card>
    </>
  );
}

/** 简易柱状图：使用纯 CSS/div 渲染水平条形图 */
function SalesBarChart({ data }) {
  const maxQty = Math.max(...data.map(d => d.quantity), 1);
  return (
    <div style={{ padding: '8px 0' }}>
      {data.slice(0, 10).map((item, idx) => (
        <div key={item.title} style={{ display: 'flex', alignItems: 'center', marginBottom: 6 }}>
          <span style={{ width: 140, fontSize: 13, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {idx + 1}. {item.title}
          </span>
          <div style={{ flex: 1, marginLeft: 8 }}>
            <div
              style={{
                width: `${(item.quantity / maxQty) * 100}%`,
                minWidth: 4,
                height: 20,
                background: `hsl(${210 - idx * 15}, 70%, 55%)`,
                borderRadius: 4,
                display: 'flex',
                alignItems: 'center',
                paddingLeft: 6,
                color: '#fff',
                fontSize: 12,
              }}
            >
              {item.quantity} 本
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

function SpendingBarChart({ data }) {
  const maxAmount = Math.max(...data.map(d => Number(d.totalAmount)), 1);
  return (
    <div style={{ padding: '8px 0' }}>
      {data.slice(0, 10).map((item, idx) => (
        <div key={item.username} style={{ display: 'flex', alignItems: 'center', marginBottom: 6 }}>
          <span style={{ width: 100, fontSize: 13, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {idx + 1}. {item.username}
          </span>
          <div style={{ flex: 1, marginLeft: 8 }}>
            <div
              style={{
                width: `${(Number(item.totalAmount) / maxAmount) * 100}%`,
                minWidth: 4,
                height: 20,
                background: `hsl(${150 - idx * 12}, 60%, 45%)`,
                borderRadius: 4,
                display: 'flex',
                alignItems: 'center',
                paddingLeft: 6,
                color: '#fff',
                fontSize: 12,
              }}
            >
              ¥{Number(item.totalAmount).toFixed(2)}
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

export default AdminStatisticsPage;
