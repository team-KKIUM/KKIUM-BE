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

    @Builder
    public Example(String name) {
        this.name = name;
    }
}
```

### 2. DTO

#### Request

- `record` 사용
- `@Valid` 검증 어노테이션 적극 활용 (`@NotBlank`, `@Email`, `@NotNull` 등)

```java
public record ExampleCreateRequest(
    @NotBlank String name,
    @Email String email
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
}
```

### 4. Service

- `@Service`, `@RequiredArgsConstructor` 사용
- 조회 메서드는 `@Transactional(readOnly = true)`
- 변경 메서드는 `@Transactional`
- 예외는 `BaseException(ErrorCode)` 사용

```java
@Service
@RequiredArgsConstructor
public class ExampleService {

    private final ExampleRepository exampleRepository;

    @Transactional(readOnly = true)
    public ExampleResponse getExample(Long id) {
        Example example = exampleRepository.findById(id)
            .orElseThrow(() -> new BaseException(ErrorCode.EXAMPLE_NOT_FOUND));
        return ExampleResponse.from(example);
    }

    @Transactional
    public ExampleResponse createExample(ExampleCreateRequest request) {
        Example example = exampleRepository.save(
            Example.builder().name(request.name()).build()
        );
        return ExampleResponse.from(example);
    }
}
```

### 5. Controller

- `@Tag`, `@Operation`으로 Swagger 문서화 필수
- 반환 타입은 `ResponseEntity<ApiResponse<T>>`
- `@RequestBody`에는 `@Valid` 추가
- URL 규칙: `GET` 조회 / `POST` 생성 / `PUT` 전체 수정 / `PATCH` 부분 수정 / `DELETE` 삭제

```java
@RestController
@Tag(name = "Example", description = "예시 API")
@RequestMapping("/api/v1/example")
@RequiredArgsConstructor
public class ExampleController {

    private final ExampleService exampleService;

    @Operation(summary = "예시 단건 조회")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ExampleResponse>> getExample(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(exampleService.getExample(id)));
    }

    @Operation(summary = "예시 생성")
    @PostMapping
    public ResponseEntity<ApiResponse<ExampleResponse>> createExample(
        @Valid @RequestBody ExampleCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(exampleService.createExample(request)));
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
| `ApiResponse.successWithNoContent()` | 데이터 없는 성공 (삭제 등) |
| `ApiResponse.fail(ErrorCode)` | 실패 (GlobalExceptionHandler에서 처리) |

### 예외 처리

1. `ErrorCode` enum에 에러 코드 추가 (HTTP 상태코드, 코드 문자열, 메시지)
2. `BaseException(errorCode)` throw
3. `GlobalExceptionHandler`가 자동으로 `ApiResponse.fail()` 변환

```java
// ErrorCode 추가 예시
EXAMPLE_NOT_FOUND(HttpStatus.NOT_FOUND, "E001", "예시를 찾을 수 없습니다.");

// 사용 예시
throw new BaseException(ErrorCode.EXAMPLE_NOT_FOUND);
```

### BaseEntity

모든 엔티티는 `BaseEntity`를 상속합니다. `createdDate`, `updatedDate`가 자동으로 관리됩니다.

---

## 패키지 구조 예시

```
com.kusitms.kkium
├── global
│   ├── config          # Security, Swagger 등 설정
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
│   └── util            # JwtTokenProvider
└── <도메인>
    ├── controller
    ├── domain
    │   └── type        # enum 타입
    ├── dto
    │   ├── request
    │   └── response
    ├── repository
    └── service
```