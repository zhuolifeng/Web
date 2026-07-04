import React from 'react';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import '@testing-library/jest-dom';
import { MemoryRouter } from 'react-router-dom';
import RegisterPage from '../pages/RegisterPage';
import { AuthProvider } from '../context/AuthContext';
import { CartProvider } from '../context/CartContext';

/**
 * RegisterPage 组件测试。
 * 验证注册表单校验：用户名、密码、确认密码、邮箱格式。
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

const renderRegisterPage = () => {
  return render(
    <MemoryRouter initialEntries={['/register']}>
      <AuthProvider>
        <CartProvider>
          <RegisterPage />
        </CartProvider>
      </AuthProvider>
    </MemoryRouter>
  );
};

describe('RegisterPage', () => {
  test('渲染注册表单', () => {
    renderRegisterPage();

    expect(screen.getByPlaceholderText('请输入用户名')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('请输入密码')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('请再次输入密码')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('请输入邮箱')).toBeInTheDocument();
  });

  test('显示登录链接', () => {
    renderRegisterPage();

    expect(screen.getByText('立即登录')).toBeInTheDocument();
  });

  test('未输入用户名时显示校验提示', async () => {
    renderRegisterPage();

    // 直接提交表单触发 Ant Design 校验
    const form = document.querySelector('form');
    fireEvent.submit(form);

    await waitFor(() => {
      expect(screen.getByText('请输入用户名')).toBeInTheDocument();
    });
  });

  test('未输入密码时显示校验提示', async () => {
    renderRegisterPage();

    const form = document.querySelector('form');
    fireEvent.submit(form);

    await waitFor(() => {
      expect(screen.getByText('请输入密码')).toBeInTheDocument();
    });
  });

  test('未输入邮箱时显示校验提示', async () => {
    renderRegisterPage();

    const form = document.querySelector('form');
    fireEvent.submit(form);

    await waitFor(() => {
      expect(screen.getByText('请输入邮箱')).toBeInTheDocument();
    });
  });

  test('两次密码不一致时显示校验提示', async () => {
    renderRegisterPage();

    const usernameInput = screen.getByPlaceholderText('请输入用户名');
    const passwordInput = screen.getByPlaceholderText('请输入密码');
    const confirmInput = screen.getByPlaceholderText('请再次输入密码');

    fireEvent.change(usernameInput, { target: { value: 'testuser' } });
    fireEvent.change(passwordInput, { target: { value: 'password123' } });
    fireEvent.change(confirmInput, { target: { value: 'different456' } });

    // 触发 blur 让 Ant Design 校验
    fireEvent.blur(confirmInput);

    await waitFor(() => {
      expect(screen.getByText('两次密码不一致')).toBeInTheDocument();
    });
  });
});
