package com.lostark.lostark.model.repository.character;

import com.lostark.lostark.model.entity.character.Raid;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RaidRepository extends JpaRepository<Raid, Long> {

    List<Raid> findAllByOrderByRaidCategoryAscRaidNameAsc();

    boolean existsByRaidCategoryAndRaidName(String raidCategory, String raidName);
}
