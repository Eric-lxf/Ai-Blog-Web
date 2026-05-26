FROM crpi-skinyl3l0124ry6m.cn-beijing.personal.cr.aliyuncs.com/lxf_ai/node:20-alpine AS build
WORKDIR /app
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ .
RUN npm run build:prod

FROM crpi-skinyl3l0124ry6m.cn-beijing.personal.cr.aliyuncs.com/lxf_ai/nginx:1.27-alpine
COPY frontend/nginx.conf /etc/nginx/templates/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
