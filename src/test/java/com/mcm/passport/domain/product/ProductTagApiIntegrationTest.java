package com.mcm.passport.domain.product;

import com.mcm.passport.domain.passport.entity.PassportCard;
import com.mcm.passport.domain.passport.entity.PassportSession;
import com.mcm.passport.domain.passport.repository.PassportCardRepository;
import com.mcm.passport.domain.passport.repository.PassportSessionRepository;
import com.mcm.passport.domain.product.entity.Product;
import com.mcm.passport.domain.product.repository.ProductRepository;
import com.mcm.passport.domain.product.repository.ProductTagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"mcm.style.analysis.provider=mock",
		"spring.datasource.url=jdbc:h2:mem:product-tag-api;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
})
class ProductTagApiIntegrationTest {

	@Autowired
	private WebApplicationContext applicationContext;

	@Autowired
	private PassportCardRepository passportCardRepository;

	@Autowired
	private PassportSessionRepository passportSessionRepository;

	@Autowired
	private ProductRepository productRepository;

	@Autowired
	private ProductTagRepository productTagRepository;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(applicationContext).build();
		productTagRepository.deleteAllInBatch();
		passportSessionRepository.deleteAllInBatch();
		passportCardRepository.deleteAllInBatch();
		productRepository.deleteAllInBatch();
	}

	@Test
	void getsActiveProduct() throws Exception {
		Product product = saveProduct();

		mockMvc.perform(get("/api/products/{productId}", product.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(product.getId()))
				.andExpect(jsonPath("$.name").value("Stark Backpack"))
				.andExpect(jsonPath("$.category").value("BACKPACK"))
				.andExpect(jsonPath("$.description").value("MCM의 아이코닉한 Visetos 소재와 구조적인 실루엣이 돋보이는 블랙 백팩입니다."))
				.andExpect(jsonPath("$.color").value("BLACK"))
				.andExpect(jsonPath("$.material").value("VISETOS"))
				.andExpect(jsonPath("$.silhouette").value("STRUCTURED"))
				.andExpect(jsonPath("$.imageUrl").value("https://example.com/stark.jpg"))
				.andExpect(jsonPath("$.recommendable").value(true));
	}

	@Test
	void createsAndGetsProductTag() throws Exception {
		PassportSession session = saveExploringSession();
		Product product = saveProduct();

		mockMvc.perform(post(endpoint(), session.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"productId\":" + product.getId() + "}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.productTagId").isNumber())
				.andExpect(jsonPath("$.product.id").value(product.getId()))
				.andExpect(jsonPath("$.product.recommendable").doesNotExist())
				.andExpect(jsonPath("$.taggedAt").isNotEmpty());

		mockMvc.perform(get(endpoint(), session.getId()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(1))
				.andExpect(jsonPath("$[0].productId").value(product.getId()))
				.andExpect(jsonPath("$[0].name").value("Stark Backpack"))
				.andExpect(jsonPath("$[0].productTagId").doesNotExist())
				.andExpect(jsonPath("$[0].product").doesNotExist());
		assertThat(productTagRepository.count()).isEqualTo(1);
	}

	@Test
	void acceptsRepeatedTagWithoutCreatingDuplicateData() throws Exception {
		PassportSession session = saveExploringSession();
		Product product = saveProduct();
		String body = "{\"productId\":" + product.getId() + "}";

		mockMvc.perform(post(endpoint(), session.getId()).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated());
		mockMvc.perform(post(endpoint(), session.getId()).contentType(MediaType.APPLICATION_JSON).content(body))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.product.id").value(product.getId()));

		assertThat(productTagRepository.count()).isEqualTo(1);
	}

	@Test
	void rejectsTagOutsideExploringStatus() throws Exception {
		PassportSession session = saveReadyToBoardSession();
		Product product = saveProduct();

		mockMvc.perform(post(endpoint(), session.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"productId\":" + product.getId() + "}"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.code").value("INVALID_SESSION_STATUS"));
	}

	@Test
	void returnsNotFoundForUnknownSessionProductAndInactiveProduct() throws Exception {
		PassportSession session = saveExploringSession();
		Product inactiveProduct = saveProduct();
		inactiveProduct.deactivate();
		productRepository.saveAndFlush(inactiveProduct);

		mockMvc.perform(get(endpoint(), 999999L))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PASSPORT_SESSION_NOT_FOUND"));
		mockMvc.perform(get("/api/products/{productId}", inactiveProduct.getId()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
		mockMvc.perform(post(endpoint(), session.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"productId\":999999}"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
	}

	@Test
	void rejectsMissingProductId() throws Exception {
		PassportSession session = saveExploringSession();

		mockMvc.perform(post(endpoint(), session.getId())
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_INPUT"));
	}

	private Product saveProduct() {
		return productRepository.saveAndFlush(Product.create(
				"Stark Backpack",
				"BACKPACK",
				"MCM의 아이코닉한 Visetos 소재와 구조적인 실루엣이 돋보이는 블랙 백팩입니다.",
				"BLACK",
				"VISETOS",
				"STRUCTURED",
				"https://example.com/stark.jpg",
				true
		));
	}

	private PassportSession saveExploringSession() {
		return passportSessionRepository.saveAndFlush(PassportSession.start(saveCard()));
	}

	private PassportSession saveReadyToBoardSession() {
		return passportSessionRepository.saveAndFlush(PassportSession.readyToBoard(saveCard()));
	}

	private PassportCard saveCard() {
		return passportCardRepository.saveAndFlush(PassportCard.issue("TEST-" + UUID.randomUUID()));
	}

	private String endpoint() {
		return "/api/passport-sessions/{passportSessionId}/product-tags";
	}
}
