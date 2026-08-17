package com.mcm.passport.domain.journey.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
		name = "guide_question",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_guide_question_spot_code",
				columnNames = {"journey_spot_id", "code"}
		)
)
public class GuideQuestion {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "journey_spot_id", nullable = false)
	private JourneySpot journeySpot;

	@Column(nullable = false, length = 100)
	private String code;

	@Column(name = "question_text", nullable = false, length = 500)
	private String questionText;

	@Column(nullable = false)
	private boolean required;

	@Column(nullable = false)
	private int sequence;

	@Column(nullable = false)
	private boolean active;

	@OneToMany(mappedBy = "guideQuestion")
	@OrderBy("sequence ASC, id ASC")
	private List<GuideOption> options = new ArrayList<>();

	protected GuideQuestion() {
	}

	private GuideQuestion(
			JourneySpot journeySpot,
			String code,
			String questionText,
			boolean required,
			int sequence
	) {
		this.journeySpot = Objects.requireNonNull(journeySpot, "journeySpot must not be null");
		this.code = JourneySpot.requireText(code, "code");
		this.questionText = JourneySpot.requireText(questionText, "questionText");
		this.required = required;
		this.sequence = JourneySpot.requirePositive(sequence, "sequence");
		this.active = true;
	}

	public static GuideQuestion create(
			JourneySpot journeySpot,
			String code,
			String questionText,
			boolean required,
			int sequence
	) {
		return new GuideQuestion(journeySpot, code, questionText, required, sequence);
	}

	public Long getId() {
		return id;
	}

	public JourneySpot getJourneySpot() {
		return journeySpot;
	}

	public String getCode() {
		return code;
	}

	public String getQuestionText() {
		return questionText;
	}

	public boolean isRequired() {
		return required;
	}

	public int getSequence() {
		return sequence;
	}

	public boolean isActive() {
		return active;
	}

	public List<GuideOption> getOptions() {
		return Collections.unmodifiableList(options);
	}
}
