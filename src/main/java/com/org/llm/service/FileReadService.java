package com.org.llm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FileReadService {

    private final ChatClient chatClient;

    public String readFile(String fileName, String message) {
        Resource fileResource = new ClassPathResource("files/" + fileName);
        return chatClient.prompt()
                .user(userSpec -> userSpec
                        .text(message)
                        .media(MediaType.APPLICATION_PDF, fileResource))
                .call()
                .content();
    }

}
