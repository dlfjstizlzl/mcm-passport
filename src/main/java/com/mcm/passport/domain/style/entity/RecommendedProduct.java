package com.mcm.passport.domain.style.entity;

public enum RecommendedProduct {
	STARK_BACKPACK("Stark Backpack"),
	AREN_CROSSBODY("Aren Crossbody"),
	DIAMANT_3D_SHOULDER_BAG("Diamant 3D Shoulder Bag");

	private final String displayName;

	RecommendedProduct(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
}
