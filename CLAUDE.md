# KKIUM Backend - 개발 가이드

## 프로젝트 개요

- **Framework**: Spring Boot 4.x, Java 21
- **Database**: PostgreSQL
- **인증**: JWT (이메일/비밀번호 기반 기본 로그인, 카카오 소셜 로그인)
- **패키지 구조**: `com.kusitms.kkium.<도메인>.<계층>`

---

## 도메인 개발 가이드

### 1. Domain (Entity)

- 테이블 이름은 복수형 권장: `@Table(name = "users")`
- `BaseEntity` 반드시 상속 (`createdDate`, `updatedDate` 자동 관리)
- `@NoArgsConstructor` 필수, 생성자는 `@Builder` 또는 named builder 활용
- `@Setter` 사용 금지 — 상태 변경은 도메인 메서드로 처리
- enum 타입 컬럼에는 `@Enumerated(EnumType.STRING)` 필수 (숫자 저장 금지)

```java
@Table(name = "examples")
@Entity
@Getter
@NoArgsConstructor
public class Example extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ExampleStatus status;

    @Builder
    public Example(String name) {
        this.name = name;
        this.status = ExampleStatus.ACTIVE;
    }
}
```

#### Named Builder 패턴

목적이 다른 생성자가 여러 개 필요할 때 named builder를 사용합니다.

```java
@Builder(builderMethodName = "basicLoginBuilder", builderClassName = "buildBasicLogin")
public User(String name, String email, String password) { ... }

// 사용
User.basicLoginBuilder().name(...).email(...).password(...).build();
```

### 2. DTO

#### Request

- `record` 사용, `@Builder` 불필요
- 문자열 필드에는 `@NotBlank` 사용 (`@NotNull`은 빈 문자열을 허용하므로 부적절)
- 이메일 필드에는 `@Email @NotBlank` 함께 사용

```java
public record ExampleCreateRequest(
    @NotBlank String name,
    @Email @NotBlank String email
) {}
```

#### Response

- `record` 사용
- `from()` 정적 팩토리 메서드로 엔티티 → DTO 변환
- **엔티티를 Response로 변환하는 책임은 Response DTO가 직접 가짐**

```java
public record ExampleResponse(Long id, String name) {
    public static ExampleResponse from(Example example) {
        return new ExampleResponse(example.getId(), example.getName());
    }
}
```

### 3. Repository

- `JpaRepository<Entity, ID>` 상속
- 커스텀 쿼리는 메서드 네이밍 또는 `@Query` 활용

```java
public interface ExampleRepository extends JpaRepository<Example, Long> {
    Optional<Example> findByEmail(String email);
    boolean existsByEmail(String email);
}
```

### 4. Service

- `@Service`, `@RequiredArgsConstructor` 사용
- 조회 메서드는 `@Transactional(readOnly = true)`
- 변경 메서드는 `@Transactional`
- 예외는 `BaseException(ErrorCode)` 사용
- `ErrorCode`는 static import로 사용

```java
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.EXAMPLE_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ExampleService {

    private final ExampleRepository exampleRepository;

    @Transactional(readOnly = true)
    public ExampleResponse getExample(Long id) {
        Example example = exampleRepository.findById(id)
            .orElseThrow(() -> new BaseException(EXAMPLE_NOT_FOUND));
        return ExampleResponse.from(example);
    }
}
```

### 5. Controller

- `@Tag`, `@Operation`으로 Swagger 문서화 필수
- 반환 타입은 `ResponseEntity<ApiResponse<T>>`
- 데이터 없는 성공 응답은 `ResponseEntity<ApiResponse<Void>>`
- `@RequestBody`에는 `@Valid` 추가
- URL 규칙: `GET` 조회 / `POST` 생성 / `PUT` 전체 수정 / `PATCH` 부분 수정 / `DELETE` 삭제

```java
@RestController
@Tag(name = "Example", description = "예시 API")
@RequestMapping("/api/v1/example")
@RequiredArgsConstructor
public class ExampleController {

    private final ExampleService exampleService;

    @Operation(summary = "예시 생성")
    @PostMapping
    public ResponseEntity<ApiResponse<ExampleResponse>> createExample(
        @Valid @RequestBody ExampleCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(exampleService.createExample(request)));
    }

    @Operation(summary = "예시 삭제")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExample(@PathVariable Long id) {
        exampleService.deleteExample(id);
        return ResponseEntity.ok(ApiResponse.successWithNoContent());
    }
}
```

### 6. Enum

- 한국어 표시명(`label`)을 생성자로 바인딩
- `getLabel()`을 `public`으로 선언

```java
public enum ExampleStatus {
    ACTIVE("활성"),
    INACTIVE("비활성");

    private final String label;

    ExampleStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
```

---

## 공통 인프라

### ApiResponse

모든 응답은 `ApiResponse`로 감싸서 반환합니다.

| 메서드 | 사용 상황 |
|--------|-----------|
| `ApiResponse.success(data)` | 데이터 있는 성공 |
| `ApiResponse.success(message, data)` | 커스텀 메시지 + 데이터 |
| `ApiResponse.successWithNoContent()` | 데이터 없는 성공 (삭제 등) → 컨트롤러 반환 타입은 `ApiResponse<Void>` |
| `ApiResponse.fail(ErrorCode)` | 실패 (GlobalExceptionHandler에서 자동 처리) |

### 예외 처리

1. `ErrorCode` enum에 에러 코드 추가
2. `BaseException(errorCode)` throw
3. `GlobalExceptionHandler`가 자동으로 `ApiResponse.fail()` 변환

#### ErrorCode 도메인 접두사 규칙

| 도메인 | 접두사 | 예시 |
|--------|--------|------|
| Common | `C` | `C001`, `C002` |
| Auth | `A` | `A001`, `A002` |
| User | `U` | `U001`, `U002` |

```java
// ErrorCode 추가 예시
USER_ALREADY_EXISTS(HttpStatus.CONFLICT, "A001", "이미 존재하는 이메일입니다."),
INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "A002", "이메일 또는 비밀번호가 올바르지 않습니다.");

// 사용 예시 (static import 권장)
import static com.kusitms.kkium.global.exception.errorcode.ErrorCode.USER_NOT_FOUND;
throw new BaseException(USER_NOT_FOUND);
```

### 환경변수 관리

민감한 설정값은 `.env` 파일로 관리합니다. `application.yml`에서 `optional:file:.env[.properties]`로 로드합니다.

```properties
# .env
DB_NAME=kkium
DB_USER=kkium
DB_PASSWORD=kkium1234
JWT_SECRET=your-secret-key
JWT_ACCESS_TOKEN_EXPIRATION=18000000
```

`.env` 파일은 `.gitignore`에 반드시 추가합니다.

### JWT 인증 흐름

1. `POST /api/v1/auth/login` → `JwtTokenProvider.createToken(userId)` → 토큰 발급
2. 이후 요청 헤더: `Authorization: Bearer <token>`
3. `JwtAuthFilter` → `JwtTokenProvider.validateToken()` → `CustomUserDetailService.loadUserByUsername()` → `SecurityContext` 저장

### Security 경로 설정

인증 없이 접근 가능한 경로만 `permitAll()` 처리합니다.

```java
.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", ...).permitAll()
.requestMatchers("/api/v1/auth/**").permitAll()  // 로그인/회원가입
.anyRequest().authenticated()
```

---

## 패키지 구조

```
com.kusitms.kkium
├── global
│   ├── config          # SecurityConfig, SwaggerConfig
│   ├── entity          # BaseEntity
│   ├── exception
│   │   ├── errorcode   # ErrorCode enum
│   │   ├── handler     # GlobalExceptionHandler
│   │   └── BaseException
│   └── response        # ApiResponse
├── auth
│   ├── controller
│   ├── dto
│   │   ├── request
│   │   └── response
│   ├── filter          # JwtAuthFilter
│   ├── service
│   └── utils           # JwtTokenProvider
└── <도메인>
    ├── controller
    ├── domain
    │   └── type        # enum 타입
    ├── dto
    │   ├── request
    │   └── response
    ├── repository
    ├── service
    └── utils           # CustomUserDetailService, CustomUserDetails 등
```