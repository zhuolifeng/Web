import React from 'react';
import { render, screen, fireEvent } from '@testing-library/react';
import '@testing-library/jest-dom';
import GuidePage from '../pages/GuidePage';

const health = { mode: 'live', modelConfigured: true, chunkCount: 8, dimensions: 1045 };
const response = (data, ok = true) => Promise.resolve({ ok, json: async () => data });
const result = {
  answer: '模拟目录推荐微服务入门书；拆封后仅因不喜欢不支持退货。[P02-01]', mode: 'live', embedding: 'local-tfidf',
  trace: [
    { step: 1, thought: '先查相关书目。', action: 'search_book_catalog', arguments: { query: '微服务' }, observation: { items: [] } },
    { step: 2, thought: '再检索拆封政策。', action: 'query_store_policy', arguments: { query: '拆封退货' }, observation: { matches: [] } },
    { step: 3, thought: '整理有依据的回答。', action: 'finish', arguments: {}, observation: { answer: '完成' } },
  ], sources: [{ id: 'P02-01', section: '拆封政策', text: '拆封后仅因不喜欢不支持退货。', source: 'store_policy.md', score: 0.3 }],
};
beforeEach(() => {
  global.fetch = jest.fn((url) => url.endsWith('/health') ? response(health) : response({ code: 200, data: result }));
  jest.spyOn(console, 'info').mockImplementation(() => {});
});
afterEach(() => jest.restoreAllMocks());

test('展示真实模型和本地词项向量信息', async () => {
  render(<GuidePage />);
  expect(await screen.findByText('真实模型 ReAct Agent')).toBeInTheDocument();
  expect(screen.getByText(/8 个片段，1045 维/)).toBeInTheDocument();
});

test('复杂案例展示回答、行动理由、两个工具和政策来源', async () => {
  render(<GuidePage />);
  await screen.findByText('真实模型 ReAct Agent');
  fireEvent.click(screen.getByRole('button', { name: '复杂案例' }));
  fireEvent.click(screen.getByRole('button', { name: '发送导购问题' }));
  expect(await screen.findByText(result.answer)).toBeInTheDocument();
  expect(screen.getByText('search_book_catalog')).toBeInTheDocument();
  expect(screen.getByText('query_store_policy')).toBeInTheDocument();
  expect(screen.getByText(/先查相关书目/)).toBeInTheDocument();
  expect(screen.getByText('[P02-01] 拆封政策')).toBeInTheDocument();
  expect(console.info).toHaveBeenCalledTimes(3);
});

test('可以检查分块及向量元信息', async () => {
  global.fetch.mockImplementation((url) => url.endsWith('/health') ? response(health) :
    response({ chunkCount: 1, dimensions: 3, chunkSize: 280, overlap: 50, embedding: 'local-tfidf',
      chunks: [{ id: 'P02-01', section: '拆封政策', text: '模拟条款', vectorPreview: [0, 0.5, 0.8] }] }));
  render(<GuidePage />);
  await screen.findByText('真实模型 ReAct Agent');
  fireEvent.click(screen.getByRole('button', { name: '查看政策分块与向量信息' }));
  expect(await screen.findByText(/政策 RAG 索引：1 个片段 \/ 3 维/)).toBeInTheDocument();
  expect(screen.getByText('模拟条款')).toBeInTheDocument();
});

test('请求失败时展示错误，允许用户重新输入', async () => {
  global.fetch.mockImplementation((url) => url.endsWith('/health') ? response(health) : response({ message: '模型接口限流' }, false));
  render(<GuidePage />);
  await screen.findByText('真实模型 ReAct Agent');
  fireEvent.change(screen.getByRole('textbox', { name: '导购问题' }), { target: { value: '推荐微服务书' } });
  fireEvent.click(screen.getByRole('button', { name: '发送导购问题' }));
  expect(await screen.findByText('导购请求失败：模型接口限流')).toBeInTheDocument();
  expect(screen.getByRole('textbox', { name: '导购问题' })).not.toBeDisabled();
});

test('StrictMode 清理请求不误报服务未连接', async () => {
  let probes = 0;
  global.fetch.mockImplementation((url, options) => {
    if (++probes === 1) return new Promise((resolve, reject) => options.signal.addEventListener('abort', () => reject(new Error('cancelled'))));
    return response(health);
  });
  render(<React.StrictMode><GuidePage /></React.StrictMode>);
  await screen.findByText('真实模型 ReAct Agent');
  expect(screen.queryByText(/导购服务未连接/)).not.toBeInTheDocument();
});
