# GoldSim 贵金属交易模拟系统

基于 Spring Cloud Alibaba 微服务架构的贵金属交易模拟平台，支持真实金价行情同步、模拟交易、实物商城、订单支付、资讯推送等业务场景。

## 技术栈

| 类别 | 技术 | 版本 |
|------|------|------|
| 语言 | Java | 21 |
| 框架 | Spring Boot / Spring Cloud | 3.2.12 / 2023.0.3 |
| 微服务 | Spring Cloud Alibaba (Nacos, Sentinel, Seata) | 2023.0.1.0 |
| 网关 | Spring Cloud Gateway | — |
| ORM | MyBatis-Plus | 3.5.5 |
| 数据库 | MySQL 8.4 / TDengine 3.2.11 | — |
| 缓存 | Redis + Redisson | 3.37.0 |
| 消息队列 | RabbitMQ (Spring AMQP) | — |
| 分布式事务 | Seata (TCC 模式) | — |
| 任务调度 | XXL-JOB | 2.4.1 |
| 认证 | Sa-Token (Redis 持久化) | 1.39.0 |
| 实时通信 | WebSocket (Netty) | 4.1.112 |
| 文件存储 | Alibaba Cloud OSS | 3.18.0 |
| 支付 | Alipay SDK | 4.38.10 |
| 搜索 | Easy-ES (Elasticsearch) | 2.1.0 |
| 文档 | Knife4j (OpenAPI 3) | 4.5.0 |
| 工具 | Lombok, Hutool, Guava | — |

## 项目模块

```
GoldSim
├── gold-common               公共模块：通用配置、异常、工具类、拦截器
├── gold-api                  Feign 接口模块：跨服务调用契约 + DTO + 降级
├── gold-mall-api             商城共享模块：商品/活动/规格 DTO & VO
├── gold-gateway              网关：路由分发、认证鉴权、限流熔断
├── gold-user-service         用户服务：注册、登录、实名认证、双账户体系
├── gold-market-service       行情服务：金价同步、K线生成、WebSocket 推送
├── gold-simulation-service   模拟交易：虚拟资金、持仓管理、实时排行榜
├── gold-flash-sale-service   秒杀服务（骨架，待实现）
├── gold-mall-service         商城前台：商品展示、活动展示、下单
├── gold-store-service        门店服务：门店管理、附近门店、到店核销
├── gold-news-service         资讯服务：新闻爬取、ES 全文检索、AI 摘要
├── gold-admin-service        管理后台：商品管理、活动管理
├── gold-order-service        订单服务：订单流程、支付宝支付、MQ 消息
├── gold-product-service      商品服务：商品/活动/库存管理、Seata TCC
└── gold-file-service         文件服务：OSS 上传
```

### 模块依赖关系

```
gold-common  ────────────────────────────────────────────────  基础层
  ├── gold-api         ← (Feign 接口 + DTO)
  │     ├── gold-user-service
  │     ├── gold-market-service
  │     ├── gold-simulation-service
  │     ├── gold-mall-service
  │     ├── gold-store-service
  │     ├── gold-order-service
  │     ├── gold-product-service
  │     └── gold-admin-service
  ├── gold-mall-api    ← (商城 DTO/VO)
  │     ├── gold-mall-service
  │     ├── gold-product-service
  │     └── gold-admin-service
  ├── gold-gateway
  ├── gold-file-service
  ├── gold-news-service
  └── gold-flash-sale-service
```

## 环境依赖

| 组件 | 说明 |
|------|------|
| Nacos | 服务注册发现 + 配置中心，`192.168.153.129:8848` |
| MySQL | 业务数据库 |
| TDengine | 时序数据库（K线、行情快照） |
| Redis | 缓存 + 分布式锁 + 会话 |
| RabbitMQ | 消息队列（订单延时、库存扣减、死信队列） |
| Elasticsearch | 资讯全文检索 |
| Seata Server | 分布式事务协调器 |
| XXL-JOB Admin | 分布式任务调度中心 |
| Alibaba Cloud OSS | 文件存储 |
| Sentinel Dashboard | 限流熔断监控（可选，`192.168.43.222:8090`） |

## 快速开始

### 1. 启动基础设施

确保 MySQL、Redis、RabbitMQ、Nacos 等服务已启动并可访问。

### 2. 编译项目

```bash
mvn clean package -DskipTests
```

### 3. 启动服务（建议顺序）

```bash
# 1. 基础设施依赖的服务先启动
mvn spring-boot:run -pl gold-gateway
mvn spring-boot:run -pl gold-user-service

# 2. 被依赖的服务
mvn spring-boot:run -pl gold-product-service
mvn spring-boot:run -pl gold-market-service
mvn spring-boot:run -pl gold-store-service
mvn spring-boot:run -pl gold-file-service

# 3. 上层业务服务
mvn spring-boot:run -pl gold-order-service
mvn spring-boot:run -pl gold-mall-service
mvn spring-boot:run -pl gold-admin-service
mvn spring-boot:run -pl gold-simulation-service
mvn spring-boot:run -pl gold-news-service
```

### 4. 访问

| 入口 | 地址 |
|------|------|
| 网关 | `http://localhost:8080` |
| 用户服务 | `http://localhost:8080/api/user/**` |
| 行情服务 | `http://localhost:8080/api/market/**` |
| 行情 WebSocket | `ws://localhost:8080/ws/market` |
| 模拟交易 | `http://localhost:8080/api/simulation/**` |
| 排行榜 WebSocket | `ws://localhost:8080/ws/leaderboard` |
| 商城 | `http://localhost:8080/api/mall/**` |
| 管理后台 | `http://localhost:8080/api/admin/**` |
| 订单 | `http://localhost:8080/api/order/**` |
| 文件上传 | `http://localhost:8080/api/file/**` |
| 资讯 | `http://localhost:8080/api/news/**` |
| 门店 | `http://localhost:8080/api/store/**` |

## API 文档

启动任意服务后，访问 Knife4j 文档页面：

```
http://localhost:{port}/doc.html
```

## 核心业务流程

### 模拟交易

```
委托请求 → 风控校验 → 创建持仓 → 价格波动 → 浮动盈亏
                                    ↓
                    WebSocket 实时推送排行榜
```

### 商城订单（TCC 分布式事务）

```
下单 → Try: 冻结库存(Seata TCC)
     → 创建订单(本地事务)
     → 发起支付(支付宝)
     → 支付回调 → Confirm: 扣减库存 + 确认订单
     → 超时未付 → Cancel: 解冻库存 + 取消订单(MQ 延时队列)
```

### 库存扣减（MQ 可靠消息）

```
订单支付成功 → 发送 MQ 消息(本地消息表 + 定时重试)
             → 消费者扣减库存
             → 失败进入死信队列(人工/告警处理)
```

## 项目配置说明

### 服务端口

| 服务 | 端口 |
|------|------|
| gold-gateway | 8080 |
| gold-user-service | 8081 |
| gold-market-service | 8082 |
| gold-simulation-service | 8083 |
| gold-mall-service | 8084 |
| gold-store-service | 8085 |
| gold-news-service | 8086 |
| gold-admin-service | 8087 |
| gold-order-service | 8088 |
| gold-product-service | 8089 |
| gold-file-service | 8090 |
| gold-flash-sale-service | 8091 |

### 环境切换

目前所有配置在本地 `application.yaml` 中。生产部署时建议：

1. 将敏感信息（数据库密码、API 密钥等）移至环境变量或 Nacos 配置中心
2. 创建 `application-prod.yaml` 配置生产环境参数
3. 启动时指定 profile：`--spring.profiles.active=prod`

## 目录结构约定

```
{module}/
├── pom.xml
└── src/main/java/com/gold/{module}/
    ├── {Module}Application.java    启动类
    ├── config/                      配置类
    ├── controller/                  控制器（对外 REST API）
    ├── service/                     服务接口
    ├── service/impl/                服务实现
    ├── domain/
    │   ├── po/ / pojo/ / entity/    数据库实体
    │   ├── dto/                     数据传输对象
    │   └── vo/                      视图对象
    ├── mapper/                      MyBatis Mapper 接口
    ├── consumer/                    MQ 消费者
    ├── producer/                    MQ 生产者
    ├── tcc/                         Seata TCC 资源
    ├── job/                         XXL-JOB 定时任务
    └── util/                        工具类
```

## 注意事项

1. **gold-file-service** 在父 POM `<modules>` 中重复声明，不影响功能但会产生 Maven 警告
2. **gold-flash-sale-service** 为骨架模块，仅含 POM 和空配置，未实现业务代码
3. **gold-admin-service** / **gold-mall-service** 为纯代理层，通过 Feign 调用 product-service，无本地数据库
4. 各服务需配置唯一的 `snowflake.worker-id` 避免分布式 ID 冲突
5. 生产环境部署前请检查安全相关配置（认证排除路径、用户身份校验等）