package com.org.llm.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecipeService {

    private final ChatClient chatClient;

    public String getDraftRecipe(String dish) {
        return chatClient.prompt()
                .user(u -> u.text("Write a recipe for {dish}. Include ingredients and preparation steps.")
                        .param("dish", dish))
                .call()
                .content();
    }

    public String refineRecipe(String draft) {
        return chatClient.prompt()
                .system("You are a recipe formatter. Convert recipes into JSON with keys: 'dish', 'ingredients', 'steps'.")
                .user(u -> u.text("Here is the recipe:\n{draft}").param("draft", draft))
                .call()
                .content();
    }
}
