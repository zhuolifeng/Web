import { http } from './http';

export const userService = {
  login:    (username, password) => http.post('/users/login', { username, password }),
  register: (data)               => http.post('/users/register', data),
  me:       ()                   => http.get('/users/me'),
  updateProfile: (data)          => http.put('/users/profile', data),
};

export default userService;
