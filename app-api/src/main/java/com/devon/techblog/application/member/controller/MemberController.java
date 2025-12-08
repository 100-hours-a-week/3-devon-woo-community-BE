package com.devon.techblog.application.member.controller;

import com.devon.techblog.application.member.controller.docs.MemberApiDocs;
import com.devon.techblog.application.member.dto.request.MemberUpdateRequest;
import com.devon.techblog.application.member.dto.request.PasswordUpdateRequest;
import com.devon.techblog.application.member.dto.response.MemberDetailsResponse;
import com.devon.techblog.application.member.service.MemberService;
import com.devon.techblog.application.security.annotation.CurrentUser;
import com.devon.techblog.common.dto.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController implements MemberApiDocs {

    private final MemberService memberService;

    @GetMapping("/me")
    public ApiResponse<MemberDetailsResponse> getMyProfile(
            @CurrentUser Long memberId
    ) {
        MemberDetailsResponse response = memberService.getMemberProfile(memberId);
        return ApiResponse.success(response);
    }

    @GetMapping("/{memberId}")
    public ApiResponse<MemberDetailsResponse> getMemberProfile(
            @PathVariable Long memberId
    ) {
        MemberDetailsResponse response = memberService.getMemberProfile(memberId);
        return ApiResponse.success(response);
    }

    @PatchMapping("/me")
    public ApiResponse<MemberDetailsResponse> updateMyProfile(
            @RequestBody @Validated MemberUpdateRequest request,
            @CurrentUser Long memberId
    ) {
        memberService.updateMember(memberId, request);
        MemberDetailsResponse detailsResponse = memberService.getMemberProfile(memberId);
        return ApiResponse.success(detailsResponse);
    }

    @PatchMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateMyPassword(
            @RequestBody @Validated PasswordUpdateRequest request,
            @CurrentUser Long memberId
    ) {
        memberService.updatePassword(memberId, request);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMyAccount(
            @CurrentUser Long memberId
    ) {
        memberService.deleteMember(memberId);
    }
}
