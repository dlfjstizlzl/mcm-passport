package com.mcm.passport.domain.journey.dto;

import com.mcm.passport.domain.journey.entity.GuideOption;
import com.mcm.passport.domain.journey.entity.GuideQuestion;
import com.mcm.passport.domain.journey.entity.JourneySpot;

import java.util.List;

public record JourneySpotDetailResponse(
		Long id,
		String code,
		String name,
		List<QuestionResponse> questions
) {

	public static JourneySpotDetailResponse of(JourneySpot journeySpot, List<QuestionResponse> questions) {
		return new JourneySpotDetailResponse(
				journeySpot.getId(),
				journeySpot.getCode(),
				journeySpot.getName(),
				List.copyOf(questions)
		);
	}

	public record QuestionResponse(
			Long id,
			String code,
			String questionText,
			boolean required,
			int sequence,
			List<OptionResponse> options
	) {

		public static QuestionResponse of(GuideQuestion question, List<OptionResponse> options) {
			return new QuestionResponse(
					question.getId(),
					question.getCode(),
					question.getQuestionText(),
					question.isRequired(),
					question.getSequence(),
					List.copyOf(options)
			);
		}
	}

	public record OptionResponse(Long id, String code, String label, int sequence) {

		public static OptionResponse from(GuideOption option) {
			return new OptionResponse(option.getId(), option.getCode(), option.getLabel(), option.getSequence());
		}
	}
}
