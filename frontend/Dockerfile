FROM crpi-skinyl3l0124ry6m.cn-beijing.personal.cr.aliyuncs.com/lxf_ai/node:20-alpine AS build
WORKDIR /app

# 2C4G 云主机：Node 堆不宜过大（需留给 esbuild/Rollup 子进程），建议宿主机加 2G swap
# 构建参数 LOW_MEM_BUILD=1（默认开启）| NODE_HEAP_MB 可按机器调整（1024~1536）
ARG LOW_MEM_BUILD=1
ARG NODE_HEAP_MB=1280

ENV NODE_ENV=production
ENV CI=true
ENV NODE_OPTIONS=--max-old-space-size=${NODE_HEAP_MB}
ENV VITE_LOW_MEM_BUILD=${LOW_MEM_BUILD}
# 镜像内由 nginx 做 gzip；构建阶段关闭 vite-plugin-compression
ENV VITE_BUILD_COMPRESS=
# 限制 libuv 线程池，避免 2 核机器上并行过高
ENV UV_THREADPOOL_SIZE=2

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund --prefer-offline
COPY frontend/ .
RUN npm run build:prod

FROM crpi-skinyl3l0124ry6m.cn-beijing.personal.cr.aliyuncs.com/lxf_ai/nginx:1.27-alpine
ENV BACKEND_HOST=backend
COPY frontend/nginx.conf /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
