"""Offline verification of Task 3 vectors and actual saved ReAct evidence."""
import hashlib
import json
import math
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]


def main():
    index = json.loads((ROOT / 'guide/data/policy_index.json').read_text())
    source_hash = hashlib.sha256((ROOT / 'guide/data/store_policy.md').read_bytes()).hexdigest()
    assert source_hash == index['sourceSha256'], 'Policy changed; rebuild index and regenerate evidence.'
    assert index['chunkCount'] == len(index['chunks']) == len(index['vectors'])
    assert index['dimensions'] == len(index['vocabulary']) == len(index['idf'])
    for chunk, vector in zip(index['chunks'], index['vectors']):
        assert 0 < len(chunk['text']) <= 280
        assert len(vector) == index['dimensions']
        assert all(isinstance(v, (int, float)) and math.isfinite(v) for v in vector)
        assert abs(sum(v * v for v in vector) - 1) < 1e-8
    print(f"PASS index: {index['chunkCount']} chunks, {index['dimensions']} dimensions, normalized vectors, source hash")
    for mode in ('demo', 'live'):
        path = ROOT / f'guide/evidence/{mode}-case.json'
        if not path.is_file():
            print(f'SKIP {mode} case: no saved evidence')
            continue
        evidence = json.loads(path.read_text())
        result = evidence['result']
        assert evidence['modelMode'] == result['mode'] == mode
        assert [s['action'] for s in result['trace']] == ['search_book_catalog', 'query_store_policy', 'finish']
        for entry in result['trace']:
            assert 1 <= len(entry['thought']) <= 120
            assert 'observation' in entry
        assert result['trace'][0]['observation']['simulated']
        assert result['trace'][1]['observation']['sourceSha256'] == source_hash
        matches = result['trace'][1]['observation']['matches']
        assert 'P02-01' in {m['id'] for m in matches}
        references = set(re.findall(r'\[(P\d+-\d+)\]', result['answer']))
        assert references and references <= {m['id'] for m in matches}
        assert all(m['score'] >= 0.12 for m in matches)
        log = path.with_suffix('.log').read_text()
        assert log.index('Action: search_book_catalog') < log.index('Action: query_store_policy') < log.index('Action: finish')
        assert log.count('Thought:') >= 3 and log.count('Observation:') >= 3
        print(f'PASS {mode} evidence: tool order, observations, source hash, citations and execution log')


if __name__ == '__main__':
    main()
