# AI博客后端

Spring Boot 多模块后端（系统管理 + 博客业务）。启动模块：`ruoyi-admin`。

## 构建与启动

```bash
cd backend
mvn -B -DskipTests package -pl ruoyi-admin -am
java -jar ruoyi-admin/target/ruoyi-admin.jar
```

默认账号：`admin` / `admin123`。冒烟：`curl -s http://localhost:8080/captchaImage`
