package com.mcm.passport.domain.style.catalog;

import com.mcm.passport.domain.style.entity.CityBackground;
import com.mcm.passport.domain.style.entity.CityCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PrototypeStyleCatalogTest {

	@Test
	void mapsSevenCityCodesToRequiredDisplayBackgrounds() {
		Map<CityCode, CityBackground> expectedMappings = Map.of(
				CityCode.BERLIN_AFTERDARK_NOMAD, CityBackground.BERLIN_AFTER_DARK,
				CityCode.TOKYO_QUIET_MINIMALIST, CityBackground.TOKYO_QUIET_LINE,
				CityCode.SEOUL_NEON_PLAYER, CityBackground.SEOUL_PULSE,
				CityCode.MUNICH_HERITAGE_TRAVELER, CityBackground.MUNICH_HERITAGE,
				CityCode.NEWYORK_GRAPHIC_MOVER, CityBackground.NEW_YORK_GRAPHIC_CITY,
				CityCode.HONG_KONG_NEON_NAVIGATOR, CityBackground.HONG_KONG_NEON_HARBOR,
				CityCode.SHANGHAI_FUTURE_VOYAGER, CityBackground.SHANGHAI_FUTURE_SKYLINE
		);

		assertThat(CityCode.values()).hasSize(7);
		assertThat(CityBackground.values()).hasSize(7);
		expectedMappings.forEach((cityCode, background) ->
				assertThat(PrototypeStyleCatalog.cityProfile(cityCode).background()).isEqualTo(background));
	}

	@Test
	void exposesRequiredDisplayBackgroundNames() {
		assertThat(CityBackground.BERLIN_AFTER_DARK.getDisplayName()).isEqualTo("Berlin After Dark");
		assertThat(CityBackground.TOKYO_QUIET_LINE.getDisplayName()).isEqualTo("Tokyo Quiet Line");
		assertThat(CityBackground.SEOUL_PULSE.getDisplayName()).isEqualTo("Seoul Pulse");
		assertThat(CityBackground.MUNICH_HERITAGE.getDisplayName()).isEqualTo("Munich Heritage");
		assertThat(CityBackground.NEW_YORK_GRAPHIC_CITY.getDisplayName()).isEqualTo("New York Graphic City");
		assertThat(CityBackground.HONG_KONG_NEON_HARBOR.getDisplayName()).isEqualTo("Hong Kong Neon Harbor");
		assertThat(CityBackground.SHANGHAI_FUTURE_SKYLINE.getDisplayName()).isEqualTo("Shanghai Future Skyline");
	}
}
