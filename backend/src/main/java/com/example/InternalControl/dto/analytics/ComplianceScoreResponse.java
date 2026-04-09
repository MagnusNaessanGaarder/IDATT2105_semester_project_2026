package com.example.InternalControl.dto.analytics;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ComplianceScoreResponse {

    private double currentScore;
    private String status;
    private LocalDate calculatedAt;
    private List<ScoreComponent> components;

    public ComplianceScoreResponse() {
    }

    @Data
    public static class ScoreComponent {
        private String name;
        private double weight;
        private double score;
        private String description;

        public ScoreComponent() {
        }

        public String getName() {
            return name;
        }

        public double getWeight() {
            return weight;
        }

        public double getScore() {
            return score;
        }

        public String getDescription() {
            return description;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setWeight(double weight) {
            this.weight = weight;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public double getCurrentScore() {
        return currentScore;
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getCalculatedAt() {
        return calculatedAt;
    }

    public List<ScoreComponent> getComponents() {
        return components;
    }

    public void setCurrentScore(double currentScore) {
        this.currentScore = currentScore;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCalculatedAt(LocalDate calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    public void setComponents(List<ScoreComponent> components) {
        this.components = components;
    }
}
