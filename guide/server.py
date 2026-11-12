"""Task 3 service, separate from Task 2; no third-party server dependency."""
import json
import logging
import os
from http.server import ThreadingHTTPServer
from urllib.parse import urlparse

from assistant.server import Handler, ServiceError
from guide.agent import policy_index, run_agent


class GuideHandler(Handler):
    def do_GET(self):
        path = urlparse(self.path).path
        if path == '/health':
            index, artifact = policy_index()
            self.send_json(200, {'status': 'ok', 'mode': os.environ.get('GUIDE_MODE', 'live'),
                                 'modelConfigured': bool(os.environ.get('OPENAI_API_KEY')),
                                 'embedding': artifact['embedding'], 'chunkCount': len(index.chunks),
                                 'dimensions': artifact['dimensions']})
        elif path == '/api/v1/guide/rag':
            index, artifact = policy_index()
            self.send_json(200, {'source': 'store_policy.md', 'sourceSha256': artifact['sourceSha256'],
                                 'embedding': artifact['embedding'], 'chunkSize': 280, 'overlap': 50,
                                 'chunkCount': len(index.chunks), 'dimensions': artifact['dimensions'],
                                 'chunks': [{**c, 'vectorPreview': v[:12]} for c, v in zip(index.chunks, index.vectors)]})
        else:
            self.send_json(404, {'message': 'Task 3 接口不存在。'})

    def do_POST(self):
        if self.path != '/api/v1/guide/chat':
            self.send_json(404, {'message': 'Task 3 接口不存在。'})
            return
        try:
            length = int(self.headers.get('Content-Length', '0'))
            if not 0 < length <= 100000:
                raise ServiceError('INVALID_REQUEST', '请求体大小不合法。', status=400)
            payload = json.loads(self.rfile.read(length))
            if not isinstance(payload, dict) or set(payload) != {'messages'}:
                raise ServiceError('INVALID_REQUEST', '请求体只接受 messages。', status=400)
            data = run_agent(payload['messages'])
            self.send_json(200, {'code': 200, 'message': 'success', 'data': data})
        except (ValueError, UnicodeError):
            self.send_json(400, {'code': 400, 'message': '请求不是合法 JSON。', 'data': None})
        except ServiceError as exc:
            self.send_json(exc.status, {'code': exc.status, 'message': exc.message, 'error': exc.code, 'data': None})
        except Exception:
            logging.getLogger('bookstore.guide').exception('Unhandled guide error')
            self.send_json(500, {'code': 500, 'message': '导购服务内部错误。', 'data': None})


def main():
    logging.basicConfig(level=logging.INFO, format='%(asctime)s %(levelname)s %(message)s')
    port = int(os.environ.get('GUIDE_PORT', '8002'))
    index, artifact = policy_index()
    server = ThreadingHTTPServer(('127.0.0.1', port), GuideHandler)
    logging.info('Guide http://127.0.0.1:%s mode=%s chunks=%s dimensions=%s', port,
                 os.environ.get('GUIDE_MODE', 'live'), len(index.chunks), artifact['dimensions'])
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
