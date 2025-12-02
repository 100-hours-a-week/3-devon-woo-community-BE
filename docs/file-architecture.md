# S3 기반 파일 저장 전략: 단일 File 엔티티 vs 타입별 엔티티

## 1. 결론 요약

정답은 **“하나의 File 테이블 + subtype 구분 전략”이 실무 표준**이다.  
이미지, 동영상, 문서(PDF)는 S3 URL과 공통 메타데이터 모델이 동일하기 때문에 **통합 엔티티가 유리**하다.  
단, **영상처럼 특수한 메타데이터(재생시간, 해상도 등)가 필요하면 서브테이블로 분리**한다.

---

## 2. 권장 구조 (DDD + 확장성 + AWS 실무 패턴)

```
file (공통 테이블)
 ├── file_type (IMAGE / VIDEO / DOCUMENT)
 ├── s3_key
 ├── original_name
 ├── size
 ├── mime_type
 ├── width / height (nullable)
 ├── duration_sec (nullable)
 └── ... 공통 메타데이터

영상 확장 정보:
file_video_metadata (1:1)
```

---

## 3. 단일 File 엔티티가 정답인 이유

### 3.1 업로드/다운로드/URL 로직이 모두 동일  
- S3 업로드  
- Presigned URL 발급  
- 삭제 처리  
- 파일명 생성 규칙  
- 업로드 정책 검증  
- Audit

파일 종류와 상관없기 때문에 **분리하면 중복 코드가 폭발한다.**

---

### 3.2 테이블 증가 + JOIN 증가 + 서비스 로직 복잡화  
- 이미지/영상/PDF 엔티티를 분리하면 도메인 객체에서 참조 관계가 복잡해진다.  
- 하나의 게시글 첨부파일 조회도 3개 테이블을 병합해야 할 수 있다.  
→ **불필요한 복잡성 증가**

---

### 3.3 공통 속성이 대부분 동일
| Field | 이미지 | 비디오 | PDF |
|-------|--------|---------|-------|
| S3 Key | O | O | O |
| MIME | O | O | O |
| Size | O | O | O |
| 원본 파일명 | O | O | O |
| 업로드 시간 | O | O | O |

공통이 많기 때문에 중앙 관리가 적절하다.

---

### 3.4 S3의 특성과 가장 일치  
S3는 오브젝트 스토리지이므로 이미지/영상/PDF를 동일하게 취급한다.  
서버에서도 같은 방식이 자연스럽다.

---

## 4. 파일 엔티티를 분리해야 하는 상황 (희귀 케이스)

### 4.1 각 파일 종류의 비즈니스 모델이 완전히 다를 때
예:
- Video = 스트리밍 자원 (HLS 변환, 썸네일, 인코딩 상태 등)
- Document = OCR + 메타데이터 분석
- Image = 다양한 사이즈 렌더링

각각 domain lifecycle이 다르면 **엔티티 분리**를 고려.

### 4.2 파일 자체가 핵심 도메인일 때
예: 강의 플랫폼의 Video 엔티티  
→ File은 metadata일 뿐, Video가 주체  
→ Aggregate로 분리

---

## 5. Insty/TechBlog/Community 기준 "정답"

이 프로젝트들은 다음과 같은 파일 용도를 갖는다:

- 프로필 이미지  
- 게시글 이미지/썸네일  
- 동영상 업로드  
- PDF 첨부파일  

→ 공통 File 엔티티가 100% 적합하다.

### 5.1 File 엔티티 예시

```java
@Entity
public class File {

    @Id @GeneratedValue
    private Long id;

    @Enumerated(EnumType.STRING)
    private FileType fileType; // IMAGE, VIDEO, DOCUMENT

    private String originalName;
    private String s3Key;
    private String url;
    private Long size;
    private String mimeType;

    private Integer width;   // 이미지/비디오
    private Integer height;  // 이미지/비디오
    private Integer duration; // 비디오 길이 (nullable)

    private Instant createdAt;
}
```

---

### 5.2 비디오 메타데이터 확장

```java
@Entity
public class FileVideoMeta {

    @Id
    private Long fileId;

    private String codec;
    private Integer bitrate;
    private Integer fps;
    private String resolution;

    @OneToOne @MapsId
    private File file;
}
```

---

### 5.3 도메인 객체에서 파일 참조

```java
public class Post {
    @OneToOne(fetch = LAZY)
    private File thumbnail;

    @OneToMany
    private List<File> attachments;
}
```

---

## 6. 실무에서 가장 많이 쓰는 방식

대기업/스타트업 공통 패턴:

- File(공통) 1개  
- file_type ENUM  
- 필요한 경우에만 부가 메타데이터 테이블  

이 구조가 테이블 수, 복잡성, 확장성, 성능 면에서 최적균형점이다.

---

## 7. 기준 최종 추천 구조

```
common: File + FileType + Validator
infra: S3Uploader + PresignedUrlService
domain: Post, Member 등에서 File 참조
```

장점:

- 도메인 계층이 단순해짐
- 파일 업로드/삭제 정책이 중앙화됨
- 타입 확장이 쉬움 (예: AUDIO 지원)
- 테스트 용이

---

## 8. 부록: ERD 설계안

```
File
 ├── id (PK)
 ├── file_type
 ├── original_name
 ├── s3_key
 ├── url
 ├── size
 ├── mime_type
 ├── width
 ├── height
 ├── duration
 └── created_at

FileVideoMeta
 ├── file_id (PK, FK → File.id)
 ├── codec
 ├── bitrate
 ├── fps
 └── resolution
```

---

## 9. 추가로 제작 가능

원하면 아래도 제공 가능함:

- S3Uploader 전체 코드
- Presigned URL 발급 REST API
- File 삭제 파이프라인
- AWS MediaConvert 영상 처리 아키텍처
- 통합 테스트 코드 샘플
- 전체 ERD

