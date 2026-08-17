package com.mcm.passport.domain.style.entity;

import com.mcm.passport.domain.style.catalog.PrototypeStyleCatalog;

/** Prototype curated background code mapped to a pre-generated MVP asset. */
public enum CityBackground {
	BERLIN_AFTERDARK,
	SEOUL_NEON_PULSE,
	PARIS_MODERN_CLASSIC;

	public String getAssetKey() {
		return PrototypeStyleCatalog.backgroundAssetKey(this);
	}
}
