package com.mcm.passport.domain.demo.benchmark;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Objects;

public final class OpenAIBenchmarkPreflight {

	public void print(OpenAIBenchmarkConfig config, boolean apiKeyConfigured, PrintStream output) {
		Objects.requireNonNull(config, "config must not be null");
		Objects.requireNonNull(output, "output must not be null");
		Path reportDirectory = config.reportDirectory().toAbsolutePath().normalize();

		output.println("OpenAI API key configured: " + yesNo(apiKeyConfigured));
		output.println("Benchmark enabled: " + yesNo(config.enabled()));
		output.println("Live confirmation: " + yesNo(config.confirmLive()));
		output.println("Models: " + config.models());
		output.println("Cases: " + OpenAIBenchmarkFixtures.caseNames());
		output.println("Repetitions: " + config.repetitions());
		output.println("Reasoning effort: " + config.reasoningEffort().value());
		output.println("Planned calls: " + config.plannedCalls());
		output.println("Maximum calls: " + config.maxCalls());
		output.println("Maximum estimated USD: " + config.maxEstimatedUsd().toPlainString());
		output.println("Report directory: " + reportDirectory);
	}

	private String yesNo(boolean value) {
		return value ? "yes" : "no";
	}
}
