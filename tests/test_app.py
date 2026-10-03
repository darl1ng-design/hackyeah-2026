import os

import numpy as np

os.environ.setdefault("OPENAI_API_KEY", "test")
from src import app as m  # noqa: E402
from src.result import Err, Ok, attempt  # noqa: E402


def test_result_composes_and_short_circuits():
    assert Ok(2).map(lambda x: x + 1).bind(lambda x: Ok(x * 10)) == Ok(30)
    assert Err("e").map(lambda x: x + 1).bind(lambda x: Ok(x)) == Err("e")
    assert attempt(lambda: 1 / 0, "div") == Err("div: division by zero")


def test_chunk_covers_text_with_overlap():
    text = "".join(chr(65 + i % 26) for i in range(5000))
    parts = m.chunk(text)
    assert parts[0] == text[: m.CHUNK]
    assert parts[-1].endswith(text[-100:])
    assert parts[1].startswith(text[m.CHUNK - m.OVERLAP : m.CHUNK])
    assert m.chunk("short") == ["short"]


def test_rank_is_pure_cosine_over_added_index():
    idx = m.add(
        m.add(m.Index(), [{"text": "a"}], np.eye(3, dtype=np.float32)[:1]),
        [{"text": "b"}, {"text": "c"}],
        np.eye(3, dtype=np.float32)[1:],
    )
    hits = m.rank(idx, np.array([0.1, 0.9, 0.3], dtype=np.float32), k=2)
    assert [h["text"] for h in hits] == ["b", "c"]


def test_rag_skips_generation_when_embed_fails(monkeypatch):
    monkeypatch.setattr(m, "embed", lambda _: Err("embed: down"))
    monkeypatch.setattr(
        m, "complete", lambda *_: (_ for _ in ()).throw(AssertionError("called"))
    )
    assert m.rag("q") == Err("embed: down")
