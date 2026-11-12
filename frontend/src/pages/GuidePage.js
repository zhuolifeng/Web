import { useEffect, useRef, useState } from 'react';
import { Alert, Button, Input, Spin, Tag, Typography } from 'antd';
import { CompassOutlined, SendOutlined } from '@ant-design/icons';
import './GuidePage.css';

const BASE = (process.env.REACT_APP_GUIDE_URL || 'http://localhost:8002').replace(/\/$/, '');
const EXAMPLES = [
  ['复杂案例', '我想买一本关于微服务的书，另外如果我买了不喜欢，拆了塑封还能退吗？'],
  ['只问政策', '银卡会员有哪些优惠？积分怎么抵扣？'],
  ['只查图书', '我刚开始学习后端，推荐一本微服务入门书。'],
  ['质量售后', '图书拆封后发现缺页，还能退换吗？'],
];

export default function GuidePage() {
  const [question, setQuestion] = useState('');
  const [messages, setMessages] = useState([]);
  const [history, setHistory] = useState([]);
  const [busy, setBusy] = useState(false);
  const [health, setHealth] = useState(null);
  const [error, setError] = useState('');
  const [index, setIndex] = useState(null);
  const working = useRef(false);
  const revision = useRef(0);

  useEffect(() => {
    let active = true;
    const initial = revision.current;
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 5000);
    fetch(`${BASE}/health`, { signal: controller.signal })
      .then(async (response) => {
        if (!response.ok) throw new Error('health');
        const data = await response.json();
        if (active && revision.current === initial) { setHealth(data); setError(''); }
      })
      .catch(() => { if (active && revision.current === initial) setError('导购服务未连接，请启动 python3 scripts/run_guide.py 后刷新。'); })
      .finally(() => clearTimeout(timer));
    return () => { active = false; clearTimeout(timer); controller.abort(); };
  }, []);

  async function inspectIndex() {
    try {
      const response = await fetch(`${BASE}/api/v1/guide/rag`);
      if (!response.ok) throw new Error('无法读取政策索引');
      setIndex(await response.json());
    } catch (e) { setError(e.message); }
  }

  async function send(event) {
    event?.preventDefault();
    const text = question.trim();
    if (!text || working.current) return;
    working.current = true;
    setBusy(true);
    setQuestion('');
    setMessages((items) => [...items, { role: 'user', content: text }]);
    const next = [...history.slice(-18), { role: 'user', content: text }];
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 360000);
    try {
      const response = await fetch(`${BASE}/api/v1/guide/chat`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ messages: next }), signal: controller.signal,
      });
      const payload = await response.json();
      if (!response.ok) throw new Error(payload.message || `HTTP ${response.status}`);
      const data = payload.data;
      if (!data || typeof data.answer !== 'string' || !Array.isArray(data.trace)) throw new Error('导购返回格式不合法');
      revision.current += 1;
      setHealth((current) => ({ ...current, mode: data.mode, modelConfigured: data.mode === 'live', embedding: data.embedding }));
      setError('');
      data.trace.forEach((step) => console.info('[ReAct]', step.step, 'Thought:', step.thought, 'Action:', step.action, step.arguments, 'Observation:', step.observation));
      setMessages((items) => [...items, { role: 'assistant', content: data.answer, ...data }]);
      setHistory([...next, { role: 'assistant', content: data.answer.slice(0, 4000) }]);
    } catch (e) {
      setMessages((items) => [...items, { role: 'assistant', failed: true,
        content: e.name === 'AbortError' ? '导购请求超时，请稍后重试。' : `导购请求失败：${e.message}` }]);
    } finally {
      clearTimeout(timer);
      working.current = false;
      setBusy(false);
    }
  }

  return <section className="guide-page">
    <div className="guide-heading"><div>
      <Typography.Title level={2}><CompassOutlined /> 图书导购 Agent</Typography.Title>
      <Typography.Paragraph type="secondary">Task 3 · 模糊需求推荐 · 政策 RAG · ReAct 执行记录</Typography.Paragraph>
    </div><Button disabled={busy} onClick={() => { setMessages([]); setHistory([]); }}>新对话</Button></div>
    {error && <Alert type="error" showIcon message={error} />}
    {health && <Alert showIcon type={health.mode === 'demo' ? 'warning' : 'info'}
      message={health.mode === 'demo' ? '离线演示 Agent：规则模拟模型决策，不调用真实大模型' : `真实模型 ReAct Agent${health.modelConfigured ? '' : '（尚未配置密钥）'}`}
      description={`模拟书目与模拟商店政策；本地 TF-IDF 词项向量检索${health.chunkCount ? `，${health.chunkCount} 个片段，${health.dimensions} 维` : ''}。Thought 为简短行动理由。`} />}
    <div className="guide-examples">{EXAMPLES.map(([label, value]) => <Button key={label} disabled={busy} onClick={() => setQuestion(value)}>{label}</Button>)}
      <Button onClick={inspectIndex}>查看政策分块与向量信息</Button></div>
    {index && <details className="guide-index" open><summary>政策 RAG 索引：{index.chunkCount} 个片段 / {index.dimensions} 维</summary>
      <p>Embedding：{index.embedding}；分块上限 {index.chunkSize} 字，重叠 {index.overlap} 字；相似度为余弦相似度。</p>
      {index.chunks.map((chunk) => <details key={chunk.id}><summary>{chunk.id} · {chunk.section}</summary><p>{chunk.text}</p><pre>向量预览：{JSON.stringify(chunk.vectorPreview)}</pre></details>)}
    </details>}
    <div className="guide-chat" role="log" aria-live="polite" aria-label="导购对话记录">
      {!messages.length && <p className="guide-empty">试试“复杂案例”，查看查书 → 查政策 → 综合回答的执行过程。</p>}
      {messages.map((item, i) => <article key={i} className={`guide-message ${item.role} ${item.failed ? 'failed' : ''}`}>
        <strong>{item.role === 'user' ? '你' : '导购 Agent'}</strong>
        {item.mode && <Tag color={item.mode === 'live' ? 'blue' : 'orange'}>{item.mode === 'live' ? '真实模型' : '离线演示'}</Tag>}
        <p>{item.content}</p>
        {item.trace && <details className="guide-trace" open><summary>ReAct：Thought → Action → Observation（{item.trace.length} 步）</summary>
          <ol>{item.trace.map((step, j) => <li key={j}>
            <div><b>Thought（行动理由）：</b>{step.thought}</div>
            <div><b>Action：</b><code>{step.action}</code> <code>{JSON.stringify(step.arguments)}</code></div>
            <details><summary>Observation（执行结果）</summary><pre>{JSON.stringify(step.observation, null, 2)}</pre></details>
          </li>)}</ol>
        </details>}
        {!!item.sources?.length && <details><summary>政策来源与引用片段</summary>{item.sources.map((source) => <div key={source.id}><b>[{source.id}] {source.section}</b><p>{source.text}</p><small>来源：{source.source}；相似度：{source.score}</small></div>)}</details>}
      </article>)}
      {busy && <div className="guide-loading"><Spin size="small" /> Agent 正在逐步查书、检索政策并整理回答…</div>}
    </div>
    <form className="guide-compose" onSubmit={send}>
      <Input.TextArea aria-label="导购问题" placeholder={EXAMPLES[0][1]} value={question} maxLength={1000}
        disabled={busy} autoSize={{ minRows: 2, maxRows: 5 }} onChange={(e) => setQuestion(e.target.value)}
        onPressEnter={(e) => { if (!e.shiftKey && !e.nativeEvent.isComposing && e.keyCode !== 229) { e.preventDefault(); send(); } }} />
      <Button aria-label="发送导购问题" type="primary" htmlType="submit" icon={<SendOutlined />} loading={busy} disabled={!question.trim()}>发送</Button>
    </form>
    <Typography.Paragraph type="secondary">无需 MySQL 即可验收本任务。目录与政策均为课程模拟；答案中的 [Pxx-xx] 可在来源片段中核对。</Typography.Paragraph>
  </section>;
}
