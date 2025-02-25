package com.neovolt.evergen.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SqsService {

    public void receiveMessages() {
        log.info("Receiving messages...");
        // TODO: 实现消息接收逻辑
    }
}
