import { http } from './http';

/**
 * 管理员服务：封装管理员专用的 API 调用。
 * 包括用户管理、全局订单查看、统计分析。
 */
export const adminService = {
  // 用户管理
  listUsers:       ()              => http.get('/admin/users'),
  toggleUserStatus: (id, enabled)  => http.put(`/admin/users/${id}/status`, { enabled }),

  // 全部订单（支持过滤）
  listAllOrders:   (params = {})   => {
    const query = buildQuery(params);
    return http.get('/admin/orders' + query);
  },

  // 统计
  bookSalesRanking:   (params = {}) => {
    const query = buildQuery(params);
    return http.get('/admin/statistics/book-sales' + query);
  },
  userSpendingRanking: (params = {}) => {
    const query = buildQuery(params);
    return http.get('/admin/statistics/user-spending' + query);
  },
};

function buildQuery(params) {
  const parts = [];
  if (params.startDate) parts.push(`startDate=${params.startDate}`);
  if (params.endDate)   parts.push(`endDate=${params.endDate}`);
  if (params.bookTitle) parts.push(`bookTitle=${encodeURIComponent(params.bookTitle)}`);
  return parts.length ? '?' + parts.join('&') : '';
}

export default adminService;
