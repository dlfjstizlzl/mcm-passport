package com.mcm.passport.domain.journey.service;

import com.mcm.passport.domain.journey.repository.JourneyResponseRepository;
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

	private final JourneyResponseRepository journeyResponseRepository;
	private final JourneyStampRepository journeyStampRepository;
	private final ProductTagRepository productTagRepository;

	public JpaJourneyDataReader(
			JourneyResponseRepository journeyResponseRepository,
			JourneyStampRepository journeyStampRepository,
			ProductTagRepository productTagRepository
	) {
		this.journeyResponseRepository = journeyResponseRepository;
		this.journeyStampRepository = journeyStampRepository;
		this.productTagRepository = productTagRepository;
	}

	@Override
	public JourneyDataSnapshot read(Long passportSessionId) {
		Objects.requireNonNull(passportSessionId, "passportSessionId must not be null");

		List<JourneyDataSnapshot.ResponseSignal> responses = journeyResponseRepository
				.findAllByPassportSession_IdOrderByIdAsc(passportSessionId)
				.stream()
				.map(response -> new JourneyDataSnapshot.ResponseSignal(
						response.getSpotCode(),
						response.getQuestionCode(),
						response.getAnswerCode(),
						response.getAnswerText()
				))
				.toList();

		List<JourneyDataSnapshot.StampSignal> stamps = journeyStampRepository
				.findAllByPassportSession_IdOrderByIdAsc(passportSessionId)
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
