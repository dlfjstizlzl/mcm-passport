package com.mcm.passport.domain.style.entity;

import com.mcm.passport.domain.style.catalog.PrototypeStyleCatalog;

/** Prototype curated mood code persisted independently from the City Code. */
public enum StyleMood {
	AFTERDARK_MOVEMENT,
	CREATIVE_PULSE,
	MODERN_ELEGANCE;

	public String getDisplayName() {
		return PrototypeStyleCatalog.moodDisplayName(this);
	}
}
