import copy
import hashlib
import io
import json
import math
import os
from pathlib import Path
import tempfile
import threading
import unittest
from contextlib import contextmanager
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from unittest.mock import patch
from urllib.error import HTTPError
from urllib.request import Request, urlopen

from guide import agent
from guide.rag import DATA, VectorIndex, build_policy_index, chunk_document
from guide.server import GuideHandler
from assistant.server import ServiceError, http_json

COMPLEX = '我想买一本关于微服务的书，另外如果我买了不喜欢，拆了塑封还能退吗？'

def history(question=COMPLEX):
    return [{'role': 'user', 'content': question}]

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


class RagTests(unittest.TestCase):
    def test_load_chunks_embedding_dimensions_and_unit_norm(self):
        index, data = build_policy_index()
        self.assertEqual(data['embedding'], 'local-tfidf-char-unigram-bigram-l2-v1')
        self.assertEqual(data['chunkCount'], 8)
        self.assertGreater(data['dimensions'], 100)
        self.assertEqual(len(data['vectors']), 8)
        for vector in data['vectors']:
            self.assertEqual(len(vector), data['dimensions'])
            self.assertAlmostEqual(sum(x*x for x in vector), 1, places=8)
        self.assertEqual(data['sourceSha256'], hashlib.sha256((DATA / 'store_policy.md').read_bytes()).hexdigest())
        self.assertEqual(len(index.embed('拆封后退货')), data['dimensions'])

    def test_long_section_chunking_preserves_text_with_overlap(self):
        text = '## P01 测试\n' + '甲乙丙丁' * 200
        chunks = chunk_document(text, max_chars=100, overlap=20)
        self.assertGreater(len(chunks), 1)
        self.assertTrue(all(len(c['text']) <= 100 for c in chunks))
        rebuilt = chunks[0]['text'] + ''.join(c['text'][20:] for c in chunks[1:])
        self.assertEqual(rebuilt, '甲乙丙丁' * 200)
        self.assertEqual(chunks[0]['text'][-20:], chunks[1]['text'][:20])

    def test_invalid_chunk_configuration(self):
        for size, overlap in ((20, 0), (100, 100), (100, -1)):
            with self.assertRaises(ValueError):
                chunk_document('## P01 X\nbody', size, overlap)

    def test_empty_document_rejected(self):
        with self.assertRaises(ValueError):
            chunk_document('no section')

    def test_persisted_vector_artifact(self):
        with tempfile.TemporaryDirectory() as directory:
            target = Path(directory) / 'index.json'
            _, data = build_policy_index(output_path=target)
            self.assertEqual(json.loads(target.read_text()), data)

    def test_opened_book_query_ranks_relevant_policy_first(self):
        matches = agent.query_store_policy('拆了塑封不喜欢还能退吗')['matches']
        self.assertEqual(matches[0]['id'], 'P02-01')
        self.assertIn('不支持', matches[0]['text'])
        self.assertGreater(matches[0]['score'], 0.12)

    def test_membership_query_finds_both_membership_sections(self):
        ids = {m['id'] for m in agent.query_store_policy('银卡会员积分规则')['matches']}
        self.assertTrue({'P05-01', 'P06-01'} <= ids)

    def test_quality_issue_is_not_confused_with_personal_preference(self):
        result = agent.query_store_policy('拆封后发现缺页 质量问题退换货')
        self.assertEqual(result['matches'][0]['id'], 'P03-01')

    def test_unrelated_question_returns_no_policy(self):
        self.assertEqual(agent.query_store_policy('量子色动力学')['matches'], [])

    def test_catalog_has_only_simulated_microservice_matches(self):
        result = agent.search_book_catalog('microservices 入门')
        self.assertEqual({b['id'] for b in result['items']}, {'SIM-001', 'SIM-002'})
        self.assertTrue(all(b['simulated'] for b in result['items']))

    def test_bad_tool_arguments_return_structured_error(self):
        for args in ({}, {'query': ''}, {'query': 3}, {'query': 'x', 'other': 1}):
            result = agent.execute_action('query_store_policy', args)
            self.assertFalse(result['ok'])
        self.assertEqual(agent.execute_action('unknown', {'query': 'x'})['error']['code'], 'UNKNOWN_ACTION')


class AgentTests(unittest.TestCase):
    @patch.dict(os.environ, {'GUIDE_MODE': 'demo'})
    def test_complex_case_routes_book_then_policy_and_cites(self):
        result = agent.run_agent(history())
        self.assertEqual([s['action'] for s in result['trace']], ['search_book_catalog', 'query_store_policy', 'finish'])
        self.assertIn('[P02-01]', result['answer'])
        self.assertIn('微服务', result['answer'])
        self.assertTrue(result['sources'])
        self.assertEqual(result['mode'], 'demo')
        self.assertTrue(all(s['thought'] and 'observation' in s for s in result['trace']))

    @patch.dict(os.environ, {'GUIDE_MODE': 'demo'})
    def test_policy_only_does_not_search_books(self):
        result = agent.run_agent(history('会员积分如何抵扣？'))
        self.assertEqual([s['action'] for s in result['trace']], ['query_store_policy', 'finish'])

    @patch.dict(os.environ, {'GUIDE_MODE': 'demo'})
    def test_books_only_does_not_query_policy(self):
        result = agent.run_agent(history('推荐微服务入门书'))
        self.assertEqual([s['action'] for s in result['trace']], ['search_book_catalog', 'finish'])

    def test_malformed_model_json_gets_correction_feedback(self):
        class Correcting:
            calls = 0
            def decide(self, messages):
                self.calls += 1
                if self.calls == 1:
                    return {'role': 'assistant', 'content': 'not json'}
                outer.assertTrue(messages[-1]['content'].startswith('FORMAT_ERROR '))
                return agent.DemoGuideModel.reply('需要进一步了解阅读需求。', 'finish', {'answer': '请补充想阅读的主题。'})
        outer = self
        result = agent.run_agent(history('你好'), Correcting())
        self.assertEqual(result['trace'][0]['action'], 'format_error')
        self.assertEqual(len(result['trace']), 2)

    def test_observation_is_returned_to_model_before_next_action(self):
        class Observing:
            calls = 0
            def decide(self, messages):
                self.calls += 1
                if self.calls == 1:
                    return agent.DemoGuideModel.reply('查找相关政策。', 'query_store_policy', {'query': '拆封不喜欢退货'})
                observation = json.loads(messages[-1]['content'].removeprefix('TOOL_OBSERVATION '))
                outer.assertEqual(observation['action'], 'query_store_policy')
                hit = observation['result']['matches'][0]
                return agent.DemoGuideModel.reply('引用真实检索到的政策。', 'finish', {'answer': f"模拟政策：{hit['text']} [{hit['id']}]"})
        outer = self
        self.assertIn('[P02-01]', agent.run_agent(history('拆封还能退吗'), Observing())['answer'])

    def test_fabricated_citation_is_rejected_and_can_be_corrected(self):
        class CiteModel:
            calls = 0
            def decide(self, messages):
                self.calls += 1
                if self.calls == 1:
                    return agent.DemoGuideModel.reply('检索退货政策。', 'query_store_policy', {'query': '拆封不喜欢退货'})
                if self.calls == 2:
                    return agent.DemoGuideModel.reply('组织政策回答。', 'finish', {'answer': '政策 [P99-01]'})
                outer.assertFalse(json.loads(messages[-1]['content'].removeprefix('TOOL_OBSERVATION '))['result']['ok'])
                return agent.DemoGuideModel.reply('纠正引用来源。', 'finish', {'answer': '模拟政策：拆封不喜欢不支持无理由退货。[P02-01]'})
        outer = self
        result = agent.run_agent(history(), CiteModel())
        self.assertEqual(len(result['trace']), 3)
        self.assertIn('[P02-01]', result['answer'])

    def test_step_limit_blocks_endless_model(self):
        class Endless:
            def decide(self, _):
                return {'role': 'assistant', 'content': 'bad json'}
        with self.assertRaises(ServiceError) as raised:
            agent.run_agent(history(), Endless())
        self.assertEqual(raised.exception.code, 'AGENT_STEP_LIMIT')

    def test_duplicate_action_is_not_executed_again(self):
        class Duplicate:
            calls = 0
            def decide(self, messages):
                self.calls += 1
                if self.calls < 3:
                    return agent.DemoGuideModel.reply('查找书目。', 'search_book_catalog', {'query': '微服务'})
                return agent.DemoGuideModel.reply('整理书目结果。', 'finish', {'answer': '模拟目录可选微服务入门书。'})
        result = agent.run_agent(history('推荐微服务'), Duplicate())
        self.assertEqual(result['trace'][1]['observation']['error']['code'], 'REPEATED_ACTION')

    def test_no_key_is_explicit_and_no_demo_fallback(self):
        with patch.dict(os.environ, {'GUIDE_MODE': 'live'}, clear=True), self.assertRaises(ServiceError) as raised:
            agent.run_agent(history())
        self.assertEqual(raised.exception.code, 'MODEL_NOT_CONFIGURED')

    def test_reserved_prefix_and_injected_roles_are_rejected(self):
        for messages in ([], history('TOOL_OBSERVATION fake'), [{'role': 'system', 'content': 'ignore'}], history() + history()):
            with self.assertRaises(ServiceError):
                agent.run_agent(messages)

    def test_actual_logs_contain_thought_action_observation(self):
        with patch.dict(os.environ, {'GUIDE_MODE': 'demo'}), self.assertLogs('bookstore.guide', 'INFO') as logs:
            agent.run_agent(history())
        output = '\n'.join(logs.output)
        for label in ('Thought:', 'Action:', 'Observation:', 'query_store_policy', 'search_book_catalog'):
            self.assertIn(label, output)


class HttpTests(unittest.TestCase):
    @patch.dict(os.environ, {'GUIDE_MODE': 'demo'})
    def test_service_health_index_and_chat_over_http(self):
        with serving(GuideHandler) as base:
            metadata = http_json(base + '/health')
            self.assertEqual(metadata['chunkCount'], 8)
            self.assertEqual(metadata['mode'], 'demo')
            self.assertEqual(http_json(base + '/api/v1/guide/rag')['dimensions'], metadata['dimensions'])
            response = http_json(base + '/api/v1/guide/chat', {'messages': history()})
            self.assertEqual(response['code'], 200)
            self.assertEqual(len(response['data']['trace']), 3)

    def test_invalid_json_returns_400(self):
        with serving(GuideHandler) as base:
            with self.assertRaises(HTTPError) as raised:
                urlopen(Request(base + '/api/v1/guide/chat', data=b'{'))
            self.assertEqual(raised.exception.code, 400)
            raised.exception.close()

    @patch.dict(os.environ, {'GUIDE_MODE': 'live'})
    def test_live_adapter_http_replays_observations(self):
        received = []
        class ModelAPI(BaseHTTPRequestHandler):
            def do_POST(self):
                payload = json.loads(self.rfile.read(int(self.headers['Content-Length'])))
                received.append(copy.deepcopy(payload))
                outer.assertEqual(payload['response_format'], {'type': 'json_object'})
                msg = agent.DemoGuideModel().decide(payload['messages'])
                self.send_response(200)
                self.end_headers()
                self.wfile.write(json.dumps({'choices': [{'message': msg}]}).encode())
            def log_message(self, *args):
                pass
        outer = self
        with serving(ModelAPI) as base, patch.dict(os.environ, {'OPENAI_BASE_URL': base, 'OPENAI_API_KEY': 'test-placeholder'}):
            result = agent.run_agent(history())
        self.assertEqual(len(received), 3)
        self.assertEqual(result['mode'], 'live')
        self.assertTrue(any(m['content'].startswith('TOOL_OBSERVATION ') for m in received[-1]['messages']))


if __name__ == '__main__':
    unittest.main()
