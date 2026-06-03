package com.org.llm.controller;

import com.org.llm.service.RecipeService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
class RecipeController {

    private final RecipeService recipeService;

    @GetMapping("/recipe")
    public String generateRecipe(@NotBlank(message = "dish is required") @RequestParam String dish) {
        String draft = recipeService.getDraftRecipe(dish);
        return recipeService.refineRecipe(draft);
    }
}
