import { http } from './http';

/**
 * 书籍服务：封装所有书籍相关的 API 调用。
 * 普通用户可浏览和搜索，管理员可增删改。
 */
export const bookService = {
  list:     (keyword) => http.get('/books' + (keyword ? `?keyword=${encodeURIComponent(keyword)}` : '')),
  getById:  (id)      => http.get(`/book/${id}`),
  // 管理员操作
  create:   (data)    => http.post('/admin/books', data),
  update:   (id, data)=> http.put(`/admin/books/${id}`, data),
  remove:   (id)      => http.del(`/admin/books/${id}`),
};

export default bookService;
