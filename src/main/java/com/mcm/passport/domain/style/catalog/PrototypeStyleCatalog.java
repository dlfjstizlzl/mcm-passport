package com.mcm.passport.domain.style.catalog;

import com.mcm.passport.domain.style.entity.CityBackground;
import com.mcm.passport.domain.style.entity.CityCode;
import com.mcm.passport.domain.style.entity.RecommendedProduct;
import com.mcm.passport.domain.style.entity.StyleMood;

import java.util.Map;
import java.util.Objects;

/** Curated MVP values and relationships used by style analysis and display. */
public final class PrototypeStyleCatalog {

	private static final Map<CityCode, CityProfile> CITY_PROFILES = Map.of(
			CityCode.BERLIN_AFTERDARK_NOMAD,
			profile(
					"Berlin Afterdark Nomad",
					RecommendedProduct.STARK_BACKPACK,
					StyleMood.AFTERDARK_MOVEMENT,
					CityBackground.BERLIN_AFTER_DARK,
					"오늘의 반응은 밤의 베를린처럼 자유로운 움직임과 선명한 존재감이 어우러지는 장면을 제안합니다."
			),
			CityCode.TOKYO_QUIET_MINIMALIST,
			profile(
					"Tokyo Quiet Minimalist",
					RecommendedProduct.DIAMANT_3D_SHOULDER_BAG,
					StyleMood.MODERN_ELEGANCE,
					CityBackground.TOKYO_QUIET_LINE,
					"오늘의 반응은 도쿄의 고요한 선처럼 정제된 구조와 가벼운 미니멀 감각이 이어지는 장면을 제안합니다."
			),
			CityCode.SEOUL_NEON_PLAYER,
			profile(
					"Seoul Neon Player",
					RecommendedProduct.AREN_CROSSBODY,
					StyleMood.CREATIVE_PULSE,
					CityBackground.SEOUL_PULSE,
					"오늘의 반응은 빠르게 변하는 서울의 리듬처럼 실용성과 감각적인 포인트가 만나는 장면을 제안합니다."
			),
			CityCode.MUNICH_HERITAGE_TRAVELER,
			profile(
					"Munich Heritage Traveler",
					RecommendedProduct.DIAMANT_3D_SHOULDER_BAG,
					StyleMood.MODERN_ELEGANCE,
					CityBackground.MUNICH_HERITAGE,
					"오늘의 반응은 뮌헨의 깊이 있는 헤리티지처럼 클래식한 질감과 오래 가는 스타일을 제안합니다."
			),
			CityCode.NEWYORK_GRAPHIC_MOVER,
			profile(
					"New York Graphic Mover",
					RecommendedProduct.STARK_BACKPACK,
					StyleMood.CREATIVE_PULSE,
					CityBackground.NEW_YORK_GRAPHIC_CITY,
					"오늘의 반응은 뉴욕의 선명한 그래픽처럼 대담한 이미지와 빠른 도시 이동이 만나는 장면을 제안합니다."
			),
			CityCode.HONG_KONG_NEON_NAVIGATOR,
			profile(
					"Hong Kong Neon Navigator",
					RecommendedProduct.AREN_CROSSBODY,
					StyleMood.CREATIVE_PULSE,
					CityBackground.HONG_KONG_NEON_HARBOR,
					"오늘의 반응은 홍콩의 겹쳐지는 네온과 밀도 높은 야경처럼 빠른 이동과 선명한 에너지가 만나는 장면을 제안합니다."
			),
			CityCode.SHANGHAI_FUTURE_VOYAGER,
			profile(
					"Shanghai Future Voyager",
					RecommendedProduct.DIAMANT_3D_SHOULDER_BAG,
					StyleMood.MODERN_ELEGANCE,
					CityBackground.SHANGHAI_FUTURE_SKYLINE,
					"오늘의 반응은 상하이의 미래적인 스카이라인처럼 세련된 구조감과 현대적인 에너지가 이어지는 장면을 제안합니다."
			)
	);

	private static final Map<RecommendedProduct, String> PRODUCT_DISPLAY_NAMES = Map.of(
			RecommendedProduct.STARK_BACKPACK, "Stark Backpack",
			RecommendedProduct.AREN_CROSSBODY, "Aren Crossbody",
			RecommendedProduct.DIAMANT_3D_SHOULDER_BAG, "Diamant 3D Shoulder Bag"
	);

	private static final Map<StyleMood, String> MOOD_DISPLAY_NAMES = Map.of(
			StyleMood.AFTERDARK_MOVEMENT, "Afterdark / Movement",
			StyleMood.CREATIVE_PULSE, "Creative / Pulse",
			StyleMood.MODERN_ELEGANCE, "Modern / Elegance"
	);

	private static final Map<CityBackground, BackgroundProfile> BACKGROUND_PROFILES = Map.of(
			CityBackground.BERLIN_AFTER_DARK,
			new BackgroundProfile("Berlin After Dark", "berlin-after-dark"),
			CityBackground.TOKYO_QUIET_LINE,
			new BackgroundProfile("Tokyo Quiet Line", "tokyo-quiet-line"),
			CityBackground.SEOUL_PULSE,
			new BackgroundProfile("Seoul Pulse", "seoul-pulse"),
			CityBackground.MUNICH_HERITAGE,
			new BackgroundProfile("Munich Heritage", "munich-heritage"),
			CityBackground.NEW_YORK_GRAPHIC_CITY,
			new BackgroundProfile("New York Graphic City", "new-york-graphic-city"),
			CityBackground.HONG_KONG_NEON_HARBOR,
			new BackgroundProfile("Hong Kong Neon Harbor", "hong-kong-neon-harbor"),
			CityBackground.SHANGHAI_FUTURE_SKYLINE,
			new BackgroundProfile("Shanghai Future Skyline", "shanghai-future-skyline")
	);

	private PrototypeStyleCatalog() {
	}

	public static CityProfile cityProfile(CityCode cityCode) {
		return requireCurated(CITY_PROFILES, cityCode, "city code");
	}

	public static String productDisplayName(RecommendedProduct product) {
		return requireCurated(PRODUCT_DISPLAY_NAMES, product, "recommended product");
	}

	public static String moodDisplayName(StyleMood mood) {
		return requireCurated(MOOD_DISPLAY_NAMES, mood, "style mood");
	}

	public static String backgroundDisplayName(CityBackground background) {
		return requireCurated(BACKGROUND_PROFILES, background, "city background").displayName();
	}

	public static String backgroundAssetKey(CityBackground background) {
		return requireCurated(BACKGROUND_PROFILES, background, "city background").assetKey();
	}

	private static CityProfile profile(
			String displayName,
			RecommendedProduct product,
			StyleMood mood,
			CityBackground background,
			String description
	) {
		return new CityProfile(displayName, product, mood, background, description);
	}

	private static <K, V> V requireCurated(Map<K, V> catalog, K key, String valueName) {
		Objects.requireNonNull(key, valueName + " must not be null");
		V value = catalog.get(key);
		if (value == null) {
			throw new IllegalArgumentException(valueName + " is not part of the prototype catalog");
		}
		return value;
	}

	public record CityProfile(
			String displayName,
			RecommendedProduct defaultProduct,
			StyleMood defaultMood,
			CityBackground background,
			String defaultDescription
	) {
	}

	public record BackgroundProfile(String displayName, String assetKey) {
	}
}
