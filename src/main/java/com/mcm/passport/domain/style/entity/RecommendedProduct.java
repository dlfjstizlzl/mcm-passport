package com.mcm.passport.domain.style.entity;

import com.mcm.passport.domain.style.catalog.PrototypeStyleCatalog;

/** Prototype curated product code; this is not the permanent MCM product catalog. */
public enum RecommendedProduct {
	STARK_BACKPACK,
	AREN_CROSSBODY,
	DIAMANT_3D_SHOULDER_BAG;

	public String getDisplayName() {
		return PrototypeStyleCatalog.productDisplayName(this);
	}
}
