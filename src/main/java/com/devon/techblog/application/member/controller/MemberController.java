package com.devon.techblog.application.member.controller;

import com.devon.techblog.application.member.dto.request.MemberUpdateRequest;
import com.devon.techblog.application.member.dto.request.PasswordUpdateRequest;
import com.devon.techblog.application.member.dto.response.MemberDetailsResponse;
import com.devon.techblog.application.member.dto.response.MemberUpdateResponse;
import com.devon.techblog.application.member.service.MemberService;
import com.devon.techblog.application.security.annotation.CurrentUser;
import com.devon.techblog.common.dto.api.ApiResponse;
import com.devon.techblog.common.swagger.CustomExceptionDescription;
import com.devon.techblog.common.swagger.SwaggerResponseDescription;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member", description = "회원 관련 API")
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "회원 정보 조회", description = "회원의 프로필 정보를 조회합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.MEMBER_GET)
    @GetMapping("/{id}")
    public ApiResponse<MemberDetailsResponse> getMemberProfile(
            @CurrentUser Long memberId
    ) {
        MemberDetailsResponse response = memberService.getMemberProfile(memberId);
        return ApiResponse.success(response, "member_get_success");
    }


    @Operation(summary = "회원 정보 수정", description = "회원의 프로필 정보를 수정합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.MEMBER_UPDATE)
    @PatchMapping("/{id}")
    public ApiResponse<MemberUpdateResponse> updateMember(
            @RequestBody @Validated MemberUpdateRequest request,
            @CurrentUser Long memberId
    ) {
        MemberUpdateResponse response = memberService.updateMember(memberId, request);
        return ApiResponse.success(response, "member_update_success");
    }

    @Operation(summary = "비밀번호 변경", description = "회원의 비밀번호를 변경합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.MEMBER_PASSWORD_UPDATE)
    @PatchMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updatePassword(
            @RequestBody @Validated PasswordUpdateRequest request,
            @CurrentUser Long memberId
    ){
        memberService.updatePassword(memberId, request);
    }

    @Operation(summary = "회원 탈퇴", description = "회원을 탈퇴 처리합니다.")
    @CustomExceptionDescription(SwaggerResponseDescription.MEMBER_DELETE)
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMember(
            @CurrentUser Long memberId)
    {
        memberService.deleteMember(memberId);
    }
}
