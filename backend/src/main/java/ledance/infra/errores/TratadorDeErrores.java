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

    // ✅ 404: Recurso no encontrado
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<DatosErrorGeneral> tratarError404(EntityNotFoundException e) {
        log.warn("Error 404 - Recurso no encontrado: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new DatosErrorGeneral("404_NOT_FOUND", "Recurso no encontrado", e.getMessage(), LocalDateTime.now()));
    }

    // ✅ 400: Validación de datos de entrada
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<DatosErrorValidacion>> tratarError400(MethodArgumentNotValidException e) {
        log.warn("Error 400 - Validación de datos fallida");
        var errores = e.getFieldErrors().stream().map(DatosErrorValidacion::new).toList();
        return ResponseEntity.badRequest().body(errores);
    }

    // ✅ 400: Parámetro faltante en la solicitud
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<DatosErrorGeneral> manejarParametroFaltante(MissingServletRequestParameterException e) {
        log.warn("Error 400 - Falta un parámetro requerido: {}", e.getParameterName());
        return ResponseEntity.badRequest()
                .body(new DatosErrorGeneral("400_BAD_REQUEST", "Falta un parámetro requerido", e.getParameterName(), LocalDateTime.now()));
    }

    // ✅ 403: Acceso denegado
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<DatosErrorGeneral> tratarError403(AccessDeniedException e) {
        log.warn("Error 403 - Acceso denegado: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new DatosErrorGeneral("403_FORBIDDEN", "Acceso denegado", e.getMessage(), LocalDateTime.now()));
    }

    // ✅ 401: Error de autenticación (caso especial)
    @ExceptionHandler(ErrorDeAutenticacionException.class)
    public ResponseEntity<DatosErrorGeneral> manejarErrorDeAutenticacion(ErrorDeAutenticacionException e) {
        log.warn("Error 401 - Autenticación fallida: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new DatosErrorGeneral("401_UNAUTHORIZED", "Autenticación fallida", e.getMessage(), LocalDateTime.now()));
    }

    // ✅ 405: Método HTTP no permitido
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<DatosErrorGeneral> manejarMetodoNoPermitido(HttpRequestMethodNotSupportedException e) {
        log.warn("Error 405 - Método HTTP no permitido: {}", e.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new DatosErrorGeneral("405_METHOD_NOT_ALLOWED", "Método no permitido", e.getMessage(), LocalDateTime.now()));
    }

    // ✅ 409: Error de negocio o lógica de la aplicación
    @ExceptionHandler(OperacionNoPermitidaException.class)
    public ResponseEntity<DatosErrorGeneral> manejarOperacionNoPermitida(OperacionNoPermitidaException e) {
        log.warn("Error 409 - Operación no permitida: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new DatosErrorGeneral("409_CONFLICT", "Operación no permitida", e.getMessage(), LocalDateTime.now()));
    }

    // ✅ 400: Argumento inválido en la solicitud
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<DatosErrorGeneral> manejarErrorDeArgumentoInvalido(IllegalArgumentException e) {
        log.warn("Error 400 - Argumento inválido: {}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(new DatosErrorGeneral("400_BAD_REQUEST", "Argumento inválido", e.getMessage(), LocalDateTime.now()));
    }

    // ✅ 500: Error interno del servidor
    @ExceptionHandler(Exception.class)
    public ResponseEntity<DatosErrorGeneral> manejarErrorInterno(Exception e) {
        log.error("Error 500 - Error interno del servidor: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new DatosErrorGeneral("500_INTERNAL_SERVER_ERROR", "Error interno del servidor", e.getMessage(), LocalDateTime.now()));
    }

    // ✅ 500: Error en la comunicación con otro servidor (APIs externas)
    @ExceptionHandler({HttpClientErrorException.class, HttpServerErrorException.class})
    public ResponseEntity<DatosErrorGeneral> manejarErrorDeCliente(HttpClientErrorException e) {
        log.error("Error 500 - Fallo en comunicación con API externa: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new DatosErrorGeneral("502_BAD_GATEWAY", "Error en API externa", e.getMessage(), LocalDateTime.now()));
    }

    // ✅ 404: Recurso no encontrado en base de datos
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<DatosErrorGeneral> manejarNoSuchElement(NoSuchElementException e) {
        log.warn("Error 404 - Elemento no encontrado: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new DatosErrorGeneral("404_NOT_FOUND", "Elemento no encontrado", e.getMessage(), LocalDateTime.now()));
    }

    // 🔹 **📌 Clases para respuestas de error** 🔹

    // ✅ Estructura para errores de validación
    private record DatosErrorValidacion(String codigo, String campo, String mensaje) {
        public DatosErrorValidacion(FieldError error) {
            this("400_VALIDATION_ERROR", error.getField(), error.getDefaultMessage());
        }
    }

    // ✅ Estructura para errores generales
    private record DatosErrorGeneral(String codigo, String tipo, String detalle, LocalDateTime timestamp) {
    }

    // ✅ Excepciones personalizadas
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

    public class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) {
            super(message);
        }
    }

    public class InvalidInscripcionException extends RuntimeException {
        public InvalidInscripcionException(String message) {
            super(message);
        }
    }
}
