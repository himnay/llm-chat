package com.org.llm.controller;

import com.org.llm.service.RecipeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/recipe")
@Tag(name = "Recipe", description = "AI-generated recipe creation endpoints")
class RecipeController {

    private final RecipeService recipeService;

    @Operation(summary = "Generate and refine an AI recipe for the given dish")
    @GetMapping
    public String generateRecipe(@NotBlank(message = "dish is required") @RequestParam String dish) {
        String draft = recipeService.getDraftRecipe(dish);
        return recipeService.refineRecipe(draft);
    }
}
