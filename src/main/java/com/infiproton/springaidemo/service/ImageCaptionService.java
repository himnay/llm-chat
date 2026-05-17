package com.infiproton.springaidemo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

@Service
public class ImageCaptionService {

    private final ChatClient chatClient;

    public ImageCaptionService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String captionImage(String imageName, String message) {
        Resource imageResource = new ClassPathResource("images/" + imageName);
        return chatClient.prompt()
                .user(userSpec -> userSpec
                        .text(message)
                        .media(MimeTypeUtils.IMAGE_JPEG, imageResource))
                .call()
                .content();
    }

}
