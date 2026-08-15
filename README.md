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

### API 상세

아래 예시는 프로토타입 fixture인 `GATE-S1`, PassportSession `1`을 사용합니다. 성공
응답은 별도 공통 wrapper 없이 DTO가 JSON 본문으로 바로 반환됩니다. 시각 값과 DB ID는
실행마다 달라집니다.

#### `POST /api/style-spots/{spotCode}/connections`

요청 본문은 필수입니다.

```json
{
  "passportSessionId": 1
}
```

성공 시 `200 OK`:

```json
{
  "spotCode": "GATE-S1",
  "status": "CONNECTED",
  "passportSessionId": 1
}
```

- 선행 상태: Style Spot은 `WAITING` 또는 `RESET`, PassportSession은
  `READY_TO_BOARD`여야 합니다.
- 성공 상태: Style Spot은 `CONNECTED`, PassportSession은 `STYLE_SPOT`이 됩니다.
- 같은 Spot과 세션의 재연결은 현재 상태를 반환합니다.
- 대표 오류: `INVALID_INPUT`(400, 세션 ID 누락), `STYLE_SPOT_NOT_FOUND`(404),
  `PASSPORT_SESSION_NOT_FOUND`(404), `INVALID_SESSION_STATUS`(409),
  `STYLE_SPOT_IN_USE`(409).

#### `POST /api/style-spots/{spotCode}/analysis`

요청 JSON은 없습니다. 요청 본문을 보내지 않습니다.

성공 시 `200 OK`:

```json
{
  "id": 1,
  "passportSessionId": 1,
  "cityCode": "BERLIN_AFTERDARK_NOMAD",
  "cityCodeName": "Berlin Afterdark Nomad",
  "recommendedProductCode": "STARK_BACKPACK",
  "recommendedProductName": "Stark Backpack",
  "styleMood": "AFTERDARK_MOVEMENT",
  "styleMoodName": "Afterdark / Movement",
  "backgroundCode": "BERLIN_AFTERDARK",
  "backgroundAssetKey": "berlin-afterdark",
  "description": "오늘의 반응은 밤의 베를린처럼 자유로운 움직임과 선명한 존재감이 어우러지는 장면을 제안합니다.",
  "matchScore": 92,
  "usedFallback": false,
  "createdAt": "2026-08-15T12:00:00Z"
}
```

- 선행 상태: Style Spot이 `CONNECTED`이고 Response와 Stamp 입력이 있어야 합니다.
  ProductTag는 없어도 됩니다.
- 성공 상태: Style Spot은 `ANALYZING`을 거쳐 `RESULT`가 되며 PassportSession은
  `STYLE_SPOT`에 머뭅니다. `RESULT`에서 재요청하면 저장된 결과를 반환합니다.
- `matchScore`는 Journey 신호와 추천 결과를 화면에 표현하기 위한 **presentation
  affinity** 값입니다. 통계적 확률, 정확도, 모델 confidence가 아닙니다.
- provider가 실패하거나 유효하지 않은 값을 반환하면 검증된 fallback 결과를 저장하고
  `usedFallback`을 `true`로 반환합니다.
- 대표 오류: `STYLE_SPOT_NOT_FOUND`(404), `JOURNEY_NOT_COMPLETED`(409),
  `INVALID_SESSION_STATUS`(409). fallback까지 결과를 만들지 못한 예상 밖 오류는
  `INTERNAL_SERVER_ERROR`(500)로 처리됩니다.

#### `GET /api/style-spots/{spotCode}/result`

요청 JSON은 없습니다. 성공 시 `200 OK`이며 응답 JSON은 위 Style Result와 같습니다.

- 선행 상태: Style Spot이 `RESULT`이고 현재 세션의 StyleResult가 저장되어 있어야 합니다.
- 상태를 변경하지 않는 조회입니다.
- 대표 오류: `STYLE_SPOT_NOT_FOUND`(404), `STYLE_RESULT_NOT_FOUND`(404).

#### `POST /api/passport-sessions/{sessionId}/souvenir`

요청 JSON은 없습니다. 요청 본문을 보내지 않습니다.

최초 생성 시 `201 Created`, 같은 세션의 재요청 시 기존 리소스와 함께 `200 OK`:

```json
{
  "id": 1,
  "passportSessionId": 1,
  "styleResultId": 1,
  "cityCode": "BERLIN_AFTERDARK_NOMAD",
  "cityCodeName": "Berlin Afterdark Nomad",
  "recommendedProductCode": "STARK_BACKPACK",
  "recommendedProductName": "Stark Backpack",
  "styleMood": "AFTERDARK_MOVEMENT",
  "styleMoodName": "Afterdark / Movement",
  "backgroundCode": "BERLIN_AFTERDARK",
  "backgroundAssetKey": "berlin-afterdark",
  "journeyStamps": [
    "ORIGIN_GATE",
    "MATERIAL_LOUNGE",
    "MOVEMENT_DECK",
    "CITY_MOOD_ROOM",
    "PRODUCT_TAGGING"
  ],
  "taggedProductCodes": ["STARK_BACKPACK"],
  "createdAt": "2026-08-15T12:01:00Z"
}
```

- 선행 상태: 해당 PassportSession의 StyleResult가 존재하고 세션이 `STYLE_SPOT`이어야
  합니다. ProductTag가 없었다면 `taggedProductCodes`는 빈 배열입니다.
- 성공 상태: Souvenir 저장과 PassportSession의 `COMPLETED` 전이가 같은 트랜잭션에서
  완료됩니다.
- 대표 오류: `PASSPORT_SESSION_NOT_FOUND`(404), `STYLE_RESULT_NOT_FOUND`(404),
  `INVALID_SESSION_STATUS`(409).

#### `GET /api/passport-sessions/{sessionId}/souvenir`

요청 JSON은 없습니다. 성공 시 `200 OK`이며 응답 JSON은 위 Souvenir와 같습니다.
상태를 변경하지 않으며, 저장된 Souvenir가 없으면 `JOURNEY_SOUVENIR_NOT_FOUND`(404)를
반환합니다.

#### `POST /api/style-spots/{spotCode}/reset`

요청 JSON은 없습니다. 요청 본문을 보내지 않습니다.

성공 시 `200 OK`:

```json
{
  "spotCode": "GATE-S1",
  "status": "RESET",
  "passportSessionId": null
}
```

- `CONNECTED` 또는 `ANALYZING`에서 reset하면 연결을 해제하고 PassportSession을
  `READY_TO_BOARD`로 되돌립니다. 진행 중이던 과거 `analysisAttempt`의 완료/실패 응답은
  이후 시도에 반영되지 않습니다.
- `RESULT`에서 reset하면 Display 연결만 해제합니다. 세션 완료 여부는 Souvenir 생성
  시점에 결정됩니다. `RESET` 재요청은 같은 상태를 반환합니다.
- 대표 오류: `STYLE_SPOT_NOT_FOUND`(404), `INVALID_SESSION_STATUS`(409, 예: `WAITING`).

오류 응답은 다음 공통 형식입니다. `errors`는 Bean Validation 필드 오류가 없으면 빈
배열입니다.

```json
{
  "timestamp": "2026-08-15T12:02:00Z",
  "status": 409,
  "code": "INVALID_SESSION_STATUS",
  "message": "현재 Passport Session 상태에서는 요청을 수행할 수 없습니다.",
  "path": "/api/style-spots/GATE-S1/analysis",
  "errors": []
}
```

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

### OpenAI 연동

OpenAI 연동은 [Responses API text generation](https://developers.openai.com/api/docs/guides/text)과
[Structured Outputs](https://developers.openai.com/api/docs/guides/structured-outputs)를
사용합니다. [공식 Java SDK](https://github.com/openai/openai-java)
`com.openai:openai-java:4.51.0`의 typed Structured Outputs 방식으로
`ResponseCreateParams.builder().text(OpenAIStyleAnalysisOutput.class)`를 호출하고, 파싱된
`OpenAIStyleAnalysisOutput`을 `StyleAnalysisCandidate`로 변환합니다.

기본 provider는 `mock`입니다. `mcm.style.analysis.provider=openai`일 때만
`OpenAIStyleAnalysisProvider`, Responses gateway, OpenAI client bean이 활성화됩니다.
이때 `OPENAI_API_KEY`와 `OPENAI_MODEL`이 비어 있으면 애플리케이션 시작 과정에서
fail-fast합니다. API key는 README, YAML, Git, 로그에 값을 기록하지 않으며 키 모양과
유사한 예제 값도 제공하지 않습니다. `OPENAI_TIMEOUT`은 선택적인 Spring `Duration`
설정이며 기본값은 `30s`입니다. `0` 또는 음수 duration도 시작 과정에서 거부합니다.

현재 Spring 설정은 다음과 같습니다.

```yaml
mcm:
  style:
    analysis:
      provider: ${MCM_STYLE_ANALYSIS_PROVIDER:mock}
      openai:
        api-key: ${OPENAI_API_KEY:}
        model: ${OPENAI_MODEL:}
        timeout: ${OPENAI_TIMEOUT:30s}
```

OpenAI prompt 입력에는 현재 Journey의 Response, Stamp, 선택적인 ProductTag를 Journey
신호로 넣고, 선택 가능한 prototype catalogue 값과 City Code-Background 관계도 함께
제공합니다. 내부 DB ID는 보내지 않습니다. Structured Outputs는
`OpenAIStyleAnalysisOutput` 모양을 강제하지만, 허용된 enum 값이나 City Code-Background
관계까지 신뢰하지는 않습니다. 응답은 반드시 기존 `StyleAnalysisValidator`를 거치고,
provider 예외 또는 검증 실패 시 `RuleBasedStyleFallback`을 거친 뒤 다시 검증합니다.

트랜잭션 경계도 유지합니다. `prepare` 트랜잭션에서 Spot을 `ANALYZING`으로 전환한 뒤
DB lock을 해제하고 외부 Responses API를 호출하며, 검증된 결과만 별도의 `complete`
트랜잭션에서 저장합니다. `analysisAttempt`가 일치하지 않는 과거 응답은 저장하지 않고,
실패 시 현재 attempt만 `CONNECTED`로 복구합니다. 이 분리는 prompt/provider,
validator, fallback, JPA transaction의 책임을 섞지 않기 위한 것입니다.

#### 실제 API smoke test

실제 OpenAI 호출은 자동 테스트와 분리합니다. 자동 테스트와 CI는 기본 mock provider를
사용하고 API key를 요구하거나 외부 네트워크를 호출하지 않아야 합니다. 실제 연결을
확인할 때만 다음 smoke 절차를 수행합니다.

1. API key를 환경의 secret manager 또는 로컬 비공개 실행 설정에 등록합니다. 값을
   README, 명령 기록, 화면 캡처에 남기지 않습니다.
2. `MCM_STYLE_ANALYSIS_PROVIDER=openai`, `OPENAI_MODEL`, 필요하면 duration 형식의
   `OPENAI_TIMEOUT`을 실행 환경에 설정합니다.
3. 테스트용 `READY_TO_BOARD` 세션에 Response와 Stamp를 준비합니다. ProductTag는
   선택 사항입니다.
4. Style Spot 연결 API를 호출한 뒤 분석 API를 한 번 호출합니다.
5. 응답이 `200 OK`, `usedFallback=false`이고 저장 후 결과 조회가 같은 ID와 catalogue
   값을 반환하는지 확인합니다. `usedFallback=true`라면 전체 흐름은 복구됐지만 실제
   OpenAI smoke는 성공한 것으로 판정하지 않습니다.
6. Souvenir 생성 전 세션이 `STYLE_SPOT`, 생성 후 `COMPLETED`인지 확인하고, 사용량과
   민감 정보가 없는 애플리케이션 로그를 함께 점검합니다.

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
