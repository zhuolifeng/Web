/**
 * Fetch-based HTTP client. Replaces the previous axios `api/client.js`.
 *
 * Every request:
 *   - prefixes the API base URL `/api/v1`
 *   - sends/receives JSON
 *   - attaches `Authorization: Bearer <token>` from localStorage when present
 *   - throws an Error (with .code/.message from backend) on non-2xx
 *   - on 401, clears local auth and redirects to /login
 */

const BASE_URL = 'http://localhost:8080/api/v1';

function handleUnauthorized() {
  localStorage.removeItem('token');
  localStorage.removeItem('user');
  if (window.location.pathname !== '/login') {
    window.location.href = '/login';
  }
}

async function request(path, { method = 'GET', body, auth = true } = {}) {
  const headers = { 'Content-Type': 'application/json' };
  if (auth) {
    const token = localStorage.getItem('token');
    if (token) headers.Authorization = `Bearer ${token}`;
  }

  let res;
  try {
    res = await fetch(BASE_URL + path, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch (networkErr) {
    const err = new Error('网络错误，请检查后端服务是否启动');
    err.code = 0;
    throw err;
  }

  let payload = null;
  const text = await res.text();
  if (text) {
    try { payload = JSON.parse(text); } catch (_) { payload = { message: text }; }
  }

  if (res.status === 401) {
    // 403 表示被禁用或无权限，不自动跳转登录页
    handleUnauthorized();
    const err = new Error(payload?.message || '未登录或登录已过期');
    err.code = 401;
    throw err;
  }

  if (res.status === 403) {
    const err = new Error(payload?.message || '无权限访问该资源');
    err.code = 403;
    throw err;
  }

  if (!res.ok) {
    const err = new Error(payload?.message || `请求失败 (HTTP ${res.status})`);
    err.code = payload?.code || res.status;
    err.payload = payload;
    throw err;
  }

  return payload;
}

export const http = {
  get:  (path)        => request(path, { method: 'GET' }),
  post: (path, body)  => request(path, { method: 'POST', body }),
  put:  (path, body)  => request(path, { method: 'PUT',  body }),
  del:  (path)        => request(path, { method: 'DELETE' }),
};

export default http;
