package com.mcm.passport.domain.style.analysis.openai;

import com.mcm.passport.domain.style.analysis.metrics.OpenAIUsageMetrics;
import com.openai.client.OpenAIClient;
import com.openai.core.JsonField;
import com.openai.core.JsonValue;
import com.openai.core.http.Headers;
import com.openai.core.http.HttpResponseFor;
import com.openai.errors.OpenAIInvalidDataException;
import com.openai.errors.RateLimitException;
import com.openai.models.ChatModel;
import com.openai.models.ErrorObject;
import com.openai.models.ResponsesModel;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseOutputMessage;
import com.openai.models.responses.ResponseOutputText;
import com.openai.models.responses.ResponseStatus;
import com.openai.models.responses.ResponseUsage;
import com.openai.models.responses.ToolChoiceOptions;
import com.openai.services.blocking.ResponseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OpenAIResponsesStyleAnalysisGatewayTest {

	private static final String MODEL = "gpt-5.6-luna";
	private static final String REQUEST_ID = "req_offline_gateway";
	private static final StyleAnalysisPrompt PROMPT = new StyleAnalysisPrompt(
			"Return the requested style result.",
			"Offline gateway fixture"
	);
	private static final OpenAIStyleAnalysisGatewayRequest REQUEST =
			new OpenAIStyleAnalysisGatewayRequest(MODEL, OpenAIReasoningEffort.NONE, 512L);
	private static final OpenAIUsageMetrics EXPECTED_USAGE = new OpenAIUsageMetrics(
			120L,
			20L,
			5L,
			40L,
			10L,
			160L
	);

	private OpenAIClient client;
	private ResponseService responseService;
	private ResponseService.WithRawResponse rawResponseService;
	private HttpResponseFor<Response> httpResponse;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() {
		client = mock(OpenAIClient.class);
		responseService = mock(ResponseService.class);
		rawResponseService = mock(ResponseService.WithRawResponse.class);
		httpResponse = mock(HttpResponseFor.class);

		when(client.responses()).thenReturn(responseService);
		when(responseService.withRawResponse()).thenReturn(rawResponseService);
		when(rawResponseService.create(any(ResponseCreateParams.class))).thenReturn(httpResponse);
		when(httpResponse.statusCode()).thenReturn(200);
		when(httpResponse.requestId()).thenReturn(Optional.of(REQUEST_ID));
	}

	@Test
	@SuppressWarnings("unchecked")
	void usesRawResponsePathWhileKeepingTheSdkStructuredSchemaAndParser() {
		when(httpResponse.parse()).thenReturn(completedResponse(validOutputJson()));

		OpenAIStyleAnalysisGatewayResult result = gateway().analyze(PROMPT, REQUEST);

		assertThat(result.output().cityCode()).isEqualTo("BERLIN_AFTERDARK_NOMAD");
		assertThat(result.output().recommendedProduct()).isEqualTo("STARK_BACKPACK");
		assertThat(result.output().matchScore()).isEqualTo(91);
		assertThat(result.usage()).isEqualTo(EXPECTED_USAGE);
		assertThat(result.model()).isEqualTo(MODEL);
		assertThat(result.providerLatencyMs()).isEqualTo(5L);

		ArgumentCaptor<ResponseCreateParams> paramsCaptor =
				ArgumentCaptor.forClass(ResponseCreateParams.class);
		verify(rawResponseService).create(paramsCaptor.capture());
		ResponseCreateParams rawParams = paramsCaptor.getValue();
		assertThat(rawParams.maxOutputTokens()).contains(512L);

		var jsonSchema = rawParams.text().orElseThrow()
				.format().orElseThrow()
				.asJsonSchema();
		assertThat(jsonSchema.name()).isEqualTo("json-schema-from-OpenAIStyleAnalysisOutput");
		assertThat(jsonSchema.strict()).contains(true);
		Map<String, Object> schema = jsonSchema._schema().asUnknown().orElseThrow().convert(Map.class);
		Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
		List<String> required = (List<String>) schema.get("required");
		assertThat(schema).containsKeys("type", "properties", "required", "additionalProperties");
		assertThat(properties).containsOnlyKeys(
				"cityCode",
				"recommendedProduct",
				"styleMood",
				"background",
				"description",
				"matchScore"
		);
		assertThat(required).containsExactlyInAnyOrder(
				"cityCode",
				"recommendedProduct",
				"styleMood",
				"background",
				"description",
				"matchScore"
		);
		assertThat(schema.get("additionalProperties")).isEqualTo(false);
		verify(httpResponse).close();
	}

	@Test
	void readsLunaChatModelVariant() {
		assertChatModelVariant(ChatModel.GPT_5_6_LUNA);
	}

	@Test
	void readsTerraChatModelVariant() {
		assertChatModelVariant(ChatModel.GPT_5_6_TERRA);
	}

	@Test
	void readsSolChatModelVariant() {
		assertChatModelVariant(ChatModel.GPT_5_6_SOL);
	}

	@Test
	void readsResponsesOnlyModelVariant() {
		ResponsesModel.ResponsesOnlyModel model = ResponsesModel.ResponsesOnlyModel.O3_PRO;
		Response response = completedResponse(validOutputJson()).toBuilder()
				.model(model)
				.build();
		when(httpResponse.parse()).thenReturn(response);

		OpenAIStyleAnalysisGatewayResult result = gateway().analyze(PROMPT, REQUEST);

		assertThat(result.model()).isEqualTo(model.asString());
		assertThat(result.output().cityCode()).isEqualTo("BERLIN_AFTERDARK_NOMAD");
		assertThat(result.usage()).isEqualTo(EXPECTED_USAGE);
	}

	@Test
	@SuppressWarnings("unchecked")
	void fallsBackToRequestModelForUnknownUnionVariantAndContinuesStructuredParsing() {
		ResponsesModel unknownModel = mock(ResponsesModel.class);
		when(unknownModel.accept(any())).thenAnswer(invocation -> {
			ResponsesModel.Visitor<String> visitor = invocation.getArgument(0);
			return visitor.unknown(JsonValue.from(Map.of("future", "model")));
		});
		Response response = completedResponse(validOutputJson()).toBuilder()
				.model(JsonField.of(unknownModel))
				.build();
		when(httpResponse.parse()).thenReturn(response);

		OpenAIStyleAnalysisGatewayResult result = gateway().analyze(PROMPT, REQUEST);

		assertThat(result.model()).isEqualTo(REQUEST.model());
		assertThat(result.output().matchScore()).isEqualTo(91);
		assertThat(result.usage()).isEqualTo(EXPECTED_USAGE);
	}

	@Test
	@SuppressWarnings("unchecked")
	void fallsBackToRequestModelWhenMetadataExtractionFailsAndContinuesStructuredParsing() {
		Response response = spy(completedResponse(validOutputJson()));
		JsonField<ResponsesModel> invalidModel = mock(JsonField.class);
		when(invalidModel.asKnown()).thenThrow(new OpenAIInvalidDataException("unsafe model metadata"));
		when(response._model()).thenReturn(invalidModel);
		when(httpResponse.parse()).thenReturn(response);

		OpenAIStyleAnalysisGatewayResult result = gateway().analyze(PROMPT, REQUEST);

		assertThat(result.model()).isEqualTo(REQUEST.model());
		assertThat(result.output().recommendedProduct()).isEqualTo("STARK_BACKPACK");
		assertThat(result.usage()).isEqualTo(EXPECTED_USAGE);
	}

	@Test
	void keepsUsageWhenTheSdkStructuredDtoParserRejectsOutputText() {
		String unsafeOutput = "not-json api-key-sentinel-never-retain raw-response-body";
		when(httpResponse.parse()).thenReturn(completedResponse(unsafeOutput));

		OpenAIMeteredException failure = catchThrowableOfType(
				() -> gateway().analyze(PROMPT, REQUEST),
				OpenAIMeteredException.class
		);

		assertSafeFailure(failure);
		assertThat(failure.usage()).isEqualTo(EXPECTED_USAGE);
		assertThat(failure.model()).isEqualTo(MODEL);
		assertThat(failure.diagnostic().failureType()).isEqualTo("OpenAIInvalidDataException");
		assertThat(failure.diagnostic().failureStage())
				.isEqualTo(OpenAIFailureStage.STRUCTURED_OUTPUT_DESERIALIZATION);
		assertThat(failure.diagnostic().safeFailureDetail())
				.isEqualTo(OpenAIFailureDetail.STRUCTURED_OUTPUT_DESERIALIZATION_FAILED);
		assertThat(failure.diagnostic().httpStatus()).isEqualTo(200);
		assertThat(failure.diagnostic().requestId()).isEqualTo(REQUEST_ID);
		assertThat(failure.toString()).doesNotContain(
				unsafeOutput,
				"api-key-sentinel",
				"raw-response-body"
		);
	}

	@Test
	void reportsSuccessfulHttpEnvelopeDeserializationFailureWithoutInventingUsage() {
		String unsafeSdkMessage = "raw response api-key-sentinel-never-retain";
		when(httpResponse.parse()).thenThrow(new OpenAIInvalidDataException(unsafeSdkMessage));

		OpenAIMeteredException failure = catchThrowableOfType(
				() -> gateway().analyze(PROMPT, REQUEST),
				OpenAIMeteredException.class
		);

		assertSafeFailure(failure);
		assertThat(failure.usage()).isEqualTo(OpenAIUsageMetrics.allUnavailable());
		assertThat(failure.diagnostic().failureType()).isEqualTo("OpenAIInvalidDataException");
		assertThat(failure.diagnostic().failureStage())
				.isEqualTo(OpenAIFailureStage.SDK_RESPONSE_DESERIALIZATION);
		assertThat(failure.diagnostic().safeFailureDetail())
				.isEqualTo(OpenAIFailureDetail.SDK_RESPONSE_DESERIALIZATION_FAILED);
		assertThat(failure.diagnostic().httpStatus()).isEqualTo(200);
		assertThat(failure.diagnostic().requestId()).isEqualTo(REQUEST_ID);
		assertThat(failure.toString()).doesNotContain(unsafeSdkMessage, "api-key-sentinel");
		verify(httpResponse).close();
	}

	@Test
	void classifiesNonSuccessfulHttpParseAsHttpFailureAndKeepsSafeRemoteMetadata() {
		Headers headers = Headers.builder().put("x-request-id", "req_rate_limit_1").build();
		RateLimitException serviceFailure = RateLimitException.builder()
				.headers(headers)
				.error(ErrorObject.builder()
						.code("rate_limit_exceeded")
						.message("unsafe server body api-key-sentinel-never-retain")
						.param(Optional.empty())
						.type("requests")
						.build())
				.build();
		when(httpResponse.statusCode()).thenReturn(429);
		when(httpResponse.requestId()).thenReturn(Optional.of("req_raw_response"));
		when(httpResponse.parse()).thenThrow(serviceFailure);

		OpenAIMeteredException failure = catchThrowableOfType(
				() -> gateway().analyze(PROMPT, REQUEST),
				OpenAIMeteredException.class
		);

		assertSafeFailure(failure);
		assertThat(failure.usage()).isEqualTo(OpenAIUsageMetrics.allUnavailable());
		assertThat(failure.diagnostic().failureType()).isEqualTo("RateLimitException");
		assertThat(failure.diagnostic().failureStage()).isEqualTo(OpenAIFailureStage.HTTP_REQUEST);
		assertThat(failure.diagnostic().safeFailureDetail())
				.isEqualTo(OpenAIFailureDetail.HTTP_REQUEST_FAILED);
		assertThat(failure.diagnostic().httpStatus()).isEqualTo(429);
		assertThat(failure.diagnostic().errorCode()).isEqualTo("rate_limit_exceeded");
		assertThat(failure.diagnostic().requestId()).isEqualTo("req_rate_limit_1");
		assertThat(failure.toString()).doesNotContain("unsafe server body", "api-key-sentinel");
		verify(httpResponse).close();
	}

	@Test
	void detectsMaxOutputTokenIncompleteResponseBeforeDtoParsingAndKeepsUsage() {
		when(httpResponse.parse()).thenReturn(incompleteForMaxOutputTokensResponse());

		OpenAIMeteredException failure = catchThrowableOfType(
				() -> gateway().analyze(PROMPT, REQUEST),
				OpenAIMeteredException.class
		);

		assertSafeFailure(failure);
		assertThat(failure.usage()).isEqualTo(EXPECTED_USAGE);
		assertThat(failure.model()).isEqualTo(MODEL);
		assertThat(failure.diagnostic().failureType()).isEqualTo("OpenAIResponseIncomplete");
		assertThat(failure.diagnostic().failureStage())
				.isEqualTo(OpenAIFailureStage.STRUCTURED_OUTPUT_DESERIALIZATION);
		assertThat(failure.diagnostic().safeFailureDetail())
				.isEqualTo(OpenAIFailureDetail.RESPONSE_INCOMPLETE_MAX_OUTPUT_TOKENS);
		assertThat(failure.diagnostic().httpStatus()).isEqualTo(200);
		assertThat(failure.diagnostic().requestId()).isEqualTo(REQUEST_ID);
	}

	@Test
	@SuppressWarnings("unchecked")
	void keepsUsageWhenIncompleteResponseMetadataCannotBeInterpreted() {
		String unsafeMetadataMessage = "status raw response api-key-sentinel-never-retain";
		Response malformedResponse = spy(completedResponse(validOutputJson()));
		JsonField<ResponseStatus> invalidStatus = mock(JsonField.class);
		when(invalidStatus.asKnown()).thenThrow(new OpenAIInvalidDataException(unsafeMetadataMessage));
		when(malformedResponse._status()).thenReturn(invalidStatus);
		when(httpResponse.parse()).thenReturn(malformedResponse);

		OpenAIMeteredException failure = catchThrowableOfType(
				() -> gateway().analyze(PROMPT, REQUEST),
				OpenAIMeteredException.class
		);

		assertSafeFailure(failure);
		assertThat(failure.usage()).isEqualTo(EXPECTED_USAGE);
		assertThat(failure.model()).isEqualTo(MODEL);
		assertThat(failure.diagnostic().failureType()).isEqualTo("OpenAIInvalidDataException");
		assertThat(failure.diagnostic().failureStage()).isEqualTo(OpenAIFailureStage.RESPONSE_METADATA);
		assertThat(failure.diagnostic().safeFailureDetail())
				.isEqualTo(OpenAIFailureDetail.RESPONSE_STATUS_UNAVAILABLE);
		assertThat(failure.diagnostic().httpStatus()).isEqualTo(200);
		assertThat(failure.diagnostic().requestId()).isEqualTo(REQUEST_ID);
		assertThat(failure.toString()).doesNotContain(unsafeMetadataMessage, "api-key-sentinel");
	}

	@Test
	void isolatesUsageMappingFailureWithoutRetainingMapperDetails() {
		String unsafeMapperMessage = "usage raw field api-key-sentinel-never-retain";
		when(httpResponse.parse()).thenReturn(completedResponse(validOutputJson()));
		OpenAIResponseUsageMapper failingMapper = new OpenAIResponseUsageMapper() {
			@Override
			public OpenAIUsageMetrics map(com.openai.core.JsonField<ResponseUsage> usageField) {
				throw new IllegalArgumentException(unsafeMapperMessage);
			}
		};
		AtomicLong now = new AtomicLong();
		OpenAIResponsesStyleAnalysisGateway gateway = new OpenAIResponsesStyleAnalysisGateway(
				client,
				failingMapper,
				() -> now.getAndAdd(5_000_000L)
		);

		OpenAIMeteredException failure = catchThrowableOfType(
				() -> gateway.analyze(PROMPT, REQUEST),
				OpenAIMeteredException.class
		);

		assertSafeFailure(failure);
		assertThat(failure.usage()).isEqualTo(OpenAIUsageMetrics.allUnavailable());
		assertThat(failure.diagnostic().failureType()).isEqualTo("IllegalArgumentException");
		assertThat(failure.diagnostic().failureStage()).isEqualTo(OpenAIFailureStage.USAGE_MAPPING);
		assertThat(failure.diagnostic().safeFailureDetail())
				.isEqualTo(OpenAIFailureDetail.USAGE_MAPPING_FAILED);
		assertThat(failure.diagnostic().httpStatus()).isEqualTo(200);
		assertThat(failure.diagnostic().requestId()).isEqualTo(REQUEST_ID);
		assertThat(failure.toString()).doesNotContain(unsafeMapperMessage, "api-key-sentinel");
	}

	@Test
	void sanitizesRemoteDiagnosticValuesAndNeverRetainsTheOriginalException() {
		OpenAIFailureDiagnostic diagnostic = OpenAIFailureDiagnostic.of(
				new IllegalStateException("prompt and api-key-sentinel-never-retain"),
				OpenAIFailureStage.HTTP_REQUEST,
				OpenAIFailureDetail.HTTP_REQUEST_FAILED,
				500,
				"api-key-error-code-sentinel",
				"api-key-request-id-sentinel"
		);
		OpenAIMeteredException failure = new OpenAIMeteredException(
				OpenAIUsageMetrics.allUnavailable(),
				MODEL,
				7L,
				diagnostic
		);

		assertSafeFailure(failure);
		assertThat(failure.diagnostic().failureType()).isEqualTo("IllegalStateException");
		assertThat(failure.diagnostic().errorCode()).isNull();
		assertThat(failure.diagnostic().requestId()).isNull();
		assertThat(failure.toString()).doesNotContain("prompt", "api-key-sentinel");
	}

	private OpenAIResponsesStyleAnalysisGateway gateway() {
		AtomicLong now = new AtomicLong();
		return new OpenAIResponsesStyleAnalysisGateway(
				client,
				new OpenAIResponseUsageMapper(),
				() -> now.getAndAdd(5_000_000L)
		);
	}

	private void assertChatModelVariant(ChatModel model) {
		Response response = completedResponse(validOutputJson()).toBuilder()
				.model(model)
				.build();
		when(httpResponse.parse()).thenReturn(response);

		OpenAIStyleAnalysisGatewayResult result = gateway().analyze(PROMPT, REQUEST);

		assertThat(result.model()).isEqualTo(model.asString());
		assertThat(result.output().background()).isEqualTo("BERLIN_AFTERDARK");
		assertThat(result.usage()).isEqualTo(EXPECTED_USAGE);
	}

	private Response completedResponse(String outputText) {
		ResponseOutputText text = ResponseOutputText.builder()
				.annotations(List.of())
				.text(outputText)
				.build();
		ResponseOutputMessage message = ResponseOutputMessage.builder()
				.id("msg_offline_gateway")
				.addContent(text)
				.status(ResponseOutputMessage.Status.COMPLETED)
				.build();
		return baseResponse()
				.addOutput(message)
				.status(ResponseStatus.COMPLETED)
				.build();
	}

	private Response incompleteForMaxOutputTokensResponse() {
		return baseResponse()
				.output(List.of())
				.status(ResponseStatus.INCOMPLETE)
				.incompleteDetails(Response.IncompleteDetails.builder()
						.reason(Response.IncompleteDetails.Reason.MAX_OUTPUT_TOKENS)
						.build())
				.build();
	}

	private Response.Builder baseResponse() {
		return Response.builder()
				.id("resp_offline_gateway")
				.createdAt(1.0)
				.error(Optional.empty())
				.incompleteDetails(Optional.empty())
				.instructions(Optional.empty())
				.metadata(Optional.empty())
				.model(MODEL)
				.parallelToolCalls(false)
				.temperature(Optional.empty())
				.toolChoice(ToolChoiceOptions.AUTO)
				.tools(List.of())
				.topP(Optional.empty())
				.usage(responseUsage());
	}

	private ResponseUsage responseUsage() {
		return ResponseUsage.builder()
				.inputTokens(120L)
				.inputTokensDetails(ResponseUsage.InputTokensDetails.builder()
						.cachedTokens(20L)
						.cacheWriteTokens(5L)
						.build())
				.outputTokens(40L)
				.outputTokensDetails(ResponseUsage.OutputTokensDetails.builder()
						.reasoningTokens(10L)
						.build())
				.totalTokens(160L)
				.build();
	}

	private String validOutputJson() {
		return """
				{
				  "cityCode": "BERLIN_AFTERDARK_NOMAD",
				  "recommendedProduct": "STARK_BACKPACK",
				  "styleMood": "AFTERDARK_MOVEMENT",
				  "background": "BERLIN_AFTERDARK",
				  "description": "Offline structured output fixture.",
				  "matchScore": 91
				}
				""";
	}

	private void assertSafeFailure(OpenAIMeteredException failure) {
		assertThat(failure).isNotNull();
		assertThat(failure.getMessage()).isEqualTo("OpenAI style analysis request failed");
		assertThat(failure.getCause()).isNull();
		assertThat(failure.diagnostic()).isNotNull();
	}
}
