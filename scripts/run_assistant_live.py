"""Start the configured model API adapter without storing a key on disk."""
import getpass
import os
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from assistant.server import main  # noqa: E402

if __name__ == '__main__':
    os.environ['ASSISTANT_MODE'] = 'live'
    os.environ.setdefault('OPENAI_BASE_URL', 'https://models.sjtu.edu.cn/api/v1')
    os.environ.setdefault('OPENAI_MODEL', 'deepseek-chat')
    os.environ.setdefault('INVENTORY_MODE', 'live')
    if not os.environ.get('OPENAI_API_KEY'):
        os.environ['OPENAI_API_KEY'] = getpass.getpass('API key (hidden, not saved): ')
    if not os.environ['OPENAI_API_KEY'].strip():
        raise SystemExit('API key is required in live mode.')
    main()
