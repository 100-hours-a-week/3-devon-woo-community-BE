package com.devon.techblog.application.member.controller.docs;

import com.devon.techblog.application.member.dto.request.MemberUpdateRequest;
import com.devon.techblog.application.member.dto.request.PasswordUpdateRequest;
import com.devon.techblog.application.member.dto.response.MemberDetailsResponse;
import com.devon.techblog.common.dto.api.ApiResponse;
import com.devon.techblog.common.swagger.CustomErrorResponseDescription;
import com.devon.techblog.common.swagger.SwaggerErrorResponseDescription;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(
        name = "Member",
        description = "회원 관련 API"
)
public interface MemberApiDocs {

    @Operation(
            summary = "내 프로필 조회",
            description = "현재 로그인한 회원의 프로필 정보를 조회합니다."
    )
    @CustomErrorResponseDescription(SwaggerErrorResponseDescription.MEMBER_GET)
    ApiResponse<MemberDetailsResponse> getMyProfile(
            Long memberId
    );

    @Operation(
            summary = "회원 정보 조회",
            description = "특정 회원의 프로필 정보를 조회합니다."
    )
    @CustomErrorResponseDescription(SwaggerErrorResponseDescription.MEMBER_GET)
    ApiResponse<MemberDetailsResponse> getMemberProfile(
            Long memberId
    );

    @Operation(
            summary = "내 프로필 수정",
            description = "현재 로그인한 회원의 프로필 정보를 수정합니다."
    )
    @CustomErrorResponseDescription(SwaggerErrorResponseDescription.MEMBER_UPDATE)
    ApiResponse<MemberDetailsResponse> updateMyProfile(
            MemberUpdateRequest request,
            Long memberId
    );

    @Operation(
            summary = "비밀번호 변경",
            description = "현재 로그인한 회원의 비밀번호를 변경합니다."
    )
    @CustomErrorResponseDescription(SwaggerErrorResponseDescription.MEMBER_PASSWORD_UPDATE)
    void updateMyPassword(
            PasswordUpdateRequest request,
            Long memberId
    );

    @Operation(
            summary = "회원 탈퇴",
            description = "현재 로그인한 회원을 탈퇴 처리합니다."
    )
    @CustomErrorResponseDescription(SwaggerErrorResponseDescription.MEMBER_DELETE)
    void deleteMyAccount(
            Long memberId
    );
}

