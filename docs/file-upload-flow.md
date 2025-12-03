# 파일 업로드 흐름 (Presigned URL 방식)

## 전체 프로세스

### 1. Presigned URL 발급 요청
프론트엔드에서 파일 업로드를 시작하기 전에 서버에 Presigned URL을 요청합니다.

**요청:**
```http
POST /api/v1/files/presign
Content-Type: application/json

{
  "fileType": "IMAGE",
  "originalName": "profile-picture.jpg",
  "mimeType": "image/jpeg"
}
```

**응답:**
```json
{
  "success": true,
  "message": "presign_url_created",
  "data": {
    "fileId": 123,
    "storageKey": "images/a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg",
    "uploadSignature": {
      "apiKey": "your-api-key",
      "cloudName": "your-cloud-name",
      "timestamp": 1234567890,
      "signature": "signature-string",
      "uploadPreset": "unsigned_preset",
      "folder": "images"
    }
  }
}
```

이 시점에 서버는:
- DB에 `status=PENDING` 상태의 File 레코드 생성
- Cloudinary 업로드를 위한 서명 정보 생성
- `fileId`와 `storageKey` 반환

### 2. Cloudinary에 직접 업로드
프론트엔드는 받은 서명 정보를 사용하여 Cloudinary에 직접 파일을 업로드합니다.

**Cloudinary 업로드 예시 (JavaScript):**
```javascript
const response = await fetch('/api/v1/files/presign', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    fileType: 'IMAGE',
    originalName: file.name,
    mimeType: file.type
  })
});

const { fileId, uploadSignature } = await response.json();

// Cloudinary에 직접 업로드
const formData = new FormData();
formData.append('file', file);
formData.append('api_key', uploadSignature.apiKey);
formData.append('timestamp', uploadSignature.timestamp);
formData.append('signature', uploadSignature.signature);
formData.append('upload_preset', uploadSignature.uploadPreset);
formData.append('folder', uploadSignature.folder);

const uploadResponse = await fetch(
  `https://api.cloudinary.com/v1_1/${uploadSignature.cloudName}/image/upload`,
  {
    method: 'POST',
    body: formData
  }
);

const uploadResult = await uploadResponse.json();
// uploadResult.secure_url: 업로드된 파일의 URL
// uploadResult.bytes: 파일 크기
```

### 3. 업로드 완료 알림
Cloudinary 업로드가 성공하면, 프론트엔드는 서버에 업로드 완료를 알립니다.

**요청:**
```http
POST /api/v1/files/123/complete
Content-Type: application/json

{
  "url": "https://res.cloudinary.com/.../images/a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg",
  "size": 102400
}
```

**응답:**
```json
{
  "success": true,
  "message": "upload_completed",
  "data": {
    "id": 123,
    "fileType": "IMAGE",
    "originalName": "profile-picture.jpg",
    "storageKey": "images/a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg",
    "url": "https://res.cloudinary.com/.../images/a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg",
    "size": 102400,
    "mimeType": "image/jpeg",
    "status": "UPLOADED",
    "isDeleted": false,
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:01:00Z"
  }
}
```

이 시점에 서버는:
- File 레코드의 `status`를 `PENDING` → `UPLOADED`로 변경
- `url`과 `size` 정보 업데이트

### 4. 마크다운 에디터에 URL 삽입
업로드가 완료되면, 프론트엔드는 받은 URL을 마크다운 형식으로 에디터에 삽입합니다.

```javascript
const completeResponse = await fetch(`/api/v1/files/${fileId}/complete`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    url: uploadResult.secure_url,
    size: uploadResult.bytes
  })
});

const { url } = await completeResponse.json();

// 마크다운 에디터에 이미지 삽입
const markdownImage = `![${file.name}](${url})`;
editor.insertText(markdownImage);
```

### 5. 게시글 작성 및 저장
사용자가 게시글 작성을 완료하고 저장하면, content에 이미 이미지 URL이 포함되어 있습니다.

**게시글 생성 요청:**
```http
POST /api/v1/posts
Content-Type: application/json

{
  "title": "제목",
  "content": "본문 내용\n\n![profile-picture.jpg](https://res.cloudinary.com/.../images/a1b2c3d4-e5f6-7890-abcd-ef1234567890.jpg)\n\n더 많은 내용..."
}
```

게시글 content에 이미지 URL이 마크다운 형식으로 저장되어 있으므로, 별도의 파일-게시글 연결 작업이 필요 없습니다.

## 에러 처리

### 업로드 실패 시
만약 Cloudinary 업로드가 실패하면, 프론트엔드는 사용자에게 에러를 표시하고, 해당 `fileId`는 `PENDING` 상태로 남게 됩니다.

서버는 주기적으로 오래된 `PENDING` 상태의 파일을 정리하는 배치 작업을 실행할 수 있습니다:
```sql
DELETE FROM file
WHERE status = 'PENDING'
  AND created_at < NOW() - INTERVAL 24 HOUR;
```

### 업로드는 성공했지만 complete 요청 실패 시
네트워크 문제 등으로 complete 요청이 실패하면:
1. 프론트엔드는 재시도 로직 실행
2. 또는 사용자에게 재업로드 요청
3. 실패한 파일은 `PENDING` 상태로 남아있다가 배치로 정리

## FileStatus 상태 전이

```
PENDING → UPLOADED → COMPLETED
        ↓
      FAILED
```

- **PENDING**: Presigned URL 발급 후, 아직 업로드되지 않음
- **UPLOADED**: Cloudinary 업로드 완료
- **PROCESSING**: (선택) 후처리(리사이즈, 썸네일 생성 등) 진행 중
- **COMPLETED**: 모든 처리 완료
- **FAILED**: 업로드 또는 처리 실패

## 장점

1. **서버 부하 감소**: 파일 데이터가 서버를 거치지 않음
2. **확장성**: 대용량 파일도 문제없이 처리
3. **빠른 업로드**: 클라이언트 ↔ Cloudinary 직접 통신
4. **유연한 후처리**: 비동기로 썸네일 생성 등 가능

## 주의사항

1. 사용자가 게시글 작성을 취소하면 업로드된 파일이 고아 파일로 남을 수 있음
2. 마크다운 content를 파싱하여 실제 사용 중인 이미지와 사용되지 않는 이미지를 구분하는 로직 필요
3. 보안: Presigned URL의 유효 시간 제한 (기본 1시간)
