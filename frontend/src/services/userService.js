import { http } from './http';

/**
 * 用户服务：封装用户相关 API 调用。
 */
export const userService = {
  login:         (username, password) => http.post('/users/login', { username, password }),
  register:      (data)               => http.post('/users/register', data),
  me:            ()                   => http.get('/users/me'),
  updateProfile: (data)               => http.put('/users/profile', data),
};

export default userService;
