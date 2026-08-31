package ai.devops.common.model;

public enum RiskLevel {
    READ_ONLY,
    LOW_RISK,
    MEDIUM_RISK,
    HIGH_RISK,
    CRITICAL;

    public boolean isMutating() {
        return this != READ_ONLY;
    }

    public boolean requiresProductionApproval() {
        return this == MEDIUM_RISK || this == HIGH_RISK || this == CRITICAL;
    }
}
