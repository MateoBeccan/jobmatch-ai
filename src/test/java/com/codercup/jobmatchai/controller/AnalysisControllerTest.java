package com.codercup.jobmatchai.controller;

import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import com.codercup.jobmatchai.exception.ApiExceptionHandler;
import com.codercup.jobmatchai.service.AnalysisService;
import com.codercup.jobmatchai.service.PdfService;

@WebMvcTest(AnalysisController.class)
@Import({AnalysisService.class, PdfService.class, ApiExceptionHandler.class})
class AnalysisControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@BeforeAll
	static void configurePdfBoxFontCache() throws IOException {
		Path fontCacheDirectory = Path.of("target", "pdfbox-font-cache");
		Files.createDirectories(fontCacheDirectory);
		System.setProperty("pdfbox.fontcache", fontCacheDirectory.toAbsolutePath().toString());
	}

	@Test
	void analyzeReturnsOkForValidRequest() throws Exception {
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
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.matchPercentage").value(0))
				.andExpect(jsonPath("$.matchingSkills", empty()))
				.andExpect(jsonPath("$.missingSkills", empty()))
				.andExpect(jsonPath("$.recommendations", empty()))
				.andExpect(jsonPath("$.interviewQuestions", empty()));
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
	void analyzeReturnsBadRequestForBlankJobDescription() throws Exception {
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
				"   ".getBytes()
		);

		mockMvc.perform(multipart("/api/analyze")
						.file(cvFile)
						.file(jobDescription))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").value("La descripcion de la oferta laboral no puede estar vacia."));
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
}
