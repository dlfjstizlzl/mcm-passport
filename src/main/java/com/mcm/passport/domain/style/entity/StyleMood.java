package com.mcm.passport.domain.style.entity;

public enum StyleMood {
	AFTERDARK_MOVEMENT("Afterdark / Movement"),
	CREATIVE_PULSE("Creative / Pulse"),
	MODERN_ELEGANCE("Modern / Elegance");

	private final String displayName;

	StyleMood(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
