package com.codercup.jobmatchai.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.codercup.jobmatchai.exception.InvalidCvContentException;
import org.junit.jupiter.api.Test;

class CvContentValidatorTest {

	private final CvContentValidator validator = new CvContentValidator();

	@Test
	void acceptsTraditionalSpanishCv() {
		assertThatCode(() -> validator.validate("""
				Perfil profesional
				Desarrollador backend con experiencia profesional en Java y Spring Boot.
				Experiencia laboral en APIs REST, bases de datos SQL y trabajo con equipos ágiles.
				Educación: Ingeniería en Sistemas, Universidad Tecnológica.
				Habilidades técnicas: Java, Spring Boot, SQL, Docker, Git.
				""")).doesNotThrowAnyException();
	}

	@Test
	void acceptsJuniorStudentCvWithoutWorkExperience() {
		assertThatCode(() -> validator.validate("""
				Juan Perez
				Email: juan.perez@example.com
				Educación: Tecnicatura en Programación, instituto técnico.
				Habilidades: JavaScript, HTML, CSS, Git y React.
				Proyectos académicos: aplicación de turnos, portfolio personal y sistema de notas.
				Cursos: desarrollo web inicial y bases de datos.
				""")).doesNotThrowAnyException();
	}

	@Test
	void acceptsJuniorCvWithEducationSkillsAndContact() {
		assertThatCode(() -> validator.validate("""
				Lucía Martinez
				Contacto: lucia.martinez@example.com
				Educación: estudiante de tecnicatura universitaria en programación.
				Habilidades técnicas: HTML, CSS, JavaScript, SQL, Git y resolución de problemas.
				Idiomas: español nativo e inglés básico para lectura técnica.
				Interés profesional en desarrollo web y aplicaciones de gestión.
				""")).doesNotThrowAnyException();
	}

	@Test
	void acceptsStudentCvWithEducationSkillsAndProjects() {
		assertThatCode(() -> validator.validate("""
				Estudiante de sistemas
				Educación: Ingeniería en Informática en curso en universidad nacional.
				Habilidades: Java, Python, SQL, Git, Linux y fundamentos de testing.
				Proyectos académicos: API para biblioteca, portfolio web y sistema de turnos.
				Cursos: algoritmos, bases de datos y desarrollo de software.
				""")).doesNotThrowAnyException();
	}

	@Test
	void acceptsEnglishCv() {
		assertThatCode(() -> validator.validate("""
				Professional Summary
				Software developer focused on backend services and cloud applications.
				Work experience building APIs for internal tools.
				Education: Computer Science degree at local university.
				Technical skills: Java, Spring Boot, SQL, Docker, Git.
				Projects: inventory service and personal portfolio.
				""")).doesNotThrowAnyException();
	}

	@Test
	void acceptsBriefButReasonableCv() {
		assertThatCode(() -> validator.validate("""
				María Gómez - maria.gomez@example.com
				Perfil: estudiante de desarrollo de software orientada a aplicaciones web.
				Formación: curso intensivo de programación full stack.
				Habilidades: JavaScript, React, Node.js, Git.
				Proyectos: portfolio web y gestor de tareas.
				""")).doesNotThrowAnyException();
	}

	@Test
	void acceptsCvWithoutWorkExperienceWhenOtherEvidenceIsEnough() {
		assertThatCode(() -> validator.validate("""
				Carlos Ruiz
				LinkedIn: linkedin.com/in/carlos-ruiz
				Educación: Licenciatura en Informática en curso.
				Habilidades técnicas: Python, SQL, Linux, Git.
				Proyectos personales: scraper de noticias y dashboard de análisis de datos.
				Idiomas: español e inglés intermedio.
				""")).doesNotThrowAnyException();
	}

	@Test
	void rejectsArgentinianInvoice() {
		assertThatThrownBy(() -> validator.validate("""
				FACTURA B
				CUIT 30-12345678-9
				Punto de Venta 0003 Nro. Comprobante 00001234
				Condición de venta contado
				IVA 21%
				Subtotal 10000
				Importe Total 12100
				CAE 12345678901234 Vencimiento 20/12/2026
				""")).isInstanceOf(InvalidCvContentException.class);
	}

	@Test
	void rejectsEnglishInvoice() {
		assertThatThrownBy(() -> validator.validate("""
				INVOICE
				Invoice number 2026-001
				VAT ID 123456
				Service description monthly subscription
				Subtotal 200
				Total 242
				Payment due date September 30
				""")).isInstanceOf(InvalidCvContentException.class);
	}

	@Test
	void rejectsGenericUniversityNoticeWithEmailEducationAndLanguage() {
		assertThatThrownBy(() -> validator.validate("""
				Universidad Nacional

				Contacto: secretaria@universidad.edu.ar

				La universidad informa a sus estudiantes que el curso de inglés
				comenzará durante septiembre. La formación académica contará con
				material adicional, actividades, horarios y documentación para los
				alumnos durante todo el ciclo lectivo.
				""")).isInstanceOf(InvalidCvContentException.class);
	}

	@Test
	void rejectsCommercialDocumentWithAccidentalCvSignals() {
		assertThatThrownBy(() -> validator.validate("""
				FACTURA B
				Cliente: Universidad Nacional contacto compras@universidad.edu.ar
				Detalle del comprobante con experiencia de compra institucional.
				CUIT 30-12345678-9
				Punto de venta 0003
				CAE 12345678901234
				IVA 21%
				Subtotal 10000
				Total 12100
				""")).isInstanceOf(InvalidCvContentException.class);
	}

	@Test
	void rejectsGenericEssay() {
		assertThatThrownBy(() -> validator.validate("""
				La tecnología moderna transforma la manera en que las personas se comunican.
				Este ensayo analiza diferentes cambios sociales vinculados al uso de internet,
				la educación digital y las nuevas formas de trabajo remoto en distintas regiones.
				""")).isInstanceOf(InvalidCvContentException.class);
	}

	@Test
	void rejectsContract() {
		assertThatThrownBy(() -> validator.validate("""
				Contrato de prestación de servicios entre las partes firmantes.
				El proveedor se compromete a entregar los servicios descriptos en el anexo.
				Las condiciones de pago, vigencia, rescisión y jurisdicción se detallan a continuación.
				""")).isInstanceOf(InvalidCvContentException.class);
	}

	@Test
	void rejectsTooShortDocument() {
		assertThatThrownBy(() -> validator.validate("Juan Perez. Java."))
				.isInstanceOf(InvalidCvContentException.class);
	}

	@Test
	void rejectsDocumentWithSingleCvKeyword() {
		assertThatThrownBy(() -> validator.validate("""
				Experiencia experiencia experiencia experiencia experiencia experiencia.
				Documento con una sola palabra repetida, varios párrafos administrativos,
				descripciones generales, fechas sueltas y contenido sin estructura razonable.
				""")).isInstanceOf(InvalidCvContentException.class);
	}

	@Test
	void acceptsBillingExperienceInsideRealCv() {
		assertThatCode(() -> validator.validate("""
				Ana López - ana.lopez@example.com
				Perfil profesional administrativo con orientación a sistemas.
				Experiencia laboral: gestión de facturas, IVA, clientes y reportes comerciales.
				Educación: Universidad Nacional, administración de empresas.
				Habilidades: Excel, SQL, herramientas de gestión, análisis de datos.
				""")).doesNotThrowAnyException();
	}

	@Test
	void doesNotCountShortCommercialSignalsInsideOtherWords() {
		assertThatCode(() -> validator.validate("""
				Sofía Castro - sofia.castro@example.com
				Perfil profesional de soporte administrativo con foco en atención interna.
				Experiencia laboral en gestión documental y comunicación con equipos.
				Educación: formación terciaria en administración.
				Habilidades: Excel, herramientas colaborativas, organización y seguimiento.
				Texto adicional cautivador sobre iniciativa, innovación y trabajo responsable.
				""")).doesNotThrowAnyException();
	}
}
