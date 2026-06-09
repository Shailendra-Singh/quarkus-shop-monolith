# syntax=docker/dockerfile:1

# ---- Build stage: compile the Vite app ----
FROM docker.io/library/node:20-alpine AS build
WORKDIR /app

# Install dependencies first for better layer caching.
COPY package.json package-lock.json* ./
RUN npm install

# Build the static site.
COPY . .
RUN npm run build

# ---- Runtime stage: serve static files with nginx ----
FROM docker.io/library/nginx:alpine AS runtime

# Built assets. nginx serves on 5173 and reverse-proxies /api to the backend.
COPY --from=build /app/dist /usr/share/nginx/html

# nginx renders this template to conf.d/default.conf at startup, substituting
# ${API_UPSTREAM}. Override at runtime with -e API_UPSTREAM=...
COPY docker/default.conf.template /etc/nginx/templates/default.conf.template
ENV API_UPSTREAM=http://127.0.0.1:8080

# Regenerate config.js (browser-side API base) from $API_BASE_URL at start.
COPY docker/render-config.sh /docker-entrypoint.d/40-render-config.sh
RUN chmod +x /docker-entrypoint.d/40-render-config.sh

EXPOSE 5173
# nginx:alpine's own entrypoint envsubsts templates and runs /docker-entrypoint.d/*.sh then starts nginx.
