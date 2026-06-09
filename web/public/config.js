// Runtime configuration. In dev, Vite serves this file as-is. In the container
// image it is regenerated from the API_BASE_URL env var at startup
// (see docker/render-config.sh), so one image works against any backend.
window.__APP_CONFIG__ = { apiBaseUrl: "/api" };
