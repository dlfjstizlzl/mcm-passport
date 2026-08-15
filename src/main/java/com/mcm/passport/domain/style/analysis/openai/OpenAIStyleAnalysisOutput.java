package com.mcm.passport.domain.style.analysis.openai;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** Java class used by the OpenAI SDK to derive and parse the Structured Output schema. */
public class OpenAIStyleAnalysisOutput {

	@JsonPropertyDescription("PrototypeStyleCatalog에 있는 CityCode enum identifier")
	public String cityCode;

	@JsonPropertyDescription("PrototypeStyleCatalog에 있는 RecommendedProduct enum identifier")
	public String recommendedProduct;

	@JsonPropertyDescription("PrototypeStyleCatalog에 있는 StyleMood enum identifier")
	public String styleMood;

	@JsonPropertyDescription("선택한 cityCode와 연결된 CityBackground enum identifier")
	public String background;

	@JsonPropertyDescription("오늘의 Journey 신호와 제안을 연결하는 짧은 한국어 1~2문장")
	public String description;

	@JsonPropertyDescription("이번 결과의 화면 presentation affinity를 나타내는 0~100 정수이며 확률이나 고객 점수가 아님")
	public Integer matchScore;

	public OpenAIStyleAnalysisOutput() {
	}

	public OpenAIStyleAnalysisOutput(
			String cityCode,
			String recommendedProduct,
			String styleMood,
			String background,
			String description,
			Integer matchScore
	) {
		this.cityCode = cityCode;
		this.recommendedProduct = recommendedProduct;
		this.styleMood = styleMood;
		this.background = background;
		this.description = description;
		this.matchScore = matchScore;
	}

	public String cityCode() {
		return cityCode;
	}

	public String recommendedProduct() {
		return recommendedProduct;
	}

	public String styleMood() {
		return styleMood;
	}

	public String background() {
		return background;
	}

	public String description() {
		return description;
	}

	public Integer matchScore() {
		return matchScore;
	}
}
