import { http } from './http';

export const bookService = {
  list:     ()   => http.get('/books'),
  getById:  (id) => http.get(`/book/${id}`),
};

export default bookService;
