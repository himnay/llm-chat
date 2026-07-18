package com.org.llm.service;

import com.org.llm.backend.TravelPlanBackend;
import com.org.llm.model.TravelPlan;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Loads the travel-guide prompt template and delegates structured-plan generation to the
 * active {@link TravelPlanBackend} strategy (gateway or local).
 */
@Service
public class TravelGuideService {

    private final TravelPlanBackend travelPlanBackend;
    private final Resource travelGuideTemplate;

    public TravelGuideService(TravelPlanBackend travelPlanBackend,
                              @Value("classpath:prompts/travel-guide.st") Resource travelGuideTemplate) {
        this.travelPlanBackend = travelPlanBackend;
        this.travelGuideTemplate = travelGuideTemplate;
    }

    /** Returns the prepare travel plan. */
    public TravelPlan prepareTravelPlan(String city, Integer days) {
        PromptTemplate template = new PromptTemplate(travelGuideTemplate);
        return travelPlanBackend.plan(template, Map.of("city", city, "days", days));
    }
}
