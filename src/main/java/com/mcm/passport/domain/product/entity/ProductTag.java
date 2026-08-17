package com.mcm.passport.domain.product.entity;

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
		name = "product_tag",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_product_tag_session_product",
				columnNames = {"passport_session_id", "product_id"}
		)
)
public class ProductTag {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "passport_session_id", nullable = false)
	private PassportSession passportSession;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;

	@Column(name = "tagged_at", nullable = false, updatable = false)
	private Instant taggedAt;

	protected ProductTag() {
	}

	private ProductTag(PassportSession passportSession, Product product) {
		this.passportSession = Objects.requireNonNull(passportSession, "passportSession must not be null");
		this.product = Objects.requireNonNull(product, "product must not be null");
		this.taggedAt = Instant.now();
	}

	public static ProductTag create(PassportSession passportSession, Product product) {
		return new ProductTag(passportSession, product);
	}

	public Long getId() {
		return id;
	}

	public PassportSession getPassportSession() {
		return passportSession;
	}

	public Product getProduct() {
		return product;
	}

	public Instant getTaggedAt() {
		return taggedAt;
	}
}
