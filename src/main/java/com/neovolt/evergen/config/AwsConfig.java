package com.neovolt.evergen.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;

/**
 * AWS服务配置，创建SQS客户端连接
 */
@Configuration
public class AwsConfig {

    @Value("${cloud.aws.region.static}")
    private String region;

    @Value("${cloud.aws.endpoint.sqs:#{null}}")
    private String sqsEndpoint;

    @Value("${cloud.aws.credentials.access-key:#{null}}")
    private String accessKey;

    @Value("${cloud.aws.credentials.secret-key:#{null}}")
    private String secretKey;

    @Bean
    public AmazonSQS amazonSQS() {
        AmazonSQSClientBuilder builder = AmazonSQSClientBuilder.standard();
        
        // 设置区域
        builder.withRegion(region);
        
        // 如果有配置端点，则使用指定端点
        if (sqsEndpoint != null && !sqsEndpoint.isEmpty()) {
            builder.withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration(sqsEndpoint, region));
        }
        
        // 如果有配置访问密钥，则使用静态凭证
        if (accessKey != null && secretKey != null) {
            BasicAWSCredentials awsCredentials = new BasicAWSCredentials(accessKey, secretKey);
            builder.withCredentials(new AWSStaticCredentialsProvider(awsCredentials));
        }
        // 否则使用默认凭证提供程序链（可以从IAM角色获取凭证）
        
        return builder.build();
    }
}
