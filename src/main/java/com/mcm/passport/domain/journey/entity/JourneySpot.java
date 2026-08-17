package com.mcm.passport.domain.journey.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
		name = "journey_spot",
		uniqueConstraints = @UniqueConstraint(name = "uk_journey_spot_code", columnNames = "code")
)
public class JourneySpot {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 50)
	private String code;

	@Column(nullable = false, length = 100)
	private String name;

	@Column(length = 500)
	private String description;

	@Column(nullable = false)
	private int sequence;

	@Column(nullable = false)
	private boolean required;

	@Column(nullable = false)
	private boolean active;

	@OneToMany(mappedBy = "journeySpot")
	@OrderBy("sequence ASC, id ASC")
	private List<GuideQuestion> questions = new ArrayList<>();

	protected JourneySpot() {
	}

	private JourneySpot(
			String code,
			String name,
			String description,
			int sequence,
			boolean required
	) {
		this.code = requireText(code, "code");
		this.name = requireText(name, "name");
		this.description = description;
		this.sequence = requirePositive(sequence, "sequence");
		this.required = required;
		this.active = true;
	}

	public static JourneySpot create(
			String code,
			String name,
			String description,
			int sequence,
			boolean required
	) {
		return new JourneySpot(code, name, description, sequence, required);
	}

	static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value;
	}

	static int requirePositive(int value, String fieldName) {
		if (value < 1) {
			throw new IllegalArgumentException(fieldName + " must be positive");
		}
		return value;
	}

	public Long getId() {
		return id;
	}

	public String getCode() {
		return code;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public int getSequence() {
		return sequence;
	}

	public boolean isRequired() {
		return required;
	}

	public boolean isActive() {
		return active;
	}

	public List<GuideQuestion> getQuestions() {
		return Collections.unmodifiableList(questions);
	}
}
