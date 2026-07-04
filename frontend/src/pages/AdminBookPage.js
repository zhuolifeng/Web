import { useEffect, useState } from 'react';
import {
  Table, Button, Typography, message, Card, Modal, Form, Input, InputNumber, Space, Popconfirm,
} from 'antd';
import { BookOutlined, PlusOutlined, EditOutlined, DeleteOutlined, SearchOutlined } from '@ant-design/icons';
import { bookService } from '../services/bookService';

const { Title } = Typography;
const { Search } = Input;

/**
 * 管理员 - 书籍管理页面：CRUD 操作，包括添加、编辑、删除书籍。
 */
function AdminBookPage() {
  const [books, setBooks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [modalOpen, setModalOpen] = useState(false);
  const [editingBook, setEditingBook] = useState(null);
  const [keyword, setKeyword] = useState('');
  const [form] = Form.useForm();

  const fetchBooks = (kw) => {
    setLoading(true);
    bookService.list(kw || undefined)
      .then((res) => setBooks(res.data || []))
      .catch((err) => message.error(err.message || '获取书籍失败'))
      .finally(() => setLoading(false));
  };

  const handleSearch = (value) => {
    setKeyword(value);
    fetchBooks(value);
  };

  useEffect(() => { fetchBooks(); }, []);

  const handleAdd = () => {
    setEditingBook(null);
    form.resetFields();
    setModalOpen(true);
  };

  const handleEdit = (record) => {
    setEditingBook(record);
    form.setFieldsValue(record);
    setModalOpen(true);
  };

  const handleDelete = async (id) => {
    try {
      await bookService.remove(id);
      message.success('删除成功');
      fetchBooks(keyword);
    } catch (err) {
      message.error(err.message || '删除失败');
    }
  };

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields();
      if (editingBook) {
        await bookService.update(editingBook.id, values);
        message.success('更新成功');
      } else {
        await bookService.create(values);
        message.success('添加成功');
      }
      setModalOpen(false);
      fetchBooks(keyword);
    } catch (err) {
      if (err.message) message.error(err.message);
    }
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 50 },
    {
      title: '封面', dataIndex: 'coverImg', key: 'coverImg', width: 60,
      render: (img, record) => img
        ? <img src={img} alt="" style={{ width: 40, height: 56, objectFit: 'cover', borderRadius: 4 }} />
        : <span style={{ fontSize: 24 }}>{record.coverEmoji || '📖'}</span>,
    },
    { title: '书名', dataIndex: 'title', key: 'title', ellipsis: true },
    { title: '作者', dataIndex: 'author', key: 'author', ellipsis: true },
    { title: 'ISBN', dataIndex: 'isbn', key: 'isbn', ellipsis: true },
    { title: '价格', dataIndex: 'price', key: 'price', width: 80 },
    { title: '库存', dataIndex: 'stock', key: 'stock', width: 60 },
    { title: '分类', dataIndex: 'category', key: 'category', width: 80 },
    {
      title: '操作', key: 'action', width: 140,
      render: (_, record) => (
        <Space>
          <Button size="small" icon={<EditOutlined />} onClick={() => handleEdit(record)}>编辑</Button>
          <Popconfirm title="确认删除?" onConfirm={() => handleDelete(record.id)}>
            <Button size="small" danger icon={<DeleteOutlined />}>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <>
      <Title level={3} className="section-title">
        <BookOutlined /> 书籍管理
      </Title>
      <Card>
        <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 16, gap: 16 }}>
          <Search
            placeholder="搜索书名、作者或分类..."
            allowClear
            enterButton={<><SearchOutlined /> 搜索</>}
            value={keyword}
            onChange={(e) => setKeyword(e.target.value)}
            onSearch={handleSearch}
            style={{ maxWidth: 400 }}
          />
          <Button type="primary" icon={<PlusOutlined />} onClick={handleAdd}>
            添加书籍
          </Button>
        </div>
        <Table
          columns={columns}
          dataSource={books}
          rowKey="id"
          loading={loading}
          pagination={{ pageSize: 10 }}
          scroll={{ x: 800 }}
        />
      </Card>

      <Modal
        title={editingBook ? '编辑书籍' : '添加书籍'}
        open={modalOpen}
        onOk={handleSubmit}
        onCancel={() => setModalOpen(false)}
        width={720}
        okText="保存"
        cancelText="取消"
      >
        <Form form={form} layout="vertical">
          <Form.Item name="title" label="书名" rules={[{ required: true, message: '请输入书名' }]}>
            <Input />
          </Form.Item>
          <Space style={{ display: 'flex' }} align="start">
            <Form.Item name="author" label="作者" rules={[{ required: true, message: '请输入作者' }]} style={{ flex: 1 }}>
              <Input />
            </Form.Item>
            <Form.Item name="isbn" label="ISBN" style={{ flex: 1 }}>
              <Input />
            </Form.Item>
          </Space>
          <Space style={{ display: 'flex' }} align="start">
            <Form.Item name="price" label="价格" rules={[{ required: true, message: '请输入价格' }]} style={{ flex: 1 }}>
              <Input placeholder="如 ¥49.00" />
            </Form.Item>
            <Form.Item name="originalPrice" label="原价" style={{ flex: 1 }}>
              <Input placeholder="如 ¥68.00" />
            </Form.Item>
            <Form.Item name="stock" label="库存" style={{ flex: 1 }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
          </Space>
          <Space style={{ display: 'flex' }} align="start">
            <Form.Item name="category" label="分类" style={{ flex: 1 }}>
              <Input />
            </Form.Item>
            <Form.Item name="publisher" label="出版社" style={{ flex: 1 }}>
              <Input />
            </Form.Item>
          </Space>
          <Space style={{ display: 'flex' }} align="start">
            <Form.Item name="publishDate" label="出版日期" style={{ flex: 1 }}>
              <Input />
            </Form.Item>
            <Form.Item name="pages" label="页数" style={{ flex: 1 }}>
              <Input />
            </Form.Item>
            <Form.Item name="binding" label="装帧" style={{ flex: 1 }}>
              <Input />
            </Form.Item>
          </Space>
          <Form.Item name="coverImg" label="封面图片路径">
            <Input placeholder="如 /images/xxx.jpg" />
          </Form.Item>
          <Form.Item name="description" label="简介">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="intro" label="详细介绍">
            <Input.TextArea rows={3} />
          </Form.Item>
          <Form.Item name="authorBio" label="作者简介">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </>
  );
}

export default AdminBookPage;
