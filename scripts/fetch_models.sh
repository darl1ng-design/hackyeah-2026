#!/bin/sh
# Download the GGUF models the llama.cpp containers need into MODELS_DIR
# (default: ./models next to docker-compose.yml, or $MODELS_DIR).
# Idempotent: existing files are skipped; interrupted downloads resume (curl -C -).
# Runs both on the host and inside the `models` compose service.
set -eu

MODELS_DIR="${MODELS_DIR:-$(pwd)/models}"
mkdir -p "$MODELS_DIR"

# friendly name | huggingface repo | file in repo
MODELS="
Qwen3-Embedding-0.6B-Q8_0.gguf|Qwen/Qwen3-Embedding-0.6B-GGUF|Qwen3-Embedding-0.6B-Q8_0.gguf
Qwen3-4B-Instruct-Q4_K_M.gguf|unsloth/Qwen3-4B-Instruct-2507-GGUF|Qwen3-4B-Instruct-2507-Q4_K_M.gguf
"

printf '%s\n' "$MODELS" | while IFS='|' read -r target repo file; do
  [ -n "$target" ] || continue
  out="$MODELS_DIR/$target"
  if [ -s "$out" ]; then
    echo "OK       $target (already present)"
    continue
  fi
  url="https://huggingface.co/$repo/resolve/main/$file"
  echo "FETCH    $target  <-  $url"
  curl -fL -C - --retry 5 --retry-delay 2 --retry-all-errors -o "$out" "$url"
done

# readiness marker for the compose `models` service healthcheck
[ -s "$MODELS_DIR/Qwen3-Embedding-0.6B-Q8_0.gguf" ] \
  && [ -s "$MODELS_DIR/Qwen3-4B-Instruct-Q4_K_M.gguf" ] \
  && : > "$MODELS_DIR/.ready"
echo "MODELS_READY $MODELS_DIR"