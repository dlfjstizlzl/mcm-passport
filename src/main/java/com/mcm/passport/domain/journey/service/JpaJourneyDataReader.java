package com.mcm.passport.domain.journey.service;

import com.mcm.passport.domain.journey.repository.GuideResponseRepository;
import com.mcm.passport.domain.journey.repository.JourneyStampRepository;
import com.mcm.passport.domain.product.entity.Product;
import com.mcm.passport.domain.product.repository.ProductTagRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class JpaJourneyDataReader implements JourneyDataReader {

	private final GuideResponseRepository guideResponseRepository;
	private final JourneyStampRepository journeyStampRepository;
	private final ProductTagRepository productTagRepository;

	public JpaJourneyDataReader(
			GuideResponseRepository guideResponseRepository,
			JourneyStampRepository journeyStampRepository,
			ProductTagRepository productTagRepository
	) {
		this.guideResponseRepository = guideResponseRepository;
		this.journeyStampRepository = journeyStampRepository;
		this.productTagRepository = productTagRepository;
	}

	@Override
	public JourneyDataSnapshot read(Long passportSessionId) {
		Objects.requireNonNull(passportSessionId, "passportSessionId must not be null");

		List<JourneyDataSnapshot.ResponseSignal> responses = guideResponseRepository
				.findAllForStyleAnalysis(passportSessionId)
				.stream()
				.map(response -> new JourneyDataSnapshot.ResponseSignal(
						response.getGuideQuestion().getJourneySpot().getCode(),
						response.getGuideQuestion().getCode(),
						response.getGuideOption().getCode(),
						response.getAnswerText() == null
								? response.getGuideOption().getLabel()
								: response.getGuideOption().getLabel() + " / 직접 답변: " + response.getAnswerText()
				))
				.toList();

		List<JourneyDataSnapshot.StampSignal> stamps = journeyStampRepository
				.findAllByPassportSession_IdOrderByJourneySpot_SequenceAscIdAsc(passportSessionId)
				.stream()
				.map(stamp -> new JourneyDataSnapshot.StampSignal(stamp.getJourneySpot().getCode()))
				.toList();

		List<JourneyDataSnapshot.ProductSignal> taggedProducts = productTagRepository
				.findAllByPassportSession_IdOrderByIdAsc(passportSessionId)
				.stream()
				.map(tag -> toProductSignal(tag.getProduct()))
				.toList();

		return new JourneyDataSnapshot(passportSessionId, responses, stamps, taggedProducts);
	}

	private JourneyDataSnapshot.ProductSignal toProductSignal(Product product) {
		return new JourneyDataSnapshot.ProductSignal(product.getId(), product.getCode(), product.getName());
	}
}
