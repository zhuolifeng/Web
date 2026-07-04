import { http } from './http';

/**
 * 订单服务：封装订单相关 API 调用。
 * 支持按日期范围和书名过滤。
 */
export const orderService = {
  create:   (orderData)  => http.post('/orders', orderData),
  list:     (params = {}) => {
    const query = buildQuery(params);
    return http.get('/orders' + query);
  },
  getById:  (id)         => http.get(`/orders/${id}`),
  // 个人购书统计
  myStats:  (params = {}) => {
    const query = buildQuery(params);
    return http.get('/orders/my-stats' + query);
  },
};

/** 构建 URL 查询字符串 */
function buildQuery(params) {
  const parts = [];
  if (params.startDate) parts.push(`startDate=${params.startDate}`);
  if (params.endDate)   parts.push(`endDate=${params.endDate}`);
  if (params.bookTitle) parts.push(`bookTitle=${encodeURIComponent(params.bookTitle)}`);
  return parts.length ? '?' + parts.join('&') : '';
}

export default orderService;
