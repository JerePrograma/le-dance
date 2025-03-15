package ledance.infra.errores;

import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

@RestControllerAdvice
public class TratadorDeErrores {

    private static final Logger log = LoggerFactory.getLogger(TratadorDeErrores.class);

    // ✅ 404: Recurso no encontrado (entidad de JPA)
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<DatosErrorGeneral> tratarError404(EntityNotFoundException e) {
        log.warn("Error 404 - Recurso no encontrado: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new DatosErrorGeneral("404_NOT_FOUND", "Recurso no encontrado", e.getMessage(), LocalDateTime.now()));
    }

    // ✅ 404: Recurso no encontrado (cuando se lance una excepcion personalizada)
    @ExceptionHandler({DisciplinaNotFoundException.class, ProfesorNotFoundException.class, NoSuchElementException.class, ResourceNotFoundException.class})
    public ResponseEntity<DatosErrorGeneral> manejarRecursoNoEncontrado(RuntimeException e) {
        log.warn("Error 404 - Elemento no encontrado: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new DatosErrorGeneral("404_NOT_FOUND", "Elemento no encontrado", e.getMessage(), LocalDateTime.now()));
    }

    // ✅ 400: Validacion de datos de entrada
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<DatosErrorValidacion>> tratarError400(MethodArgumentNotValidException e) {
        log.warn("Error 400 - Validacion de datos fallida");
        var errores = e.getFieldErrors().stream().map(DatosErrorValidacion::new).toList();
        return ResponseEntity.badRequest().body(errores);
    }

    // ✅ 400: Parametro faltante en la solicitud
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<DatosErrorGeneral> manejarParametroFaltante(MissingServletRequestParameterException e) {
        log.warn("Error 400 - Falta un parametro requerido: {}", e.getParameterName());
        return ResponseEntity.badRequest()
                .body(new DatosErrorGeneral("400_BAD_REQUEST", "Falta un parametro requerido", e.getParameterName(), LocalDateTime.now()));
    }

    // ✅ 403: Acceso denegado
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<DatosErrorGeneral> tratarError403(AccessDeniedException e) {
        log.warn("Error 403 - Acceso denegado: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new DatosErrorGeneral("403_FORBIDDEN", "Acceso denegado", e.getMessage(), LocalDateTime.now()));
    }

    // ✅ 401: Error de autenticacion (caso especial)
    @ExceptionHandler(ErrorDeAutenticacionException.class)
    public ResponseEntity<DatosErrorGeneral> manejarErrorDeAutenticacion(ErrorDeAutenticacionException e) {
        log.warn("Error 401 - Autenticacion fallida: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new DatosErrorGeneral("401_UNAUTHORIZED", "Autenticacion fallida", e.getMessage(), LocalDateTime.now()));
    }

    // ✅ 405: Metodo HTTP no permitido
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<DatosErrorGeneral> manejarMetodoNoPermitido(HttpRequestMethodNotSupportedException e) {
        log.warn("Error 405 - Metodo HTTP no permitido: {}", e.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new DatosErrorGeneral("405_METHOD_NOT_ALLOWED", "Metodo no permitido", e.getMessage(), LocalDateTime.now()));
    }

    // ✅ 409: Error de negocio o logica de la aplicacion
    @ExceptionHandler(OperacionNoPermitidaException.class)
    public ResponseEntity<DatosErrorGeneral> manejarOperacionNoPermitida(OperacionNoPermitidaException e) {
        log.warn("Error 409 - Operacion no permitida: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new DatosErrorGeneral("409_CONFLICT", "Operacion no permitida", e.getMessage(), LocalDateTime.now()));
    }

    // ✅ 400: Argumento invalido en la solicitud
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<DatosErrorGeneral> manejarErrorDeArgumentoInvalido(IllegalArgumentException e) {
        log.warn("Error 400 - Argumento invalido: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(new DatosErrorGeneral("400_BAD_REQUEST", "Argumento invalido", e.getMessage(), LocalDateTime.now()));
    }

    // ✅ 500: Error interno del servidor
    @ExceptionHandler(Exception.class)
    public ResponseEntity<DatosErrorGeneral> manejarErrorInterno(Exception e) {
        log.error("Error 500 - Error interno del servidor: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new DatosErrorGeneral("500_INTERNAL_SERVER_ERROR", "Error interno del servidor", e.getMessage(), LocalDateTime.now()));
    }

    // ✅ 502: Error en la comunicacion con otro servidor (APIs externas)
    @ExceptionHandler({HttpClientErrorException.class, HttpServerErrorException.class})
    public ResponseEntity<DatosErrorGeneral> manejarErrorDeCliente(Exception e) {
        log.error("Error 502 - Fallo en comunicacion con API externa: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new DatosErrorGeneral("502_BAD_GATEWAY", "Error en API externa", e.getMessage(), LocalDateTime.now()));
    }

    // 🔹 **📌 Clases para respuestas de error** 🔹

    // Estructura para errores de validacion
    private record DatosErrorValidacion(String codigo, String campo, String mensaje) {
        public DatosErrorValidacion(FieldError error) {
            this("400_VALIDATION_ERROR", error.getField(), error.getDefaultMessage());
        }
    }

    // Estructura para errores generales
    private record DatosErrorGeneral(String codigo, String tipo, String detalle, LocalDateTime timestamp) {
    }

    // 🔹 **Excepciones Personalizadas** 🔹

    public static class RecursoNoEncontradoException extends RuntimeException {
        public RecursoNoEncontradoException(String mensaje) {
            super(mensaje);
        }
    }

    public static class OperacionNoPermitidaException extends RuntimeException {
        public OperacionNoPermitidaException(String mensaje) {
            super(mensaje);
        }
    }

    public static class ErrorDeAutenticacionException extends RuntimeException {
        public ErrorDeAutenticacionException(String mensaje) {
            super(mensaje);
        }
    }

    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String mensaje) {
            super(mensaje);
        }
    }

    public static class InvalidInscripcionException extends RuntimeException {
        public InvalidInscripcionException(String mensaje) {
            super(mensaje);
        }
    }

    // Excepciones adicionales para la gestion de disciplinas y profesores
    public static class DisciplinaNotFoundException extends RuntimeException {
        public DisciplinaNotFoundException(Long id) {
            super("No se encontro la disciplina con id=" + id);
        }
    }

    public static class ProfesorNotFoundException extends RuntimeException {
        public ProfesorNotFoundException(Long id) {
            super("No se encontro el profesor con id=" + id);
        }
    }
}
