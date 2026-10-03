"""Parallel LLM classification, one Langfuse trace per batch.

usage: uv run --env-file .env python -m src.classify positive negative neutral < texts.txt
"""

import json
import sys
from concurrent.futures import ThreadPoolExecutor
from contextvars import copy_context

from langfuse import get_client, observe

from src.app import complete
from src.result import Err, Ok, Result, attempt

WORKERS = 16


def messages(text: str, labels: list[str]) -> list[dict]:
    return [
        {
            "role": "system",
            "content": f"Classify the text into exactly one label: {', '.join(labels)}.",
        },
        {"role": "user", "content": text},
    ]


def schema(labels: list[str]) -> dict:
    s = {
        "type": "object",
        "properties": {"label": {"enum": labels}},
        "required": ["label"],
        "additionalProperties": False,
    }
    return {
        "type": "json_schema",
        "json_schema": {"name": "label", "strict": True, "schema": s},
    }


@observe()
def classify(text: str, labels: list[str]) -> Result[str, str]:
    return complete(messages(text, labels), response_format=schema(labels)).bind(
        lambda content: attempt(lambda: json.loads(content)["label"], "parse")
    )


@observe()
def classify_all(texts: list[str], labels: list[str]) -> list[Result[str, str]]:
    # threads don't inherit OTel context: copy it here (parent thread), one copy per item, or spans detach
    with ThreadPoolExecutor(WORKERS) as ex:
        futures = [ex.submit(copy_context().run, classify, t, labels) for t in texts]
        return [f.result() for f in futures]


if __name__ == "__main__":
    if len(sys.argv) < 3:
        sys.exit(__doc__)
    texts = [line.strip() for line in sys.stdin if line.strip()]
    for r, text in zip(classify_all(texts, sys.argv[1:]), texts):
        match r:
            case Ok(label):
                print(f"{label}\t{text}")
            case Err(error):
                print(f"ERROR\t{text}\t{error}")
    get_client().flush()
