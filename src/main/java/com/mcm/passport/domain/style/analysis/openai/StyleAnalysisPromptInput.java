package com.mcm.passport.domain.style.analysis.openai;

import com.mcm.passport.domain.journey.service.JourneyDataSnapshot;
import com.mcm.passport.domain.style.catalog.PrototypeStyleCatalog;
import com.mcm.passport.domain.style.entity.CityBackground;
import com.mcm.passport.domain.style.entity.CityCode;
import com.mcm.passport.domain.style.entity.RecommendedProduct;
import com.mcm.passport.domain.style.entity.StyleMood;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * External-analysis input containing only today's Journey signals and the prototype catalog.
 * Internal database identifiers are intentionally not sent to the model.
 */
public record StyleAnalysisPromptInput(
		List<ResponseInput> responses,
		List<StampInput> stamps,
		List<ProductTagInput> taggedProducts,
		CatalogInput catalog
) {

	public StyleAnalysisPromptInput {
		responses = List.copyOf(Objects.requireNonNull(responses, "responses must not be null"));
		stamps = List.copyOf(Objects.requireNonNull(stamps, "stamps must not be null"));
		taggedProducts = List.copyOf(Objects.requireNonNull(taggedProducts, "taggedProducts must not be null"));
		Objects.requireNonNull(catalog, "catalog must not be null");
	}

	public static StyleAnalysisPromptInput from(JourneyDataSnapshot snapshot) {
		Objects.requireNonNull(snapshot, "snapshot must not be null");
		return new StyleAnalysisPromptInput(
				snapshot.responses().stream()
						.map(response -> new ResponseInput(
								response.spotCode(),
								response.questionCode(),
								response.answerCode(),
								response.answerText()
						))
						.toList(),
				snapshot.stamps().stream()
						.map(stamp -> new StampInput(stamp.spotCode()))
						.toList(),
				snapshot.taggedProducts().stream()
						.map(product -> new ProductTagInput(product.code(), product.name()))
						.toList(),
				prototypeCatalog()
		);
	}

	private static CatalogInput prototypeCatalog() {
		return new CatalogInput(
				codes(CityCode.values()),
				codes(RecommendedProduct.values()),
				codes(StyleMood.values()),
				codes(CityBackground.values()),
				Arrays.stream(CityCode.values())
						.map(cityCode -> new CityBackgroundRelation(
								cityCode.name(),
								PrototypeStyleCatalog.cityProfile(cityCode).background().name()
						))
						.toList()
		);
	}

	private static <E extends Enum<E>> List<String> codes(E[] values) {
		return Arrays.stream(values)
				.map(Enum::name)
				.toList();
	}

	public record ResponseInput(
			String spotCode,
			String questionCode,
			String answerCode,
			String answerText
	) {
	}

	public record StampInput(String spotCode) {
	}

	public record ProductTagInput(String code, String name) {
	}

	public record CatalogInput(
			List<String> cityCodes,
			List<String> recommendedProducts,
			List<String> styleMoods,
			List<String> backgrounds,
			List<CityBackgroundRelation> cityBackgroundRelations
	) {

		public CatalogInput {
			cityCodes = List.copyOf(Objects.requireNonNull(cityCodes, "cityCodes must not be null"));
			recommendedProducts = List.copyOf(Objects.requireNonNull(
					recommendedProducts,
					"recommendedProducts must not be null"
			));
			styleMoods = List.copyOf(Objects.requireNonNull(styleMoods, "styleMoods must not be null"));
			backgrounds = List.copyOf(Objects.requireNonNull(backgrounds, "backgrounds must not be null"));
			cityBackgroundRelations = List.copyOf(Objects.requireNonNull(
					cityBackgroundRelations,
					"cityBackgroundRelations must not be null"
			));
		}
	}

	public record CityBackgroundRelation(String cityCode, String background) {
	}
}
