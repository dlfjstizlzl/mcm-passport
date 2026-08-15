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

## BE2 Style & Result 프로토타입

현재 프로토타입은 BE1이 Journey를 완료해 PassportSession을 `READY_TO_BOARD`로
전환한 이후부터 동작합니다. 이 상태가 Style Spot 연결의 핵심 선행조건입니다. BE2는
BE1 Repository를 직접 조합하지 않고 `JourneyDataReader` 공개 인터페이스를 통해
Response, Stamp, 태그 제품을 분석 입력으로 읽습니다.

현재 분석 입력 정책은 Response와 Journey 진행을 나타내는 Stamp가 존재하는지
확인하지만, 특정 Stamp 개수를 완료 조건으로 하드코딩하지 않습니다. ProductTag는
**선택적 분석 신호**입니다. 태그 제품이 있으면 추천 입력에 함께 사용하고, 없어도
`READY_TO_BOARD` 세션의 Response와 Stamp를 이용해 분석할 수 있습니다. 최종 Journey
완료 판정 책임은 실제 BE1의 PassportSession 상태 또는 공개 인터페이스로 교체되어야
합니다.

프로토타입용 Style Spot `GATE-S1`은 애플리케이션 시작 시 생성됩니다.

| Method | URL | 동작 |
|------|------|------|
| `POST` | `/api/style-spots/{spotCode}/connections` | `passportSessionId`로 Style Spot 연결 |
| `POST` | `/api/style-spots/{spotCode}/analysis` | Journey 데이터 분석 및 결과 저장 |
| `GET` | `/api/style-spots/{spotCode}/result` | 현재 Display 결과 조회 |
| `POST` | `/api/passport-sessions/{sessionId}/souvenir` | Souvenir 생성 및 세션 완료 |
| `GET` | `/api/passport-sessions/{sessionId}/souvenir` | 저장된 Souvenir 조회 |
| `POST` | `/api/style-spots/{spotCode}/reset` | Display 결과 제거 및 Spot reset |

내부 상태는 `WAITING → CONNECTED → ANALYZING → RESULT → RESET` 순서를 지키고,
PassportSession은 `READY_TO_BOARD → STYLE_SPOT → COMPLETED`로 전이합니다. StyleResult
생성만으로 세션을 완료하지 않으며, Souvenir 생성이 성공한 시점에 `COMPLETED`가
됩니다. 분석 실패와 `ANALYZING` 중 reset은 세션을 `READY_TO_BOARD`로 복구하고,
`analysisAttempt`가 지난 응답은 현재 분석 결과를 덮어쓸 수 없습니다.

### 프로토타입 데이터와 실제 기획 데이터의 경계

- **화면 기준 예시:** Figma MVP 결과 화면에서 직접 확인된 조합은
  `Berlin Afterdark Nomad`와 `Stark Backpack`입니다.
- **프로토타입 fixture:** 그 밖의 City Code, Product, Style Mood, Background 값은 API와
  검증 흐름을 시험하기 위한 임시 curated catalog일 수 있으며 확정 기획으로 간주하지
  않습니다. 현재 profile 관계, 표시명, asset key는 `PrototypeStyleCatalog`에 모아
  두었습니다. `RecommendedProduct` enum도 현재는 이 카탈로그의 일부이며 실제 Product
  Entity 또는 Product Code 조회·검증으로 교체해야 합니다.
- **Mock provider:** 현재 `MockStyleAnalysisProvider`는 실제 AI 추천 알고리즘이 아닌
  deterministic test double입니다. API 전체 흐름, Validator, 결과 저장, Souvenir 생성,
  fallback 작동을 확인하기 위한 구현입니다.
- **Fallback:** 현재 `RuleBasedStyleFallback`의 키워드 매핑과 기본 선택은 장애 시
  전체 흐름을 재현하기 위한 임시 deterministic prototype rule입니다. 실제 운영
  fallback 규칙은 확정 기획 데이터로 교체해야 합니다. 테스트 이름도 이 규칙을
  영구 비즈니스 규칙이 아니라 현재 프로토타입 동작으로 표현합니다.

Mock 결과와 fallback 결과도 City Code, 추천 Product, Style Mood, 사전 제작
Background 각각의 카탈로그 유효성과 City Code–Background 관계에 대한 Validator
검증을 통과해야 합니다. 향후에는 확정 curated catalog와 실제 Product 조회/검증을
카탈로그 경계에서 교체하고, 도메인 흐름은 유지하는 것을 목표로 합니다.

Souvenir POST는 최초 생성 시 `201 Created`, 동일 세션 재요청 시 기존 결과와 함께
`200 OK`를 반환합니다. `CONNECTED` 또는 `ANALYZING` 상태에서 reset하면 중단된
연결을 해제하고 세션을 `READY_TO_BOARD`로 복구합니다.

실제 AI 연동은 `StyleAnalysisProvider` 구현을 추가하고
`mcm.style.analysis.provider` 설정으로 provider를 선택하는 방식으로 교체합니다. 실제
OpenAI provider도 현재 Validator와 fallback 뒤에 연결되므로 결과 저장 흐름을 변경할
필요가 없습니다.

현재 `PassportSession`, `JourneyResponse`, `JourneyStamp`, `Product`, `ProductTag`,
`JpaJourneyDataReader`는 BE2 프로토타입 실행을 위한 최소 공통 모델입니다. 실제 BE1
구현과 병합할 때 BE1 Entity/Service로 교체하거나 매핑해야 하며, BE2 쪽 교체 지점은
`JourneyDataReader`로 유지합니다. 이 프로토타입은 BE1의 Journey·Boarding 기능을 대신
구현하지 않습니다.

이번 범위에는 실시간 이미지 생성, Style Portrait, 완성된 My Passport 화면/전용
aggregate, MCM 온라인 계정 연동, NFC/QR 하드웨어 판독을 포함하지 않습니다.


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
```
