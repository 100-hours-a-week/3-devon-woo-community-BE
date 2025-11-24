package com.kakaotechbootcamp.community.domain.common.policy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kakaotechbootcamp.community.common.exception.CustomException;
import com.kakaotechbootcamp.community.config.annotation.UnitTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@UnitTest
class OwnershipPolicyTest {

    private final OwnershipPolicy ownershipPolicy = new OwnershipPolicy();

    @Test
    @DisplayName("소유자 ID와 요청자 ID가 같으면 예외가 발생하지 않는다")
    void validateOwnership_success() {
        ownershipPolicy.validateOwnership(1L, 1L);
    }

    @Test
    @DisplayName("소유자 ID와 요청자 ID가 다르면 예외가 발생한다")
    void validateOwnership_throwsException() {
        assertThatThrownBy(() -> ownershipPolicy.validateOwnership(1L, 2L))
                .isInstanceOf(CustomException.class);
    }
}
