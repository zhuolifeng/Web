import { http } from './http';

export const cartService = {
  list:      ()                  => http.get('/cart'),
  add:       (bookId, quantity)  => http.post('/cart', { bookId, quantity }),
  update:    (id, quantity)      => http.put(`/cart/${id}`, { quantity }),
  remove:    (id)                => http.del(`/cart/${id}`),
  clear:     ()                  => http.del('/cart'),
};

export default cartService;
