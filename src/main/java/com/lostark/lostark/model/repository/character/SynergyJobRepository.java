package com.lostark.lostark.model.repository.character;

import com.lostark.lostark.model.entity.character.SynergyJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SynergyJobRepository extends JpaRepository<SynergyJob, Long> {
    Optional<SynergyJob> findByClassName(String className);
    Optional<SynergyJob> findByClassNameAndIsDeletedFalse(String className);
    java.util.List<SynergyJob> findAllByIsDeletedFalse();
    java.util.List<SynergyJob> findAllByIsDeletedTrue();
    boolean existsByClassName(String className);
}
