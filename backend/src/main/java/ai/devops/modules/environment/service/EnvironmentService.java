package ai.devops.modules.environment.service;

import ai.devops.common.exception.ResourceNotFoundException;
import ai.devops.modules.environment.entity.EnvironmentEntity;
import ai.devops.modules.environment.repository.EnvironmentRepository;
import ai.devops.security.rbac.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class EnvironmentService {

    private final EnvironmentRepository environmentRepository;

    public EnvironmentService(EnvironmentRepository environmentRepository) {
        this.environmentRepository = environmentRepository;
    }

    @Transactional(readOnly = true)
    public List<EnvironmentEntity> getAllEnvironments() {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        if (orgId == null) {
            return List.of();
        }
        return environmentRepository.findByOrganizationIdOrderByCreatedAtAsc(orgId);
    }

    @Transactional(readOnly = true)
    public EnvironmentEntity getEnvironmentById(UUID id) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        if (orgId == null) {
            throw new ResourceNotFoundException("Environment", id);
        }
        return environmentRepository.findByIdAndOrganizationId(id, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Environment", id));
    }

    @Transactional(readOnly = true)
    public EnvironmentEntity getEnvironmentByName(String name) {
        UUID orgId = SecurityUtils.getCurrentOrganizationId();
        if (orgId == null) {
            throw new ResourceNotFoundException("Environment", name);
        }
        return environmentRepository.findByNameIgnoreCaseAndOrganizationId(name, orgId)
                .orElseThrow(() -> new ResourceNotFoundException("Environment", name));
    }
}
