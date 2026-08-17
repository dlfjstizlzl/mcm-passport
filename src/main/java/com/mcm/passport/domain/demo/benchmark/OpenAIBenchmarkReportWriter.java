package com.mcm.passport.domain.demo.benchmark;

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public final class OpenAIBenchmarkReportWriter {

	public static final String RUNS_CSV = "runs.csv";
	public static final String SUMMARY_JSON = "summary.json";
	public static final String SUMMARY_MARKDOWN = "summary.md";

	private final ObjectMapper objectMapper;

	public OpenAIBenchmarkReportWriter() {
		this(new ObjectMapper());
	}

	OpenAIBenchmarkReportWriter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public List<Path> write(
			Path reportDirectory,
			OpenAIBenchmarkExecution execution,
			OpenAIBenchmarkSummary summary
	) throws IOException {
		Path directory = reportDirectory.toAbsolutePath().normalize();
		Files.createDirectories(directory);
		Path runs = directory.resolve(RUNS_CSV);
		Path json = directory.resolve(SUMMARY_JSON);
		Path markdown = directory.resolve(SUMMARY_MARKDOWN);

		writeAtomically(runs, renderCsv(execution.runs()));
		writeAtomically(json, objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(summary));
		writeAtomically(markdown, renderMarkdown(summary));
		return List.of(runs, json, markdown);
	}

	String renderCsv(List<OpenAIBenchmarkRun> runs) {
		List<String> rows = new ArrayList<>();
		rows.add(String.join(",",
				"runNumber", "caseName", "model", "reasoningEffort", "cityCode",
				"recommendedProduct", "styleMood", "background", "matchScore", "usedFallback",
				"inputTokens", "cachedInputTokens", "cacheWriteTokens", "outputTokens",
				"reasoningTokens", "totalTokens", "providerLatencyMs", "endToEndLatencyMs",
				"estimatedCostUsd", "success", "errorCategory", "failureType", "failureStage",
				"safeFailureDetail", "httpStatus", "errorCode", "requestId", "responseModel"
		));
		for (OpenAIBenchmarkRun run : runs) {
			rows.add(csvRow(
					run.runNumber(), run.caseName(), run.model(), run.reasoningEffort(), run.cityCode(),
					run.recommendedProduct(), run.styleMood(), run.background(), run.matchScore(),
					run.usedFallback(), run.inputTokens(), run.cachedInputTokens(), run.cacheWriteTokens(),
					run.outputTokens(), run.reasoningTokens(), run.totalTokens(), run.providerLatencyMs(),
					run.endToEndLatencyMs(), run.estimatedCostUsd(), run.success(), run.errorCategory(),
					run.failureType(), run.failureStage(), run.safeFailureDetail(), run.httpStatus(),
					run.errorCode(), run.requestId(), run.responseModel()
			));
		}
		return String.join(System.lineSeparator(), rows) + System.lineSeparator();
	}

	String renderMarkdown(OpenAIBenchmarkSummary summary) {
		StringBuilder builder = new StringBuilder();
		builder.append("# OpenAI Style Analysis Benchmark\n\n")
				.append("- Status: `").append(summary.status()).append("`\n")
				.append("- Termination reason: `").append(summary.terminationReason()).append("`\n")
				.append("- Pricing checked at: `").append(summary.pricingCheckedAt()).append("`\n")
				.append("- Started at: `").append(summary.startedAt()).append("`\n")
				.append("- Completed at: `").append(summary.completedAt()).append("`\n\n")
				.append("Estimated cost is a benchmark estimate based on the pricing snapshot, not an actual invoice. ")
				.append("Consistency is not accuracy. This report compares candidates and does not declare a best model.\n\n")
				.append("## Model comparison\n\n")
				.append("| Model | Calls | Success rate | Fallback rate | Avg input | Avg cached | Avg cache write | Avg output | Avg reasoning | Avg total | Provider avg / p50 / p95 ms | E2E avg / p50 / p95 ms | Avg / total estimated USD | sameResultRate | A/B result |\n")
				.append("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|---|---|---:|---|\n");
		for (OpenAIBenchmarkSummary.ModelSummary model : summary.models()) {
			OpenAIBenchmarkSummary.Aggregate aggregate = model.aggregate();
			builder.append('|').append(model.model())
					.append('|').append(aggregate.callCount())
					.append('|').append(value(aggregate.successRate()))
					.append('|').append(value(aggregate.fallbackRate()))
					.append('|').append(value(aggregate.averageInputTokens()))
					.append('|').append(value(aggregate.averageCachedInputTokens()))
					.append('|').append(value(aggregate.averageCacheWriteTokens()))
					.append('|').append(value(aggregate.averageOutputTokens()))
					.append('|').append(value(aggregate.averageReasoningTokens()))
					.append('|').append(value(aggregate.averageTotalTokens()))
					.append('|').append(value(aggregate.averageProviderLatencyMs())).append(" / ")
					.append(value(aggregate.p50ProviderLatencyMs())).append(" / ")
					.append(value(aggregate.p95ProviderLatencyMs()))
					.append('|').append(value(aggregate.averageEndToEndLatencyMs())).append(" / ")
					.append(value(aggregate.p50EndToEndLatencyMs())).append(" / ")
					.append(value(aggregate.p95EndToEndLatencyMs()))
					.append('|').append(value(aggregate.averageEstimatedCostUsd())).append(" / ")
					.append(value(aggregate.totalEstimatedCostUsd()))
					.append('|').append(value(aggregate.sameResultRate()))
					.append('|').append(model.productTagDifference()).append("|\n");
		}

		for (OpenAIBenchmarkSummary.ModelSummary model : summary.models()) {
			builder.append("\n## ").append(model.model()).append(" — Case A / B\n\n")
					.append("| Case | Calls | Success | Fallback | Avg input | Avg cached | Avg cache write | Avg output | Avg reasoning | Avg total | Avg cost | sameResultRate | Match score avg/min/max |\n")
					.append("|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|\n");
			for (OpenAIBenchmarkSummary.CaseSummary caseSummary : model.cases().values()) {
				OpenAIBenchmarkSummary.Aggregate aggregate = caseSummary.aggregate();
				OpenAIBenchmarkSummary.MatchScoreSummary score = caseSummary.matchScore();
				builder.append('|').append(caseSummary.caseName())
						.append('|').append(aggregate.callCount())
						.append('|').append(aggregate.successCount())
						.append('|').append(aggregate.fallbackCount())
						.append('|').append(value(aggregate.averageInputTokens()))
						.append('|').append(value(aggregate.averageCachedInputTokens()))
						.append('|').append(value(aggregate.averageCacheWriteTokens()))
						.append('|').append(value(aggregate.averageOutputTokens()))
						.append('|').append(value(aggregate.averageReasoningTokens()))
						.append('|').append(value(aggregate.averageTotalTokens()))
						.append('|').append(value(aggregate.averageEstimatedCostUsd()))
						.append('|').append(value(aggregate.sameResultRate()))
						.append('|').append(value(score.average())).append(" / ")
						.append(value(score.min())).append(" / ").append(value(score.max())).append("|\n");
			}
			builder.append("\nProductTag comparison: `").append(model.productTagDifference()).append("`\n")
					.append("\nMetric sample counts: `").append(model.aggregate().metricSampleCounts()).append("`\n")
					.append("\nResponse model distribution: `")
					.append(model.aggregate().responseModelDistribution()).append("`\n")
					.append("\nCity distribution: `").append(model.aggregate().cityCodeDistribution()).append("`\n")
					.append("\nProduct distribution: `").append(model.aggregate().recommendedProductDistribution()).append("`\n")
					.append("\nMood distribution: `").append(model.aggregate().styleMoodDistribution()).append("`\n");
		}

		builder.append("\n## Safe failure diagnostics\n\n");
		if (summary.failures().isEmpty()) {
			builder.append("No failed OpenAI benchmark runs were recorded.\n");
		}
		else {
			builder.append("Exception messages, prompts, raw response bodies, Journey text, and API keys are excluded.\n\n")
					.append("| Run | Case | Configured model | Response model | Category | Type | Stage | Safe detail | HTTP | Error code | Request ID |\n")
					.append("|---:|---|---|---|---|---|---|---|---:|---|---|\n");
			for (OpenAIBenchmarkSummary.FailureSummary failure : summary.failures()) {
				builder.append('|').append(failure.runNumber())
						.append('|').append(markdownValue(failure.caseName()))
						.append('|').append(markdownValue(failure.model()))
						.append('|').append(markdownValue(failure.responseModel()))
						.append('|').append(markdownValue(failure.errorCategory()))
						.append('|').append(markdownValue(failure.failureType()))
						.append('|').append(markdownValue(failure.failureStage()))
						.append('|').append(markdownValue(failure.safeFailureDetail()))
						.append('|').append(markdownValue(failure.httpStatus()))
						.append('|').append(markdownValue(failure.errorCode()))
						.append('|').append(markdownValue(failure.requestId())).append("|\n");
			}
		}
		return builder.toString();
	}

	private String markdownValue(Object value) {
		return value == null
				? "N/A"
				: value.toString().replace("\\", "\\\\").replace("|", "\\|");
	}

	private String csvRow(Object... fields) {
		List<String> escaped = new ArrayList<>(fields.length);
		for (Object field : fields) {
			escaped.add(csv(field));
		}
		return String.join(",", escaped);
	}

	private String csv(Object value) {
		if (value == null) {
			return "";
		}
		String text = value instanceof BigDecimal decimal ? decimal.toPlainString() : value.toString();
		return '"' + text.replace("\"", "\"\"") + '"';
	}

	private String value(Object value) {
		if (value == null) {
			return "N/A";
		}
		return value instanceof BigDecimal decimal ? decimal.toPlainString() : value.toString();
	}

	private void writeAtomically(Path target, String content) throws IOException {
		Path temporary = Files.createTempFile(
				target.getParent(),
				target.getFileName() + ".",
				".tmp"
		);
		try {
			Files.writeString(temporary, content, StandardCharsets.UTF_8);
			try {
				Files.move(
						temporary,
						target,
						StandardCopyOption.ATOMIC_MOVE,
						StandardCopyOption.REPLACE_EXISTING
				);
			}
			catch (AtomicMoveNotSupportedException exception) {
				Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		finally {
			Files.deleteIfExists(temporary);
		}
	}
}
