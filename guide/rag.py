"""Dependency-free lexical embeddings: chunking -> TF-IDF -> cosine retrieval.

These are fitted term vectors, not neural/semantic embedding API outputs.
"""
import hashlib
import json
import math
from pathlib import Path
import re

DATA = Path(__file__).resolve().parent / 'data'


def normalize(text):
    text = text.lower()
    # Small declared query expansion improves common classroom paraphrases.
    for source, target in [('microservices', '微服务'), ('microservice', '微服务'),
                           ('撕开包装', '拆封 塑封'), ('拆了包装', '拆封 塑封'),
                           ('打开包装', '拆封 塑封'), ('退钱', '退款'),
                           ('不合口味', '不喜欢'), ('vip', '会员')]:
        text = text.replace(source, target)
    return text


def tokens(text):
    chars = re.findall(r'[\u4e00-\u9fff]|[a-z0-9]+', normalize(text))
    return chars + [chars[i] + chars[i + 1] for i in range(len(chars) - 1)]


def chunk_document(text, max_chars=280, overlap=50):
    if max_chars < 80 or not 0 <= overlap < max_chars:
        raise ValueError('invalid chunk size or overlap')
    chunks = []
    # Keep sections separate so citations retain policy identifiers.
    for section in re.split(r'(?m)^##\s+', text)[1:]:
        heading, _, body = section.partition('\n')
        match = re.match(r'(P\d+)\s+', heading)
        if not match:
            continue
        body = body.strip()
        start, index = 0, 1
        while start < len(body):
            end = min(start + max_chars, len(body))
            chunks.append({'id': f'{match.group(1)}-{index:02}',
                           'section': heading.strip(), 'source': 'store_policy.md',
                           'start': start, 'end': end, 'text': body[start:end]})
            if end == len(body):
                break
            start, index = end - overlap, index + 1
    if not chunks:
        raise ValueError('policy document has no usable sections')
    return chunks


class VectorIndex:
    def __init__(self, chunks):
        self.chunks = chunks
        counts = []
        for chunk in chunks:
            count = {}
            for token in tokens(chunk['section'] + ' ' + chunk['text']):
                count[token] = count.get(token, 0) + 1
            counts.append(count)
        vocab = sorted({token for count in counts for token in count})
        self.vocabulary = vocab
        self.idf = {token: math.log((1 + len(chunks)) / (1 + sum(token in c for c in counts))) + 1
                    for token in vocab}
        self.vectors = [self.embed_count(c) for c in counts]

    def embed_count(self, count):
        vector = [(1 + math.log(count[t])) * self.idf[t] if count.get(t) else 0.0
                  for t in self.vocabulary]
        norm = math.sqrt(sum(x * x for x in vector))
        return [x / norm for x in vector] if norm else vector

    def embed(self, text):
        count = {}
        for token in tokens(text):
            count[token] = count.get(token, 0) + 1
        return self.embed_count(count)

    def search(self, query, top_k=3, min_score=0.12):
        vector = self.embed(query)
        ranked = sorted(((sum(a * b for a, b in zip(vector, v)), i)
                         for i, v in enumerate(self.vectors)), reverse=True)
        return [{**self.chunks[i], 'score': round(score, 6)}
                for score, i in ranked[:top_k] if score >= min_score]

    def artifact(self, source_hash):
        return {'embedding': 'local-tfidf-char-unigram-bigram-l2-v1',
                'sourceSha256': source_hash, 'chunkCount': len(self.chunks),
                'dimensions': len(self.vocabulary), 'vocabulary': self.vocabulary,
                'idf': [self.idf[t] for t in self.vocabulary],
                'chunks': self.chunks, 'vectors': self.vectors}


def build_policy_index(policy_path=None, output_path=None):
    path = Path(policy_path) if policy_path else DATA / 'store_policy.md'
    raw = path.read_bytes()
    text = raw.decode('utf-8')
    index = VectorIndex(chunk_document(text))
    artifact = index.artifact(hashlib.sha256(raw).hexdigest())
    if output_path:
        target = Path(output_path)
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(json.dumps(artifact, ensure_ascii=False, indent=2) + '\n')
    return index, artifact
