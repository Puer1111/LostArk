package com.lostark.lostark.controller.admin;

import com.lostark.lostark.model.dto.character.RaidDto;
import com.lostark.lostark.service.character.RaidAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/admin/raids")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRaidController {

    private final RaidAdminService raidAdminService;

    @GetMapping
    public String raidManagePage() {
        return "admin/raidManage";
    }

    @GetMapping("/api")
    @ResponseBody
    public ResponseEntity<List<RaidDto>> getRaids() {
        return ResponseEntity.ok(raidAdminService.getAllRaids());
    }

    @PostMapping("/api")
    @ResponseBody
    public ResponseEntity<RaidDto> saveRaid(@RequestBody RaidDto dto) {
        return ResponseEntity.ok(raidAdminService.saveRaid(dto));
    }

    @DeleteMapping("/api/{id}")
    @ResponseBody
    public ResponseEntity<Void> deleteRaid(@PathVariable Long id) {
        raidAdminService.deleteRaid(id);
        return ResponseEntity.noContent().build();
    }
}
