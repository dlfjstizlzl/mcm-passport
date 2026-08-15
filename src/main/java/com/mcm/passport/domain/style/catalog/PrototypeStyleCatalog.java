package com.mcm.passport.domain.style.catalog;

import com.mcm.passport.domain.style.entity.CityBackground;
import com.mcm.passport.domain.style.entity.CityCode;
import com.mcm.passport.domain.style.entity.RecommendedProduct;
import com.mcm.passport.domain.style.entity.StyleMood;

import java.util.Map;
import java.util.Objects;

/**
 * Prototype-only curated values and relationships used by the MVP.
 *
 * <p>Production catalog integration should replace this boundary while keeping the analysis
 * provider and persisted result contract stable.</p>
 */
public final class PrototypeStyleCatalog {

	private static final Map<CityCode, CityProfile> CITY_PROFILES = Map.of(
			CityCode.BERLIN_AFTERDARK_NOMAD,
			new CityProfile(
					"Berlin Afterdark Nomad",
					RecommendedProduct.STARK_BACKPACK,
					StyleMood.AFTERDARK_MOVEMENT,
					CityBackground.BERLIN_AFTERDARK,
					"오늘의 반응은 밤의 베를린처럼 자유로운 움직임과 선명한 존재감이 어우러지는 장면을 제안합니다."
			),
			CityCode.SEOUL_CREATIVE_MOVER,
			new CityProfile(
					"Seoul Creative Mover",
					RecommendedProduct.AREN_CROSSBODY,
					StyleMood.CREATIVE_PULSE,
					CityBackground.SEOUL_NEON_PULSE,
					"오늘의 반응은 빠르게 변하는 서울의 리듬처럼 실용성과 창의적인 포인트가 만나는 장면을 제안합니다."
			),
			CityCode.PARIS_MODERN_VOYAGER,
			new CityProfile(
					"Paris Modern Voyager",
					RecommendedProduct.DIAMANT_3D_SHOULDER_BAG,
					StyleMood.MODERN_ELEGANCE,
					CityBackground.PARIS_MODERN_CLASSIC,
					"오늘의 반응은 파리의 정제된 균형처럼 모던한 선과 우아한 디테일이 이어지는 장면을 제안합니다."
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

	private static final Map<CityBackground, String> BACKGROUND_ASSET_KEYS = Map.of(
			CityBackground.BERLIN_AFTERDARK, "berlin-afterdark",
			CityBackground.SEOUL_NEON_PULSE, "seoul-neon-pulse",
			CityBackground.PARIS_MODERN_CLASSIC, "paris-modern-classic"
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

	public static String backgroundAssetKey(CityBackground background) {
		return requireCurated(BACKGROUND_ASSET_KEYS, background, "city background");
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
}
