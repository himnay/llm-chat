package com.org.llm.model;

import java.util.List;

public record TravelPlan(String city, int days, List<DayPlan> itinerary) {
}
