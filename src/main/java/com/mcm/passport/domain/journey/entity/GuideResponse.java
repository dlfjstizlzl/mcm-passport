package com.mcm.passport.domain.journey.entity;

import com.mcm.passport.domain.passport.entity.PassportSession;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
		name = "guide_response",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_guide_response_session_question",
				columnNames = {"passport_session_id", "guide_question_id"}
		)
)
public class GuideResponse {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "passport_session_id", nullable = false)
	private PassportSession passportSession;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "guide_question_id", nullable = false)
	private GuideQuestion guideQuestion;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "guide_option_id", nullable = false)
	private GuideOption guideOption;

	@Column(name = "answered_at", nullable = false)
	private Instant answeredAt;

	protected GuideResponse() {
	}

	private GuideResponse(
			PassportSession passportSession,
			GuideQuestion guideQuestion,
			GuideOption guideOption
	) {
		this.passportSession = Objects.requireNonNull(passportSession, "passportSession must not be null");
		this.guideQuestion = Objects.requireNonNull(guideQuestion, "guideQuestion must not be null");
		this.guideOption = Objects.requireNonNull(guideOption, "guideOption must not be null");
		this.answeredAt = Instant.now();
	}

	public static GuideResponse create(
			PassportSession passportSession,
			GuideQuestion guideQuestion,
			GuideOption guideOption
	) {
		return new GuideResponse(passportSession, guideQuestion, guideOption);
	}

	public void changeOption(GuideOption guideOption) {
		this.guideOption = Objects.requireNonNull(guideOption, "guideOption must not be null");
		this.answeredAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public PassportSession getPassportSession() {
		return passportSession;
	}

	public GuideQuestion getGuideQuestion() {
		return guideQuestion;
	}

	public GuideOption getGuideOption() {
		return guideOption;
	}

	public Instant getAnsweredAt() {
		return answeredAt;
	}
}
