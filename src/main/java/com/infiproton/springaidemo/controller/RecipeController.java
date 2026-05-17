package com.infiproton.springaidemo.controller;

import com.infiproton.springaidemo.service.RecipeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
class RecipeController {

    @Autowired
    RecipeService recipeService;

    @GetMapping("/recipe")
    public String generateRecipe(@RequestParam String dish) {
        // Step 1: generate draft recipe
        String draft = recipeService.getDraftRecipe(dish);
        // Step 2: refine into structured JSON
        String json = recipeService.refineRecipe(draft);
        return json;
    }
}
