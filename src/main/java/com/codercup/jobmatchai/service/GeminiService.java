package com.codercup.jobmatchai.service;

import com.codercup.jobmatchai.dto.AnalysisResponse;
import com.codercup.jobmatchai.exception.AiServiceTimeoutException;
import com.codercup.jobmatchai.exception.AiServiceUnavailableException;
import com.codercup.jobmatchai.exception.AnalysisConfigurationException;
import com.codercup.jobmatchai.exception.InvalidAiResponseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.genai.Client;
import com.google.genai.errors.ApiException;
import com.google.genai.errors.GenAiIOException;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.Part;
import com.google.genai.types.Schema;
import com.google.genai.types.Type;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class GeminiService {

	private final ObjectMapper objectMapper;
	private final String apiKey;
	private final String model;
	private final int timeoutMs;

	public GeminiService(
			@Value("${gemini.api.key:}") String apiKey,
			@Value("${gemini.model}") String model,
			@Value("${gemini.timeout-ms}") int timeoutMs
	) {
		this.objectMapper = new ObjectMapper();
		this.apiKey = apiKey;
		this.model = model;
		if (timeoutMs <= 0) {
			throw new AnalysisConfigurationException("El timeout de Gemini debe ser mayor a 0 ms.");
		}
		this.timeoutMs = timeoutMs;
	}

	public AnalysisResponse analyze(String cvText, String jobDescription) {
		if (apiKey == null || apiKey.isBlank()) {
			throw new AnalysisConfigurationException("El servicio de analisis no esta configurado correctamente.");
		}

		String responseText = generateContent(buildPrompt(cvText, jobDescription));

		return parseResponse(responseText);
	}

	public AnalysisResponse analyze(String cvText, MultipartFile jobImage) {
		if (apiKey == null || apiKey.isBlank()) {
			throw new AnalysisConfigurationException("El servicio de analisis no esta configurado correctamente.");
		}

		String responseText;
		try {
			responseText = generateContent(buildImageContent(cvText, jobImage));
		}
		catch (IOException exception) {
			throw new InvalidAiResponseException("No se pudo interpretar la imagen de la oferta laboral.", exception);
		}

		try {
			return parseResponse(responseText);
		}
		catch (InvalidAiResponseException exception) {
			throw new InvalidAiResponseException("No se pudo interpretar la imagen de la oferta laboral.", exception);
		}
	}

	private String generateContent(String prompt) {
		try (Client client = buildClient()) {
			GenerateContentResponse response = client.models.generateContent(model, prompt, buildConfig());
			return response.text();
		}
		catch (ApiException exception) {
			throw new AiServiceUnavailableException(
					"El servicio de inteligencia artificial no esta disponible temporalmente.",
					exception
			);
		}
		catch (GenAiIOException exception) {
			throw mapGeminiIOException(exception);
		}
	}

	private String generateContent(Content content) {
		try (Client client = buildClient()) {
			GenerateContentResponse response = client.models.generateContent(model, content, buildConfig());
			return response.text();
		}
		catch (ApiException exception) {
			throw new AiServiceUnavailableException(
					"El servicio de inteligencia artificial no esta disponible temporalmente.",
					exception
			);
		}
		catch (GenAiIOException exception) {
			throw mapGeminiIOException(exception);
		}
	}

	private Client buildClient() {
		return Client.builder()
				.apiKey(apiKey)
				.httpOptions(buildHttpOptions())
				.build();
	}

	private HttpOptions buildHttpOptions() {
		return HttpOptions.builder()
				.timeout(timeoutMs)
				.build();
	}

	private RuntimeException mapGeminiIOException(GenAiIOException exception) {
		if (isTimeoutException(exception)) {
			return new AiServiceTimeoutException(
					"El servicio de inteligencia artificial tard\u00f3 demasiado en responder.",
					exception
			);
		}

		return new AiServiceUnavailableException(
				"El servicio de inteligencia artificial no esta disponible temporalmente.",
				exception
		);
	}

	private boolean isTimeoutException(Throwable exception) {
		Throwable current = exception;
		while (current != null) {
			if (current instanceof SocketTimeoutException || current instanceof InterruptedIOException) {
				return true;
			}
			current = current.getCause();
		}

		return false;
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

				%s

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
				""".formatted(buildAlternativeAndRecommendationRules(), cvText, jobDescription);
	}

	private Content buildImageContent(String cvText, MultipartFile jobImage) throws IOException {
		return Content.fromParts(
				Part.fromText(buildImagePrompt(cvText)),
				Part.fromBytes(jobImage.getBytes(), jobImage.getContentType())
		);
	}

	private String buildImagePrompt(String cvText) {
		return """
				Actua como un asistente de analisis profesional de postulaciones laborales.
				La imagen adjunta contiene una oferta laboral.
				Lee e interpreta unicamente la informacion visible en la imagen.
				Ignora elementos visuales que no sean relevantes para la vacante.

				Identifica internamente:
				- requisitos obligatorios;
				- requisitos deseables;
				- experiencia requerida;
				- seniority;
				- tecnologias;
				- responsabilidades relevantes;
				- habilidades tecnicas principales;
				- conocimientos complementarios.
				No devuelvas estas categorias en el JSON; usalas solo para comparar esos requisitos con el CV proporcionado.

				%s

				Criterio orientativo para matchPercentage:
				- Requisitos obligatorios y habilidades tecnicas principales: 60%%.
				- Experiencia y seniority requerido: 20%%.
				- Requisitos deseables: 10%%.
				- Habilidades complementarias relevantes: 10%%.
				No apliques una formula exacta si la imagen no permite hacerlo, pero respeta esta ponderacion como guia.
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
				- No inventes tecnologias ni requisitos que no sean visibles en la imagen.
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
				- Inclui habilidades o requisitos concretos presentes tanto en el CV como en la oferta visible en la imagen.
				- Prioriza tecnologias y practicas especificas como Java, Spring Boot, SQL, MySQL, REST APIs, Git o Scrum.
				- Evita elementos demasiado genericos como "Tecnologia", "Informatica" o "Desarrollo de Software",
				  salvo que sean realmente relevantes para la oferta.
				- No inventes equivalencias fuertes: Java no implica Node.js, y MySQL no implica PostgreSQL si
				  PostgreSQL es un requisito especifico.

				missingSkills:
				- Inclui solo requisitos relevantes visibles en la imagen y no demostrados por el CV.
				- No agregues tecnologias que la oferta no menciona.
				- Si la oferta exige experiencia temporal, conserva esa informacion cuando sea relevante.
				  Ejemplo: "Experiencia requerida: 4 anos; el CV no demuestra ese nivel de experiencia."
				- Mantene textos breves.

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
				""".formatted(buildAlternativeAndRecommendationRules(), cvText);
	}

	private String buildAlternativeAndRecommendationRules() {
		return """
				Requisitos alternativos:
				- Identifica expresiones de alternativa como "Java or Kotlin", "PHP or similar server-side technology",
				  "AWS or Azure", "React, Vue or Angular", "PostgreSQL or MySQL",
				  "Bachelor degree or equivalent experience", "Node.js and/or Java", "X or equivalent" y "X or similar".
				- Cuando la oferta expresa claramente que distintas opciones son alternativas validas, cumplir una alternativa
				  debe considerarse suficiente o parcialmente suficiente segun el contexto.
				- No marques automaticamente como faltantes las otras alternativas si una alternativa valida esta demostrada.
				- Si la oferta dice "Java or Kotlin" y el CV demuestra Java, considera el requisito cumplido y no agregues
				  Kotlin a missingSkills.
				- Si la oferta dice "PostgreSQL or MySQL" y el CV demuestra MySQL, considera el requisito cumplido y no
				  agregues PostgreSQL a missingSkills.
				- Si la oferta dice "PHP or similar server-side technology" y el CV demuestra Java y Spring Boot, puede ser
				  una alternativa server-side razonablemente equivalente; no marques PHP automaticamente como faltante.
				- Distingue alternativas de requisitos acumulativos: "Java, Spring Boot, Docker and AWS" normalmente expresa
				  requisitos separados, salvo que el contexto indique que son intercambiables.
				- Interpreta con cuidado AND, OR, AND/OR, "o similar", "equivalente" y tecnologias explicitamente
				  intercambiables.
				- Puede haber coincidencias parciales razonables, como MySQL ante un requisito generico de base de datos
				  relacional, Java/Spring Boot ante "server-side technology", o Vue ante "modern frontend framework".
				- No inventes equivalencias fuertes: Java no equivale a Node.js si Node.js es especifico, MySQL no equivale
				  a MongoDB, y Spring Boot no equivale a AWS.

				Recomendaciones seguras para el CV:
				- Nunca recomiendes agregar al CV una habilidad, tecnologia, experiencia o certificacion que no este
				  demostrada en el contenido del CV.
				- Si una tecnologia requerida podria existir pero no esta demostrada, usa redaccion condicional.
				  Ejemplo: "Si contas con conocimientos de JavaScript, HTML o CSS que actualmente no figuran en el CV,
				  considera incorporarlos de forma explicita."
				- Si una tecnologia claramente falta, recomienda aprender, practicar o desarrollar experiencia real.
				  Ejemplo: "Sumar practica con Docker mediante un proyecto personal para fortalecer este requisito."
				- No sugieras falsificar, exagerar ni agregar conocimientos inexistentes.
				- Un proyecto academico o personal demuestra conocimiento o experiencia practica, pero no debe convertirse
				  automaticamente en anos de experiencia profesional.
				- Si el CV muestra un proyecto full-stack con Spring Boot, Vue.js, MySQL o integracion con frontend, puede
				  contar como evidencia practica relacionada, pero no infieras automaticamente JavaScript, TypeScript,
				  HTML, CSS ni 1 ano profesional Full Stack si no estan demostrados.
				""";
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
