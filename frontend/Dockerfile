# Default frontend image (2C4G-safe): requires frontend/dist built on host/CI first.
#   bash scripts/build-frontend-image.sh <tag>
# 8G+ in-docker vite build: docker build -f frontend/Dockerfile.full .
FROM alpine:3.20 AS check
COPY frontend/dist /dist
RUN test -f /dist/index.html || ( \
  echo "ERROR: frontend/dist/index.html not found." >&2; \
  echo "2C4G cannot run vite inside docker. Build dist on host first:" >&2; \
  echo "  cd frontend && npm ci && npm run build:prod" >&2; \
  echo "  bash scripts/build-frontend-image.sh <image-tag>" >&2; \
  exit 1 )

FROM crpi-skinyl3l0124ry6m.cn-beijing.personal.cr.aliyuncs.com/lxf_ai/nginx:1.27-alpine
ENV BACKEND_HOST=backend
COPY frontend/nginx.conf /etc/nginx/templates/default.conf.template
COPY --from=check /dist /usr/share/nginx/html
EXPOSE 80
