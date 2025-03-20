package com.neovolt.evergen;

import com.neovolt.evergen.service.SqsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Evergen应用程序入口类
 * 
 * 该应用程序实现了与ByteWatt VPP平台的集成，处理设备命令、遥测数据和上下线请求。
 */
@Slf4j
@SpringBootApplication
@EnableScheduling
@RequiredArgsConstructor
public class EvergenApplication implements CommandLineRunner {

    private final SqsService sqsService;

    public static void main(String[] args) {
        SpringApplication.run(EvergenApplication.class, args);
    }

    @Override
    public void run(String... args) {
        log.info("Starting Evergen Application...");
        log.info("正在验证SQS连接...");
        
        // 检查SQS可用性
        boolean sqsAvailable = sqsService.checkSqsAvailability();
        
        if (sqsAvailable) {
            log.info("SQS连接验证成功，启动消息轮询...");
            // 初始轮询（如果SQS可用）
            sqsService.pollAllQueues();
        } else {
            log.warn("SQS连接不可用，应用程序将在后台定期尝试重新连接");
            log.warn("确保LocalStack或AWS SQS服务已启动，并且配置正确");
            log.info("应用程序将继续运行，并在SQS可用时自动开始处理消息");
        }
        
        log.info("Evergen应用程序启动完成");
        
        // 应用程序将继续运行，由于@EnableScheduling注解，
        // SqsService中的定时任务将继续尝试连接SQS并处理消息
    }
}
