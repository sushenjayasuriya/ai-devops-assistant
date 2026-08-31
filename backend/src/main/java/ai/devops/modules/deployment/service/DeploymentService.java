package ai.devops.modules.deployment.service;

import ai.devops.common.exception.ResourceNotFoundException;
import ai.devops.modules.deployment.entity.DeploymentEntity;
import ai.devops.modules.deployment.repository.DeploymentRepository;
import ai.devops.security.rbac.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DeploymentService {

    private final DeploymentRepository deploymentRepository;

    public DeploymentService(DeploymentRepository deploymentRepository) {
        this.deploymentRepository = deploymentRepository;
    }

    @Transactional(readOnly = true)
    public List<DeploymentEntity> getDeployments(UUID environmentId) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        if (orgId == null) {
            return List.of();
        }

        if (environmentId != null) {
            return deploymentRepository.findByOrganizationIdAndEnvironmentIdOrderByStartedAtDesc(orgId, environmentId);
        }
        return deploymentRepository.findByOrganizationIdOrderByStartedAtDesc(orgId);
    }

    @Transactional(readOnly = true)
    public DeploymentEntity getDeploymentById(UUID id) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        if (orgId == null) {
            throw new ResourceNotFoundException("Deployment", id);
        }

        return deploymentRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Deployment", id));
    }
}
