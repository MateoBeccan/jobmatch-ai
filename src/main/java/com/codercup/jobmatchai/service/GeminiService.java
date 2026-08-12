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
		Schema recommendationsSchema = Schema.builder()
				.type(Type.Known.ARRAY)
				.items(Schema.builder().type(Type.Known.STRING).build())
				.minItems(2L)
				.maxItems(4L)
				.build();
		Schema interviewQuestionsSchema = Schema.builder()
				.type(Type.Known.ARRAY)
				.items(Schema.builder().type(Type.Known.STRING).build())
				.minItems(3L)
				.maxItems(5L)
				.build();

		Map<String, Schema> properties = new LinkedHashMap<>();
		properties.put("matchPercentage", Schema.builder()
				.type(Type.Known.INTEGER)
				.minimum(0.0)
				.maximum(100.0)
				.build());
		properties.put("matchingSkills", stringArraySchema);
		properties.put("missingSkills", stringArraySchema);
		properties.put("recommendations", recommendationsSchema);
		properties.put("interviewQuestions", interviewQuestionsSchema);

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

				Antes de responder, analiza internamente la oferta y separa:
				- requisitos obligatorios;
				- requisitos deseables;
				- experiencia y seniority requerido;
				- habilidades tecnicas principales;
				- conocimientos complementarios.
				No devuelvas estas categorias en el JSON; usalas solo para mejorar la evaluacion.

				Criterio orientativo para matchPercentage:
				- Requisitos obligatorios y habilidades tecnicas principales: 60%%.
				- Experiencia y seniority requerido: 20%%.
				- Requisitos deseables: 10%%.
				- Habilidades complementarias relevantes: 10%%.
				No apliques una formula exacta si la oferta no permite hacerlo, pero respeta esta ponderacion como guia.
				Si el candidato cumple tecnologias obligatorias como Java, Spring Boot, SQL o REST APIs, eso debe pesar
				mucho mas que no conocer una herramienta deseable. Si la oferta exige seniority o anos de experiencia
				y el CV no demuestra ese nivel, reflejalo de forma significativa en el porcentaje.

				Interpretacion aproximada de matchPercentage:
				- 80 a 100: compatibilidad alta.
				- 60 a 79: compatibilidad buena.
				- 40 a 59: compatibilidad media.
				- 20 a 39: compatibilidad baja.
				- 0 a 19: compatibilidad muy baja.
				No fuerces artificialmente el resultado dentro de una banda.

				Instrucciones obligatorias:
				- No inventes experiencia, conocimientos, titulos ni habilidades.
				- No asumas que el candidato conoce una tecnologia si no aparece en el CV.
				- Diferencia coincidencias y requisitos faltantes.
				- Evalua habilidades tecnicas y requisitos relevantes.
				- El porcentaje debe ser orientativo y estar entre 0 y 100.
				- No rechaces automaticamente a una persona por requisitos faltantes.
				- No realices inferencias sobre edad, genero, raza, religion, nacionalidad, discapacidad,
				  orientacion sexual u otros atributos sensibles.
				- Analiza exclusivamente compatibilidad profesional.
				- Responde solo con JSON valido que cumpla el schema solicitado.

				matchingSkills:
				- Inclui habilidades o requisitos concretos presentes tanto en el CV como en la oferta.
				- Priorizá tecnologias y practicas especificas como Java, Spring Boot, SQL, MySQL, REST APIs, Git o Scrum.
				- Evita elementos demasiado genericos como "Tecnologia", "Informatica" o "Desarrollo de Software",
				  salvo que sean realmente relevantes para la oferta.
				- No inventes equivalencias fuertes: Java no implica Node.js, y MySQL no implica PostgreSQL si
				  PostgreSQL es un requisito especifico.

				missingSkills:
				- Inclui solo requisitos relevantes que esten en la oferta y no esten demostrados por el CV.
				- No agregues tecnologias que la oferta no menciona.
				- Si la oferta exige experiencia temporal, conserva esa informacion cuando sea relevante.
				  Ejemplo: "Experiencia requerida: 4 anos; el CV no demuestra ese nivel de experiencia."
				- Mantené textos breves.

				Experiencia vs conocimiento:
				- Una tecnologia mencionada en skills, estudios, proyectos o experiencia laboral puede contar como conocimiento.
				- Si la oferta exige experiencia profesional temporal con una tecnologia y el CV solo muestra un proyecto
				  academico, no consideres cumplido completamente ese requisito; puede ser una coincidencia parcial.

				recommendations:
				- Genera entre 2 y 4 recomendaciones concretas y breves.
				- Relacionalas directamente con esta postulacion.
				- Puede incluir que destacar del CV, que reforzar, que requisito preparar para entrevista o si el
				  seniority de la vacante esta claramente por encima del perfil.
				- No uses frases desmotivadoras ni recomiendes automaticamente no postularse.

				interviewQuestions:
				- Genera entre 3 y 5 preguntas realistas para una entrevista de este puesto.
				- Prioriza tecnologias coincidentes importantes, tecnologias faltantes importantes, experiencia solicitada
				  y responsabilidades especificas de la oferta.

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
