package ai.devops.modules.ai.core;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FOIRResponse {
    private String summary;
    private List<String> facts;
    private List<String> observations;
    private List<String> inferences;
    private List<Recommendation> recommendations;
    private double confidenceScore;
    private List<ToolCallRecord> toolExecutionTrail;

    public static class Recommendation {
        private String action;
        private Map<String, Object> parameters;
        private String riskLevel;
        private boolean requiresApproval;
        private String rationale;
        private String expectedImpact;

        public Recommendation() {}

        public Recommendation(String action, Map<String, Object> parameters, String riskLevel, boolean requiresApproval, String rationale, String expectedImpact) {
            this.action = action;
            this.parameters = parameters;
            this.riskLevel = riskLevel;
            this.requiresApproval = requiresApproval;
            this.rationale = rationale;
            this.expectedImpact = expectedImpact;
        }

        public String getAction() {
            return action;
        }

        public void setAction(String action) {
            this.action = action;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }

        public String getRiskLevel() {
            return riskLevel;
        }

        public void setRiskLevel(String riskLevel) {
            this.riskLevel = riskLevel;
        }

        public boolean isRequiresApproval() {
            return requiresApproval;
        }

        public void setRequiresApproval(boolean requiresApproval) {
            this.requiresApproval = requiresApproval;
        }

        public String getRationale() {
            return rationale;
        }

        public void setRationale(String rationale) {
            this.rationale = rationale;
        }

        public String getExpectedImpact() {
            return expectedImpact;
        }

        public void setExpectedImpact(String expectedImpact) {
            this.expectedImpact = expectedImpact;
        }
    }

    public static class ToolCallRecord {
        private String toolName;
        private Map<String, Object> parameters;
        private Object result;
        private boolean success;

        public ToolCallRecord() {}

        public ToolCallRecord(String toolName, Map<String, Object> parameters, Object result, boolean success) {
            this.toolName = toolName;
            this.parameters = parameters;
            this.result = result;
            this.success = success;
        }

        public String getToolName() {
            return toolName;
        }

        public void setToolName(String toolName) {
            this.toolName = toolName;
        }

        public Map<String, Object> getParameters() {
            return parameters;
        }

        public void setParameters(Map<String, Object> parameters) {
            this.parameters = parameters;
        }

        public Object getResult() {
            return result;
        }

        public void setResult(Object result) {
            this.result = result;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }
    }

    public FOIRResponse() {}

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getFacts() {
        return facts;
    }

    public void setFacts(List<String> facts) {
        this.facts = facts;
    }

    public List<String> getObservations() {
        return observations;
    }

    public void setObservations(List<String> observations) {
        this.observations = observations;
    }

    public List<String> getInferences() {
        return inferences;
    }

    public void setInferences(List<String> inferences) {
        this.inferences = inferences;
    }

    public List<Recommendation> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<Recommendation> recommendations) {
        this.recommendations = recommendations;
    }

    public double getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(double confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public List<ToolCallRecord> getToolExecutionTrail() {
        return toolExecutionTrail;
    }

    public void setToolExecutionTrail(List<ToolCallRecord> toolExecutionTrail) {
        this.toolExecutionTrail = toolExecutionTrail;
    }
}
