#!/usr/bin/env bash
# secrets.env (encrypted, committed) <-> .env (runtime, gitignored).
set -euo pipefail
cd "$(dirname "$0")/.."

case "${1:-}" in
  pull)
    # never clobber silently: .env may hold local tweaks
    [ -f .env ] && cp .env ".env.bak.$(date +%Y%m%d-%H%M%S)"
    sops -d secrets.env > .env
    echo "secrets.env -> .env"
    ;;
  edit)
    sops secrets.env
    echo "encrypted in place. run '$0 pull' to refresh .env"
    ;;
  push)
    cp .env secrets.env
    sops -e -i secrets.env
    echo ".env -> secrets.env (encrypted)"
    ;;
  check)
    a=$(sops -d secrets.env | grep -E '^[A-Z_]+=' | sort | sha256sum)
    b=$(grep -E '^[A-Z_]+=' .env | sort | sha256sum)
    [ "$a" = "$b" ] && echo "in sync" || { echo "DRIFT .env vs secrets.env"; exit 1; }
    ;;
  *)
    echo "usage: $0 {pull|edit|push|check}" >&2
    exit 2
    ;;
esac
