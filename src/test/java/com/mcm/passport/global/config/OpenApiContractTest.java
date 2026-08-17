package com.mcm.passport.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiContractTest {

	private static final Set<String> HTTP_METHODS = Set.of("get", "post", "put", "delete", "patch");

	@Test
	void parsesFrontendOpenApiContractAndContainsAllSpecifiedOperations() {
		YamlMapFactoryBean yaml = new YamlMapFactoryBean();
		yaml.setResources(new ClassPathResource("static/openapi.yaml"));
		Map<String, Object> document = yaml.getObject();

		assertThat(document).isNotNull();
		assertThat(document.get("openapi")).isEqualTo("3.1.0");

		Map<?, ?> paths = (Map<?, ?>) document.get("paths");
		long operationCount = paths.values().stream()
				.map(value -> (Map<?, ?>) value)
				.flatMap(path -> path.keySet().stream())
				.filter(HTTP_METHODS::contains)
				.count();

		assertThat(paths).hasSize(17);
		assertThat(operationCount).isEqualTo(22);
	}
}
