package com.lostark.lostark.controller.admin;

import com.lostark.lostark.model.dto.character.synergy.SynergyJobDto;
import com.lostark.lostark.service.character.SynergyAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/admin/synergies")
@RequiredArgsConstructor
public class AdminSynergyController {

    private final SynergyAdminService synergyAdminService;

    /**
     * 어드민 시너지 관리 뷰 페이지 반환
     */
    @GetMapping
    public String synergyManagePage(Model model) {
        return "admin/synergyManage";
    }

    /**
     * 전체 시너지 목록 API
     */
    @GetMapping("/api/list")
    @ResponseBody
    public ResponseEntity<List<SynergyJobDto>> getSynergyList() {
        return ResponseEntity.ok(synergyAdminService.getAllSynergies());
    }

    /**
     * 단일 직업 시너지 조회 API
     */
    @GetMapping("/api/{className}")
    @ResponseBody
    public ResponseEntity<SynergyJobDto> getSynergy(@PathVariable String className) {
        return synergyAdminService.getSynergyByClassName(className)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 직업 시너지 등록 / 수정 API (ADMIN 권한 필요)
     */
    @PostMapping("/api")
    @ResponseBody
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SynergyJobDto> saveSynergy(@RequestBody SynergyJobDto dto) {
        SynergyJobDto saved = synergyAdminService.saveOrUpdateSynergy(dto);
        return ResponseEntity.ok(saved);
    }

    /**
     * 직업 시너지 삭제 API (ADMIN 권한 필요)
     */
    @DeleteMapping("/api/{className}")
    @ResponseBody
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSynergy(@PathVariable String className) {
        synergyAdminService.deleteSynergy(className);
        return ResponseEntity.ok().build();
    }
}
