package com.example.InternalControl.dto.analytics;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class ComplianceScoreResponse {

    private double currentScore;
    private String status;
    private LocalDate calculatedAt;
    private List<ScoreComponent> components;

    @Data
    @Builder
    public static class ScoreComponent {
        private String name;
        private double weight;
        private double score;
        private String description;
    }
}
