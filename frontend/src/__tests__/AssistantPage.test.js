import React from 'react';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import '@testing-library/jest-dom';
import AssistantPage from '../pages/AssistantPage';

const health = { status: 'ok', mode: 'demo', modelConfigured: false, inventoryMode: 'demo' };
const response = (body, ok = true) => Promise.resolve({ ok, json: async () => body });

beforeEach(() => {
  global.fetch = jest.fn();
  global.fetch.mockImplementation((url) => url.endsWith('/health') ? response(health) :
    response({ code: 200, data: { answer: '演示库存100本，模拟竞品55.90元。', mode: 'demo',
      toolCalls: [{ id: 'call_1', name: 'check_inventory', arguments: '{"isbn":"12345"}', result: { ok: true, stock: 100 } }] } }));
});
afterEach(() => { jest.restoreAllMocks(); });

test('明确显示离线模式与模拟库存来源', async () => {
  render(<AssistantPage />);
  expect(await screen.findByText(/离线演示模式：规则模拟工具调用/)).toBeInTheDocument();
  expect(screen.getByText(/库存来源：演示数据/)).toBeInTheDocument();
});

test('发送问题并显示自然语言回复和工具名参数', async () => {
  jest.spyOn(console, 'info').mockImplementation(() => {});
  render(<AssistantPage />);
  await screen.findByText(/离线演示模式：规则模拟工具调用/);
  fireEvent.click(screen.getByRole('button', { name: '库存与比价' }));
  fireEvent.click(screen.getByRole('button', { name: '发送' }));
  expect(await screen.findByText('演示库存100本，模拟竞品55.90元。')).toBeInTheDocument();
  expect(screen.getByText('check_inventory')).toBeInTheDocument();
  const chat = global.fetch.mock.calls.find(([url]) => url.endsWith('/chat'));
  expect(JSON.parse(chat[1].body).messages[0].role).toBe('user');
  expect(console.info).toHaveBeenCalledWith('[Function Calling]', 'check_inventory', '{"isbn":"12345"}', { ok: true, stock: 100 });
});

test('模型API配置错误显示可读错误，允许继续提问', async () => {
  global.fetch.mockImplementation((url) => url.endsWith('/health') ? response(health) :
    response({ message: '服务端尚未设置 OPENAI_API_KEY' }, false));
  render(<AssistantPage />);
  await screen.findByText(/离线演示模式：规则模拟工具调用/);
  fireEvent.change(screen.getByRole('textbox', { name: '用户问题' }), { target: { value: 'ISBN为12345库存' } });
  fireEvent.click(screen.getByRole('button', { name: '发送' }));
  expect(await screen.findByText(/查询失败：服务端尚未设置 OPENAI_API_KEY/)).toBeInTheDocument();
  await waitFor(() => expect(screen.getByRole('textbox', { name: '用户问题' })).not.toBeDisabled());
});

test('新对话清除历史，中文输入法组合输入不触发发送', async () => {
  render(<AssistantPage />);
  await screen.findByText(/离线演示模式：规则模拟工具调用/);
  const input = screen.getByRole('textbox', { name: '用户问题' });
  fireEvent.change(input, { target: { value: 'ISBN为12345库存' } });
  fireEvent.keyDown(input, { key: 'Enter', code: 'Enter', keyCode: 229, isComposing: true });
  expect(global.fetch.mock.calls.filter(([url]) => url.endsWith('/chat'))).toHaveLength(0);
  fireEvent.click(screen.getByRole('button', { name: '新对话' }));
  expect(screen.getByText(/输入 ISBN，查询这本书/)).toBeInTheDocument();
});

test('StrictMode 清理首次健康检查时不显示连接失败', async () => {
  let probes = 0;
  global.fetch.mockImplementation((url, options) => {
    probes += 1;
    if (probes === 1) {
      return new Promise((resolve, reject) => {
        options.signal.addEventListener('abort', () => {
          const error = new Error('effect cleanup');
          error.name = 'AbortError';
          reject(error);
        });
      });
    }
    return response(health);
  });
  render(<React.StrictMode><AssistantPage /></React.StrictMode>);
  await screen.findByText(/离线演示模式：规则模拟工具调用/);
  expect(probes).toBe(2);
  expect(screen.queryByText(/助手服务尚未连接/)).not.toBeInTheDocument();
});

test('健康检查失败后聊天成功清除旧错误并更新运行模式', async () => {
  jest.spyOn(console, 'info').mockImplementation(() => {});
  global.fetch.mockImplementation((url) => url.endsWith('/health') ? Promise.reject(new Error('offline')) :
    response({ code: 200, data: { answer: '恢复连接后的回答', mode: 'live', inventoryMode: 'demo', toolCalls: [] } }));
  render(<AssistantPage />);
  await screen.findByText(/助手服务尚未连接/);
  fireEvent.change(screen.getByRole('textbox', { name: '用户问题' }), { target: { value: 'ISBN为12345库存' } });
  fireEvent.click(screen.getByRole('button', { name: '发送' }));
  await screen.findByText('恢复连接后的回答');
  expect(screen.queryByText(/助手服务尚未连接/)).not.toBeInTheDocument();
  expect(screen.getByText('真实模型 API 模式')).toBeInTheDocument();
  expect(screen.queryByText(/尚未配置服务端 API 密钥/)).not.toBeInTheDocument();
});

test('聊天成功后迟到的健康检查失败不能覆盖连接状态', async () => {
  let failHealth;
  global.fetch.mockImplementation((url) => url.endsWith('/health') ?
    new Promise((resolve, reject) => { failHealth = reject; }) :
    response({ code: 200, data: { answer: '服务已连接', mode: 'live', inventoryMode: 'demo', toolCalls: [] } }));
  render(<AssistantPage />);
  fireEvent.change(screen.getByRole('textbox', { name: '用户问题' }), { target: { value: 'ISBN为12345库存' } });
  fireEvent.click(screen.getByRole('button', { name: '发送' }));
  await screen.findByText('服务已连接');
  await act(async () => { failHealth(new Error('late failure')); });
  expect(screen.queryByText(/助手服务尚未连接/)).not.toBeInTheDocument();
  expect(screen.getByText('真实模型 API 模式')).toBeInTheDocument();
});
