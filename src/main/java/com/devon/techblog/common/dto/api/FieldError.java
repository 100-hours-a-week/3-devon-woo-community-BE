package com.devon.techblog.common.dto.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldError {

    private String field;
    private String message;
    private Object rejectedValue;

    public static FieldError of(String field, String message, Object rejectedValue) {
        return FieldError.builder()
                .field(field)
                .message(message)
                .rejectedValue(rejectedValue)
                .build();
    }
}
