package com.infiproton.springaidemo.model;

public record ChatRequest(String conversationId, String message, String imageName) {
}
