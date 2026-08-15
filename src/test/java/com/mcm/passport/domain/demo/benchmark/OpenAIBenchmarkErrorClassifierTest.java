package com.mcm.passport.domain.demo.benchmark;

import com.mcm.passport.domain.style.analysis.openai.OpenAIFailureDetail;
import com.mcm.passport.domain.style.analysis.openai.OpenAIFailureDiagnostic;
import com.mcm.passport.domain.style.analysis.openai.OpenAIFailureStage;
import com.mcm.passport.domain.style.analysis.openai.OpenAIStructuredOutputException;
import com.openai.errors.OpenAIInvalidDataException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAIBenchmarkErrorClassifierTest {

	private final OpenAIBenchmarkErrorClassifier classifier = new OpenAIBenchmarkErrorClassifier();

	@Test
	void stageTakesPriorityOverAnOtherwiseAmbiguousExceptionType() {
		OpenAIStructuredOutputException failure = new OpenAIStructuredOutputException();
		OpenAIFailureDiagnostic diagnostic = OpenAIFailureDiagnostic.of(
				failure,
				OpenAIFailureStage.SDK_RESPONSE_DESERIALIZATION,
				OpenAIFailureDetail.SDK_RESPONSE_DESERIALIZATION_FAILED,
				200,
				null,
				"req_stage_first"
		);

		assertThat(classifier.classify(failure, diagnostic))
				.isEqualTo(OpenAIBenchmarkErrorCategory.SDK_RESPONSE_DESERIALIZATION);
	}

	@Test
	void distinguishesStructuredOutputUsageMetadataAndValidationStages() {
		IllegalStateException failure = new IllegalStateException("message must not be classified");

		assertThat(classifier.classify(failure, diagnostic(
				failure,
				OpenAIFailureStage.STRUCTURED_OUTPUT_DESERIALIZATION,
				OpenAIFailureDetail.STRUCTURED_OUTPUT_DESERIALIZATION_FAILED
		))).isEqualTo(OpenAIBenchmarkErrorCategory.STRUCTURED_OUTPUT_PARSE);
		assertThat(classifier.classify(failure, diagnostic(
				failure,
				OpenAIFailureStage.USAGE_MAPPING,
				OpenAIFailureDetail.USAGE_MAPPING_FAILED
		))).isEqualTo(OpenAIBenchmarkErrorCategory.USAGE_MAPPING);
		assertThat(classifier.classify(failure, diagnostic(
				failure,
				OpenAIFailureStage.RESPONSE_METADATA,
				OpenAIFailureDetail.RESPONSE_MODEL_UNAVAILABLE
		))).isEqualTo(OpenAIBenchmarkErrorCategory.RESPONSE_METADATA);
		assertThat(classifier.classify(failure, diagnostic(
				failure,
				OpenAIFailureStage.VALIDATION,
				OpenAIFailureDetail.VALIDATION_FAILED
		))).isEqualTo(OpenAIBenchmarkErrorCategory.VALIDATION);
	}

	@Test
	void classifiesSafeHttpStatusOnlyAfterRecognizingTheHttpRequestStage() {
		IllegalStateException failure = new IllegalStateException("not retained");

		assertThat(classifier.classify(failure, httpDiagnostic(failure, 401)))
				.isEqualTo(OpenAIBenchmarkErrorCategory.AUTHENTICATION);
		assertThat(classifier.classify(failure, httpDiagnostic(failure, 404)))
				.isEqualTo(OpenAIBenchmarkErrorCategory.MODEL_UNAVAILABLE);
		assertThat(classifier.classify(failure, httpDiagnostic(failure, 429)))
				.isEqualTo(OpenAIBenchmarkErrorCategory.RATE_LIMIT);
	}

	@Test
	void classifiesAllowlistedSdkTypesWhenHttpStatusIsUnavailable() {
		assertThat(classifier.classify(null, httpDiagnostic("UnauthorizedException", null)))
				.isEqualTo(OpenAIBenchmarkErrorCategory.AUTHENTICATION);
		assertThat(classifier.classify(null, httpDiagnostic("RateLimitException", null)))
				.isEqualTo(OpenAIBenchmarkErrorCategory.RATE_LIMIT);
		assertThat(classifier.classify(null, httpDiagnostic("PermissionDeniedException", null)))
				.isEqualTo(OpenAIBenchmarkErrorCategory.MODEL_UNAVAILABLE);
		assertThat(classifier.classify(null, httpDiagnostic("NotFoundException", null)))
				.isEqualTo(OpenAIBenchmarkErrorCategory.MODEL_UNAVAILABLE);
	}

	@Test
	void classifiesAllowlistedErrorCodesWhenHttpStatusIsUnavailable() {
		assertThat(classifier.classify(null, httpDiagnostic("OpenAIServiceException", "invalid_api_key")))
				.isEqualTo(OpenAIBenchmarkErrorCategory.AUTHENTICATION);
		assertThat(classifier.classify(null, httpDiagnostic("OpenAIServiceException", "insufficient_quota")))
				.isEqualTo(OpenAIBenchmarkErrorCategory.RATE_LIMIT);
		assertThat(classifier.classify(null, httpDiagnostic("OpenAIServiceException", "rate_limit_exceeded")))
				.isEqualTo(OpenAIBenchmarkErrorCategory.RATE_LIMIT);
		assertThat(classifier.classify(null, httpDiagnostic("OpenAIServiceException", "model_not_found")))
				.isEqualTo(OpenAIBenchmarkErrorCategory.MODEL_UNAVAILABLE);
	}

	@Test
	void unknownFailureWithoutAStageRemainsUnknown() {
		assertThat(classifier.classify(new IllegalStateException("ambiguous")))
				.isEqualTo(OpenAIBenchmarkErrorCategory.UNKNOWN);
		assertThat(classifier.classify(new OpenAIInvalidDataException("ambiguous SDK data")))
				.isEqualTo(OpenAIBenchmarkErrorCategory.UNKNOWN);
	}

	private OpenAIFailureDiagnostic diagnostic(
			Throwable failure,
			OpenAIFailureStage stage,
			OpenAIFailureDetail detail
	) {
		return OpenAIFailureDiagnostic.of(failure, stage, detail, null, null, null);
	}

	private OpenAIFailureDiagnostic httpDiagnostic(Throwable failure, int status) {
		return OpenAIFailureDiagnostic.of(
				failure,
				OpenAIFailureStage.HTTP_REQUEST,
				OpenAIFailureDetail.HTTP_REQUEST_FAILED,
				status,
				null,
				"req_http_status"
		);
	}

	private OpenAIFailureDiagnostic httpDiagnostic(String failureType, String errorCode) {
		return new OpenAIFailureDiagnostic(
				failureType,
				OpenAIFailureStage.HTTP_REQUEST,
				OpenAIFailureDetail.HTTP_REQUEST_FAILED,
				null,
				errorCode,
				"req_http_diagnostic"
		);
	}
}
