package com.neovolt.evergen.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProcessor {

    public void processMessage(String message) {
        log.info("Processing message: {}", message);
        // TODO: 实现消息处理逻辑
    }
}
