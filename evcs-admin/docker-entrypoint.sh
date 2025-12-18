#!/bin/sh
set -eu

if [ -z "${EVCS_GATEWAY_IP:-}" ]; then
  echo "ERROR: EVCS_GATEWAY_IP is required" >&2
  exit 1
fi

# Render Nginx config template without requiring envsubst/gettext.
# Template contains literal ${EVCS_GATEWAY_IP} placeholder.
sed -e "s|\${EVCS_GATEWAY_IP}|${EVCS_GATEWAY_IP}|g" \
  /etc/nginx/conf.d/default.conf.template \
  > /etc/nginx/conf.d/default.conf

exec nginx -g 'daemon off;'
