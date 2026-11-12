"""Run the Task 3 complex case and save an actual labeled ReAct execution log."""
import argparse
import getpass
import json
import logging
import os
from pathlib import Path
import sys

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT))
from guide.agent import run_agent

QUESTION = '我想买一本关于微服务的书，另外如果我买了不喜欢，拆了塑封还能退吗？'

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--live', action='store_true', help='Use real model API; default is explicit offline demo')
    parser.add_argument('--question', default=QUESTION)
    parser.add_argument('--output', help='Evidence JSON path; defaults to guide/evidence/<mode>-case.json')
    args = parser.parse_args()
    mode = 'live' if args.live else 'demo'
    os.environ['GUIDE_MODE'] = mode
    os.environ.setdefault('OPENAI_BASE_URL', 'https://models.sjtu.edu.cn/api/v1')
    os.environ.setdefault('OPENAI_MODEL', 'deepseek-chat')
    if args.live and not os.environ.get('OPENAI_API_KEY'):
        os.environ['OPENAI_API_KEY'] = getpass.getpass('Task 3 API key (hidden, not saved): ')
    target = Path(args.output) if args.output else ROOT / 'guide/evidence' / f'{mode}-case.json'
    target.parent.mkdir(parents=True, exist_ok=True)
    log_path = target.with_suffix('.log')
    handler = logging.FileHandler(log_path, encoding='utf-8', mode='w')
    logging.basicConfig(level=logging.INFO, format='%(asctime)s %(levelname)s %(message)s',
                        handlers=[handler, logging.StreamHandler()])
    logging.info('Task 3 case mode=%s; mock catalog and mock store policy; query=%s', mode, args.question)
    try:
        result = run_agent([{'role': 'user', 'content': args.question}])
    except Exception as exc:
        logging.error('Case failed: %s', exc)
        raise SystemExit(1) from exc
    target.write_text(json.dumps({'question': args.question, 'modelMode': mode, 'result': result}, ensure_ascii=False, indent=2) + '\n')
    print('\nFinal answer:\n' + result['answer'])
    print('\nEvidence JSON:', target)
    print('Thought -> Action -> Observation log:', log_path)
