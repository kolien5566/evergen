package com.neovolt.evergen.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequest;
import com.amazonaws.services.sqs.model.DeleteMessageBatchRequestEntry;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.GetQueueAttributesRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import com.amazonaws.services.sqs.model.SendMessageRequest;

import io.cloudevents.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * SQS服务，提供与AWS SQS队列交互的功能
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SqsService {

    private final AmazonSQS sqsClient;
    private final CloudEventService cloudEventService;
    
    @Autowired
    private CommandService commandService;
    
    @Autowired
    private BoardingService boardingService;
    
    @Value("${sqs.queues.command}")
    private String commandQueueUrl;
    
    @Value("${sqs.queues.telemetry}")
    private String telemetryQueueUrl;
    
    @Value("${sqs.queues.onboarding}")
    private String onboardingQueueUrl;
    
    @Value("${sqs.queues.offboarding}")
    private String offboardingQueueUrl;
    
    /**
     * 检查SQS可用性
     * 
     * @return SQS是否可用
     */
    public boolean checkSqsAvailability() {
        try {
            // 尝试获取队列属性，如果成功则表示SQS可用
            sqsClient.getQueueAttributes(
                    new GetQueueAttributesRequest()
                        .withQueueUrl(commandQueueUrl)
                        .withAttributeNames("QueueArn"));
            return true;
        } catch (Exception e) {
            log.warn("SQS not available: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * 轮询所有队列
     * 启动定时任务，定期轮询所有队列
     */
    public void pollAllQueues() {
        log.info("Starting to poll all queues");
        // 初始轮询
        pollCommandQueue();
        pollOnboardingQueue();
        pollOffboardingQueue();
    }
    
    /**
     * 定时轮询命令队列
     * 每10秒执行一次
     */
    @Scheduled(fixedRate = 10000)
    public void pollCommandQueue() {
        try {
            log.debug("Polling command queue");
            commandService.processCommands(10, 5);
        } catch (Exception e) {
            log.error("Error polling command queue: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 定时轮询上线请求队列
     * 每30秒执行一次
     */
    @Scheduled(fixedRate = 30000)
    public void pollOnboardingQueue() {
        try {
            log.debug("Polling onboarding queue");
            boardingService.processOnboardingRequests(10, 5);
        } catch (Exception e) {
            log.error("Error polling onboarding queue: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 定时轮询下线请求队列
     * 每30秒执行一次
     */
    @Scheduled(fixedRate = 30000)
    public void pollOffboardingQueue() {
        try {
            log.debug("Polling offboarding queue");
            boardingService.processOffboardingRequests(10, 5);
        } catch (Exception e) {
            log.error("Error polling offboarding queue: {}", e.getMessage(), e);
        }
    }

    /**
     * 发送消息到SQS队列
     *
     * @param queueUrl 队列URL
     * @param event CloudEvent事件
     */
    public void sendMessage(String queueUrl, CloudEvent event) {
        try {
            String messageBody = cloudEventService.serializeToString(event);
            SendMessageRequest sendMessageRequest = new SendMessageRequest()
                    .withQueueUrl(queueUrl)
                    .withMessageBody(messageBody);
            
            sqsClient.sendMessage(sendMessageRequest);
            log.info("Message sent to queue: {}", queueUrl);
        } catch (Exception e) {
            log.error("Failed to send message to queue {}: {}", queueUrl, e.getMessage());
            throw new RuntimeException("Failed to send message to SQS", e);
        }
    }

    /**
     * 接收SQS队列中的消息
     *
     * @param queueUrl 队列URL
     * @param maxMessages 最大消息数量
     * @param waitTimeSeconds 等待时间（秒）
     * @return 接收到的消息列表
     */
    public List<Message> receiveMessages(String queueUrl, int maxMessages, int waitTimeSeconds) {
        try {
            ReceiveMessageRequest receiveMessageRequest = new ReceiveMessageRequest()
                    .withQueueUrl(queueUrl)
                    .withMaxNumberOfMessages(maxMessages)
                    .withWaitTimeSeconds(waitTimeSeconds);
            
            ReceiveMessageResult receiveMessageResult = sqsClient.receiveMessage(receiveMessageRequest);
            List<Message> messages = receiveMessageResult.getMessages();
            
            if (!messages.isEmpty()) {
                log.info("Received {} messages from queue: {}", messages.size(), queueUrl);
            }
            
            return messages;
        } catch (Exception e) {
            log.error("Failed to receive messages from queue {}: {}", queueUrl, e.getMessage());
            throw new RuntimeException("Failed to receive messages from SQS", e);
        }
    }

    /**
     * 删除SQS队列中的消息
     *
     * @param queueUrl 队列URL
     * @param receiptHandle 消息接收句柄
     */
    public void deleteMessage(String queueUrl, String receiptHandle) {
        try {
            DeleteMessageRequest deleteMessageRequest = new DeleteMessageRequest()
                    .withQueueUrl(queueUrl)
                    .withReceiptHandle(receiptHandle);
            
            sqsClient.deleteMessage(deleteMessageRequest);
            log.debug("Deleted message from queue: {}", queueUrl);
        } catch (Exception e) {
            log.error("Failed to delete message from queue {}: {}", queueUrl, e.getMessage());
            throw new RuntimeException("Failed to delete message from SQS", e);
        }
    }

    /**
     * 批量删除SQS队列中的消息
     *
     * @param queueUrl 队列URL
     * @param messages 要删除的消息列表
     */
    public void deleteMessages(String queueUrl, List<Message> messages) {
        if (messages.isEmpty()) {
            return;
        }
        
        try {
            List<DeleteMessageBatchRequestEntry> entries = messages.stream()
                    .map(message -> new DeleteMessageBatchRequestEntry()
                            .withId(message.getMessageId())
                            .withReceiptHandle(message.getReceiptHandle()))
                    .collect(Collectors.toList());
            
            DeleteMessageBatchRequest batchRequest = new DeleteMessageBatchRequest()
                    .withQueueUrl(queueUrl)
                    .withEntries(entries);
            
            sqsClient.deleteMessageBatch(batchRequest);
            log.info("Deleted {} messages from queue: {}", messages.size(), queueUrl);
        } catch (Exception e) {
            log.error("Failed to batch delete messages from queue {}: {}", queueUrl, e.getMessage());
            throw new RuntimeException("Failed to batch delete messages from SQS", e);
        }
    }

    /**
     * 发送命令消息到命令队列
     *
     * @param event CloudEvent事件
     */
    public void sendCommandMessage(CloudEvent event) {
        sendMessage(commandQueueUrl, event);
    }

    /**
     * 发送遥测消息到遥测队列
     *
     * @param event CloudEvent事件
     */
    public void sendTelemetryMessage(CloudEvent event) {
        sendMessage(telemetryQueueUrl, event);
    }

    /**
     * 发送上线请求消息到上线队列
     *
     * @param event CloudEvent事件
     */
    public void sendOnboardingMessage(CloudEvent event) {
        sendMessage(onboardingQueueUrl, event);
    }

    /**
     * 发送下线请求消息到下线队列
     *
     * @param event CloudEvent事件
     */
    public void sendOffboardingMessage(CloudEvent event) {
        sendMessage(offboardingQueueUrl, event);
    }

    /**
     * 接收命令队列中的消息
     *
     * @param maxMessages 最大消息数量
     * @param waitTimeSeconds 等待时间（秒）
     * @return 接收到的消息列表
     */
    public List<Message> receiveCommandMessages(int maxMessages, int waitTimeSeconds) {
        return receiveMessages(commandQueueUrl, maxMessages, waitTimeSeconds);
    }

    /**
     * 接收遥测队列中的消息
     *
     * @param maxMessages 最大消息数量
     * @param waitTimeSeconds 等待时间（秒）
     * @return 接收到的消息列表
     */
    public List<Message> receiveTelemetryMessages(int maxMessages, int waitTimeSeconds) {
        return receiveMessages(telemetryQueueUrl, maxMessages, waitTimeSeconds);
    }

    /**
     * 接收上线响应队列中的消息
     *
     * @param maxMessages 最大消息数量
     * @param waitTimeSeconds 等待时间（秒）
     * @return 接收到的消息列表
     */
    public List<Message> receiveOnboardingMessages(int maxMessages, int waitTimeSeconds) {
        return receiveMessages(onboardingQueueUrl, maxMessages, waitTimeSeconds);
    }

    /**
     * 接收下线响应队列中的消息
     *
     * @param maxMessages 最大消息数量
     * @param waitTimeSeconds 等待时间（秒）
     * @return 接收到的消息列表
     */
    public List<Message> receiveOffboardingMessages(int maxMessages, int waitTimeSeconds) {
        return receiveMessages(offboardingQueueUrl, maxMessages, waitTimeSeconds);
    }
}
