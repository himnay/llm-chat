package com.infiproton.springaidemo.service;

import com.infiproton.springaidemo.tool.ContactsTool;
import com.infiproton.springaidemo.tool.WeatherTools;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final VectorStore vectorStore;
    private final WeatherTools weatherTools;
    private final ContactsTool contactsTool;

    public String chat(String conversationId, String message) {
        // generate id if not passed
        String convId = (conversationId == null || conversationId.isBlank())
                ? UUID.randomUUID().toString()
                : conversationId;

        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);

        Prompt prompt = new Prompt(List.of(
                new SystemMessage("Today’s date is " + today + ". " +
                        "You are a friendly travel guide. Suggest 3 attractions and 1 food item.")
        ));

        MessageChatMemoryAdvisor messageChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                .conversationId(convId)
                .build();
        return chatClient.prompt(prompt)
                .advisors(new QuestionAnswerAdvisor(vectorStore), messageChatMemoryAdvisor)
                .tools(weatherTools, contactsTool)
                .user(message)
                .call().content();
    }

    public Flux<String> streamChat(String message) {
        Prompt prompt = new Prompt(new UserMessage(message));

        return chatClient.prompt(prompt)
                .advisors(new QuestionAnswerAdvisor(vectorStore))
                .tools(weatherTools, contactsTool)
                .stream()
                .content();
    }

}

