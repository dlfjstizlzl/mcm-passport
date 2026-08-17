package com.mcm.passport.domain.style.entity;

import com.mcm.passport.domain.style.catalog.PrototypeStyleCatalog;

/** Prototype curated code persisted as a string in Style Result and Souvenir records. */
public enum CityCode {
	BERLIN_AFTERDARK_NOMAD,
	SEOUL_CREATIVE_MOVER,
	PARIS_MODERN_VOYAGER;

	public String getDisplayName() {
		return PrototypeStyleCatalog.cityProfile(this).displayName();
	}

	public RecommendedProduct getDefaultProduct() {
		return PrototypeStyleCatalog.cityProfile(this).defaultProduct();
	}

	public StyleMood getDefaultMood() {
		return PrototypeStyleCatalog.cityProfile(this).defaultMood();
	}

	public CityBackground getBackground() {
		return PrototypeStyleCatalog.cityProfile(this).background();
	}

	public String getDefaultDescription() {
		return PrototypeStyleCatalog.cityProfile(this).defaultDescription();
	}
}
