package com.codercup.jobmatchai.service;

import com.codercup.jobmatchai.dto.AnalysisResponse;
import com.codercup.jobmatchai.exception.AiServiceUnavailableException;
import com.codercup.jobmatchai.exception.AnalysisConfigurationException;
import com.codercup.jobmatchai.exception.InvalidAiResponseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GeminiService {

	private final ObjectMapper objectMapper;
	private final String apiKey;
	private final String model;

	public GeminiService(
			@Value("${gemini.api.key:}") String apiKey,
			@Value("${gemini.model}") String model
	) {
		this.objectMapper = new ObjectMapper();
		this.apiKey = apiKey;
		this.model = model;
	}

	public AnalysisResponse analyze(String cvText, String jobDescription) {
		if (apiKey == null || apiKey.isBlank()) {
			throw new AnalysisConfigurationException("El servicio de analisis no esta configurado correctamente.");
		}

		String responseText;
		try (Client client = Client.builder().apiKey(apiKey).build()) {
			GenerateContentResponse response = client.models.generateContent(
					model,
					buildPrompt(cvText, jobDescription),
					buildConfig()
			);
			responseText = response.text();
		}
		catch (ApiException | GenAiIOException exception) {
			throw new AiServiceUnavailableException(
					"El servicio de inteligencia artificial no esta disponible temporalmente.",
					exception
			);
		}

		return parseResponse(responseText);
	}

	private GenerateContentConfig buildConfig() {
		return GenerateContentConfig.builder()
				.responseMimeType("application/json")
				.responseSchema(buildResponseSchema())
				.temperature(0.2f)
				.build();
	}

	private Schema buildResponseSchema() {
		Schema stringArraySchema = Schema.builder()
				.type(Type.Known.ARRAY)
				.items(Schema.builder().type(Type.Known.STRING).build())
				.build();

		Map<String, Schema> properties = new LinkedHashMap<>();
		properties.put("matchPercentage", Schema.builder()
				.type(Type.Known.INTEGER)
				.minimum(0.0)
				.maximum(100.0)
				.build());
		properties.put("matchingSkills", stringArraySchema);
		properties.put("missingSkills", stringArraySchema);
		properties.put("recommendations", stringArraySchema);
		properties.put("interviewQuestions", stringArraySchema);

		return Schema.builder()
				.type(Type.Known.OBJECT)
				.properties(properties)
				.required(
						"matchPercentage",
						"matchingSkills",
						"missingSkills",
						"recommendations",
						"interviewQuestions"
				)
				.propertyOrdering(
						"matchPercentage",
						"matchingSkills",
						"missingSkills",
						"recommendations",
						"interviewQuestions"
				)
				.build();
	}

	private String buildPrompt(String cvText, String jobDescription) {
		return """
				Actua como un asistente de analisis profesional de postulaciones laborales.
				Compara unicamente la informacion proporcionada.

				Instrucciones obligatorias:
				- No inventes experiencia, conocimientos, titulos ni habilidades.
				- No asumas que el candidato conoce una tecnologia si no aparece en el CV.
				- Diferencia coincidencias y requisitos faltantes.
				- Evalua habilidades tecnicas y requisitos relevantes.
				- Da recomendaciones concretas y breves.
				- Crea posibles preguntas de entrevista relacionadas con la oferta.
				- El porcentaje debe ser orientativo y estar entre 0 y 100.
				- No rechaces automaticamente a una persona por requisitos faltantes.
				- No realices inferencias sobre edad, genero, raza, religion, nacionalidad, discapacidad,
				  orientacion sexual u otros atributos sensibles.
				- Analiza exclusivamente compatibilidad profesional.
				- Responde solo con JSON valido que cumpla el schema solicitado.

				CV DEL CANDIDATO:
				---
				%s
				---

				OFERTA LABORAL:
				---
				%s
				---
				""".formatted(cvText, jobDescription);
	}

	private AnalysisResponse parseResponse(String responseText) {
		if (responseText == null || responseText.isBlank()) {
			throw new InvalidAiResponseException("No se pudo interpretar la respuesta del servicio de analisis.");
		}

		try {
			AnalysisResponse response = objectMapper.readValue(responseText, AnalysisResponse.class);
			return validateAndNormalizeResponse(response);
		}
		catch (JsonProcessingException exception) {
			throw new InvalidAiResponseException(
					"No se pudo interpretar la respuesta del servicio de analisis.",
					exception
			);
		}
	}

	private AnalysisResponse validateAndNormalizeResponse(AnalysisResponse response) {
		if (response.matchPercentage() == null
				|| response.matchPercentage() < 0
				|| response.matchPercentage() > 100) {
			throw new InvalidAiResponseException("No se pudo interpretar la respuesta del servicio de analisis.");
		}

		return new AnalysisResponse(
				response.matchPercentage(),
				emptyIfNull(response.matchingSkills()),
				emptyIfNull(response.missingSkills()),
				emptyIfNull(response.recommendations()),
				emptyIfNull(response.interviewQuestions())
		);
	}

	private List<String> emptyIfNull(List<String> values) {
		if (values == null) {
			return List.of();
		}

		return List.copyOf(values);
	}
}
