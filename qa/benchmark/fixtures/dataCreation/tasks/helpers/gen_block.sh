#!/bin/bash

# set -eEuo pipefail

command -v jq || { echo "jq is required to run this script"; exit 1; }
command -v bc || { echo "bc is required to run this script"; exit 1; }
( docker compose version 2>&1 || docker-compose version 2>&1 ) | grep -q v2 || { echo "docker compose v2 is required to run this script"; exit 1; }
compose_cmd="$(docker compose version 2>&1 | grep -q v2 && echo 'docker compose' || echo 'docker-compose')"

 zen-cli_cmd () {
   local cmd="$1"
   local args="${*:2}"
   # shellcheck disable=SC2086
   $compose_cmd exec zend gosu user zen-cli "$cmd" $args
 }

zen-cli_cmd generate "$1"