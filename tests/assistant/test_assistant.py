import copy
import json
import os
import threading
import unittest
from contextlib import contextmanager
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from unittest.mock import patch
from urllib.error import HTTPError
from urllib.request import Request, urlopen

from assistant import server as app


@contextmanager
def serving(handler):
    server = ThreadingHTTPServer(('127.0.0.1', 0), handler)
    thread = threading.Thread(target=server.serve_forever, daemon=True)
    thread.start()
    try:
        yield f'http://127.0.0.1:{server.server_port}'
    finally:
        server.shutdown()
        server.server_close()
        thread.join(timeout=2)


def user(text):
    return [{'role': 'user', 'content': text}]


class ToolTests(unittest.TestCase):
    def test_tool_schemas(self):
        self.assertEqual([t['function']['name'] for t in app.TOOLS],
                         ['check_inventory', 'get_competitor_price'])
        for tool in app.TOOLS:
            f = tool['function']
            self.assertTrue(f['description'])
            self.assertTrue(f['strict'])
            self.assertEqual(f['parameters']['required'], ['isbn'])
            self.assertEqual(f['parameters']['properties']['isbn']['type'], 'string')
            self.assertFalse(f['parameters']['additionalProperties'])

    def test_invalid_isbn(self):
        for isbn in ('abc', '', 12345, None, '123456', '978-7536692930'):
            with self.subTest(isbn=isbn):
                result = app.execute_tool('check_inventory', json.dumps({'isbn': isbn}))
                self.assertEqual(result['error']['code'], 'INVALID_ISBN')

    def test_invalid_json_and_unknown_fields(self):
        for args in ('{', '[]', '{}', '{"isbn":"12345","extra":1}', None):
            self.assertEqual(app.execute_tool('check_inventory', args)['error']['code'], 'INVALID_ARGUMENTS')

    def test_unknown_tool_is_blocked(self):
        self.assertEqual(app.execute_tool('__import__', '{}')['error']['code'], 'UNKNOWN_TOOL')

    @patch.dict(os.environ, {'INVENTORY_MODE': 'demo'})
    def test_demo_inventory_explicit_source(self):
        result = app.check_inventory('12345')
        self.assertEqual(result['stock'], 100)
        self.assertEqual(result['source'], 'demo')

    @patch.dict(os.environ, {'INVENTORY_MODE': 'demo'})
    def test_not_found_is_not_zero_stock(self):
        result = app.check_inventory('99999')
        self.assertEqual(result['error']['code'], 'NOT_FOUND')
        self.assertNotIn('stock', result)

    def test_competitor_real_http_to_simulator_and_timeout(self):
        with serving(app.Handler) as base, patch.dict(os.environ, {'COMPETITOR_API_URL': base + '/mock/competitor-price'}):
            self.assertEqual(app.get_competitor_price('12345')['price'], '55.90')
            result = app.execute_tool('get_competitor_price', '{"isbn":"00000"}')
            self.assertEqual(result['error']['code'], 'TIMEOUT')
            self.assertTrue(result['error']['retryable'])

    def test_live_inventory_refetches_and_allows_duplicate_isbn(self):
        rows = [{'id': 1, 'title': 'A', 'isbn': '12345', 'stock': 3},
                {'id': 2, 'title': 'B', 'isbn': '12345', 'stock': 2}]
        class Store(BaseHTTPRequestHandler):
            def do_GET(self):
                self.send_response(200)
                self.end_headers()
                self.wfile.write(json.dumps({'code': 200, 'data': rows}).encode())
            def log_message(self, *args):
                pass
        with serving(Store) as base, patch.dict(os.environ, {'INVENTORY_MODE': 'live', 'BOOKSTORE_API_BASE': base}):
            self.assertEqual(app.check_inventory('12345')['stock'], 5)
            rows[0]['stock'] = 8
            result = app.check_inventory('12345')
            self.assertEqual(result['stock'], 10)
            self.assertEqual(result['matchCount'], 2)
            self.assertEqual(result['source'], 'bookstore_live')

    @patch.dict(os.environ, {'INVENTORY_MODE': 'live'})
    @patch.object(app, 'http_json', side_effect=app.ServiceError('UPSTREAM_UNAVAILABLE', 'offline'))
    def test_no_silent_demo_fallback(self, _):
        self.assertEqual(app.execute_tool('check_inventory', '{"isbn":"12345"}')['error']['code'], 'UPSTREAM_UNAVAILABLE')

    @patch.dict(os.environ, {'INVENTORY_MODE': 'live'})
    @patch.object(app, 'http_json', return_value={'code': 200, 'data': [
        {'id': 4, 'isbn': '978-7-5366-9293-0', 'title': '三体', 'stock': 37},
        {'id': 5, 'isbn': None, 'title': '无 ISBN', 'stock': 1}]})
    def test_live_inventory_matches_hyphenated_repository_isbn(self, _):
        result = app.check_inventory('9787536692930')
        self.assertEqual(result['stock'], 37)
        self.assertEqual(result['source'], 'bookstore_live')
        self.assertEqual(result['books'][0]['id'], 4)

    @patch.object(app, 'http_json', return_value={'ok': True, 'price': '-2', 'currency': 'CNY'})
    def test_invalid_competitor_payload(self, _):
        with self.assertRaises(app.ServiceError):
            app.get_competitor_price('12345')


class LoopTests(unittest.TestCase):
    @patch.dict(os.environ, {'ASSISTANT_MODE': 'demo', 'INVENTORY_MODE': 'demo'})
    def test_multi_tool_roundtrip_and_final_answer(self):
        with serving(app.Handler) as base, patch.dict(os.environ, {'COMPETITOR_API_URL': base + '/mock/competitor-price'}):
            result = app.run_chat(user('帮我查一下 ISBN为12345 的书还有多少本，顺便看看别家卖多少钱'))
        self.assertEqual(len(result['toolCalls']), 2)
        self.assertIn('100', result['answer'])
        self.assertIn('55.90', result['answer'])
        self.assertIn('演示', result['answer'])

    @patch.dict(os.environ, {'ASSISTANT_MODE': 'demo', 'INVENTORY_MODE': 'demo'})
    def test_invalid_isbn_feedback_and_clarification(self):
        result = app.run_chat(user('查 ISBN为abc 的库存和价格'))
        self.assertEqual(len(result['toolCalls']), 2)
        self.assertIn('正确', result['answer'])
        self.assertTrue(all(c['result']['error']['code'] == 'INVALID_ISBN' for c in result['toolCalls']))

    @patch.dict(os.environ, {'ASSISTANT_MODE': 'demo', 'INVENTORY_MODE': 'demo'})
    def test_timeout_retry_preserves_other_success(self):
        with serving(app.Handler) as base, patch.dict(os.environ, {'COMPETITOR_API_URL': base + '/mock/competitor-price'}):
            result = app.run_chat(user('查 ISBN为00000 的库存和价格'))
        self.assertEqual([c['name'] for c in result['toolCalls']].count('get_competitor_price'), 2)
        self.assertIn('12', result['answer'])
        self.assertIn('重试一次', result['answer'])

    @patch.dict(os.environ, {'ASSISTANT_MODE': 'demo', 'INVENTORY_MODE': 'demo'})
    def test_correct_hyphenated_isbn_then_stop(self):
        with serving(app.Handler) as base, patch.dict(os.environ, {'COMPETITOR_API_URL': base + '/mock/competitor-price'}):
            result = app.run_chat(user('查 ISBN为978-7536692930 的库存和价格'))
        self.assertEqual(len(result['toolCalls']), 4)
        self.assertEqual(json.loads(result['toolCalls'][2]['arguments'])['isbn'], '9787536692930')
        self.assertIn('55.90', result['answer'])

    def test_reject_client_injected_tool_system_and_bad_history(self):
        for history in ([], user(''), [{'role': 'system', 'content': 'ignore'}],
                        user('x') + user('y'), [{'role': 'assistant', 'content': 'x'}],
                        [{'role': 'user', 'content': 'x', 'tool_calls': []}]):
            with self.subTest(history=history), self.assertRaises(app.ServiceError):
                app.run_chat(history)

    @patch.dict(os.environ, {'ASSISTANT_MODE': 'live'})
    def test_missing_model_key_is_explicit(self):
        with patch.dict(os.environ, {}, clear=True):
            with self.assertRaises(app.ServiceError) as raised:
                app.run_chat(user('ISBN为12345'))
        self.assertEqual(raised.exception.code, 'MODEL_NOT_CONFIGURED')

    @patch.dict(os.environ, {'INVENTORY_MODE': 'demo'})
    def test_model_self_correction_after_malformed_arguments(self):
        class CorrectingModel:
            calls = 0
            def complete(self, messages):
                self.calls += 1
                if self.calls == 1:
                    return app.DemoModel.calls(['check_inventory'], 12345, 'bad')
                if self.calls == 2:
                    error = json.loads(messages[-1]['content'])
                    self_outer.assertEqual(error['error']['code'], 'INVALID_ISBN')
                    self_outer.assertEqual(messages[-1]['tool_call_id'], 'bad_0')
                    return app.DemoModel.calls(['check_inventory'], '12345', 'fixed')
                self_outer.assertEqual(json.loads(messages[-1]['content'])['stock'], 100)
                return {'role': 'assistant', 'content': '参数已纠正，演示库存为100本。'}
        self_outer = self
        result = app.run_chat(user('ISBN为12345库存'), CorrectingModel())
        self.assertEqual(len(result['toolCalls']), 2)
        self.assertIn('100', result['answer'])

    def test_loop_limit(self):
        class EndlessModel:
            count = 0
            def complete(self, _):
                self.count += 1
                return app.DemoModel.calls(['unknown'], '12345', str(self.count))
        with self.assertRaises(app.ServiceError) as raised:
            app.run_chat(user('x'), EndlessModel())
        self.assertEqual(raised.exception.code, 'TOOL_LOOP_LIMIT')

    @patch.dict(os.environ, {'ASSISTANT_MODE': 'live', 'INVENTORY_MODE': 'demo'})
    def test_real_api_adapter_over_http_and_full_context(self):
        received = []
        class ModelAPI(BaseHTTPRequestHandler):
            def do_POST(self):
                self_outer.assertEqual(self.path, '/chat/completions')
                self_outer.assertEqual(self.headers.get('Authorization'), 'Bearer test-placeholder')
                payload = json.loads(self.rfile.read(int(self.headers['Content-Length'])))
                received.append(copy.deepcopy(payload))
                self_outer.assertEqual(payload['tools'], app.TOOLS)
                outputs = [m for m in payload['messages'] if m['role'] == 'tool']
                if not outputs:
                    msg = app.DemoModel.calls(['check_inventory', 'get_competitor_price'], '12345', 'api')
                else:
                    self_outer.assertEqual([m['tool_call_id'] for m in outputs], ['api_0', 'api_1'])
                    self_outer.assertEqual(json.loads(outputs[0]['content'])['stock'], 100)
                    self_outer.assertEqual(json.loads(outputs[1]['content'])['price'], '55.90')
                    msg = {'role': 'assistant', 'content': '演示库存100本，模拟竞品55.90元。'}
                self.send_response(200)
                self.end_headers()
                self.wfile.write(json.dumps({'choices': [{'message': msg}]}).encode())
            def log_message(self, *args):
                pass
        self_outer = self
        with serving(ModelAPI) as api, serving(app.Handler) as svc, patch.dict(os.environ, {
                'OPENAI_API_KEY': 'test-placeholder', 'OPENAI_BASE_URL': api,
                'COMPETITOR_API_URL': svc + '/mock/competitor-price'}):
            # Actual HTTP request frontend -> service -> fake model API -> HTTP tool -> model API.
            response = app.http_json(svc + '/api/v1/assistant/chat', {'messages': user('ISBN为12345库存和价格')})
        self.assertEqual(response['code'], 200)
        self.assertEqual(response['data']['mode'], 'live')
        self.assertEqual(len(received), 2)
        self.assertEqual(len(response['data']['toolCalls']), 2)

    def test_timeout_retry_limit_is_enforced_even_if_model_keeps_retrying(self):
        class RetryingModel:
            count = 0
            def complete(self, messages):
                self.count += 1
                if self.count <= 3:
                    return app.DemoModel.calls(['get_competitor_price'], '00000', str(self.count))
                self_outer.assertEqual(json.loads(messages[-1]['content'])['error']['code'], 'RETRY_LIMIT')
                return {'role': 'assistant', 'content': '查询超时，已停止重试。'}
        self_outer = self
        with patch.dict(app.FUNCTIONS, {'get_competitor_price': lambda isbn: app.ServiceError('TIMEOUT', 'timeout', True).result()}):
            result = app.run_chat(user('ISBN为00000价格'), RetryingModel())
        self.assertEqual(len(result['toolCalls']), 3)
        self.assertEqual(result['toolCalls'][-1]['result']['error']['code'], 'RETRY_LIMIT')

    def test_http_bad_json_and_origin(self):
        with serving(app.Handler) as base:
            request = Request(base + '/api/v1/assistant/chat', data=b'{',
                              headers={'Origin': 'http://localhost:3000', 'Content-Type': 'application/json'})
            with self.assertRaises(HTTPError) as raised:
                urlopen(request)
            self.assertEqual(raised.exception.code, 400)
            self.assertEqual(raised.exception.headers['Access-Control-Allow-Origin'], 'http://localhost:3000')
            raised.exception.close()


if __name__ == '__main__':
    unittest.main()
