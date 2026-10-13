#!/usr/bin/env bash
set -euo pipefail
[[ $# -eq 1 ]]
python -X utf8 "$(dirname "$0")/rollback.py" "$1"
