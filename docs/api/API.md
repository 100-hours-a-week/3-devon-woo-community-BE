# WebFlux API Documentation

## Health Check

### GET /health
헬스체크 엔드포인트

**Response**
- Type: `String`
- Example: `"WebFlux is running!"`

---

## AI Chat APIs

### GET /ai/chat
AI 채팅 요청

**Parameters**
- `prompt` (required): 채팅 프롬프트
- `strategy` (optional): 전략 이름 (예: "summarize", "review", "generateText")

**Response**
- Type: `String`

### GET /ai/chat/stream
AI 채팅 스트리밍 요청

**Parameters**
- `prompt` (required): 채팅 프롬프트
- `strategy` (optional): 전략 이름

**Response**
- Type: `Server-Sent Events (text/event-stream)`
- Data: `String`

---

## Text Generation APIs

### POST /ai/generate
텍스트 생성 요청 - 플레이스홀더를 문맥과 명령에 맞춰 생성

**Request Body**
```json
{
  "content": "현재 문맥",
  "instruction": "생성 지시사항"
}
```

**Response**
- Type: `String`

### POST /ai/generate/stream
텍스트 생성 스트리밍 요청

**Request Body**
```json
{
  "content": "현재 문맥",
  "instruction": "생성 지시사항"
}
```

**Response**
- Type: `Server-Sent Events (text/event-stream)`
- Data: `String`

---

## Text Summarization APIs

### POST /ai/summarize
텍스트 요약 요청 - 50-100자로 요약

**Request Body**
```json
{
  "text": "요약할 텍스트"
}
```

**Response**
- Type: `String`

### POST /ai/summarize/stream
텍스트 요약 스트리밍 요청

**Request Body**
```json
{
  "text": "요약할 텍스트"
}
```

**Response**
- Type: `Server-Sent Events (text/event-stream)`
- Data: `String`

---

## Text Review APIs

### POST /ai/review
텍스트 리뷰 요청 - 문법, 구조 검토 및 피드백 제공

**Request Body**
```json
{
  "text": "리뷰할 텍스트"
}
```

**Response**
- Type: `String`

### POST /ai/review/stream
텍스트 리뷰 스트리밍 요청

**Request Body**
```json
{
  "text": "리뷰할 텍스트"
}
```

**Response**
- Type: `Server-Sent Events (text/event-stream)`
- Data: `String`
