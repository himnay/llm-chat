package com.infiproton.springaidemo.service;

import com.infiproton.springaidemo.model.TravelPlan;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class TravelGuideService {

    private final ChatClient chatClient;
    @Value("classpath:prompts/travel-guide.st")
    private Resource travelGuideTemplate;

    TravelGuideService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public TravelPlan prepareTravelPlan(String city, Integer days) {
        PromptTemplate template = new PromptTemplate(travelGuideTemplate);

        Map<String, Object> params = Map.of(
                "city", city,
                "days", days
        );
        Prompt prompt = template.create(params);

        return chatClient.prompt(prompt)
                .call()
                .entity(TravelPlan.class);
    }

}
