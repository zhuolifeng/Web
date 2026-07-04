import { useState } from 'react';
import {
  Card, Typography, DatePicker, Button, Space, Table, Spin, Statistic, Row, Col, Empty, message,
} from 'antd';
import { BarChartOutlined, SearchOutlined, BookOutlined, DollarOutlined } from '@ant-design/icons';
import { orderService } from '../services/orderService';

const { Title } = Typography;
const { RangePicker } = DatePicker;

/**
 * 个人购书统计页面：展示指定时间范围内每种书的购买数量、总本数和总金额。
 */
function MyStatisticsPage() {
  const [dateRange, setDateRange] = useState(null);
  const [stats, setStats] = useState(null);
  const [loading, setLoading] = useState(false);

  const handleSearch = async () => {
    setLoading(true);
    try {
      const params = {};
      if (dateRange && dateRange[0]) params.startDate = dateRange[0].format('YYYY-MM-DD');
      if (dateRange && dateRange[1]) params.endDate = dateRange[1].format('YYYY-MM-DD');
      const res = await orderService.myStats(params);
      setStats(res.data);
    } catch (err) {
      message.error(err.message || '获取统计数据失败');
    } finally {
      setLoading(false);
    }
  };

  const columns = [
    { title: '书名', dataIndex: 'title', key: 'title' },
    { title: '购买数量（本）', dataIndex: 'count', key: 'count', sorter: (a, b) => a.count - b.count },
    {
      title: '消费金额（元）', dataIndex: 'amount', key: 'amount',
      render: (v) => `¥${Number(v).toFixed(2)}`,
      sorter: (a, b) => Number(a.amount) - Number(b.amount),
    },
  ];

  return (
    <>
      <Title level={3} className="section-title">
        <BarChartOutlined /> 我的购书统计
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

      <Spin spinning={loading}>
        {stats ? (
          <>
            {/* 汇总统计卡片 */}
            <Row gutter={16} style={{ marginBottom: 16 }}>
              <Col span={12}>
                <Card>
                  <Statistic
                    title="购书总本数"
                    value={stats.totalCount || 0}
                    prefix={<BookOutlined />}
                    suffix="本"
                  />
                </Card>
              </Col>
              <Col span={12}>
                <Card>
                  <Statistic
                    title="购书总金额"
                    value={Number(stats.totalAmount || 0).toFixed(2)}
                    prefix={<DollarOutlined />}
                    precision={2}
                    suffix="元"
                  />
                </Card>
              </Col>
            </Row>

            {/* 明细表格 */}
            <Card>
              {stats.books && stats.books.length > 0 ? (
                <>
                  <div style={{ marginBottom: 16 }}>
                    <StatsBarChart data={stats.books} />
                  </div>
                  <Table
                    columns={columns}
                    dataSource={stats.books}
                    rowKey="title"
                    pagination={false}
                  />
                </>
              ) : (
                <Empty description="该时间段内没有购书记录" />
              )}
            </Card>
          </>
        ) : (
          <Card>
            <Empty description="请选择时间范围后点击查询" />
          </Card>
        )}
      </Spin>
    </>
  );
}

/** 简易柱状图 */
function StatsBarChart({ data }) {
  const maxCount = Math.max(...data.map(d => d.count), 1);
  return (
    <div style={{ padding: '8px 0' }}>
      {data.map((item, idx) => (
        <div key={item.title} style={{ display: 'flex', alignItems: 'center', marginBottom: 6 }}>
          <span style={{ width: 140, fontSize: 13, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {item.title}
          </span>
          <div style={{ flex: 1, marginLeft: 8 }}>
            <div
              style={{
                width: `${(item.count / maxCount) * 100}%`,
                minWidth: 4,
                height: 20,
                background: `hsl(${200 - idx * 20}, 65%, 50%)`,
                borderRadius: 4,
                display: 'flex',
                alignItems: 'center',
                paddingLeft: 6,
                color: '#fff',
                fontSize: 12,
              }}
            >
              {item.count} 本 / ¥{Number(item.amount).toFixed(2)}
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

export default MyStatisticsPage;
