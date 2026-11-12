"""Load, chunk and embed the Task 3 policy; save an inspectable vector artifact."""
from pathlib import Path
import sys
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from guide.rag import build_policy_index

if __name__ == '__main__':
    output = Path(__file__).resolve().parents[1] / 'guide/data/policy_index.json'
    _, metadata = build_policy_index(output_path=output)
    print(f"PASS: {metadata['chunkCount']} chunks, {metadata['dimensions']} dimensions, {metadata['embedding']}")
    print('Saved:', output)
