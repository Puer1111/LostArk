package com.lostark.lostark.controller.admin;

import com.lostark.lostark.model.dto.admin.AdminUserResponseDto;
import com.lostark.lostark.model.entity.users.UserRole;
import com.lostark.lostark.service.admin.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminMainController {

    private final AdminUserService adminUserService;

    /**
     * 관리자 대시보드 메인 페이지 (/admin/main)
     */
    @GetMapping({"", "/", "/main"})
    public String adminMainPage(Model model) {
        return "admin/main";
    }

    /**
     * 멤버(유저) 관리 뷰 페이지 (/admin/users)
     */
    @GetMapping("/users")
    public String adminUsersPage(Model model) {
        return "admin/userManage";
    }

    /**
     * 회원 전체 목록 조회 API
     */
    @GetMapping("/api/users")
    @ResponseBody
    public ResponseEntity<List<AdminUserResponseDto>> getUserList() {
        return ResponseEntity.ok(adminUserService.getAllUsers());
    }

    /**
     * 회원 역할(Role) 변경 API
     */
    @PutMapping("/api/users/{userId}/role")
    @ResponseBody
    public ResponseEntity<AdminUserResponseDto> updateUserRole(
            @PathVariable String userId,
            @RequestBody Map<String, String> requestBody) {
        String roleStr = requestBody.get("role");
        UserRole newRole = UserRole.valueOf(roleStr.toUpperCase());
        return ResponseEntity.ok(adminUserService.updateUserRole(userId, newRole));
    }
}
