package com.mcm.passport.domain.demo.benchmark;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAIBenchmarkReportWriterTest {

	@TempDir
	Path temporaryDirectory;

	@Test
	void writesCsvJsonAndHumanReadableMarkdownReportsOffline() throws Exception {
		OpenAIBenchmarkExecution execution = execution(List.of(
				run(1, "CASE_A_WITHOUT_PRODUCT_TAG", "BERLIN_AFTERDARK_NOMAD", false, true),
				run(2, "CASE_B_WITH_PRODUCT_TAG", "SEOUL_NEON_PLAYER", false, true)
		));
		OpenAIBenchmarkSummary summary = OpenAIBenchmarkSummary.from(
				execution,
				OpenAIBenchmarkPricingSnapshot.standardShortContext()
		);
		Path reportDirectory = temporaryDirectory.resolve("nested/report");

		List<Path> written = new OpenAIBenchmarkReportWriter().write(
				reportDirectory,
				execution,
				summary
		);

		assertThat(written)
				.extracting(path -> path.getFileName().toString())
				.containsExactly(
						OpenAIBenchmarkReportWriter.RUNS_CSV,
						OpenAIBenchmarkReportWriter.SUMMARY_JSON,
						OpenAIBenchmarkReportWriter.SUMMARY_MARKDOWN
				);
		String csv = Files.readString(reportDirectory.resolve("runs.csv"), StandardCharsets.UTF_8);
		String json = Files.readString(reportDirectory.resolve("summary.json"), StandardCharsets.UTF_8);
		String markdown = Files.readString(reportDirectory.resolve("summary.md"), StandardCharsets.UTF_8);

		assertThat(csv)
				.startsWith("runNumber,caseName,model,reasoningEffort")
				.contains(
						"cachedInputTokens",
						"cacheWriteTokens",
						"reasoningTokens",
						"failureType",
						"failureStage",
						"safeFailureDetail",
						"responseModel"
				)
				.contains("\"CASE_A_WITHOUT_PRODUCT_TAG\"", "\"0.001\"");
		assertThat(json)
				.contains("\"status\"", "\"COMPLETED\"")
				.contains("\"pricingCheckedAt\"", "2026-08-16")
				.contains("\"models\"", "\"sameResultRate\"", "\"failures\"");
		assertThat(markdown)
				.contains("# OpenAI Style Analysis Benchmark")
				.contains("Estimated cost is a benchmark estimate", "Consistency is not accuracy")
				.contains(
						"gpt-5.6-luna",
						"sameResultRate",
						"ProductTag comparison",
						"Safe failure diagnostics",
						"No failed OpenAI benchmark runs were recorded"
				)
				.doesNotContain("OPENAI_API_KEY", "sk-");
	}

	@Test
	void writesOnlyAllowlistedFailureMetadataToEveryReportFormat() throws Exception {
		OpenAIBenchmarkRun failedRun = diagnosticRun();
		OpenAIBenchmarkExecution execution = new OpenAIBenchmarkExecution(
				"2026-08-16T00:00:00Z",
				"2026-08-16T00:00:05Z",
				OpenAIBenchmarkErrorCategory.COST_ESTIMATE_UNAVAILABLE,
				List.of(failedRun)
		);
		OpenAIBenchmarkSummary summary = OpenAIBenchmarkSummary.from(
				execution,
				OpenAIBenchmarkPricingSnapshot.standardShortContext()
		);
		Path reportDirectory = temporaryDirectory.resolve("failure-report");

		new OpenAIBenchmarkReportWriter().write(reportDirectory, execution, summary);

		String csv = Files.readString(reportDirectory.resolve("runs.csv"), StandardCharsets.UTF_8);
		String json = Files.readString(reportDirectory.resolve("summary.json"), StandardCharsets.UTF_8);
		String markdown = Files.readString(reportDirectory.resolve("summary.md"), StandardCharsets.UTF_8);
		assertThat(csv).contains(
				"\"OpenAIInvalidDataException\"",
				"\"SDK_RESPONSE_DESERIALIZATION\"",
				"\"SDK_RESPONSE_DESERIALIZATION_FAILED\"",
				"\"req_safe_789\"",
				"\"gpt-5.6-luna-2026-08-01\""
		);
		assertThat(json).contains(
				"\"failureType\"",
				"\"OpenAIInvalidDataException\"",
				"\"failureStage\"",
				"\"SDK_RESPONSE_DESERIALIZATION\"",
				"\"safeFailureDetail\"",
				"\"SDK_RESPONSE_DESERIALIZATION_FAILED\"",
				"\"requestId\"",
				"\"req_safe_789\"",
				"\"responseModel\"",
				"\"gpt-5.6-luna-2026-08-01\""
		);
		assertThat(markdown).contains(
				"Safe failure diagnostics",
				"OpenAIInvalidDataException",
				"SDK_RESPONSE_DESERIALIZATION",
				"SDK_RESPONSE_DESERIALIZATION_FAILED",
				"req_safe_789",
				"gpt-5.6-luna-2026-08-01"
		);
		assertThat(csv + json + markdown)
				.doesNotContain("RAW_RESPONSE_SECRET_SENTINEL", "API_KEY_SECRET_SENTINEL");
	}

	@Test
	void csvEscapesTextAndLeavesUnavailableMetricsEmptyInsteadOfWritingZero() {
		OpenAIBenchmarkRun run = new OpenAIBenchmarkRun(
				1,
				"CASE, \"quoted\"",
				"gpt-5.6-luna",
				"none",
				null,
				null,
				null,
				null,
				null,
				true,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				9L,
				null,
				false,
				OpenAIBenchmarkErrorCategory.FALLBACK
		);

		String csv = new OpenAIBenchmarkReportWriter().renderCsv(List.of(run));
		String row = csv.lines().skip(1).findFirst().orElseThrow();

		assertThat(row).contains("\"CASE, \"\"quoted\"\"\"");
		assertThat(row).contains(",,,,,");
		assertThat(row).doesNotContain("null", "UNAVAILABLE");
	}

	private OpenAIBenchmarkExecution execution(List<OpenAIBenchmarkRun> runs) {
		return new OpenAIBenchmarkExecution(
				"2026-08-16T00:00:00Z",
				"2026-08-16T00:01:00Z",
				OpenAIBenchmarkErrorCategory.NONE,
				runs
		);
	}

	private OpenAIBenchmarkRun run(
			int runNumber,
			String caseName,
			String cityCode,
			boolean usedFallback,
			boolean success
	) {
		return new OpenAIBenchmarkRun(
				runNumber,
				caseName,
				"gpt-5.6-luna",
				"none",
				cityCode,
				"STARK_BACKPACK",
				"AFTERDARK_MOVEMENT",
				"BERLIN_AFTER_DARK",
				90,
				usedFallback,
				1_000L,
				100L,
				50L,
				100L,
				20L,
				1_100L,
				25L,
				30L,
				new BigDecimal("0.001"),
				success,
				OpenAIBenchmarkErrorCategory.NONE
		);
	}

	private OpenAIBenchmarkRun diagnosticRun() {
		return new OpenAIBenchmarkRun(
				1,
				"CASE_A_WITHOUT_PRODUCT_TAG",
				"gpt-5.6-luna",
				"none",
				"BERLIN_AFTERDARK_NOMAD",
				"STARK_BACKPACK",
				"AFTERDARK_MOVEMENT",
				"BERLIN_AFTER_DARK",
				90,
				true,
				null,
				null,
				null,
				null,
				null,
				null,
				3_505L,
				3_689L,
				null,
				false,
				OpenAIBenchmarkErrorCategory.SDK_RESPONSE_DESERIALIZATION,
				"OpenAIInvalidDataException",
				"SDK_RESPONSE_DESERIALIZATION",
				"SDK_RESPONSE_DESERIALIZATION_FAILED",
				200,
				null,
				"req_safe_789",
				"gpt-5.6-luna-2026-08-01"
		);
	}
}
