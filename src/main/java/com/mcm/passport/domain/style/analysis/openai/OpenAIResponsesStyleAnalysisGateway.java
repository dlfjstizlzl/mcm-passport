package com.mcm.passport.domain.style.analysis.openai;

import com.mcm.passport.domain.style.analysis.metrics.OpenAIUsageMetrics;
import com.openai.client.OpenAIClient;
import com.openai.core.http.HttpResponseFor;
import com.openai.errors.OpenAIServiceException;
import com.openai.models.Reasoning;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseStatus;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.openai.services.blocking.ResponseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

@Component
@ConditionalOnProperty(name = "mcm.style.analysis.provider", havingValue = "openai")
public class OpenAIResponsesStyleAnalysisGateway implements OpenAIStyleAnalysisGateway {

	private final OpenAIClient client;
	private final OpenAIResponseUsageMapper usageMapper;
	private final LongSupplier nanoTime;

	@Autowired
	public OpenAIResponsesStyleAnalysisGateway(OpenAIClient client) {
		this(client, new OpenAIResponseUsageMapper(), System::nanoTime);
	}

	OpenAIResponsesStyleAnalysisGateway(OpenAIClient client, LongSupplier nanoTime) {
		this(client, new OpenAIResponseUsageMapper(), nanoTime);
	}

	OpenAIResponsesStyleAnalysisGateway(
			OpenAIClient client,
			OpenAIResponseUsageMapper usageMapper,
			LongSupplier nanoTime
	) {
		this.client = Objects.requireNonNull(client, "OpenAI client must not be null");
		this.usageMapper = Objects.requireNonNull(usageMapper, "OpenAI usage mapper must not be null");
		this.nanoTime = Objects.requireNonNull(nanoTime, "OpenAI latency clock must not be null");
	}

	@Override
	public OpenAIStyleAnalysisGatewayResult analyze(
			StyleAnalysisPrompt prompt,
			OpenAIStyleAnalysisGatewayRequest request
	) {
		Objects.requireNonNull(prompt, "Style analysis prompt must not be null");
		Objects.requireNonNull(request, "OpenAI gateway request must not be null");

		ResponseCreateParams.Builder baseParams = ResponseCreateParams.builder()
				.input(prompt.input())
				.instructions(prompt.instructions())
				.model(request.model())
				.reasoning(Reasoning.builder().effort(request.effort().toSdk()).build());
		if (request.maxOutputTokens() != null) {
			baseParams.maxOutputTokens(request.maxOutputTokens());
		}
		StructuredResponseCreateParams<OpenAIStyleAnalysisOutput> params = baseParams
				.text(OpenAIStyleAnalysisOutput.class)
				.build();

		ResponseService responseService = client.responses();
		long startedAt = nanoTime.getAsLong();
		HttpResponseFor<Response> httpResponse;
		try {
			httpResponse = responseService.withRawResponse().create(params.rawParams());
		}
		catch (RuntimeException httpFailure) {
			throw meteredFailure(
					httpFailure,
					OpenAIUsageMetrics.allUnavailable(),
					request.model(),
					elapsedMillis(startedAt, nanoTime.getAsLong()),
					OpenAIFailureStage.HTTP_REQUEST,
					OpenAIFailureDetail.HTTP_REQUEST_FAILED,
					httpStatus(httpFailure),
					errorCode(httpFailure),
					requestId(httpFailure)
			);
		}

		Integer httpStatus = safeHttpStatus(httpResponse);
		String requestId = safeRequestId(httpResponse);
		Response rawResponse;
		try {
			rawResponse = Objects.requireNonNull(
					httpResponse.parse(),
					"OpenAI SDK parsed response must not be null"
			);
		}
		catch (RuntimeException responseFailure) {
			boolean httpFailure = isHttpFailureStatus(httpStatus)
					|| responseFailure instanceof OpenAIServiceException;
			throw meteredFailure(
					responseFailure,
					OpenAIUsageMetrics.allUnavailable(),
					request.model(),
					elapsedMillis(startedAt, nanoTime.getAsLong()),
					httpFailure
							? OpenAIFailureStage.HTTP_REQUEST
							: OpenAIFailureStage.SDK_RESPONSE_DESERIALIZATION,
					httpFailure
							? OpenAIFailureDetail.HTTP_REQUEST_FAILED
							: OpenAIFailureDetail.SDK_RESPONSE_DESERIALIZATION_FAILED,
					firstNonNull(httpStatus(responseFailure), httpStatus),
					errorCode(responseFailure),
					firstNonNull(requestId(responseFailure), requestId)
			);
		}
		finally {
			closeQuietly(httpResponse);
		}
		if (isHttpFailureStatus(httpStatus)) {
			throw meteredFailure(
					"OpenAIHttpStatusFailure",
					OpenAIUsageMetrics.allUnavailable(),
					request.model(),
					elapsedMillis(startedAt, nanoTime.getAsLong()),
					OpenAIFailureStage.HTTP_REQUEST,
					OpenAIFailureDetail.HTTP_REQUEST_FAILED,
					httpStatus,
					null,
					requestId
			);
		}
		long providerLatencyMs = elapsedMillis(startedAt, nanoTime.getAsLong());

		OpenAIUsageMetrics usage;
		try {
			usage = usageMapper.map(rawResponse._usage());
		}
		catch (RuntimeException usageFailure) {
			throw meteredFailure(
					usageFailure,
					OpenAIUsageMetrics.allUnavailable(),
					request.model(),
					providerLatencyMs,
					OpenAIFailureStage.USAGE_MAPPING,
					OpenAIFailureDetail.USAGE_MAPPING_FAILED,
					httpStatus,
					null,
					requestId
			);
		}

		String responseModel;
		try {
			responseModel = rawResponse._model().asKnown()
					.map(model -> model.asString())
					.filter(model -> !model.isBlank())
					.orElse(request.model());
		}
		catch (RuntimeException metadataFailure) {
			throw meteredFailure(
					metadataFailure,
					usage,
					request.model(),
					providerLatencyMs,
					OpenAIFailureStage.RESPONSE_METADATA,
					OpenAIFailureDetail.RESPONSE_MODEL_UNAVAILABLE,
					httpStatus,
					null,
					requestId
			);
		}
		boolean incompleteForMaxOutputTokens;
		try {
			incompleteForMaxOutputTokens = isIncompleteForMaxOutputTokens(rawResponse);
		}
		catch (RuntimeException metadataFailure) {
			throw meteredFailure(
					metadataFailure,
					usage,
					responseModel,
					providerLatencyMs,
					OpenAIFailureStage.RESPONSE_METADATA,
					OpenAIFailureDetail.RESPONSE_STATUS_UNAVAILABLE,
					httpStatus,
					null,
					requestId
			);
		}
		if (incompleteForMaxOutputTokens) {
			throw meteredFailure(
					"OpenAIResponseIncomplete",
					usage,
					responseModel,
					providerLatencyMs,
					OpenAIFailureStage.STRUCTURED_OUTPUT_DESERIALIZATION,
					OpenAIFailureDetail.RESPONSE_INCOMPLETE_MAX_OUTPUT_TOKENS,
					httpStatus,
					null,
					requestId
			);
		}

		StructuredResponse<OpenAIStyleAnalysisOutput> response = new StructuredResponse<>(
				params.responseType(),
				rawResponse
		);
		OpenAIStyleAnalysisOutput output;
		try {
			output = response.output().stream()
					.flatMap(item -> item.message().stream())
					.flatMap(message -> message.content().stream())
					.flatMap(content -> content.outputText().stream())
					.findFirst()
					.orElseThrow(OpenAIStructuredOutputException::new);
		}
		catch (RuntimeException structuredOutputFailure) {
			throw meteredFailure(
					structuredOutputFailure,
					usage,
					responseModel,
					providerLatencyMs,
					OpenAIFailureStage.STRUCTURED_OUTPUT_DESERIALIZATION,
					structuredOutputFailure instanceof OpenAIStructuredOutputException
							? OpenAIFailureDetail.STRUCTURED_OUTPUT_MISSING
							: OpenAIFailureDetail.STRUCTURED_OUTPUT_DESERIALIZATION_FAILED,
					httpStatus,
					null,
					requestId
			);
		}

		return new OpenAIStyleAnalysisGatewayResult(
				output,
				usage,
				responseModel,
				providerLatencyMs
		);
	}

	private long elapsedMillis(long startedAt, long completedAt) {
		return TimeUnit.NANOSECONDS.toMillis(Math.max(0L, completedAt - startedAt));
	}

	private OpenAIMeteredException meteredFailure(
			RuntimeException failure,
			OpenAIUsageMetrics usage,
			String model,
			long providerLatencyMs,
			OpenAIFailureStage stage,
			OpenAIFailureDetail detail,
			Integer httpStatus,
			String errorCode,
			String requestId
	) {
		return new OpenAIMeteredException(
				usage,
				model,
				providerLatencyMs,
				OpenAIFailureDiagnostic.of(
						failure,
						stage,
						detail,
						httpStatus,
						errorCode,
						requestId
				)
		);
	}

	private OpenAIMeteredException meteredFailure(
			String failureType,
			OpenAIUsageMetrics usage,
			String model,
			long providerLatencyMs,
			OpenAIFailureStage stage,
			OpenAIFailureDetail detail,
			Integer httpStatus,
			String errorCode,
			String requestId
	) {
		return new OpenAIMeteredException(
				usage,
				model,
				providerLatencyMs,
				new OpenAIFailureDiagnostic(
						failureType,
						stage,
						detail,
						httpStatus,
						errorCode,
						requestId
				)
		);
	}

	private Integer httpStatus(RuntimeException failure) {
		if (!(failure instanceof OpenAIServiceException serviceFailure)) {
			return null;
		}
		try {
			int status = serviceFailure.statusCode();
			return status >= 100 && status <= 599 ? status : null;
		}
		catch (RuntimeException ignored) {
			return null;
		}
	}

	private String errorCode(RuntimeException failure) {
		if (!(failure instanceof OpenAIServiceException serviceFailure)) {
			return null;
		}
		try {
			return safeOptional(serviceFailure.code());
		}
		catch (RuntimeException ignored) {
			return null;
		}
	}

	private String requestId(RuntimeException failure) {
		if (!(failure instanceof OpenAIServiceException serviceFailure)) {
			return null;
		}
		try {
			return serviceFailure.headers().values("x-request-id").stream()
					.findFirst()
					.orElse(null);
		}
		catch (RuntimeException ignored) {
			return null;
		}
	}

	private String safeOptional(Optional<String> value) {
		return value == null ? null : value.orElse(null);
	}

	private Integer safeHttpStatus(HttpResponseFor<?> response) {
		try {
			int status = response.statusCode();
			return status >= 100 && status <= 599 ? status : null;
		}
		catch (RuntimeException ignored) {
			return null;
		}
	}

	private String safeRequestId(HttpResponseFor<?> response) {
		try {
			return safeOptional(response.requestId());
		}
		catch (RuntimeException ignored) {
			return null;
		}
	}

	private boolean isHttpFailureStatus(Integer status) {
		return status != null && (status < 200 || status >= 300);
	}

	private boolean isIncompleteForMaxOutputTokens(Response response) {
		return response._status().asKnown()
				.filter(ResponseStatus.INCOMPLETE::equals)
				.flatMap(ignored -> response._incompleteDetails().asKnown())
				.flatMap(details -> details._reason().asKnown())
				.filter(Response.IncompleteDetails.Reason.MAX_OUTPUT_TOKENS::equals)
				.isPresent();
	}

	private void closeQuietly(HttpResponseFor<?> response) {
		try {
			response.close();
		}
		catch (RuntimeException ignored) {
			// A close failure must not mask the already captured response or expose SDK details.
		}
	}

	private <T> T firstNonNull(T preferred, T fallback) {
		return preferred != null ? preferred : fallback;
	}
}
