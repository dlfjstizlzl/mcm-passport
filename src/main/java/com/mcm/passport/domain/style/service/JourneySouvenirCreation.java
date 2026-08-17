package com.mcm.passport.domain.style.service;

import com.mcm.passport.domain.style.dto.JourneySouvenirResponse;

public record JourneySouvenirCreation(
		JourneySouvenirResponse souvenir,
		boolean created
) {
}
