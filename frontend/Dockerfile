FROM crpi-skinyl3l0124ry6m.cn-beijing.personal.cr.aliyuncs.com/lxf_ai/node:20-alpine AS build
WORKDIR /app
# Vite 打包 + gzip 压缩峰值内存高；容器内存不足时会在 rendering chunks 被 OOM SIGKILL
ENV NODE_OPTIONS=--max-old-space-size=2048
# 镜像内由 nginx 做 gzip；构建阶段关闭 vite-plugin-compression 以降低内存
ENV VITE_BUILD_COMPRESS=
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ .
RUN npm run build:prod

FROM crpi-skinyl3l0124ry6m.cn-beijing.personal.cr.aliyuncs.com/lxf_ai/nginx:1.27-alpine
ENV BACKEND_HOST=backend
COPY frontend/nginx.conf /etc/nginx/templates/default.conf.template
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
