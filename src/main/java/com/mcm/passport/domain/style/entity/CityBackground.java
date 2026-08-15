package com.mcm.passport.domain.style.entity;

public enum CityBackground {
	BERLIN_AFTERDARK("berlin-afterdark"),
	SEOUL_NEON_PULSE("seoul-neon-pulse"),
	PARIS_MODERN_CLASSIC("paris-modern-classic");

	private final String assetKey;

	CityBackground(String assetKey) {
		this.assetKey = assetKey;
	}

	public String getAssetKey() {
		return assetKey;
	}
}
