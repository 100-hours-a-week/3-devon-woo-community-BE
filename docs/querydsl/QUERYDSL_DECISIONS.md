# QueryDSL 설계 결정 사항

## 1. CustomHibernate5Templates 사용 여부

### CustomHibernate5Templates란?
QueryDSL이 JPQL을 생성할 때 사용하는 템플릿을 커스터마이징하는 클래스입니다.

```java
public class CustomHibernate5Templates extends Hibernate5Templates {
    @Override
    public QueryHandler getQueryHandler() {
        return DefaultQueryHandler.DEFAULT;
    }
}
```

### 사용 목적
1. **쿼리 힌트 추가**: 데이터베이스별 최적화 힌트
2. **함수 커스터마이징**: 데이터베이스 특화 함수 사용
3. **키워드 이스케이핑**: 특정 DB의 예약어 처리

### 현재 프로젝트에 필요한가?

**결론: 현재는 불필요, 향후 필요 시 추가**

**이유:**
- ✅ MySQL 기본 기능으로 충분
- ✅ 특수한 SQL 함수 사용하지 않음
- ✅ QueryDSL 기본 템플릿으로 모든 요구사항 해결 가능

**필요한 경우:**
- MySQL 특화 함수 사용 (예: `MATCH AGAINST` 전문 검색)
- 복잡한 서브쿼리에서 성능 힌트 필요
- 특정 데이터베이스 방언 필요

### 향후 추가 시나리오 예시

```java
// MySQL 전문 검색을 사용할 때
public class CustomMySQLTemplates extends MySQLTemplates {

    public static final MySQLTemplates DEFAULT = new CustomMySQLTemplates();

    protected CustomMySQLTemplates() {
        // FULLTEXT SEARCH 함수 등록
        add(SQLOps.MATCH, "MATCH({0}) AGAINST({1} IN BOOLEAN MODE)");
    }
}

// Configuration에서 사용
@Bean
public JPAQueryFactory jpaQueryFactory(EntityManager em) {
    return new JPAQueryFactory(CustomMySQLTemplates.DEFAULT, em);
}
```

---

## 2. 정렬 기준 및 순서 처리 전략

### 현재 상황
- Pageable에 정렬 정보가 포함되어 있음
- 하드코딩된 정렬 기준 사용 중 (`post.createdAt.desc()`)

### 해결 방안

#### Option 1: Pageable의 Sort를 직접 활용 (권장)
**장점:**
- Spring Data의 표준 방식
- 클라이언트가 자유롭게 정렬 가능
- 코드 재사용성 높음

**단점:**
- 필드명 노출 위험
- 허용되지 않은 필드로 정렬 시도 가능

#### Option 2: Enum으로 정렬 옵션 제한
**장점:**
- 타입 안전성
- 허용된 정렬만 가능
- 비즈니스 요구사항과 명확한 매핑

**단점:**
- 새로운 정렬 추가 시 코드 수정 필요
- 유연성 감소

### 권장 구현: 하이브리드 방식
화이트리스트 기반 + Pageable 활용

---

## 3. QueryDSL 공통 메서드 및 설정

### 필요한 공통 기능들

1. **동적 OrderBy 처리**
2. **Null-safe 조건 생성**
3. **페이징 유틸리티**
4. **공통 조건 필터**
5. **Base Repository 인터페이스**

### 구현 방향
- `QueryDslSupport`: 공통 유틸리티 클래스
- `QueryDslOrderUtil`: 동적 정렬 처리
- `Querydsl4RepositorySupport`: Spring Data의 지원 클래스 활용
