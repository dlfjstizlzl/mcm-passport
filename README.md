# MCM Passport Backend

오프라인 Journey 데이터를 수집하고 Style Spot에서 개인화 결과를 만드는 Spring Boot 백엔드입니다.

## 기술 스택

- Java 21, Spring Boot 4, Gradle
- Spring MVC, Spring Data JPA, Bean Validation
- MySQL (로컬/운영), H2 (테스트)

## 실행 준비

1. MySQL에 `mcm_passport` 데이터베이스를 생성합니다.
2. `src/main/resources/application-secret.example.yml`을 `application-secret.yml`로 복사하고 로컬 비밀번호를 입력합니다.
3. `.\gradlew.bat bootRun`으로 실행합니다.

설정은 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JPA_DDL_AUTO`, `SERVER_PORT` 환경변수로 덮어쓸 수 있습니다.
테스트는 로컬 MySQL과 무관한 H2를 사용하며 `.\gradlew.bat test`로 실행합니다.

## 패키지 구조

```text
com.mcm.passport
├─ domain
│  ├─ passport   # PassportCard, PassportSession과 상태
│  ├─ journey    # Spot, 질문/답변, Stamp
│  ├─ product    # Product, ProductTag
│  ├─ boarding   # BoardingPass
│  └─ style      # StyleSpot, AI 결과, Portrait, Souvenir
└─ global        # 설정, 예외, 공통 응답
```

각 도메인은 `controller`, `dto`, `entity`, `repository`, `service` 계층을 사용합니다.

## 두 명의 작업 경계

### BE1 — Journey & Boarding

- `domain.passport`, `domain.journey`, `domain.product`, `domain.boarding`
- Session, Guide Response, Journey Stamp, Product Tag, Boarding Pass
- `READY_TO_BOARD` 상태 전이

### BE2 — Style & Result

- `domain.style`
- Style Spot 연결/해제, Display, AI 분석과 fallback
- Style Result, Portrait, Souvenir, My Passport
- `STYLE_SPOT`, `COMPLETED` 상태 전이

`global`, Gradle 설정, `PassportSession`의 공개 메서드와 상태 Enum, DB 스키마는 공동
소유입니다. 공통 파일은 변경 전에 인터페이스를 합의하고 기능 커밋과 분리합니다.
BE2는 BE1 Repository를 직접 조합하지 않고 공개 Service 또는 명시적인 도메인
인터페이스를 사용합니다.


## Commit Convention

- 커밋은 **기능 단위**로 작성한다.
- Commit Message Convention을 반드시 준수한다.
- 커밋 메시지는 `<type>: <작업 내용>` 형식으로 작성한다.

### Commit Type

| Type | 설명 |
|------|------|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 기능 변화 없이 코드 개선 |
| `test` | 테스트 코드 추가 및 수정 |
| `docs` | 문서 추가 및 수정 |
| `chore` | 설정, 빌드 등 기타 작업 |

### Example

```text
feat: add passport session API
fix: prevent duplicate product tag
refactor: simplify boarding pass service
test: add passport session service test
docs: update README
chore: configure H2 test database
