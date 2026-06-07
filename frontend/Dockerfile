# Full in-docker build. Needs ~2G+ RAM for the build container (vite transform peak).
# 2C4G cloud hosts: use deploy.sh or Dockerfile.prebuilt instead.
FROM crpi-skinyl3l0124ry6m.cn-beijing.personal.cr.aliyuncs.com/lxf_ai/node:20-alpine AS build
WORKDIR /app

ARG LOW_MEM_BUILD=1
ARG NODE_HEAP_MB=1280
ARG NPM_REGISTRY=https://registry.npmmirror.com

ENV CI=true
ENV NODE_OPTIONS=--max-old-space-size=${NODE_HEAP_MB}
ENV VITE_LOW_MEM_BUILD=${LOW_MEM_BUILD}
ENV VITE_BUILD_COMPRESS=
ENV UV_THREADPOOL_SIZE=2

COPY frontend/package.json frontend/package-lock.json frontend/.npmrc ./
RUN npm config set registry "${NPM_REGISTRY}" \
  && npm config set fetch-retries 5 \
  && npm config set fetch-retry-mintimeout 20000 \
  && npm config set fetch-retry-maxtimeout 120000 \
  && set -e; \
     for attempt in 1 2 3 4 5; do \
       echo ">>> npm ci attempt ${attempt}/5 (registry=${NPM_REGISTRY})"; \
       npm ci --no-audit --no-fund --include=dev && break; \
       if [ "$attempt" = "5" ]; then exit 1; fi; \
       sleep $((attempt * 15)); \
     done

COPY frontend/ .
RUN if [ -f dist/index.html ]; then \
      echo ">>> skip vite build, use prebuilt frontend/dist"; \
    else \
      echo ">>> vite build LOW_MEM_BUILD=${LOW_MEM_BUILD} NODE_OPTIONS=${NODE_OPTIONS}" \
      && npm run build:prod; \
    fi

FROM crpi-skinyl3l0124ry6m.cn-beijing.personal.cr.aliyuncs.com/lxf_ai/nginx:1.27-alpine
ENV BACKEND_HOST=backend
COPY frontend/nginx.conf /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
