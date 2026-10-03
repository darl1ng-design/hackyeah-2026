"""Hackathon RAG: FastAPI + any OpenAI-compatible LLM, traced to Langfuse.

Functional core (pure: chunk, rank, prompt, add) / imperative shell (I/O returns Result).
"""

import os
import threading
from dataclasses import dataclass

import numpy as np
from fastapi import FastAPI, HTTPException
from langfuse import observe

# drop-in: every chat/embedding call shows up as a Langfuse generation
from langfuse.openai import OpenAI
from pydantic import BaseModel, Field

from src.result import Err, Result, attempt

LLM_MODEL = os.environ.get("LLM_MODEL", "gpt-6-luna")
EMBED_MODEL = os.environ.get("EMBED_MODEL", "text-embedding-3-small")
CHUNK, OVERLAP, TOP_K, BATCH = 2000, 200, 5, 100
SYSTEM = "Answer only from the provided context. If the context lacks the answer, say so. Be concise."


# --- core (pure) ---


@dataclass(frozen=True, slots=True)
class Index:
    chunks: tuple[dict, ...] = ()
    vectors: np.ndarray | None = None


def chunk(text: str) -> list[str]:
    return [
        text[i : i + CHUNK]
        for i in range(0, max(len(text) - OVERLAP, 1), CHUNK - OVERLAP)
    ]


def add(idx: Index, new: list[dict], vecs: np.ndarray) -> Index:
    return Index(
        idx.chunks + tuple(new),
        vecs if idx.vectors is None else np.vstack([idx.vectors, vecs]),
    )


def rank(idx: Index, qvec: np.ndarray, k: int = TOP_K) -> list[dict]:
    scores = idx.vectors @ qvec  # ponytail: brute-force cosine, fine to ~100k chunks
    return [
        idx.chunks[i] | {"score": float(scores[i])} for i in np.argsort(-scores)[:k]
    ]


def prompt(question: str, hits: list[dict]) -> list[dict]:
    context = "\n\n".join(
        f"[{i}] {h['title']}\n{h['text']}" for i, h in enumerate(hits, 1)
    )
    return [
        {"role": "system", "content": SYSTEM},
        {"role": "user", "content": f"Context:\n{context}\n\nQuestion: {question}"},
    ]


# --- shell (I/O -> Result) ---

llm = OpenAI()  # reads OPENAI_API_KEY / OPENAI_BASE_URL
# ponytail: in-memory index, lost on restart; np.save or pgvector when data outgrows a demo
index, lock = Index(), threading.Lock()


def embed(texts: list[str]) -> Result[np.ndarray, str]:
    def call():
        data = [
            d.embedding
            for i in range(0, len(texts), BATCH)
            for d in llm.embeddings.create(
                model=EMBED_MODEL, input=texts[i : i + BATCH]
            ).data
        ]
        v = np.array(data, dtype=np.float32)
        return v / np.linalg.norm(v, axis=1, keepdims=True)

    return attempt(call, "embed")


def complete(messages: list[dict], **kw) -> Result[str, str]:
    return attempt(
        lambda: (
            llm.chat.completions.create(model=LLM_MODEL, messages=messages, **kw)
            .choices[0]
            .message.content
        ),
        "generate",
    )


@observe(as_type="retriever")
def retrieve(question: str) -> Result[list[dict], str]:
    idx = index  # snapshot: ingest swaps the global, never mutates it
    return embed([question]).map(lambda q: rank(idx, q[0]))


@observe()
def rag(question: str) -> Result[dict, str]:
    return retrieve(question).bind(
        lambda hits: complete(prompt(question, hits)).map(
            lambda answer: {"answer": answer, "sources": hits}
        )
    )


# --- HTTP edge: Result -> response ---

app = FastAPI(title="HackYeah RAG")


class Doc(BaseModel):
    text: str = Field(min_length=1)
    title: str = ""
    url: str = ""


class Query(BaseModel):
    question: str = Field(min_length=1)


def unwrap[T](r: Result[T, str]) -> T:
    if isinstance(r, Err):
        raise HTTPException(502, r.error)
    return r.value


@app.get("/health")
def health():
    return {"ok": True, "chunks": len(index.chunks)}


@app.post("/ingest")
def ingest(docs: list[Doc]):
    global index
    new = [
        {"text": c, "title": d.title, "url": d.url} for d in docs for c in chunk(d.text)
    ]
    vecs = unwrap(embed([c["text"] for c in new]))
    with lock:
        index = add(index, new, vecs)
    return {"added": len(new), "total": len(index.chunks)}


@app.post("/query")
def query(q: Query):
    if not index.chunks:
        raise HTTPException(409, "Index empty: POST /ingest first")
    return unwrap(rag(q.question))
