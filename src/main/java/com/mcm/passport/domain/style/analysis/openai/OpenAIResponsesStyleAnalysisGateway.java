package com.mcm.passport.domain.style.analysis.openai;

import com.mcm.passport.domain.style.analysis.metrics.OpenAIUsageMetrics;
import com.openai.client.OpenAIClient;
import com.openai.models.Reasoning;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponse;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.openai.services.blocking.ResponseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Objects;
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
		StructuredResponse<OpenAIStyleAnalysisOutput> response;
		try {
			response = responseService.create(params);
		}
		catch (RuntimeException requestFailure) {
			throw new OpenAIMeteredException(
					requestFailure,
					OpenAIUsageMetrics.allUnavailable(),
					request.model(),
					elapsedMillis(startedAt, nanoTime.getAsLong())
			);
		}
		long completedAt = nanoTime.getAsLong();
		long providerLatencyMs = elapsedMillis(startedAt, completedAt);
		OpenAIUsageMetrics usage;
		String responseModel;
		try {
			usage = usageMapper.map(response.usage());
			responseModel = response.rawResponse()._model().asKnown()
					.map(model -> model.asString())
					.filter(model -> !model.isBlank())
					.orElse(request.model());
		}
		catch (RuntimeException metadataFailure) {
			throw new OpenAIMeteredException(
					metadataFailure,
					OpenAIUsageMetrics.allUnavailable(),
					request.model(),
					providerLatencyMs
			);
		}

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
			throw new OpenAIMeteredException(
					structuredOutputFailure,
					usage,
					responseModel,
					providerLatencyMs
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
}
