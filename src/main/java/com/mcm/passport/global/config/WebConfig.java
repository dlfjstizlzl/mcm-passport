package com.mcm.passport.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.stream.Stream;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private static final String PRODUCTION_FRONTEND_ORIGIN = "https://hackathonfront.devdlfjstizlzl.xyz";

	private final String[] allowedOrigins;

	public WebConfig(@Value("${mcm.cors.allowed-origins:http://localhost:3000,http://127.0.0.1:3000,https://hackathonfront.devdlfjstizlzl.xyz}") String[] allowedOrigins) {
		this.allowedOrigins = Stream.concat(Arrays.stream(allowedOrigins), Stream.of(PRODUCTION_FRONTEND_ORIGIN))
				.distinct()
				.toArray(String[]::new);
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/api/**")
				.allowedOriginPatterns(allowedOrigins)
				.allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
				.allowedHeaders("*")
				.maxAge(3600);
	}
}
