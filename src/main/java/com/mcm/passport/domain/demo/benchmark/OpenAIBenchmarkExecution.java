package com.mcm.passport.domain.demo.benchmark;

import java.util.List;
import java.util.Objects;

public record OpenAIBenchmarkExecution(
		String startedAt,
		String completedAt,
		OpenAIBenchmarkErrorCategory terminationReason,
		List<OpenAIBenchmarkRun> runs
) {

	public OpenAIBenchmarkExecution {
		Objects.requireNonNull(startedAt, "startedAt must not be null");
		Objects.requireNonNull(completedAt, "completedAt must not be null");
		Objects.requireNonNull(terminationReason, "terminationReason must not be null");
		runs = List.copyOf(Objects.requireNonNull(runs, "runs must not be null"));
	}
}
