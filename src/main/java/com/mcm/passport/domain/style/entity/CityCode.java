package com.mcm.passport.domain.style.entity;

public enum CityCode {
	BERLIN_AFTERDARK_NOMAD(
			"Berlin Afterdark Nomad",
			RecommendedProduct.STARK_BACKPACK,
			StyleMood.AFTERDARK_MOVEMENT,
			CityBackground.BERLIN_AFTERDARK,
			"오늘의 반응은 밤의 베를린처럼 자유로운 움직임과 선명한 존재감이 어우러지는 장면을 제안합니다."
	),
	SEOUL_CREATIVE_MOVER(
			"Seoul Creative Mover",
			RecommendedProduct.AREN_CROSSBODY,
			StyleMood.CREATIVE_PULSE,
			CityBackground.SEOUL_NEON_PULSE,
			"오늘의 반응은 빠르게 변하는 서울의 리듬처럼 실용성과 창의적인 포인트가 만나는 장면을 제안합니다."
	),
	PARIS_MODERN_VOYAGER(
			"Paris Modern Voyager",
			RecommendedProduct.DIAMANT_3D_SHOULDER_BAG,
			StyleMood.MODERN_ELEGANCE,
			CityBackground.PARIS_MODERN_CLASSIC,
			"오늘의 반응은 파리의 정제된 균형처럼 모던한 선과 우아한 디테일이 이어지는 장면을 제안합니다."
	);

	private final String displayName;
	private final RecommendedProduct defaultProduct;
	private final StyleMood defaultMood;
	private final CityBackground background;
	private final String defaultDescription;

	CityCode(
			String displayName,
			RecommendedProduct defaultProduct,
			StyleMood defaultMood,
			CityBackground background,
			String defaultDescription
	) {
		this.displayName = displayName;
		this.defaultProduct = defaultProduct;
		this.defaultMood = defaultMood;
		this.background = background;
		this.defaultDescription = defaultDescription;
	}

	public String getDisplayName() {
		return displayName;
	}

	public RecommendedProduct getDefaultProduct() {
		return defaultProduct;
	}

	public StyleMood getDefaultMood() {
		return defaultMood;
	}

	public CityBackground getBackground() {
		return background;
	}

	public String getDefaultDescription() {
		return defaultDescription;
	}
}
