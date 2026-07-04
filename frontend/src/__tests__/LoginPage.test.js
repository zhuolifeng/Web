import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { MemoryRouter } from 'react-router-dom';
import LoginPage from '../pages/LoginPage';
import { AuthProvider } from '../context/AuthContext';
import { CartProvider } from '../context/CartContext';

/**
 * LoginPage 组件测试。
 * 验证表单校验：用户名和密码为空时显示提示信息。
 */

jest.mock('../services/userService', () => ({
  userService: {
    login: jest.fn(),
    register: jest.fn(),
    me: jest.fn(),
  },
}));

jest.mock('../services/cartService', () => ({
  cartService: {
    list: jest.fn().mockResolvedValue({ data: [] }),
    add: jest.fn(),
    remove: jest.fn(),
    update: jest.fn(),
    clear: jest.fn(),
  },
}));

const renderLoginPage = () => {
  return render(
    <MemoryRouter initialEntries={['/login']}>
      <AuthProvider>
        <CartProvider>
          <LoginPage />
        </CartProvider>
      </AuthProvider>
    </MemoryRouter>
  );
};

describe('LoginPage', () => {
  test('渲染登录表单', () => {
    renderLoginPage();

    expect(screen.getByPlaceholderText('请输入用户名')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('请输入密码')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /登录/i })).toBeInTheDocument();
  });

  test('显示注册链接', () => {
    renderLoginPage();

    expect(screen.getByText('立即注册')).toBeInTheDocument();
  });

  test('未输入用户名时点击登录显示校验提示', async () => {
    renderLoginPage();

    const loginBtn = screen.getByRole('button', { name: /登录/i });
    fireEvent.click(loginBtn);

    await waitFor(() => {
      expect(screen.getByText('请输入用户名')).toBeInTheDocument();
    });
  });

  test('未输入密码时点击登录显示校验提示', async () => {
    renderLoginPage();

    const loginBtn = screen.getByRole('button', { name: /登录/i });
    fireEvent.click(loginBtn);

    await waitFor(() => {
      expect(screen.getByText('请输入密码')).toBeInTheDocument();
    });
  });
});
