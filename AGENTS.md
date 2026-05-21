# KKIUM Backend

## 스택
- Spring Boot 4.x, Java 21, PostgreSQL
- JWT 인증 (이메일/비밀번호, 카카오 소셜)
- 패키지: `com.kusitms.kkium.<도메인>.<계층>`

---

## 코딩 컨벤션

### Entity
- `BaseEntity` 상속 필수 (`createdDate`, `updatedDate` 자동 관리)
- `@Setter` 금지 — 상태 변경은 도메인 메서드로
- `@Enumerated(EnumType.STRING)` 필수
- 테이블명 복수형: `@Table(name = "users")`
- 생성자는 `@Builder` 또는 named builder (`@Builder(builderMethodName = "...")`)

### DTO
- Request/Response 모두 `record` 사용
- Request: 문자열 `@NotBlank`, 이메일 `@Email @NotBlank`
- Response: `from(Entity)` 정적 팩토리로 변환 — 변환 책임은 Response DTO가 가짐

### Service
- `@Service` + `@RequiredArgsConstructor`
- 조회: `@Transactional(readOnly = true)` / 변경: `@Transactional`
- 예외: `throw new BaseException(ERROR_CODE)` — ErrorCode는 static import

### Controller
- `@Tag`, `@Operation` Swagger 문서화 필수
- 반환: `ResponseEntity<ApiResponse<T>>`, 데이터 없으면 `ApiResponse<Void>`
- `@RequestBody`에 `@Valid` 필수
- HTTP 메서드: GET 조회 / POST 생성 / PUT 전체수정 / PATCH 부분수정 / DELETE 삭제

### Enum
- 한국어 표시명 `label` 필드 + `getLabel()` public 선언

---

## 공통 인프라

### ApiResponse
| 메서드 | 사용 상황 |
|--------|-----------|
| `ApiResponse.success(data)` | 데이터 있는 성공 |
| `ApiResponse.successWithNoContent()` | 데이터 없는 성공 (반환 타입 `ApiResponse<Void>`) |
| `ApiResponse.fail(ErrorCode)` | GlobalExceptionHandler에서 자동 처리 |

### 예외 처리
1. `ErrorCode` enum에 추가
2. `throw new BaseException(ERROR_CODE)`
3. `GlobalExceptionHandler`가 자동으로 `ApiResponse.fail()` 변환

### ErrorCode 접두사
| 도메인 | 접두사 |
|--------|--------|
| Common | `C` |
| Auth | `A` |
| User | `U` |
| JD | `J` |
| Experience | `E` |
| Notion | `N` |

### 환경변수
`.env` 파일로 관리, `.gitignore` 필수. `application.yml`에서 `optional:file:.env[.properties]`로 로드.

### JWT 흐름
`POST /api/v1/auth/login` → 토큰 발급 → 이후 요청: `Authorization: Bearer <token>`  
`JwtAuthFilter` → 검증 → `SecurityContext` 저장

---

## 패키지 구조

```
com.kusitms.kkium
├── global
│   ├── config        # SecurityConfig, SwaggerConfig
│   ├── entity        # BaseEntity
│   ├── exception     # ErrorCode, BaseException, GlobalExceptionHandler
│   └── response      # ApiResponse
├── auth
│   ├── filter        # JwtAuthFilter
│   └── utils         # JwtTokenProvider
└── <도메인>
    ├── controller
    ├── domain
    │   └── type      # enum
    ├── dto
    │   ├── request
    │   └── response
    ├── repository
    ├── service
    └── utils
```
