package com.mcm.passport.global.config;

import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlMapFactoryBean;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiContractTest {

	private static final Set<String> HTTP_METHODS = Set.of("get", "post", "put", "delete", "patch");

	private static final Set<String> BE1_OPERATION_IDS = Set.of(
			"createPassportSession",
			"getPassportSession",
			"getJourneyProgress",
			"getJourneySpots",
			"getJourneySpot",
			"upsertGuideResponse",
			"completeJourneySpot",
			"getProduct",
			"createProductTag",
			"getProductTags",
			"issueBoardingPass",
			"getBoardingPass"
	);

	private static final Map<Endpoint, String> BE2_OPERATIONS = Map.ofEntries(
			Map.entry(new Endpoint("post", "/api/style-spots/{styleSpotId}/connect"), "connectStyleSpot"),
			Map.entry(new Endpoint("post", "/api/style-spot-sessions/{styleSpotSessionId}/disconnect"),
					"disconnectStyleSpot"),
			Map.entry(new Endpoint("get", "/api/style-spots/{styleSpotId}/display"), "getStyleSpotDisplay"),
			Map.entry(new Endpoint("get", "/api/passport-sessions/{passportSessionId}/style-result"),
					"getStyleResult"),
			Map.entry(new Endpoint("post", "/api/passport-sessions/{passportSessionId}/portrait"),
					"createStylePortrait"),
			Map.entry(new Endpoint("get", "/api/passport-sessions/{passportSessionId}/portrait"),
					"getStylePortrait"),
			Map.entry(new Endpoint("delete", "/api/passport-sessions/{passportSessionId}/portrait"),
					"deleteStylePortrait"),
			Map.entry(new Endpoint("post", "/api/passport-sessions/{passportSessionId}/souvenir"),
					"createJourneySouvenir"),
			Map.entry(new Endpoint("get", "/api/passport-sessions/{passportSessionId}/souvenir"),
					"getJourneySouvenir"),
			Map.entry(new Endpoint("get", "/api/passport-sessions/{passportSessionId}/my-passport"),
					"getMyPassport")
	);

	private static final Set<String> LEGACY_PATHS = Set.of(
			"/api/style-spots/{spotCode}/connections",
			"/api/style-spots/{spotCode}/analysis",
			"/api/style-spots/{spotCode}/result",
			"/api/style-spots/{spotCode}/reset"
	);

	@Test
	void isAValidResolvedOpenApi31Document() throws IOException {
		ClassPathResource resource = new ClassPathResource("static/openapi.yaml");
		ParseOptions options = new ParseOptions();
		options.setResolve(true);

		SwaggerParseResult result = new OpenAPIV3Parser()
				.readLocation(resource.getURL().toExternalForm(), null, options);

		assertThat(result.getOpenAPI()).isNotNull();
		assertThat(result.getOpenAPI().getOpenapi()).isEqualTo("3.1.0");
		assertThat(result.getMessages())
				.as("OpenAPI parser validation and $ref resolution messages")
				.isNullOrEmpty();
	}

	@Test
	void containsExactlyTheSupportedBe1AndBe2Operations() {
		Map<String, Object> document = document();
		Map<String, Object> paths = map(document.get("paths"));
		Map<Endpoint, String> operations = operations(paths);

		assertThat(document.get("openapi")).isEqualTo("3.1.0");
		assertThat(paths).hasSize(17);
		assertThat(operations).hasSize(22);

		Set<String> expectedOperationIds = new LinkedHashSet<>(BE1_OPERATION_IDS);
		expectedOperationIds.addAll(BE2_OPERATIONS.values());
		assertThat(operations.values()).containsExactlyInAnyOrderElementsOf(expectedOperationIds);

		assertThat(operations).containsAllEntriesOf(BE2_OPERATIONS);
		assertThat(paths.keySet()).doesNotContainAnyElementsOf(LEGACY_PATHS);
	}

	@Test
	void definesTheIntegratedStyleConnectAndResultContract() {
		Map<String, Object> document = document();

		Map<String, Object> styleSpotId = parameter(document, "StyleSpotId");
		assertThat(styleSpotId).containsEntry("name", "styleSpotId").containsEntry("in", "path");
		assertThat(map(styleSpotId.get("schema"))).containsEntry("type", "string");

		Map<String, Object> connect = operation(document, "post", "/api/style-spots/{styleSpotId}/connect");
		Map<String, Object> responses = map(connect.get("responses"));
		assertThat(responses.keySet()).containsExactlyInAnyOrder("200", "400", "404", "409", "415", "500");
		assertResponseSchema(responses, "200", "#/components/schemas/StyleSpotConnectResponse");

		Map<String, Object> connectResponse = schema(document, "StyleSpotConnectResponse");
		assertRequiredExactly(connectResponse,
				"styleSpotSessionId", "styleSpotId", "passportSessionId", "status", "styleResult");
		assertThat(property(connectResponse, "styleSpotId")).containsEntry("type", "string");
		assertThat(list(property(connectResponse, "status").get("enum"))).containsExactly("RESULT");
		assertReference(property(connectResponse, "styleResult"), "#/components/schemas/StyleResultResponse");

		Map<String, Object> disconnect = operation(
				document, "post", "/api/style-spot-sessions/{styleSpotSessionId}/disconnect");
		assertResponseSchema(map(disconnect.get("responses")), "200",
				"#/components/schemas/StyleSpotSessionResponse");
		Map<String, Object> disconnectedSession = schema(document, "StyleSpotSessionResponse");
		assertRequiredExactly(disconnectedSession,
				"id", "styleSpotId", "passportSessionId", "status", "connectedAt", "disconnectedAt");
		assertThat(list(property(disconnectedSession, "status").get("enum")))
				.containsExactly("DISCONNECTED");

		Map<String, Object> display = schema(document, "StyleSpotDisplayResponse");
		assertRequiredExactly(display, "styleSpotId", "activeStyleSpotSessionId", "status", "styleResult");
		assertThat(list(property(display, "activeStyleSpotSessionId").get("type")))
				.containsExactlyInAnyOrder("integer", "null");
		assertReference(property(display, "status"), "#/components/schemas/StyleSpotStatus");
		assertNullableReference(property(display, "styleResult"),
				"#/components/schemas/StyleResultResponse");
		assertThat(list(schema(document, "StyleSpotStatus").get("enum")))
				.containsExactly("WAITING", "CONNECTED", "ANALYZING", "RESULT", "RESET");

		Map<String, Object> styleResult = schema(document, "StyleResultResponse");
		Set<String> styleResultFields = Set.of(
				"id", "passportSessionId", "cityCode", "cityCodeName",
				"recommendedProductCode", "recommendedProductName", "recommendedProductImageUrl", "styleMood", "styleMoodName",
				"backgroundCode", "backgroundName", "backgroundAssetKey", "description",
				"matchScore", "usedFallback", "createdAt"
		);
		assertThat(properties(styleResult).keySet()).containsExactlyInAnyOrderElementsOf(styleResultFields);
		assertThat(new LinkedHashSet<>(list(styleResult.get("required"))))
				.containsExactlyInAnyOrderElementsOf(styleResultFields);
		assertThat(property(styleResult, "matchScore"))
				.containsEntry("type", "integer")
				.containsEntry("minimum", 0)
				.containsEntry("maximum", 100);
		assertThat(property(styleResult, "usedFallback")).containsEntry("type", "boolean");
	}

	@Test
	void definesPortraitSouvenirAndMyPassportBoundarySemantics() {
		Map<String, Object> document = document();

		Map<String, Object> portraitRequest = schema(document, "StylePortraitRequest");
		assertRequiredExactly(portraitRequest, "imageUrl", "consent");
		assertThat(property(portraitRequest, "imageUrl"))
				.containsEntry("type", "string")
				.containsEntry("maxLength", 1000);
		assertThat(property(portraitRequest, "consent"))
				.containsEntry("type", "boolean")
				.containsEntry("const", true);

		Map<String, Object> portraitResponse = schema(document, "StylePortraitResponse");
		assertRequiredExactly(portraitResponse, "id", "passportSessionId", "imageUrl", "consent", "createdAt");
		assertThat(property(portraitResponse, "consent")).containsEntry("type", "boolean");

		Map<String, Object> souvenirPost = operation(
				document, "post", "/api/passport-sessions/{passportSessionId}/souvenir");
		Map<String, Object> souvenirResponses = map(souvenirPost.get("responses"));
		assertThat(souvenirResponses.keySet())
				.containsExactlyInAnyOrder("200", "201", "400", "404", "409", "500");
		assertResponseSchema(souvenirResponses, "200", "#/components/schemas/JourneySouvenirResponse");
		assertResponseSchema(souvenirResponses, "201", "#/components/schemas/JourneySouvenirResponse");

		Map<String, Object> souvenir = schema(document, "JourneySouvenirResponse");
		assertRequiredExactly(souvenir,
				"id", "passportSessionId", "styleResultId", "cityCode", "cityCodeName",
				"recommendedProductCode", "recommendedProductName", "styleMood", "styleMoodName",
				"backgroundCode", "backgroundName", "backgroundAssetKey", "journeyStamps",
				"taggedProductCodes", "createdAt");
		assertArrayOfStrings(property(souvenir, "journeyStamps"));
		assertArrayOfStrings(property(souvenir, "taggedProductCodes"));

		Map<String, Object> myPassport = schema(document, "MyPassportResponse");
		assertRequiredExactly(myPassport,
				"passportSessionId", "cardUid", "status", "startedAt", "completedAt", "journey",
				"styleResult", "portrait", "souvenir");
		assertNullableString(property(myPassport, "completedAt"));
		assertNullableReference(property(myPassport, "styleResult"),
				"#/components/schemas/StyleResultResponse");
		assertNullableReference(property(myPassport, "portrait"),
				"#/components/schemas/StylePortraitResponse");
		assertNullableReference(property(myPassport, "souvenir"),
				"#/components/schemas/JourneySouvenirResponse");
	}

	private Map<String, Object> document() {
		YamlMapFactoryBean yaml = new YamlMapFactoryBean();
		yaml.setResources(new ClassPathResource("static/openapi.yaml"));
		Map<String, Object> document = yaml.getObject();
		assertThat(document).isNotNull();
		return document;
	}

	private Map<Endpoint, String> operations(Map<String, Object> paths) {
		Map<Endpoint, String> operations = new LinkedHashMap<>();
		paths.forEach((path, value) -> map(value).forEach((method, operation) -> {
			if (HTTP_METHODS.contains(method)) {
				operations.put(new Endpoint(method, path), String.valueOf(map(operation).get("operationId")));
			}
		}));
		return operations;
	}

	private Map<String, Object> operation(Map<String, Object> document, String method, String path) {
		return map(map(map(document.get("paths")).get(path)).get(method));
	}

	private Map<String, Object> parameter(Map<String, Object> document, String name) {
		return map(map(map(document.get("components")).get("parameters")).get(name));
	}

	private Map<String, Object> schema(Map<String, Object> document, String name) {
		return map(map(map(document.get("components")).get("schemas")).get(name));
	}

	private Map<String, Object> properties(Map<String, Object> schema) {
		return map(schema.get("properties"));
	}

	private Map<String, Object> property(Map<String, Object> schema, String name) {
		return map(properties(schema).get(name));
	}

	private void assertRequiredExactly(Map<String, Object> schema, String... fields) {
		assertThat(list(schema.get("required"))).containsExactlyInAnyOrder((Object[]) fields);
		assertThat(properties(schema).keySet()).containsExactlyInAnyOrder(fields);
	}

	private void assertResponseSchema(
			Map<String, Object> responses,
			String status,
			String expectedReference
	) {
		Map<String, Object> response = map(responses.get(status));
		Map<String, Object> content = map(response.get("content"));
		Map<String, Object> mediaType = map(content.get("application/json"));
		assertReference(map(mediaType.get("schema")), expectedReference);
	}

	private void assertReference(Map<String, Object> schema, String expectedReference) {
		assertThat(schema).containsEntry("$ref", expectedReference);
	}

	private void assertArrayOfStrings(Map<String, Object> schema) {
		assertThat(schema).containsEntry("type", "array");
		assertThat(map(schema.get("items"))).containsEntry("type", "string");
	}

	private void assertNullableString(Map<String, Object> schema) {
		assertThat(list(schema.get("type"))).containsExactlyInAnyOrder("string", "null");
	}

	private void assertNullableReference(Map<String, Object> schema, String expectedReference) {
		List<?> oneOf = list(schema.get("oneOf"));
		assertThat(oneOf).anySatisfy(candidate -> assertReference(map(candidate), expectedReference));
		assertThat(oneOf).anySatisfy(candidate -> assertThat(map(candidate)).containsEntry("type", "null"));
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> map(Object value) {
		assertThat(value).isInstanceOf(Map.class);
		return (Map<String, Object>) value;
	}

	@SuppressWarnings("unchecked")
	private List<Object> list(Object value) {
		assertThat(value).isInstanceOf(List.class);
		return (List<Object>) value;
	}

	private record Endpoint(String method, String path) {
	}
}
