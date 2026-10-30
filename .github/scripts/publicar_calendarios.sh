#!/usr/bin/env bash
set -euo pipefail

branch="${GITHUB_REF_NAME:-main}"
max_attempts=5

# Outra categoria pode publicar na mesma branch enquanto este workflow executa.
# Atualiza a base e tenta novamente caso o push perca essa corrida.
for ((attempt = 1; attempt <= max_attempts; attempt++)); do
  git fetch origin "$branch"
  git rebase FETCH_HEAD

  if git push origin "HEAD:$branch"; then
    exit 0
  fi

  if (( attempt < max_attempts )); then
    echo "Push concorrente ou temporariamente indisponível; nova tentativa ($attempt/$max_attempts)."
    sleep "$attempt"
  fi
done

echo "Não foi possível publicar após $max_attempts tentativas." >&2
exit 1
