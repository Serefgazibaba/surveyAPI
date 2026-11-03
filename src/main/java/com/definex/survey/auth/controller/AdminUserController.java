package com.definex.survey.auth.controller;

import com.definex.survey.auth.dto.UserListResponse;
import com.definex.survey.auth.service.UserDirectoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@Validated
@Tag(name = "Admin - Users", description = "Ankete davet edilecek kullanıcılar")
public class AdminUserController {

    private final UserDirectoryService userDirectoryService;

    public AdminUserController(UserDirectoryService userDirectoryService) {
        this.userDirectoryService = userDirectoryService;
    }

    @GetMapping("/getAllUsers")
    @Operation(operationId = "adminGetAllUsers", summary = "Davet edilebilecek kullanıcıları listele",
            description = "USER rolündeki aktif kullanıcıları ada göre sıralı ve sayfalı döner; anket düzenleme ekranında "
                    + "katılımcı seçimi için kullanılır.")
    public UserListResponse getAllUsers(
            @Parameter(description = "Ad veya e-postada arama") @RequestParam(required = false) @Size(max = 100) String q,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return userDirectoryService.listInvitable(q, page, size);
    }
}
