package ai.devops.common.exception;

import ai.devops.common.model.RiskLevel;
import java.util.Map;
import java.util.UUID;

public class ApprovalRequiredException extends BaseException {
    public ApprovalRequiredException(String action, String environmentName, RiskLevel riskLevel, UUID approvalRequestId) {
        super("APPROVAL_REQUIRED",
                String.format("Action '%s' on %s environment has risk level '%s' and requires explicit human approval",
                        action, environmentName, riskLevel),
                Map.of(
                        "action", action,
                        "environment", environmentName,
                        "riskLevel", riskLevel.name(),
                        "approvalRequestId", approvalRequestId != null ? approvalRequestId.toString() : ""
                ));
    }
}
