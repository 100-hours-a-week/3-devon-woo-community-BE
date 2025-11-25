# Spring @Sql 사용 예제

## 기본 사용법

### 1. 클래스 레벨 (모든 테스트 메서드에 적용)
```java
@IntegrationTest
@Sql("/sql/member-test-data.sql")
class MemberIntegrationTest {
    // 모든 테스트 전에 SQL 실행
}
```

### 2. 메서드 레벨 (특정 테스트에만 적용)
```java
@Test
@Sql("/sql/member-test-data.sql")
void testWithSpecificData() {
    // 이 테스트 전에만 SQL 실행
}
```

### 3. 여러 스크립트 실행
```java
@Test
@Sql({"/sql/schema.sql", "/sql/member-test-data.sql", "/sql/post-test-data.sql"})
void testWithMultipleScripts() {
    // 여러 SQL 순서대로 실행
}
```

### 4. 실행 시점 지정
```java
@Test
@Sql(scripts = "/sql/setup.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/cleanup.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
void testWithCleanup() {
    // 테스트 전/후 SQL 실행
}
```

### 5. 설정 커스터마이징
```java
@Test
@Sql(
    scripts = "/sql/member-test-data.sql",
    config = @SqlConfig(
        encoding = "UTF-8",
        separator = ";",
        commentPrefix = "--"
    )
)
void testWithCustomConfig() {
    // SQL 실행 설정 변경
}
```

## 권장 사항

1. **간단한 데이터**: Fixture 클래스 사용
2. **복잡한 데이터/관계**: @Sql 사용
3. **재사용 필요**: SQL 파일로 분리
