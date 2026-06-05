# KKIUM Backend (KKIUM-BE)
<img width="3840" height="2160" alt="image" src="https://github.com/user-attachments/assets/c682c083-1b07-41c8-b2f4-2435bf95ca46" />
KKIUM 은 흩어진 경험을 한곳으로 모아 AI와 함께 공고와의 매칭률, 자기소개서 작성을 도와주는 서비스입니다. 

- **서비스:** [https://www.kkium.com](https://www.kkium.com)
- **저장소:** [team-KKIUM/KKIUM-BE](https://github.com/team-KKIUM/KKIUM-BE)
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/f834174a-e8da-43de-af4f-8321b6898981" />
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/de5230b7-b776-472c-a51c-c0cdf984c059" />
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/2ca840d9-54ba-407d-8c48-f9f8d616886d" />
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/be139364-45be-4ef7-b860-db9fc7af5736" />
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/744b9672-e8d6-4567-86ec-3769b03dfffb" />
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/cec913a3-312b-4790-864d-38db3097ec43" />
<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/df966bd3-89ac-4c4c-ad29-17bdadf500fe" />
<img width="3840" height="2160" alt="image" src="https://github.com/user-attachments/assets/c6d43700-257b-4316-a12c-0db3276c67c3" />
<img width="3840" height="2160" alt="image" src="https://github.com/user-attachments/assets/ae8ad543-e901-4989-b1dc-3373dec99411" />
<img width="3840" height="2160" alt="image" src="https://github.com/user-attachments/assets/566c65eb-1640-4156-baaa-1d99c0d52611" />
<img width="3840" height="2160" alt="image" src="https://github.com/user-attachments/assets/74cf2ac1-f64d-4591-9640-baec3f77eb8f" />
<img width="3840" height="2160" alt="image" src="https://github.com/user-attachments/assets/9d1a4d44-438a-41c3-99eb-cd04ca797511" />
<img width="3840" height="2160" alt="image" src="https://github.com/user-attachments/assets/ba0bb4c9-8119-49c9-a46b-e166dc3a6215" />
<img width="3840" height="2160" alt="image" src="https://github.com/user-attachments/assets/a970037b-79df-40e4-bf47-1ff7f3b3f578" />
<img width="3840" height="2160" alt="image" src="https://github.com/user-attachments/assets/c8bea3f2-ef36-4972-a18f-c13a1dc4aa13" />
<img width="3840" height="2160" alt="image" src="https://github.com/user-attachments/assets/8324756e-ba5f-4347-b9de-42011d96eae4" />

---

## 프로젝트 구성

### 기술 스택

| 구분 | 기술 |
|------|------|
| 언어 / 프레임워크 | Java 21, Spring Boot 4.0.5 |
| 웹 / API | Spring Web MVC, Spring WebFlux WebClient |
| 인증 / 인가 | Spring Security, JWT, OAuth2 Client |
| 소셜 로그인 | Kakao, Google |
| ORM / DB 접근 | Spring Data JPA, JPQL `@Query`, JdbcTemplate |
| 데이터베이스 | PostgreSQL |
| 벡터 검색 / 임베딩 저장 | pgvector |
| 캐시 / 토큰 저장 | Redis |
| AI / LLM | Gemini API, OpenAI Embedding |
| 크롤링 / 파싱 | Jsoup, Playwright |
| 파일 / 문서 처리 | Apache PDFBox |
| API 문서화 | Swagger / Springdoc OpenAPI |
| 모니터링 | Spring Actuator, Micrometer, Prometheus, Grafana |
| 테스트 | JUnit 5, Mockito, Spring Security Test, MockWebServer, Testcontainers, JaCoCo |
| 코드 품질 | Spotless, google-java-format |
| 패키지 매니저 / 빌드 | Gradle |
| 컨테이너 / 배포 | Docker, GitHub Actions, Artifact Registry, GKE, ArgoCD |
| 시크릿 관리 | GCP Secret Manager, External Secrets, Workload Identity |

### 시스템 아키텍처 
<img width="7680" height="4320" alt="image" src="https://github.com/user-attachments/assets/b2f42b9d-5610-4c23-9273-d1a330703350" />


### 디렉터리 구조

```text
KKIUM-BE/
├── src/
│   ├── main/
│   │   ├── java/com/kusitms/kkium/
│   │   │   ├── auth/              # 인증/인가, JWT, 소셜 로그인, refresh token
│   │   │   ├── user/              # 회원 프로필, 계정 삭제/복구
│   │   │   ├── experience/        # 경험 관리, 경험 분석, 태그, 임베딩
│   │   │   ├── jd/                # 공고 등록/분석, 크롤링, 매칭, 공고 임베딩
│   │   │   ├── resume/            # 자기소개서 답변, AI 초안, 작성 가이드
│   │   │   ├── notion/            # Notion OAuth, 페이지 조회, 경험 분석 연동
│   │   │   ├── home/              # 홈 대시보드
│   │   │   └── global/            # 공통 설정, 예외, 응답, BaseEntity, 공통 유틸
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-local.yml
│   │       ├── application-prod.yml
│   │       └── prompts/           # LLM 프롬프트 및 JSON Schema
│   └── test/
│       ├── java/com/kusitms/kkium/
│       │   ├── global/            # Testcontainers 기반 통합 테스트 베이스
│       │   ├── experience/        # 경험 관리, LLM 분석, 임베딩, N+1, E2E 테스트
│       │   ├── jd/                # 공고 크롤링, 임베딩, 매칭, 문항 테스트
│       │   ├── resume/            # 자기소개서 답변, AI 초안, 작성 가이드 테스트
│       │   ├── notion/            # Notion 연동 테스트
│       │   └── home/              # 홈 대시보드 테스트
│       └── resources/
│           ├── application.yml    # 테스트 전용 profile
│           └── init-pgvector.sql  # pgvector extension 초기화
├── k8s/
│   ├── app/                       # GKE 애플리케이션 배포 매니페스트
│   └── monitoring/                # Prometheus / Grafana 매니페스트
├── argocd/                        # ArgoCD Application, Ingress, Notifications 설정
├── .github/
│   ├── workflows/                 # GitHub Actions 배포 파이프라인
│   ├── ISSUE_TEMPLATE/
│   └── pull_request_template.md
├── Dockerfile
├── docker-compose.yml             # 로컬 PostgreSQL(pgvector) 실행
├── build.gradle
└── settings.gradle
```

---

### Contributors

<table>
  <tr>
    <td align="center"><b>김유진</b></td>
    <td align="center"><b>우지원</b></td>
    <td align="center"><b>최수희</b></td>
  </tr>
  <tr>
    <td align="center">
      <a href="https://github.com/ouob123">
        <img src="https://github.com/ouob123.png" width="100" height="100" alt="김유진" style="border-radius:50%;" />
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/jiwonniy">
        <img src="https://github.com/jiwonniy.png" width="100" height="100" alt="우지원" style="border-radius:50%;" />
      </a>
    </td>
    <td align="center">
      <a href="https://github.com/erika0915">
        <img src="https://github.com/erika0915.png" width="100" height="100" alt="최수희" style="border-radius:50%;" />
      </a>
    </td>
  </tr>
  <tr>
    <td align="center">
      <a href="https://github.com/ouob123">@ouob123</a>
    </td>
    <td align="center">
      <a href="https://github.com/jiwonniy">@jiwonniy</a>
    </td>
    <td align="center">
      <a href="https://github.com/erika0915">@erika0915</a>
    </td>
  </tr>
</table>
