# Evergen VPP Integration

这是一个与Evergen虚拟电厂(VPP)平台集成的Java应用程序，用于处理设备命令、遥测数据以及设备的上线和下线流程。

## 项目结构

该项目采用Spring Boot框架开发，主要组件包括：

- `CloudEventService`: 用于创建、序列化和反序列化CloudEvent格式的消息
- `BytewattService`: 设备数据的来源，贝瓦的apicloud
- `TelemetryService`: 向sqs上传数据的消息
- `CommandService`: 从sqs取下发指令的消息
- 请求

## 支持的消息类型

- 命令消息：用于控制设备的运行模式（自消费、充电、放电等）
- 遥测数据：设备定期上报的运行状态数据
- 上线请求/响应：处理设备加入VPP的请求和响应
- 下线请求/响应：处理设备退出VPP的请求和响应

## 运行项目

1. 确保已安装JDK 17和Maven
2. 检查`application.yml`中的配置是否正确
3. 执行以下命令启动应用：

```bash
./mvnw spring-boot:run
```

## 消息处理流程

1. 应用程序启动后，会定期轮询SQS队列中的消息
2. 接收到消息后，会根据消息类型交由相应的处理器处理
3. 处理完成后，会生成响应并发送回Evergen平台

## 配置说明

主要配置项在`application.yml`文件中：

- AWS配置：包括证书、区域和SQS端点
- SQS队列配置：各种消息类型的队列URL
- ByteWatt API配置：API基础URL和密钥

## 测试

运行单元测试：

```bash
./mvnw test
```

## 生产环境部署注意事项

- 使用正确的AWS IAM角色和权限
- 使用适当的日志级别