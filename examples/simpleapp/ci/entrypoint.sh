#!/bin/bash

set -euo pipefail

USER_ID="${LOCAL_USER_ID:-9001}"
GRP_ID="${LOCAL_GRP_ID:-9001}"
LD_PRELOAD="${LD_PRELOAD:-}"

if [ "$USER_ID" != "0"  ]; then
    getent group "$GRP_ID" > /dev/null 2>&1 || groupadd -g "$GRP_ID" user
    id -u user > /dev/null 2>&1 || useradd --shell /bin/bash -u "$USER_ID" -g "$GRP_ID" -o -c "" -m user
    CURRENT_UID="$(id -u user)"
    CURRENT_GID="$(id -g user)"
    if [ "$USER_ID" != "$CURRENT_UID" ] || [ "$GRP_ID" != "$CURRENT_GID" ]; then
        echo -e "WARNING: User with differing UID $CURRENT_UID/GID $CURRENT_GID already exists, most likely this container was started before with a different UID/GID. Re-create it to change UID/GID.\n"
    fi
else
    CURRENT_UID="$USER_ID"
    CURRENT_GID="$GRP_ID"
    echo -e "WARNING: Starting container processes as root. This has some security implications and goes against docker best practice.\n"
fi

# set $HOME
if [ "$CURRENT_UID" != "0" ]; then
    export USERNAME=user
    export HOME=/home/"$USERNAME"
else
    export USERNAME=root
    export HOME=/root
fi

sleep 5
zend_oo_IP="$(dig A +short zend_oo)"
if [ -z "$zend_oo_IP" ] || [ -z "$RPC_USER" ] || [ -z "$RPC_PASSWORD" ] || [ -z "$RPC_PORT" ]; then
    echo "Error: Linked container named 'zend_oo' running image 'zencash/zen-node:v2.1.0-beta4-6e1224ddf' and RPC_USER, RPC_PASSWORD and RPC_PORT variables required."
    exit 1
fi

i=0
while ! curl --data-binary '{"jsonrpc": "1.0", "id":"curltest", "method": "getblockcount", "params": [] }' -H 'content-type: text/plain;' "http://${RPC_USER}:${RPC_PASSWORD}@${zend_oo_IP}:${RPC_PORT}/" >/dev/null  2>&1; do
   echo "Waiting for 'zend_oo' container to be ready."
   sleep 5
   i="$((i+1))"
   if [ "$i" -gt 48 ]; then
       echo "Error: 'zend_oo' container not ready after 4 minutes."
       exit 1
   fi
done

RPC_USER=""
RPC_PASSWORD=""
RPC_PORT=""
unset RPC_USER
unset RPC_PASSWORD
unset RPC_PORT

cp /simpleapp/config/docker_simpleapp_settings.conf /simpleapp/config/simpleapp_settings.conf
sed -i "s/zend_oo_IP/$zend_oo_IP/g" "/simpleapp/config/simpleapp_settings.conf"
chown -fR "$CURRENT_UID":"$CURRENT_GID" /simpleapp
path_to_jemalloc="$(ldconfig -p | grep "$(arch)" | grep 'libjemalloc\.so\.1$' | tr -d ' ' | cut -d '>' -f 2)"
export LD_PRELOAD="$path_to_jemalloc:$LD_PRELOAD"

echo "Username: $USERNAME, UID: $CURRENT_UID, GID: $CURRENT_GID"
echo "zend_oo container IP: $zend_oo_IP"
echo "LD_PRELOAD: $LD_PRELOAD"
echo "Starting simple app..."

if [ "$USERNAME" = "user" ]; then
    exec /usr/sbin/gosu user "$@"
else
    exec "$@"
fi

