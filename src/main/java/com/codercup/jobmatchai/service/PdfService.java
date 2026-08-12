package com.codercup.jobmatchai.service;

import com.codercup.jobmatchai.exception.InvalidAnalysisRequestException;
import java.io.IOException;
import java.util.Locale;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PdfService {

	private static final long MAX_CV_FILE_SIZE_BYTES = 5L * 1024L * 1024L;
	private static final String PDF_CONTENT_TYPE = "application/pdf";

	public String extractText(MultipartFile file) {
		validatePdfFile(file);

		try (PDDocument document = Loader.loadPDF(file.getBytes())) {
			String text = new PDFTextStripper().getText(document);

			if (text == null || text.trim().isEmpty()) {
				throw new InvalidAnalysisRequestException(
						"No se pudo extraer texto del CV. Verifica que el PDF contenga texto seleccionable."
				);
			}

			return text;
		}
		catch (InvalidAnalysisRequestException exception) {
			throw exception;
		}
		catch (IOException exception) {
			throw new InvalidAnalysisRequestException("No se pudo leer el archivo PDF.");
		}
	}

	private void validatePdfFile(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new InvalidAnalysisRequestException("El archivo del CV no puede estar vacio.");
		}

		if (file.getSize() > MAX_CV_FILE_SIZE_BYTES) {
			throw new InvalidAnalysisRequestException("El archivo del CV no puede superar los 5 MB.");
		}

		String contentType = file.getContentType();
		String filename = file.getOriginalFilename();
		boolean hasContentType = contentType != null && !contentType.isBlank();
		boolean hasPdfContentType = hasContentType && PDF_CONTENT_TYPE.equalsIgnoreCase(contentType);
		boolean hasPdfExtension = filename != null && filename.toLowerCase(Locale.ROOT).endsWith(".pdf");

		if ((hasContentType && !hasPdfContentType) || !hasPdfExtension) {
			throw new InvalidAnalysisRequestException("El archivo del CV debe ser un PDF valido.");
		}
	}
}
