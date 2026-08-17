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
BE1 Repository를 Style 계층에서 직접 조합하지 않고 `JpaJourneyDataReader` 구현을 통해
실제 `GuideResponse`, `JourneyStamp`, 선택적인 `ProductTag`를 분석 입력으로 읽습니다.

현재 분석 입력 정책은 Response와 Journey 진행을 나타내는 Stamp가 존재하는지
확인하지만, 특정 Stamp 개수를 완료 조건으로 하드코딩하지 않습니다. ProductTag는
**선택적 분석 신호**입니다. 태그 제품이 있으면 추천 입력에 함께 사용하고, 없어도
`READY_TO_BOARD` 세션의 Response와 Stamp를 이용해 분석할 수 있습니다. 최종 Journey
완료 판정 책임은 실제 BE1의 PassportSession 상태 또는 공개 인터페이스로 교체되어야
합니다.

프로토타입용 Style Spot `GATE-S1`은 애플리케이션 시작 시 생성됩니다.

| Method | URL | 동작 |
|------|------|------|
| `POST` | `/api/style-spots/{styleSpotId}/connect` | StyleSpotSession 생성 및 동기 분석 |
| `POST` | `/api/style-spot-sessions/{styleSpotSessionId}/disconnect` | 연결 종료 |
| `GET` | `/api/style-spots/{styleSpotId}/display` | Spot 상태와 현재 결과 조회 |
| `GET` | `/api/passport-sessions/{sessionId}/style-result` | 세션의 Style Result 조회 |
| `POST/GET/DELETE` | `/api/passport-sessions/{sessionId}/portrait` | Portrait metadata 관리 |
| `POST` | `/api/passport-sessions/{sessionId}/souvenir` | Souvenir 생성 및 세션 완료 |
| `GET` | `/api/passport-sessions/{sessionId}/souvenir` | 저장된 Souvenir 조회 |
| `GET` | `/api/passport-sessions/{sessionId}/my-passport` | Journey와 결과 aggregate 조회 |

기존 `/connections`, `/analysis`, `/result`, `/reset` 경로는 prototype regression 확인을
위해 호환 경로로 남아 있으며 신규 연동은 위 공식 API를 사용합니다.

### Legacy prototype API 상세

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
`com.openai:openai-java:4.51.0`으로
`ResponseCreateParams.builder().text(OpenAIStyleAnalysisOutput.class)`를 호출합니다. 이 호출이
생성한 `StructuredResponseCreateParams.rawParams()`에는 server-side JSON Schema 설정이 그대로
포함됩니다. Gateway가 일반 `Response`를 먼저 받도록 분리되어도 Structured Outputs를 단순한
JSON prompt로 대체하지 않으며, 서버의 schema enforcement를 유지합니다.

SDK 4.51.0의 typed `create`는 먼저 일반 `Response`를 생성한 뒤 이를
`StructuredResponse<T>`로 감싸며, 사용자 DTO 변환은 `outputText()`를 읽을 때 지연 실행됩니다.
이 프로젝트는 그 공식 흐름을 명시적으로 분리해 usage를 먼저 보존합니다. 실제 처리 순서와
진단 stage 대응은 다음과 같습니다.

| 순서 | 진단 stage | 처리 내용 |
|---:|---|---|
| 1 | `HTTP_REQUEST` | `withRawResponse().create(structuredParams.rawParams())`로 HTTP 응답 handle을 받고 status와 request ID를 확보합니다. |
| 2 | `SDK_RESPONSE_DESERIALIZATION` | `HttpResponseFor<Response>.parse()`로 일반 Responses API `Response`를 해석합니다. |
| 3 | `USAGE_MAPPING` | 응답의 input, cached, cache-write, output, reasoning, total token usage를 benchmark metric으로 변환합니다. |
| 4 | `RESPONSE_METADATA` | 응답 model과 안전한 HTTP metadata를 확정합니다. |
| 5 | `STRUCTURED_OUTPUT_DESERIALIZATION` | 일반 `Response`를 `StructuredResponse<OpenAIStyleAnalysisOutput>`으로 감싼 뒤 output text를 사용자 DTO로 지연 파싱합니다. |
| 6 | `VALIDATION` | 파싱된 DTO를 `StyleAnalysisCandidate`로 변환하고 기존 `StyleAnalysisValidator`로 검증합니다. |

일반 응답 status가 `INCOMPLETE`이고 reason이 `MAX_OUTPUT_TOKENS`이면 DTO 파싱 전에
`STRUCTURED_OUTPUT_DESERIALIZATION` / `RESPONSE_INCOMPLETE_MAX_OUTPUT_TOKENS`로 기록합니다.
이 경로에서도 앞서 읽은 usage와 metadata는 보존되므로 단순 DTO 불일치와 token 상한으로 인한
응답 중단을 구분할 수 있습니다.

일반 `Response`까지 정상적으로 확보했다면 이후 Structured Output DTO 파싱이 실패해도 먼저
읽은 usage, model, provider latency를 실패 결과에 보존합니다. 반대로 SDK가 일반 `Response`
자체를 해석하지 못하면 typed usage를 얻을 수 없으므로 임의로 raw body를 재해석하지 않습니다.
이 경우 usage를 측정 불가로 유지하고 비용 guard가 fail-closed로 benchmark를 중단합니다.

실패 결과에는 `failureType`, `failureStage`, `safeFailureDetail`, `httpStatus`, `errorCode`,
`requestId`만 진단 metadata로 기록합니다. SDK의 Structured Output 변환 예외 message에는 응답
JSON이 포함될 수 있으므로 `exception.getMessage()`를 report나 로그에 복사하지 않습니다.
`safeFailureDetail`은 미리 정의한 비민감 진단 문구만 허용하며 raw response body, prompt,
Journey 원문, description, API key는 저장하거나 출력하지 않습니다.

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
        reasoning-effort: ${MCM_OPENAI_REASONING_EFFORT:none}
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

BE2 전용 Demo Journey production seed는 BE1 통합 후 제거되었습니다. PassportCard와
Stark Backpack 기본 데이터는 BE1 `ReferenceDataInitializer`만 생성합니다.

#### STEP A — 로컬 OpenAI usage benchmark

Benchmark는 BE1, MySQL, REST endpoint 없이 전용 `openAiBenchmark` Gradle task로 실행합니다.
일반 `bootRun`이나 CI에서는 실행되지 않으며, 아래 두 flag가 모두 `true`일 때만 실제
유료 요청을 보낼 수 있습니다.

```text
MCM_OPENAI_BENCHMARK=true
MCM_OPENAI_BENCHMARK_CONFIRM_LIVE=true
```

기본 baseline은 다음과 같습니다.

- models: `gpt-5.6-luna`, `gpt-5.6-terra`, `gpt-5.6-sol`
- cases: ProductTag가 없는 `CASE_A_WITHOUT_PRODUCT_TAG`, `STARK_BACKPACK`만 추가한
  `CASE_B_WITH_PRODUCT_TAG`
- repetitions: 각 조합 5회
- reasoning effort: `none`
- planned calls: 3 models × 2 cases × 5 = 최대 30회
- call cap: 30
- estimated-cost cap: USD 1.00
- maximum output tokens: 512

`MCM_OPENAI_BENCHMARK_CASES`는 실행할 fixture의 정확한 대소문자 이름을 comma-separated
목록으로 지정하며 기본값은 Case A와 Case B 모두입니다. 현재 허용값은
`CASE_A_WITHOUT_PRODUCT_TAG`, `CASE_B_WITH_PRODUCT_TAG`입니다.
`MCM_OPENAI_BENCHMARK_MAX_OUTPUT_TOKENS`는 실제 요청과 다음 호출의 비용 예약에 함께 사용하는
상한이며 기본값은 512, 허용 범위는 1~25,000입니다.

[GPT-5.6 Luna 모델 문서](https://developers.openai.com/api/docs/models/gpt-5.6-luna)는
`reasoning.effort=none`과 Structured Outputs를 지원합니다. 따라서 첫 진단 pilot과 baseline은
`none`을 유지합니다. [`max_output_tokens`](https://developers.openai.com/api/docs/guides/reasoning)는
reasoning token, visible output, non-visible formatting token을 모두 제한하고, 부족하면 응답이
`incomplete`가 될 수 있습니다. 기본 30회 baseline은 기존 512를 유지하되 첫 1회 진단에서는
truncation 가능성을 줄이기 위해 4,096을 명시합니다.

비용 guard는 고정 fixture의 rendered prompt가 4,096 UTF-8 bytes 이하인지 실행 전에 검증하고,
첫 호출부터 현재 모델의 8,192 input-token 예약분을 input/cache-write
중 더 비싼 요율로 계산하고, 설정된 maximum output token 전부를 output 요율로 예약합니다. 이
예약 비용과 기존 누적 추정액의 합이 cap을 넘으면 다음 유료 호출 전에 중단합니다.
이는 Standard short-context snapshot에 기반한 보수적 benchmark 안전장치이지 실제 청구 상한을
보증하는 billing control은 아닙니다.

이번 수정 이후 diagnostic pilot 재검증 상태는 **`PENDING_LOCAL_LIVE_RUN`**입니다. Cloud, 자동 테스트,
GitHub Actions에서는 live benchmark를 실행하지 않으며 token, latency, 비용, 모델 결과를
추측해서 기록하지 않습니다.

##### Luna / Case A 1회 diagnostic pilot

첫 재검증은 30회 baseline이 아니라 Luna와 ProductTag가 없는 Case A를 정확히 한 번만
호출합니다. Windows CMD에서 프로젝트 경로로 이동한 뒤 아래 블록을 그대로 실행합니다.
API key 값은 출력하지 않고 존재 여부만 확인합니다.

```bat
cd /d "C:\Users\user\mcm-passport"
if defined OPENAI_API_KEY (echo OpenAI API key configured: yes) else (echo OpenAI API key configured: no)

set "MCM_OPENAI_BENCHMARK=true"
set "MCM_OPENAI_BENCHMARK_CONFIRM_LIVE=true"
set "MCM_OPENAI_BENCHMARK_MODELS=gpt-5.6-luna"
set "MCM_OPENAI_BENCHMARK_CASES=CASE_A_WITHOUT_PRODUCT_TAG"
set "MCM_OPENAI_BENCHMARK_REPETITIONS=1"
set "MCM_OPENAI_BENCHMARK_MAX_CALLS=1"
set "MCM_OPENAI_BENCHMARK_MAX_OUTPUT_TOKENS=4096"
set "MCM_OPENAI_BENCHMARK_MAX_ESTIMATED_USD=0.10"
set "MCM_OPENAI_REASONING_EFFORT=none"
set "MCM_OPENAI_BENCHMARK_REPORT_DIR=build/reports/openai-benchmark"
set "MCM_STYLE_ANALYSIS_PROVIDER=openai"
call gradlew.bat openAiBenchmark
```

호출 전 preflight에서 다음 값이 보여야 합니다. `Report directory`는 같은 경로의 절대 경로로
표시됩니다.

```text
OpenAI API key configured: yes
Benchmark enabled: yes
Live confirmation: yes
Models: [gpt-5.6-luna]
Cases: [CASE_A_WITHOUT_PRODUCT_TAG]
Repetitions: 1
Reasoning effort: none
Planned calls: 1
Maximum calls: 1
Maximum output tokens: 4096
Maximum estimated USD: 0.10
Report directory: ...\build\reports\openai-benchmark
```

Pilot 성공 조건은 report에 정확히 한 run만 존재하고 `usedFallback=false`,
`errorCategory=NONE`이며 `inputTokens`, `outputTokens`, `totalTokens`가 실제 숫자로 기록되는
것입니다. `cachedInputTokens`, `cacheWriteTokens`, `reasoningTokens`는 SDK가 제공한 경우 실제
값을 기록하고 미제공 값은 `UNAVAILABLE`로 유지합니다. cache 관련 usage가 미제공된 비용
계산에서는 해당 input을 가장 비싼 input category로 보수적으로 계산하며 0으로 추정하지
않습니다. 성공 run에는 failure diagnostic이 없으므로 콘솔에서는 해당 필드가 `UNAVAILABLE`,
CSV에서는 빈 값으로 남습니다. 결과의 City Code, Product, Style Mood, Background, matchScore도
Validator를 통과해야 합니다.
콘솔의 최종 상태는 `Benchmark status: COMPLETED`, `Termination reason: NONE`이어야 합니다.

실패하거나 fallback이 사용되면 먼저 한 run의 `failureStage`, `failureType`,
`safeFailureDetail`, 안전한 HTTP metadata와 usage를 확인합니다. usage를 얻지 못하거나 핵심
`inputTokens`, `outputTokens`, `totalTokens` 중 하나가 없거나 pricing을 확인할 수 없으면
`Termination reason: COST_ESTIMATE_UNAVAILABLE`로 중단되는 것이 정상적인 비용 안전 동작입니다.
이 pilot이 위 성공 조건을 만족하기 전에는 아래 30회 baseline으로 확대하지 않습니다.

Windows CMD에서 저장된 key의 실제 값을 출력하지 않고 존재 여부만 확인한 뒤 다음 명령을
그대로 실행합니다. `setx`는 사용하지 않으며 아래 `set` 값은 현재 CMD 프로세스에만
적용됩니다. `OPENAI_MODEL`은 STEP B REST smoke용 설정이고, benchmark는 아래 model 목록을
사용합니다.

```bat
if defined OPENAI_API_KEY (echo OpenAI API key configured: yes) else (echo OpenAI API key configured: no)

set "MCM_OPENAI_BENCHMARK=true"
set "MCM_OPENAI_BENCHMARK_CONFIRM_LIVE=true"
set "MCM_OPENAI_BENCHMARK_MODELS=gpt-5.6-luna,gpt-5.6-terra,gpt-5.6-sol"
set "MCM_OPENAI_BENCHMARK_CASES=CASE_A_WITHOUT_PRODUCT_TAG,CASE_B_WITH_PRODUCT_TAG"
set "MCM_OPENAI_BENCHMARK_REPETITIONS=5"
set "MCM_OPENAI_BENCHMARK_MAX_CALLS=30"
set "MCM_OPENAI_BENCHMARK_MAX_OUTPUT_TOKENS=512"
set "MCM_OPENAI_BENCHMARK_MAX_ESTIMATED_USD=1.00"
set "MCM_OPENAI_REASONING_EFFORT=none"
set "MCM_STYLE_ANALYSIS_PROVIDER=openai"
call gradlew.bat openAiBenchmark
```

Runner preflight에서 key의 존재 여부, models, cases, repetitions, reasoning effort, planned
calls, call cap, maximum output tokens, estimated-cost cap, report directory를 먼저 확인합니다. Key가 없거나 두 live
flag 중 하나라도 `false`이면 유료 호출 전에 종료해야 합니다. 실행 결과는 Git에서 제외된
다음 경로에 생성됩니다.

```text
build/reports/openai-benchmark/runs.csv
build/reports/openai-benchmark/summary.json
build/reports/openai-benchmark/summary.md
```

Report에는 model/case/effort별 결과, fallback 여부, provider/end-to-end latency, token usage,
예상 비용과 결과 일관성 지표를 기록합니다. Raw prompt, raw response, description 원문, API
key는 로그나 report에 저장하지 않습니다. 실제 live report도 Git에 commit하지 않습니다.
`usedFallback=true`는 BE2 복구 흐름은 성공했지만 OpenAI benchmark run은 실패한 것으로
집계합니다.

Responses API usage의 `inputTokens`는 cached read와 cache write를 포함한 전체 input입니다.
`cachedInputTokens`와 `cacheWriteTokens`는 그 하위 과금 구간이며, 나머지만 일반 input으로
계산합니다. [Reasoning](https://developers.openai.com/api/docs/guides/reasoning)의
`reasoningTokens`는 `outputTokens`에 포함되어 output 가격으로 과금되므로 다시
더하지 않습니다. SDK가 제공하지 않은 usage 값은 0으로 만들지 않고 측정 불가로 유지합니다.
예상 비용은 다음 구간을 한 번씩만 계산한 benchmark 추정치이며 실제 청구 금액이 아닙니다.

```text
ordinaryInput = inputTokens - cachedInputTokens - cacheWriteTokens
estimatedCost = ordinaryInput * inputPrice
              + cachedInputTokens * cachedInputPrice
              + cacheWriteTokens * cacheWritePrice
              + outputTokens * outputPrice
```

[OpenAI API Pricing](https://developers.openai.com/api/docs/pricing)을 2026-08-16에 확인한
Standard short-context 가격 snapshot은 다음과 같습니다. 단위는 USD / 1M tokens입니다.

| Model | Input | Cached input | Cache write | Output |
|---|---:|---:|---:|---:|
| `gpt-5.6-luna` | 0.20 | 0.02 | 0.25 | 1.20 |
| `gpt-5.6-terra` | 2.00 | 0.20 | 2.50 | 12.00 |
| `gpt-5.6-sol` | 5.00 | 0.50 | 6.25 | 30.00 |

이 snapshot과 계산기는 Standard short-context만 지원합니다. 272K input tokens를 초과하면
계산기는 잘못된 short-context 요율을 적용하지 않고 비용을 측정 불가로 처리하여
benchmark를 중단합니다. Long-context는 별도 가격 band가 구현된 후에만 측정해야 합니다.
Regional processing 등 별도 service tier의 실제 청구는 이 Standard 추정에서 제외합니다.

[Prompt Caching](https://developers.openai.com/api/docs/guides/prompt-caching)은 exact prefix와
최소 1,024-token cacheable prefix를 전제로 하며 cache hit를 보장하지 않습니다. 첫 로컬
benchmark에서는 현재 prompt 구조를 그대로 측정하고 `cachedInputTokens`와
`cacheWriteTokens`만 관찰합니다. Static prefix 재배치, cache key, explicit breakpoint 최적화는
baseline 결과를 확보한 뒤 별도 실험으로 진행합니다.

`low` effort는 기본 30회에 자동 포함하지 않습니다. 필요한 모델 하나만 고른 후 아래처럼
반복 수와 cap을 함께 낮춘 수동 후속 실험으로 실행합니다. 다음 블록도 새 Windows CMD에서
독립적으로 실행할 수 있는 전체 설정입니다.

```bat
if defined OPENAI_API_KEY (echo OpenAI API key configured: yes) else (echo OpenAI API key configured: no)

set "MCM_OPENAI_BENCHMARK=true"
set "MCM_OPENAI_BENCHMARK_CONFIRM_LIVE=true"
set "MCM_OPENAI_BENCHMARK_MODELS=gpt-5.6-terra"
set "MCM_OPENAI_BENCHMARK_CASES=CASE_A_WITHOUT_PRODUCT_TAG,CASE_B_WITH_PRODUCT_TAG"
set "MCM_OPENAI_BENCHMARK_REPETITIONS=5"
set "MCM_OPENAI_BENCHMARK_MAX_CALLS=10"
set "MCM_OPENAI_BENCHMARK_MAX_OUTPUT_TOKENS=512"
set "MCM_OPENAI_BENCHMARK_MAX_ESTIMATED_USD=0.50"
set "MCM_OPENAI_REASONING_EFFORT=low"
set "MCM_STYLE_ANALYSIS_PROVIDER=openai"
call gradlew.bat openAiBenchmark
```

#### STEP B — 통합 REST smoke test

BE1 API로 PassportSession과 Journey 데이터를 만들고 Boarding Pass 발급으로
`READY_TO_BOARD`가 된 실제 session ID를 사용합니다. 이후
`POST /api/style-spots/GATE-S1/connect` 한 번이 StyleSpotSession 생성과 분석을 함께
수행합니다. 저장 결과는 Display와 session 기반 Style Result API에서 조회하고, Souvenir
생성 후 `COMPLETED`를 확인합니다. 자동 테스트와 CI는 항상 mock provider를 사용합니다.

#### GitHub Actions Backend CI

개인 저장소의 `.github/workflows/backend-ci.yml`은 push, pull request, 수동 실행에서
Ubuntu와 Java 21로 Gradle wrapper의 `clean build`를 실행합니다. CI는
`MCM_STYLE_ANALYSIS_PROVIDER=mock`,
`MCM_OPENAI_BENCHMARK=false`, `MCM_OPENAI_BENCHMARK_CONFIRM_LIVE=false`, 테스트용 H2를
명시하며 `OPENAI_API_KEY`나 MySQL을 요구하지 않습니다. 따라서 CI에서는 실제 OpenAI
호출이나 비용이 발생하지 않으며 배포, Docker build도 수행하지 않습니다.

`JpaJourneyDataReader`가 BE1의 `GuideResponse`, `JourneyStamp`, `ProductTag`를
`JourneyDataSnapshot`으로 변환합니다. Style 분석 계층은 BE1 repository를 직접 알지
않으며 `JourneyDataReader` 경계를 유지합니다.

이번 범위에는 실시간 이미지 생성, 외부 object storage, MCM 온라인 계정 연동,
NFC/QR 하드웨어 판독을 포함하지 않습니다. Portrait는 URL metadata만 저장합니다.


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
