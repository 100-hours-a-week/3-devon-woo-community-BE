# 이미지 업로드 가이드

## 개요

이 시스템은 Cloudinary를 통한 안전한 파일 업로드를 지원합니다. 클라이언트는 presigned URL을 발급받아 직접 Cloudinary에 업로드하고, 완료 후 서버에 알리는 방식입니다.

## 업로드 플로우

```
1. Client → Server: Presigned URL 요청
2. Server → Client: fileId, storageKey, uploadSignature 반환
3. Client → Cloudinary: 파일 업로드 (presigned URL 사용)
4. Client → Server: 업로드 완료 알림 (fileId, url, size)
5. Server → Client: 최종 파일 정보 반환
```

## API 엔드포인트

### 1. Presigned URL 발급

**Endpoint:** `POST /api/v1/files/presign`

서버로부터 Cloudinary 업로드를 위한 서명된 인증 정보를 발급받습니다.

**Request Body:**
```json
{
  "fileType": "IMAGE",
  "originalName": "example.jpg",
  "mimeType": "image/jpeg"
}
```

**Parameters:**
- `fileType` (required): 파일 타입
  - `IMAGE`: 이미지 파일
  - `VIDEO`: 비디오 파일
  - `DOCUMENT`: 문서 파일
- `originalName` (required): 원본 파일명
- `mimeType` (required): MIME 타입 (예: `image/jpeg`, `image/png`)

**Response:** `201 Created`
```json
{
  "success": true,
  "message": "presign_url_created",
  "data": {
    "fileId": 123,
    "storageKey": "images/uuid-example.jpg",
    "uploadSignature": {
      "apiKey": "your-cloudinary-api-key",
      "cloudName": "your-cloud-name",
      "timestamp": 1234567890,
      "signature": "generated-signature",
      "uploadPreset": "your-preset",
      "folder": "images"
    }
  }
}
```

### 2. Cloudinary 업로드 (클라이언트 → Cloudinary)

presigned URL을 사용하여 Cloudinary에 직접 업로드합니다.

**Endpoint:** `POST https://api.cloudinary.com/v1_1/{cloudName}/image/upload`

**Request Body (multipart/form-data):**
```
file: [바이너리 파일]
api_key: {apiKey}
timestamp: {timestamp}
signature: {signature}
upload_preset: {uploadPreset}
folder: {folder}
```

**Cloudinary Response:**
```json
{
  "public_id": "images/uuid-example",
  "secure_url": "https://res.cloudinary.com/.../images/uuid-example.jpg",
  "bytes": 123456,
  ...
}
```

### 3. 업로드 완료 알림

**Endpoint:** `POST /api/v1/files/{fileId}/complete`

Cloudinary 업로드 완료 후 서버에 최종 파일 정보를 전달합니다.

**Path Parameters:**
- `fileId`: presigned URL 발급 시 받은 파일 ID

**Request Body:**
```json
{
  "url": "https://res.cloudinary.com/.../images/uuid-example.jpg",
  "size": 123456
}
```

**Parameters:**
- `url` (required): Cloudinary에서 반환받은 `secure_url`
- `size` (required): 파일 크기 (bytes)

**Response:** `200 OK`
```json
{
  "success": true,
  "message": "upload_completed",
  "data": {
    "id": 123,
    "fileType": "IMAGE",
    "originalName": "example.jpg",
    "storageKey": "images/uuid-example.jpg",
    "url": "https://res.cloudinary.com/.../images/uuid-example.jpg",
    "size": 123456,
    "mimeType": "image/jpeg",
    "status": "ACTIVE",
    "isDeleted": false,
    "createdAt": "2024-01-01T00:00:00Z",
    "updatedAt": "2024-01-01T00:00:00Z"
  }
}
```

## 이미지 서명 발급 (레거시)

**Endpoint:** `GET /api/v1/images/sign`

Cloudinary 업로드 서명만 발급합니다. (파일 메타데이터 생성 없음)

**Query Parameters:**
- `type` (optional, default: "post"): 이미지 타입

**Response:** `200 OK`
```json
{
  "success": true,
  "data": {
    "apiKey": "your-cloudinary-api-key",
    "cloudName": "your-cloud-name",
    "timestamp": 1234567890,
    "signature": "generated-signature",
    "uploadPreset": "your-preset",
    "folder": "post"
  }
}
```

## 예시 코드

### JavaScript/TypeScript

```typescript
async function uploadImage(file: File): Promise<FileResponse> {
  const presignResponse = await fetch('/api/v1/files/presign', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      fileType: 'IMAGE',
      originalName: file.name,
      mimeType: file.type
    })
  });

  const { fileId, storageKey, uploadSignature } = await presignResponse.json();

  const formData = new FormData();
  formData.append('file', file);
  formData.append('api_key', uploadSignature.apiKey);
  formData.append('timestamp', uploadSignature.timestamp);
  formData.append('signature', uploadSignature.signature);
  formData.append('upload_preset', uploadSignature.uploadPreset);
  formData.append('folder', uploadSignature.folder);

  const cloudinaryResponse = await fetch(
    `https://api.cloudinary.com/v1_1/${uploadSignature.cloudName}/image/upload`,
    { method: 'POST', body: formData }
  );

  const { secure_url, bytes } = await cloudinaryResponse.json();

  const completeResponse = await fetch(`/api/v1/files/${fileId}/complete`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({
      url: secure_url,
      size: bytes
    })
  });

  return await completeResponse.json();
}
```

## FileType 종류

- `IMAGE`: 이미지 파일 → `images/` 폴더
- `VIDEO`: 비디오 파일 → `videos/` 폴더
- `DOCUMENT`: 문서 파일 → `documents/` 폴더

## 참고사항

- Presigned URL은 일정 시간 후 만료됩니다
- 업로드는 클라이언트에서 직접 Cloudinary로 진행되므로 서버 부하가 없습니다
- 파일 메타데이터는 presigned URL 발급 시 서버에 생성되며, 업로드 완료 시 URL과 크기 정보가 업데이트됩니다
