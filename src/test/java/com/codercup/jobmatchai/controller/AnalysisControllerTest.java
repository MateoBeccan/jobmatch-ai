package com.codercup.jobmatchai.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import com.codercup.jobmatchai.dto.internal.GeminiAnalysisResult;
import com.codercup.jobmatchai.dto.internal.GeminiJobSearchProfile;
import com.codercup.jobmatchai.dto.JobSeniority;
import com.codercup.jobmatchai.exception.AiServiceUnavailableException;
import com.codercup.jobmatchai.exception.ApiExceptionHandler;
import com.codercup.jobmatchai.exception.InvalidAiResponseException;
import com.codercup.jobmatchai.scoring.MatchScoreCalculator;
import com.codercup.jobmatchai.scoring.RequirementAssessment;
import com.codercup.jobmatchai.scoring.RequirementCategory;
import com.codercup.jobmatchai.scoring.RequirementCriticality;
import com.codercup.jobmatchai.scoring.RequirementStatus;
import com.codercup.jobmatchai.service.AnalysisService;
import com.codercup.jobmatchai.service.AnalysisHistoryService;
import com.codercup.jobmatchai.service.AnalysisEvidenceValidator;
import com.codercup.jobmatchai.service.CvContentValidator;
import com.codercup.jobmatchai.service.GeminiService;
import com.codercup.jobmatchai.service.PdfService;
import com.codercup.jobmatchai.service.ProfessionalKnowledgeExtractor;

@WebMvcTest(value = AnalysisController.class, properties = "rate-limit.per-minute=100")
@Import({
		AnalysisService.class,
		AnalysisEvidenceValidator.class,
		PdfService.class,
		ProfessionalKnowledgeExtractor.class,
		MatchScoreCalculator.class,
		AnalysisControllerTest.TestHistoryConfiguration.class,
		ApiExceptionHandler.class,
		AnalysisControllerTest.TestGeminiConfiguration.class
})
class AnalysisControllerTest {

	private static boolean simulateGeminiUnavailable;
	private static boolean simulateImageGeminiUnavailable;
	private static boolean simulateUnexpectedError;
	private static boolean simulateCriticalExperienceGap;

	@Autowired
	private MockMvc mockMvc;

	@BeforeAll
	static void configurePdfBoxFontCache() throws IOException {
		Path fontCacheDirectory = Path.of("target", "pdfbox-font-cache");
		Files.createDirectories(fontCacheDirectory);
		System.setProperty("pdfbox.fontcache", fontCacheDirectory.toAbsolutePath().toString());
	}

	@BeforeEach
	void resetGeminiFake() {
		simulateGeminiUnavailable = false;
		simulateImageGeminiUnavailable = false;
		simulateUnexpectedError = false;
		simulateCriticalExperienceGap = false;
	}

	@Test
	void analyzeReturnsOkForValidRequest() throws Exception {
		MockMultipartFile cvFile = new MockMultipartFile(
				"cvFile",
				"cv.pdf",
				"application/pdf",
				createPdfWithText("Java developer with Spring Boot, SQL and REST APIs experience")
		);
		MockMultipartFile jobDescription = new MockMultipartFile(
				"jobDescription",
				"",
				"text/plain",
				"Java developer role requiring Java, Spring Boot and Docker".getBytes()
		);

		mockMvc.perform(multipart("/api/analyze")
						.file(cvFile)
						.file(jobDescription))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.matchPercentage").value(67))
				.andExpect(jsonPath("$.matchingSkills[0]").value("Java"))
				.andExpect(jsonPath("$.matchingSkills[1]").value("Spring Boot"))
				.andExpect(jsonPath("$.missingSkills[0]").value("Docker"))
				.andExpect(jsonPath("$.recommendations[0]").value("Aprender fundamentos de Docker"))
				.andExpect(jsonPath("$.interviewQuestions[0]").value("Como crearias una API REST?"))
				.andExpect(jsonPath("$.requirements").isArray())
				.andExpect(jsonPath("$.requirements.length()").value(3))
				.andExpect(jsonPath("$.requirements[0].name").value("Java"))
				.andExpect(jsonPath("$.requirements[0].status").value("match"))
				.andExpect(jsonPath("$.requirements[2].name").value("Docker"))
				.andExpect(jsonPath("$.requirements[2].status").value("missing"))
				.andExpect(jsonPath("$.breakdown.mandatoryTechnical").value(67))
				.andExpect(jsonPath("$.breakdown.experienceSeniority").doesNotExist())
				.andExpect(jsonPath("$.criticalMissingRequirements").isArray())
				.andExpect(jsonPath("$.criticalMissingRequirements.length()").value(0))
				.andExpect(jsonPath("$.experienceGap").doesNotExist())
				.andExpect(jsonPath("$.warnings").isArray())
				.andExpect(jsonPath("$.warnings.length()").value(0))
				.andExpect(jsonPath("$.jobSearchProfile.role").value("Java Backend Developer"))
				.andExpect(jsonPath("$.jobSearchProfile.seniority").value("JUNIOR"))
				.andExpect(jsonPath("$.jobSearchProfile.keywords[0]").value("Java"))
				.andExpect(jsonPath("$.jobSearchProfile.keywords[1]").value("Spring Boot"))
				.andExpect(jsonPath("$.jobSearchProfile.keywords[2]").value("SQL"))
				.andExpect(jsonPath("$.jobSearchProfile.keywords[3]").value("REST APIs"));
	}

	@Test
	void analyzeReturnsExplanationFieldsForCriticalExperienceGap() throws Exception {
		simulateCriticalExperienceGap = true;
		MockMultipartFile cvFile = new MockMultipartFile(
				"cvFile",
				"cv.pdf",
				"application/pdf",
				createPdfWithText("Java developer with Spring Boot, SQL, REST APIs and Git experience")
		);
		MockMultipartFile jobDescription = new MockMultipartFile(
				"jobDescription",
				"",
				"text/plain",
				"Senior Java developer role".getBytes()
		);

		mockMvc.perform(multipart("/api/analyze")
						.file(cvFile)
						.file(jobDescription))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.matchPercentage").value(69))
				.andExpect(jsonPath("$.criticalMissingRequirements.length()").value(1))
				.andExpect(jsonPath("$.criticalMissingRequirements[0].requirement").value("5+ anos de experiencia profesional"))
				.andExpect(jsonPath("$.criticalMissingRequirements[0].category").value("experience_seniority"))
				.andExpect(jsonPath("$.experienceGap.requirement").value("5+ anos de experiencia profesional"))
				.andExpect(jsonPath("$.experienceGap.status").value("missing"))
				.andExpect(jsonPath("$.experienceGap.critical").value(true))
				.andExpect(jsonPath("$.warnings[0]").value("Falta 1 requisito critico de la oferta."))
				.andExpect(jsonPath("$.warnings[1]").value(
						"La experiencia profesional requerida no esta completamente respaldada por el CV."
				))
				.andExpect(jsonPath("$.warnings[2]").value(
						"El score esta limitado por requisitos criticos no cumplidos."
				));
	}

	@Test
	void analyzeReturnsBadRequestForEmptyCvFile() throws Exception {
		MockMultipartFile cvFile = new MockMultipartFile(
				"cvFile",
				"cv.pdf",
				"application/pdf",
				new byte[0]
		);
		MockMultipartFile jobDescription = new MockMultipartFile(
				"jobDescription",
				"",
				"text/plain",
				"Java developer role".getBytes()
		);

		mockMvc.perform(multipart("/api/analyze")
						.file(cvFile)
						.file(jobDescription))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("El archivo del CV no puede estar vacio."));
	}

	@Test
	void analyzeReturnsBadRequestForCvFileLargerThanFiveMb() throws Exception {
		MockMultipartFile cvFile = new MockMultipartFile(
				"cvFile",
				"cv.pdf",
				"application/pdf",
				new byte[(5 * 1024 * 1024) + 1]
		);
		MockMultipartFile jobDescription = new MockMultipartFile(
				"jobDescription",
				"",
				"text/plain",
				"Java developer role".getBytes()
		);

		mockMvc.perform(multipart("/api/analyze")
						.file(cvFile)
						.file(jobDescription))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("El archivo del CV no puede superar los 5 MB."));
	}

	@Test
	void analyzeReturnsBadRequestForJobDescriptionLongerThanLimit() throws Exception {
		MockMultipartFile cvFile = new MockMultipartFile(
				"cvFile",
				"cv.pdf",
				"application/pdf",
				"fake pdf content".getBytes()
		);
		MockMultipartFile jobDescription = new MockMultipartFile(
				"jobDescription",
				"",
				"text/plain",
				"a".repeat(5001).getBytes()
		);

		mockMvc.perform(multipart("/api/analyze")
						.file(cvFile)
						.file(jobDescription))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(
						"La descripcion de la oferta no puede superar los 5000 caracteres."
				));
	}

	@Test
	void analyzeReturnsBadRequestForBlankJobDescription() throws Exception {
		MockMultipartFile cvFile = new MockMultipartFile(
				"cvFile",
				"cv.pdf",
				"application/pdf",
				createPdfWithText("Java developer with Spring Boot, SQL, REST APIs and Git experience")
		);
		MockMultipartFile jobDescription = new MockMultipartFile(
				"jobDescription",
				"",
				"text/plain",
				"   ".getBytes()
		);

		mockMvc.perform(multipart("/api/analyze")
				.file(cvFile)
						.file(jobDescription))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Debes proporcionar la oferta laboral como texto o imagen."));
	}

	@Test
	void analyzeReturnsBadRequestForNonPdfFile() throws Exception {
		MockMultipartFile cvFile = new MockMultipartFile(
				"cvFile",
				"cv.txt",
				"text/plain",
				"plain text cv".getBytes()
		);
		MockMultipartFile jobDescription = new MockMultipartFile(
				"jobDescription",
				"",
				"text/plain",
				"Java developer role".getBytes()
		);

		mockMvc.perform(multipart("/api/analyze")
						.file(cvFile)
						.file(jobDescription))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("El archivo del CV debe ser un PDF valido."));
	}

	@Test
	void analyzeReturnsBadRequestForPdfWithoutText() throws Exception {
		MockMultipartFile cvFile = new MockMultipartFile(
				"cvFile",
				"cv.pdf",
				"application/pdf",
				createBlankPdf()
		);
		MockMultipartFile jobDescription = new MockMultipartFile(
				"jobDescription",
				"",
				"text/plain",
				"Java developer role".getBytes()
		);

		mockMvc.perform(multipart("/api/analyze")
						.file(cvFile)
						.file(jobDescription))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value(
						"No se pudo extraer texto del CV. Verifica que el PDF contenga texto seleccionable."
				));
	}

	@Test
	void analyzeReturnsBadRequestForCorruptPdf() throws Exception {
		MockMultipartFile cvFile = new MockMultipartFile(
				"cvFile",
				"cv.pdf",
				"application/pdf",
				"not a real pdf".getBytes()
		);
		MockMultipartFile jobDescription = new MockMultipartFile(
				"jobDescription",
				"",
				"text/plain",
				"Java developer role".getBytes()
		);

		mockMvc.perform(multipart("/api/analyze")
						.file(cvFile)
						.file(jobDescription))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("No se pudo leer el archivo PDF."));
	}

	@Test
	void analyzeReturnsServiceUnavailableWhenGeminiFails() throws Exception {
		simulateGeminiUnavailable = true;
		MockMultipartFile cvFile = new MockMultipartFile(
				"cvFile",
				"cv.pdf",
				"application/pdf",
				createPdfWithText("Java developer with Spring Boot experience")
		);
		MockMultipartFile jobDescription = new MockMultipartFile(
				"jobDescription",
				"",
				"text/plain",
				"Java developer role".getBytes()
		);

		mockMvc.perform(multipart("/api/analyze")
						.file(cvFile)
						.file(jobDescription))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.message").value(
						"El servicio de inteligencia artificial no esta disponible temporalmente."
				));
	}

	@Test
	void analyzeReturnsInternalServerErrorForUnexpectedError() throws Exception {
		simulateUnexpectedError = true;
		MockMultipartFile cvFile = new MockMultipartFile(
				"cvFile",
				"cv.pdf",
				"application/pdf",
				createPdfWithText("Java developer with Spring Boot experience")
		);
		MockMultipartFile jobDescription = new MockMultipartFile(
				"jobDescription",
				"",
				"text/plain",
				"Java developer role".getBytes()
		);

		mockMvc.perform(multipart("/api/analyze")
						.file(cvFile)
						.file(jobDescription))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.message").value("Ocurrio un error interno al procesar la solicitud."))
				.andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.not(
						org.hamcrest.Matchers.containsString("sensitive internal detail")
				)));
	}

	@Test
	void analyzeReturnsOkForValidImageRequest() throws Exception {
		MockMultipartFile cvFile = new MockMultipartFile(
				"cvFile",
				"cv.pdf",
				"application/pdf",
				createPdfWithText("Java developer with Spring Boot, SQL, REST APIs and Git experience")
		);
		MockMultipartFile jobImage = new MockMultipartFile(
				"jobImage",
				"job.png",
				"image/png",
				createPng()
		);

		mockMvc.perform(multipart("/api/analyze")
						.file(cvFile)
						.file(jobImage))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.matchPercentage").value(71))
				.andExpect(jsonPath("$.matchingSkills[0]").value("Java"))
				.andExpect(jsonPath("$.matchingSkills[1]").value("Spring Boot"))
				.andExpect(jsonPath("$.missingSkills[0]").value("Docker"))
				.andExpect(jsonPath("$.recommendations[0]").value("Destacar proyectos realizados con Spring Boot"))
				.andExpect(jsonPath("$.interviewQuestions[0]").value("Como disenarias una API REST con Spring Boot?"))
				.andExpect(jsonPath("$.jobSearchProfile.role").value("Java Backend Developer"))
				.andExpect(jsonPath("$.jobSearchProfile.seniority").value("JUNIOR"))
				.andExpect(jsonPath("$.jobSearchProfile.keywords[2]").value("SQL"));
	}

	@Test
	void analyzeReturnsBadRequestWhenOfferIsMissing() throws Exception {
		MockMultipartFile cvFile = new MockMultipartFile(
				"cvFile",
				"cv.pdf",
				"application/pdf",
				createPdfWithText("Java developer with Spring Boot experience")
		);

		mockMvc.perform(multipart("/api/analyze")
						.file(cvFile))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Debes proporcionar la oferta laboral como texto o imagen."));
	}

	@Test
	void analyzeReturnsBadRequestWhenTextAndImageAreProvided() throws Exception {
		MockMultipartFile cvFile = new MockMultipartFile(
				"cvFile",
				"cv.pdf",
				"application/pdf",
				createPdfWithText("Java developer with Spring Boot experience")
		);
		MockMultipartFile jobDescription = new MockMultipartFile(
				"jobDescription",
				"",
				"text/plain",
				"Java developer role".getBytes()
		);
		MockMultipartFile jobImage = new MockMultipartFile(
				"jobImage",
				"job.png",
				"image/png",
				createPng()
		);

		mockMvc.perform(multipart("/api/analyze")
						.file(cvFile)
						.file(jobDescription)
						.file(jobImage))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("Proporciona la oferta laboral como texto o imagen, no ambas."));
	}

	@Test
	void analyzeReturnsBadRequestForEmptyJobImage() throws Exception {
		MockMultipartFile cvFile = new MockMultipartFile(
				"cvFile",
				"cv.pdf",
				"application/pdf",
				createPdfWithText("Java developer with Spring Boot experience")
		);
		MockMultipartFile jobImage = new MockMultipartFile(
				"jobImage",
				"job.png",
				"image/png",
				new byte[0]
		);

		mockMvc.perform(multipart("/api/analyze")
						.file(cvFile)
						.file(jobImage))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("La imagen de la oferta esta vacia."));
	}

	@Test
	void analyzeReturnsBadRequestForUnsupportedJobImageFormat() throws Exception {
		MockMultipartFile cvFile = new MockMultipartFile(
				"cvFile",
				"cv.pdf",
				"application/pdf",
				createPdfWithText("Java developer with Spring Boot experience")
		);
		MockMultipartFile jobImage = new MockMultipartFile(
				"jobImage",
				"job.txt",
				"text/plain",
				"not an image".getBytes()
		);

		mockMvc.perform(multipart("/api/analyze")
						.file(cvFile)
						.file(jobImage))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("La imagen de la oferta debe ser PNG, JPEG o WEBP."));
	}

	@Test
	void analyzeReturnsBadRequestForJobImageLargerThanFiveMb() throws Exception {
		MockMultipartFile cvFile = new MockMultipartFile(
				"cvFile",
				"cv.pdf",
				"application/pdf",
				createPdfWithText("Java developer with Spring Boot experience")
		);
		MockMultipartFile jobImage = new MockMultipartFile(
				"jobImage",
				"job.png",
				"image/png",
				new byte[(5 * 1024 * 1024) + 1]
		);

		mockMvc.perform(multipart("/api/analyze")
						.file(cvFile)
						.file(jobImage))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("La imagen de la oferta no puede superar los 5 MB."));
	}

	@Test
	void analyzeReturnsServiceUnavailableWhenGeminiFailsForImageRequest() throws Exception {
		simulateImageGeminiUnavailable = true;
		MockMultipartFile cvFile = new MockMultipartFile(
				"cvFile",
				"cv.pdf",
				"application/pdf",
				createPdfWithText("Java developer with Spring Boot experience")
		);
		MockMultipartFile jobImage = new MockMultipartFile(
				"jobImage",
				"job.png",
				"image/png",
				createPng()
		);

		mockMvc.perform(multipart("/api/analyze")
						.file(cvFile)
						.file(jobImage))
				.andExpect(status().isServiceUnavailable())
				.andExpect(jsonPath("$.message").value(
						"El servicio de inteligencia artificial no esta disponible temporalmente."
				));
	}

	private byte[] createPdfWithText(String text) {
		String content = "BT /F1 12 Tf 50 700 Td (" + escapePdfText(text) + ") Tj ET\n";
		String[] objects = {
				"<< /Type /Catalog /Pages 2 0 R >>",
				"<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
				"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] "
						+ "/Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>",
				"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
				"<< /Length " + content.getBytes(StandardCharsets.US_ASCII).length + " >>\nstream\n"
						+ content
						+ "endstream"
		};

		StringBuilder pdf = new StringBuilder("%PDF-1.4\n");
		int[] offsets = new int[objects.length + 1];

		for (int index = 0; index < objects.length; index++) {
			offsets[index + 1] = pdf.toString().getBytes(StandardCharsets.US_ASCII).length;
			pdf.append(index + 1).append(" 0 obj\n")
					.append(objects[index]).append("\n")
					.append("endobj\n");
		}

		int xrefOffset = pdf.toString().getBytes(StandardCharsets.US_ASCII).length;
		pdf.append("xref\n")
				.append("0 ").append(objects.length + 1).append("\n")
				.append("0000000000 65535 f \n");

		for (int index = 1; index < offsets.length; index++) {
			pdf.append(String.format("%010d 00000 n \n", offsets[index]));
		}

		pdf.append("trailer\n")
				.append("<< /Size ").append(objects.length + 1).append(" /Root 1 0 R >>\n")
				.append("startxref\n")
				.append(xrefOffset).append("\n")
				.append("%%EOF\n");

		return pdf.toString().getBytes(StandardCharsets.US_ASCII);
	}

	private byte[] createPng() throws IOException {
		BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
		try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			ImageIO.write(image, "png", outputStream);
			return outputStream.toByteArray();
		}
	}

	private String escapePdfText(String text) {
		return text.replace("\\", "\\\\")
				.replace("(", "\\(")
				.replace(")", "\\)");
	}

	private byte[] createBlankPdf() throws Exception {
		try (PDDocument document = new PDDocument(); ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
			document.addPage(new PDPage());
			document.save(outputStream);
			return outputStream.toByteArray();
		}
	}

	@TestConfiguration
	static class TestGeminiConfiguration {

		@Bean
		GeminiService geminiService() {
			return new GeminiService("test-key", "test-model", 30000, 2, 500) {
				@Override
				public GeminiAnalysisResult analyze(String cvText, String jobDescription) {
					if (simulateUnexpectedError) {
						throw new IllegalStateException("sensitive internal detail");
					}

					if (simulateGeminiUnavailable) {
						throw new AiServiceUnavailableException(
								"El servicio de inteligencia artificial no esta disponible temporalmente.",
								new RuntimeException("Simulated Gemini error")
						);
					}

					if (cvText == null || !cvText.contains("Spring Boot")) {
						throw new InvalidAiResponseException("No se pudo interpretar la respuesta del servicio de analisis.");
					}

					if (simulateCriticalExperienceGap) {
						return new GeminiAnalysisResult(
								List.of(
										assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
										assessment("Spring Boot", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
										assessment(
												"5+ anos de experiencia profesional",
												RequirementCategory.EXPERIENCE_SENIORITY,
												RequirementCriticality.CRITICAL,
												RequirementStatus.MISSING,
												"El CV no demuestra 5+ anos de experiencia profesional."
										)
								),
								List.of("Java", "Spring Boot"),
								List.of(),
								List.of("Preparar experiencia senior"),
								List.of("Como validarias experiencia profesional?"),
								jobSearchProfile()
						);
					}

					return new GeminiAnalysisResult(
							List.of(
									assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
									assessment("Spring Boot", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
									assessment("Docker", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.PARTIAL)
							),
							List.of("Java", "Spring Boot"),
							List.of("Docker"),
							List.of("Aprender fundamentos de Docker"),
							List.of("Como crearias una API REST?"),
							jobSearchProfile()
					);
				}

				@Override
				public GeminiAnalysisResult analyze(
						String cvText,
						String jobDescription,
						List<String> cvKnowledgeHints,
						List<String> jobKnowledgeHints
				) {
					return analyze(cvText, jobDescription);
				}

				@Override
				public GeminiAnalysisResult analyze(String cvText, MultipartFile jobImage) {
					if (simulateImageGeminiUnavailable) {
						throw new AiServiceUnavailableException(
								"El servicio de inteligencia artificial no esta disponible temporalmente.",
								new RuntimeException("Simulated Gemini image error")
						);
					}

					if (cvText == null || !cvText.contains("Spring Boot") || jobImage == null || jobImage.isEmpty()) {
						throw new InvalidAiResponseException("No se pudo interpretar la imagen de la oferta laboral.");
					}

					return new GeminiAnalysisResult(
							List.of(
									assessment("Java", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
									assessment("Spring Boot", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.MATCH),
									assessment("Docker", RequirementCategory.MANDATORY_TECHNICAL, RequirementStatus.PARTIAL),
									assessment("Git", RequirementCategory.COMPLEMENTARY, RequirementStatus.MATCH)
							),
							List.of("Java", "Spring Boot"),
							List.of("Docker"),
							List.of("Destacar proyectos realizados con Spring Boot"),
							List.of("Como disenarias una API REST con Spring Boot?"),
							jobSearchProfile()
					);
				}

				@Override
				public GeminiAnalysisResult analyze(String cvText, MultipartFile jobImage, List<String> cvKnowledgeHints) {
					return analyze(cvText, jobImage);
				}

				private RequirementAssessment assessment(
						String name,
						RequirementCategory category,
						RequirementStatus status
				) {
					return new RequirementAssessment(name, category, status, "Evidencia de test");
				}

				private RequirementAssessment assessment(
						String name,
						RequirementCategory category,
						RequirementCriticality criticality,
						RequirementStatus status,
						String evidence
				) {
					return new RequirementAssessment(name, category, criticality, status, evidence);
				}

				private GeminiJobSearchProfile jobSearchProfile() {
					return new GeminiJobSearchProfile(
							"Java Backend Developer",
							JobSeniority.JUNIOR,
							List.of("Java", "Spring Boot", "SQL", "REST API")
					);
				}
			};
		}

		@Bean
		CvContentValidator cvContentValidator() {
			return new CvContentValidator() {
				@Override
				public void validate(String cvText) {
				}
			};
		}
	}

	@TestConfiguration
	static class TestHistoryConfiguration {

		@Bean
		AnalysisHistoryService analysisHistoryService() {
			return org.mockito.Mockito.mock(AnalysisHistoryService.class);
		}
	}
}
