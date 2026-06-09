#!/bin/sh
# Generates the app's runtime config from environment at container start.
# Runs automatically via nginx:alpine's /docker-entrypoint.d mechanism.
set -e

: "${API_BASE_URL:=/api}"

cat > /usr/share/nginx/html/config.js <<EOF
window.__APP_CONFIG__ = { apiBaseUrl: "${API_BASE_URL}" };
EOF

echo "render-config: apiBaseUrl=${API_BASE_URL}"
