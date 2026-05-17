package com.infiproton.springaidemo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RecipeService {
    @Autowired
    private ChatClient chatClient;

    public String getDraftRecipe(String dish) {
        Prompt draftPrompt = new Prompt(
                new UserMessage("Write a recipe for " + dish + ". Include ingredients and preparation steps.")
        );
        return chatClient.prompt(draftPrompt).call().content();
    }

    public String refineRecipe(String draft) {
        Prompt refinePrompt = new Prompt(List.of(
                new SystemMessage("You are a recipe formatter. Convert recipes into JSON with keys: 'dish', 'ingredients', 'steps'."),
                new UserMessage("Here is the recipe:\n" + draft)
        ));
        return chatClient.prompt(refinePrompt).call().content();
    }
}
