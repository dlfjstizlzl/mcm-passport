package com.mcm.passport.domain.style.entity;

import com.mcm.passport.domain.style.catalog.PrototypeStyleCatalog;

/** Prototype curated background code mapped to a pre-generated MVP asset. */
public enum CityBackground {
	BERLIN_AFTER_DARK,
	TOKYO_QUIET_LINE,
	SEOUL_PULSE,
	MUNICH_HERITAGE,
	NEW_YORK_GRAPHIC_CITY,
	HONG_KONG_NEON_HARBOR,
	SHANGHAI_FUTURE_SKYLINE;

	public String getDisplayName() {
		return PrototypeStyleCatalog.backgroundDisplayName(this);
	}

	public String getAssetKey() {
		return PrototypeStyleCatalog.backgroundAssetKey(this);
	}
}
