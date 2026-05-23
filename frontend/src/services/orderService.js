import { http } from './http';

export const orderService = {
  create:   (orderData) => http.post('/orders', orderData),
  list:     ()          => http.get('/orders'),
  getById:  (id)        => http.get(`/orders/${id}`),
};

export default orderService;
