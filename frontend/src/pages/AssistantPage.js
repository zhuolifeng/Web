import { useEffect, useRef, useState } from 'react';
import { Alert, Button, Input, Spin, Tag, Typography } from 'antd';
import { SendOutlined, RobotOutlined, ClearOutlined } from '@ant-design/icons';
import './AssistantPage.css';

const BASE = (process.env.REACT_APP_ASSISTANT_URL || 'http://localhost:8001').replace(/\/$/, '');
const EXAMPLES = [
  '帮我查一下 ISBN为12345 的书还有多少本，顺便看看别家卖多少钱',
  '查一下 ISBN为abc 的库存和竞品价格',
  '查一下 ISBN为00000 的库存和竞品价格',
  '查一下 ISBN为978-7536692930 的库存和竞品价格',
];

export default function AssistantPage() {
  const [input, setInput] = useState('');
  const [messages, setMessages] = useState([]);
  const [history, setHistory] = useState([]);
  const [loading, setLoading] = useState(false);
  const [health, setHealth] = useState(null);
  const [healthError, setHealthError] = useState('');
  const busy = useRef(false);
  const connectionRevision = useRef(0);
  const end = useRef(null);

  useEffect(() => {
    let active = true;
    const revision = connectionRevision.current;
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 5000);
    fetch(`${BASE}/health`, { signal: controller.signal })
      .then(async (response) => {
        if (!response.ok) throw new Error();
        const status = await response.json();
        if (active && connectionRevision.current === revision) {
          setHealth(status);
          setHealthError('');
        }
      })
      .catch(() => {
        // StrictMode cleans up the first effect and aborts its request.
        // Neither that cancellation nor an older probe can overwrite a newer success.
        if (active && connectionRevision.current === revision) {
          setHealth(null);
          setHealthError('助手服务尚未连接。请按说明启动 Python 服务，再刷新此页。');
        }
      })
      .finally(() => clearTimeout(timer));
    return () => { active = false; clearTimeout(timer); controller.abort(); };
  }, []);

  useEffect(() => { end.current?.scrollIntoView?.({ behavior: 'smooth' }); }, [messages, loading]);

  async function send() {
    const question = input.trim();
    if (!question || busy.current) return;
    busy.current = true;
    setLoading(true);
    setInput('');
    setMessages((items) => [...items, { role: 'user', content: question }]);
    // Send only completed user/assistant pairs; tool history stays on the server.
    const nextHistory = [...history.slice(-18), { role: 'user', content: question }];
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), 180000);
    try {
      const response = await fetch(`${BASE}/api/v1/assistant/chat`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ messages: nextHistory }), signal: controller.signal,
      });
      const payload = await response.json();
      if (!response.ok) throw new Error(payload.message || `HTTP ${response.status}`);
      const data = payload.data;
      if (!data || typeof data.answer !== 'string' || !Array.isArray(data.toolCalls)) {
        throw new Error('助手返回格式不正确。');
      }
      connectionRevision.current += 1;
      setHealthError('');
      setHealth((current) => ({ ...current, mode: data.mode,
        inventoryMode: data.inventoryMode, modelConfigured: data.mode === 'live' }));
      data.toolCalls.forEach((call) => console.info('[Function Calling]', call.name, call.arguments, call.result));
      setMessages((items) => [...items, { role: 'assistant', content: data.answer, ...data }]);
      setHistory([...nextHistory, { role: 'assistant', content: data.answer }]);
    } catch (error) {
      setMessages((items) => [...items, { role: 'assistant', error: true,
        content: error.name === 'AbortError' ? '请求超时，请稍后重试。' : `查询失败：${error.message}` }]);
    } finally {
      clearTimeout(timer);
      busy.current = false;
      setLoading(false);
    }
  }

  return (
    <section className="assistant-page">
      <div className="assistant-heading">
        <div><Typography.Title level={2}><RobotOutlined /> 书店智能助手</Typography.Title>
          <Typography.Paragraph type="secondary">查询库存 · 比较模拟竞品报价 · 查看工具调用过程</Typography.Paragraph></div>
        <Button aria-label="新对话" icon={<ClearOutlined />} disabled={loading} onClick={() => { setMessages([]); setHistory([]); }}>新对话</Button>
      </div>
      {healthError && <Alert type="error" showIcon message={healthError} />}
      {health && <Alert showIcon type={health.mode === 'demo' ? 'warning' : 'info'}
        message={health.mode === 'demo' ? '离线演示模式：规则模拟工具调用，没有请求真实大模型。' :
          `真实模型 API 模式${health.modelConfigured ? '' : '：尚未配置服务端 API 密钥'}`}
        description={`库存来源：${health.inventoryMode === 'demo' ? '演示数据，不是实时库存' : '现有书店后端，每次实时读取'}；竞品价格始终是模拟数据。`} />}
      <div className="assistant-examples" aria-label="示例问题">
        {EXAMPLES.map((example, index) => <Button key={example} size="small" disabled={loading}
          onClick={() => setInput(example)}>{['库存与比价', 'ISBN 错误', '模拟超时', 'ISBN 参数纠正'][index]}</Button>)}
      </div>
      <div className="assistant-messages" role="log" aria-live="polite" aria-label="助手对话记录">
        {!messages.length && <div className="assistant-empty"><RobotOutlined /><p>输入 ISBN，查询这本书的库存和模拟竞品报价。</p><small>课堂示例 ISBN：12345；超时示例：00000。</small></div>}
        {messages.map((item, i) => <article key={i} className={`assistant-message ${item.role} ${item.error ? 'error' : ''}`}>
          <strong>{item.role === 'user' ? '你' : '书店助手'}</strong>
          {item.mode && <Tag color={item.mode === 'demo' ? 'orange' : 'blue'}>{item.mode === 'demo' ? '离线演示' : '模型 API'}</Tag>}
          <p>{item.content}</p>
          {!!item.toolCalls?.length && <details><summary>工具调用记录（{item.toolCalls.length} 次）</summary>
            {item.toolCalls.map((call, index) => <div className="assistant-tool" key={`${call.id}-${index}`}>
              <b>{call.name}</b><Tag color={call.result.ok ? 'green' : 'red'}>{call.result.ok ? '成功' : call.result.error?.code}</Tag>
              <pre>{JSON.stringify({ arguments: call.arguments, result: call.result }, null, 2)}</pre>
            </div>)}
          </details>}
        </article>)}
        {loading && <div className="assistant-thinking"><Spin size="small" /> 正在查询并整理回答…</div>}
        <div ref={end} />
      </div>
      <form className="assistant-compose" onSubmit={(event) => { event.preventDefault(); send(); }}>
        <Input.TextArea aria-label="用户问题" placeholder={EXAMPLES[0]} value={input} maxLength={4000}
          autoSize={{ minRows: 2, maxRows: 5 }} disabled={loading} onChange={(event) => setInput(event.target.value)}
          onPressEnter={(event) => {
            if (!event.shiftKey && !event.nativeEvent.isComposing && event.keyCode !== 229) {
              event.preventDefault(); send();
            }
          }} />
        <Button aria-label="发送" type="primary" htmlType="submit" icon={<SendOutlined />} loading={loading} disabled={!input.trim()}>发送</Button>
      </form>
      <Typography.Paragraph type="secondary">Enter 发送，Shift+Enter 换行。竞品数据仅用于作业演示；助手不会修改库存。</Typography.Paragraph>
    </section>
  );
}
