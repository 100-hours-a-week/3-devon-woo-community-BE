package com.kakaotechbootcamp.community.application.security.handler;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakaotechbootcamp.community.application.security.util.CookieUtil;
import com.kakaotechbootcamp.community.common.dto.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LogoutHandler {

    private final ObjectMapper objectMapper;
    private final CookieUtil cookieUtil;

    public void onLogout(HttpServletRequest request, HttpServletResponse response) throws IOException {

        SecurityContextHolder.clearContext();

        // todo: 그 외 추가적인 로그아웃 동작 (ex. 세션 제거, Refresh 블랙리스트 등록)
        cookieUtil.deleteRefreshTokenCookie(response);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<Object> result = ApiResponse.success("로그아웃되었습니다.");
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }


}
