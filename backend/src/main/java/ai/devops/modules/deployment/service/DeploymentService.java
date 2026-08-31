package ai.devops.modules.deployment.service;

import ai.devops.common.exception.ResourceNotFoundException;
import ai.devops.modules.deployment.entity.DeploymentEntity;
import ai.devops.modules.deployment.repository.DeploymentRepository;
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
        if (environmentId != null) {
            return deploymentRepository.findByEnvironmentIdOrderByStartedAtDesc(environmentId);
        }
        return deploymentRepository.findAllByOrderByStartedAtDesc();
    }

    @Transactional(readOnly = true)
    public DeploymentEntity getDeploymentById(UUID id) {
        return deploymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Deployment", id));
    }
}
