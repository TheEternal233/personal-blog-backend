

# 个人博客后端系统 (Personal Blog Backend)

一个基于 Spring Boot 构建的现代化个人博客后端管理系统，提供完整的文章管理、用户认证、评论互动等功能支持。

## 技术栈

- **框架**: Spring Boot 3.x
- **数据库**: MyBatis Plus
- **缓存**: Redis
- **消息队列**: RabbitMQ
- **对象存储**: MinIO
- **安全**: Spring Security + JWT
- **API文档**: Knife4j (Swagger)

## 主要功能

### 文章管理
- 文章发布、编辑、删除
- 文章分类与标签管理
- 文章搜索（标题/内容）
- 热门/推荐/随机文章
- 文章访问统计

### 用户系统
- 用户注册/登录
- 第三方登录（Gitee、GitHub）
- 用户信息管理
- 邮箱验证

### 互动功能
- 评论系统（支持嵌套评论）
- 点赞/收藏
- 留言板
- 友链申请

### 系统管理
- 角色与权限管理
- 菜单管理
- 黑名单管理
- 操作日志
- 登录日志

### 服务监控
- 服务器状态监控（CPU、内存、JVM、磁盘）

## 项目结构

```
src/main/java/xyz/kuailemao/
├── annotation/          # 自定义注解
├── aop/              # 切面编程
├── config/            # 配置类
├── constants/         # 常量定义
├── controller/        # 控制器
├── domain/           # 实体与DTO
│   ├── dto/         # 数据传输对象
│   ├── entity/       # 实体类
│   ├── response/    # 响应对象
│   └── vo/         # 视图对象
├── enums/            # 枚举类
├── exceptions/       # 异常处理
├── filter/          # 过滤器
├── handler/        # 处理器
├── interceptor/    # 拦截器
├── mapper/         # 数据访问层
├── service/       # 业务逻辑层
│   └── impl/     # 服务实现
├── quartz/        # 定时任务
└── utils/        # 工具类
```

## 快速开始

### 环境要求

- JDK 17+
- MySQL 8.0+
- Redis 6.0+
- RabbitMQ 3.8+
- MinIO（可选）

### 配置文件

在 `src/main/resources/application.yml` 中配置数据库、Redis 等连接信息：

```yaml
server:
  port: 8088

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/blog
    username: root
    password: your_password
  redis:
    host: localhost
    port: 6379
```

### 运行项目

```bash
# 编译打包
mvn clean package -DskipTests

# 运行
java -jar blog-backend-0.0.1-SNAPSHOT.jar
```

### Docker 部署

```bash
docker build -t personal-blog-backend .
docker run -d -p 8088:8088 --name blog-backend personal-blog-backend
```

## API 文档

项目启动后访问: `http://localhost:8088/doc.html`

## 请求头说明

| 头名称 | 值 | 说明 |
|--------|-----|------|
| X-Client-Type | Frontend | 前台请求 |
| X-Client-Type | Backend | 后台请求 |

## 许可证

MIT License