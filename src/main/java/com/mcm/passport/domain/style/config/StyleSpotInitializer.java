package com.mcm.passport.domain.style.config;

import com.mcm.passport.domain.style.entity.StyleSpot;
import com.mcm.passport.domain.style.repository.StyleSpotRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class StyleSpotInitializer implements ApplicationRunner {

	public static final String PROTOTYPE_STYLE_SPOT_CODE = "GATE-S1";

	private final StyleSpotRepository styleSpotRepository;

	public StyleSpotInitializer(StyleSpotRepository styleSpotRepository) {
		this.styleSpotRepository = styleSpotRepository;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!styleSpotRepository.existsById(PROTOTYPE_STYLE_SPOT_CODE)) {
			styleSpotRepository.save(StyleSpot.waiting(PROTOTYPE_STYLE_SPOT_CODE));
		}
	}
}
