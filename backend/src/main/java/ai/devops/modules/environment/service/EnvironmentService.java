package ai.devops.modules.environment.service;

import ai.devops.common.exception.ResourceNotFoundException;
import ai.devops.modules.environment.entity.EnvironmentEntity;
import ai.devops.modules.environment.repository.EnvironmentRepository;
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
        return environmentRepository.findAllByOrderByCreatedAtAsc();
    }

    @Transactional(readOnly = true)
    public EnvironmentEntity getEnvironmentById(UUID id) {
        return environmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Environment", id));
    }

    @Transactional(readOnly = true)
    public EnvironmentEntity getEnvironmentByName(String name) {
        return environmentRepository.findByNameIgnoreCase(name)
                .orElseThrow(() -> new ResourceNotFoundException("Environment", name));
    }
}
