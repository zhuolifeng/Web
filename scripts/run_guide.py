"""Task 3 launcher: no dependency installation; key stays in process memory."""
import argparse
import getpass
import os
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from guide.server import main

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--demo', action='store_true', help='Offline model fixture; not a real LLM')
    args = parser.parse_args()
    os.environ['GUIDE_MODE'] = 'demo' if args.demo else 'live'
    os.environ.setdefault('OPENAI_BASE_URL', 'https://models.sjtu.edu.cn/api/v1')
    os.environ.setdefault('OPENAI_MODEL', 'deepseek-chat')
    if not args.demo and not os.environ.get('OPENAI_API_KEY'):
        os.environ['OPENAI_API_KEY'] = getpass.getpass('Task 3 API key (hidden, not saved): ')
    if not args.demo and not os.environ.get('OPENAI_API_KEY', '').strip():
        raise SystemExit('API key is required; use --demo for offline checks.')
    main()
