"""HTTP smoke test. A live service may consume your model API quota."""
import argparse
import json
from pathlib import Path
from urllib.error import HTTPError, URLError
from urllib.request import Request, urlopen

SCENARIOS = {
    'normal': '帮我查一下 ISBN为12345 的书还有多少本，顺便看看别家卖多少钱',
    'correction': '查一下 ISBN为978-7536692930 的库存和竞品价格',
    'timeout': '查一下 ISBN为00000 的库存和竞品价格',
    'invalid': '查一下 ISBN为abc 的库存和竞品价格',
}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--url', default='http://127.0.0.1:8001')
    parser.add_argument('--scenario', choices=list(SCENARIOS) + ['all'], default='normal')
    parser.add_argument('--output', help='Optional JSON evidence path; contains no API key.')
    args = parser.parse_args()
    results = []
    names = list(SCENARIOS) if args.scenario == 'all' else [args.scenario]
    for name in names:
        question = SCENARIOS[name]
        request = Request(args.url.rstrip('/') + '/api/v1/assistant/chat',
                          data=json.dumps({'messages': [{'role': 'user', 'content': question}]}).encode(),
                          headers={'Content-Type': 'application/json'})
        try:
            with urlopen(request, timeout=180) as response:
                payload = json.load(response)
        except HTTPError as exc:
            body = exc.read().decode('utf-8', errors='replace')
            exc.close()
            raise SystemExit(f'{name}: HTTP {exc.code}: {body}') from exc
        except URLError as exc:
            raise SystemExit('无法连接助手服务，请先启动服务。') from exc
        data = payload.get('data')
        if payload.get('code') != 200 or not isinstance(data, dict) or not data.get('answer'):
            raise SystemExit(f'{name}: 无有效回答')
        calls = data.get('toolCalls', [])
        if name != 'invalid' and not {'check_inventory', 'get_competitor_price'} <= {call['name'] for call in calls}:
            raise SystemExit(f'{name}: 模型未调用要求的两个工具，请检查模型工具调用支持。')
        for call in calls:
            json.loads(call['arguments'])
        if name == 'normal' and data['inventoryMode'] == 'demo':
            stocks = [c for c in calls if c['name'] == 'check_inventory' and c['result'].get('stock') == 100]
            prices = [c for c in calls if c['name'] == 'get_competitor_price' and c['result'].get('price') == '55.90']
            if not stocks or not prices:
                raise SystemExit('normal: 工具结果与演示数据不一致')
        if name == 'invalid' and not calls and 'ISBN' not in data['answer'].upper():
            raise SystemExit('invalid: 未返回 ISBN 纠正提示')
        if name == 'timeout' and not any(c['result'].get('error', {}).get('code') == 'TIMEOUT' for c in calls):
            raise SystemExit('timeout: 未触发模拟超时')
        if name == 'correction' and not any(json.loads(c['arguments']).get('isbn') == '9787536692930' for c in calls):
            raise SystemExit('correction: 模型未纠正 ISBN 参数')
        results.append({'scenario': name, 'question': question, 'response': payload})
        print(f"PASS {name}: mode={data['mode']} inventory={data['inventoryMode']} toolCalls={len(calls)}")
        print(data['answer'])
    if args.output:
        Path(args.output).write_text(json.dumps(results, ensure_ascii=False, indent=2) + '\n')
        print('Evidence saved:', args.output)


if __name__ == '__main__':
    main()
