package com.mcm.passport.domain.style.analysis.openai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "mcm.style.analysis.provider", havingValue = "openai")
public class OpenAIStyleAnalysisConfiguration {

	@Bean
	OpenAIClient openAIClient(
			@Value("${mcm.style.analysis.openai.api-key:}") String apiKey,
			@Value("${mcm.style.analysis.openai.timeout:30s}") Duration timeout
	) {
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException(
					"OPENAI_API_KEY must be configured when mcm.style.analysis.provider=openai"
			);
		}
		if (timeout == null || timeout.isZero() || timeout.isNegative()) {
			throw new IllegalStateException("OPENAI_TIMEOUT must be a positive duration");
		}

		return OpenAIOkHttpClient.builder()
				.apiKey(apiKey.trim())
				.timeout(timeout)
				.build();
	}
}
