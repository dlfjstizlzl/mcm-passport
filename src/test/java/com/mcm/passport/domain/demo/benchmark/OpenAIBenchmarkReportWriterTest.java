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
				run(2, "CASE_B_WITH_PRODUCT_TAG", "SEOUL_CREATIVE_MOVER", false, true)
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
				.contains("cachedInputTokens", "cacheWriteTokens", "reasoningTokens")
				.contains("\"CASE_A_WITHOUT_PRODUCT_TAG\"", "\"0.001\"");
		assertThat(json)
				.contains("\"status\"", "\"COMPLETED\"")
				.contains("\"pricingCheckedAt\"", "2026-08-16")
				.contains("\"models\"", "\"sameResultRate\"");
		assertThat(markdown)
				.contains("# OpenAI Style Analysis Benchmark")
				.contains("Estimated cost is a benchmark estimate", "Consistency is not accuracy")
				.contains("gpt-5.6-luna", "sameResultRate", "ProductTag comparison")
				.doesNotContain("OPENAI_API_KEY", "sk-");
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
				"BERLIN_AFTERDARK",
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
}
