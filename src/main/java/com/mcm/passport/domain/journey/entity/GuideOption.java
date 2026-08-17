package com.mcm.passport.domain.journey.entity;

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

import java.util.Objects;

@Entity
@Table(
		name = "guide_option",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_guide_option_question_code",
				columnNames = {"guide_question_id", "code"}
		)
)
public class GuideOption {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "guide_question_id", nullable = false)
	private GuideQuestion guideQuestion;

	@Column(nullable = false, length = 100)
	private String code;

	@Column(nullable = false, length = 255)
	private String label;

	@Column(nullable = false)
	private int sequence;

	@Column(nullable = false)
	private boolean active;

	protected GuideOption() {
	}

	private GuideOption(GuideQuestion guideQuestion, String code, String label, int sequence) {
		this.guideQuestion = Objects.requireNonNull(guideQuestion, "guideQuestion must not be null");
		this.code = JourneySpot.requireText(code, "code");
		this.label = JourneySpot.requireText(label, "label");
		this.sequence = JourneySpot.requirePositive(sequence, "sequence");
		this.active = true;
	}

	public static GuideOption create(GuideQuestion guideQuestion, String code, String label, int sequence) {
		return new GuideOption(guideQuestion, code, label, sequence);
	}

	public Long getId() {
		return id;
	}

	public GuideQuestion getGuideQuestion() {
		return guideQuestion;
	}

	public String getCode() {
		return code;
	}

	public String getLabel() {
		return label;
	}

	public int getSequence() {
		return sequence;
	}

	public boolean isActive() {
		return active;
	}
}
