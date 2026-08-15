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
		name = "journey_responses",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_journey_response_session_spot_question",
				columnNames = {"passport_session_id", "spot_code", "question_code"}
		)
)
public class JourneyResponse {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "passport_session_id", nullable = false)
	private PassportSession passportSession;

	@Column(name = "spot_code", nullable = false, length = 64)
	private String spotCode;

	@Column(name = "question_code", nullable = false, length = 64)
	private String questionCode;

	@Column(name = "answer_code", nullable = false, length = 64)
	private String answerCode;

	@Column(name = "answer_text", nullable = false, length = 500)
	private String answerText;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected JourneyResponse() {
	}

	private JourneyResponse(
			PassportSession passportSession,
			String spotCode,
			String questionCode,
			String answerCode,
			String answerText
	) {
		this.passportSession = Objects.requireNonNull(passportSession, "passportSession must not be null");
		this.spotCode = requireText(spotCode, "spotCode");
		this.questionCode = requireText(questionCode, "questionCode");
		this.answerCode = requireText(answerCode, "answerCode");
		this.answerText = requireText(answerText, "answerText");
		this.createdAt = Instant.now();
	}

	public static JourneyResponse create(
			PassportSession passportSession,
			String spotCode,
			String questionCode,
			String answerCode,
			String answerText
	) {
		return new JourneyResponse(passportSession, spotCode, questionCode, answerCode, answerText);
	}

	private static String requireText(String value, String name) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(name + " must not be blank");
		}
		return value;
	}

	public Long getId() {
		return id;
	}

	public PassportSession getPassportSession() {
		return passportSession;
	}

	public String getSpotCode() {
		return spotCode;
	}

	public String getQuestionCode() {
		return questionCode;
	}

	public String getAnswerCode() {
		return answerCode;
	}

	public String getAnswerText() {
		return answerText;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}
}
