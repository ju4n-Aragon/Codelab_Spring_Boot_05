package com.denkitronik.clienteservice.delivery.exception;

import com.denkitronik.clienteservice.domain.exception.ClienteNotFoundException;
import com.denkitronik.clienteservice.domain.exception.ClienteServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Traduce las excepciones a respuestas HTTP estandarizadas.
 * Los errores 500 nunca exponen stacktrace ni detalles internos.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private static final String MENSAJE_GENERICO_500 = "Error interno del servidor";

    @ExceptionHandler(ClienteNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleClienteNotFound(ClienteNotFoundException ex) {
        log.warn("Cliente no encontrado: {}", ex.getMessage());
        return build(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(ClienteServiceException.class)
    public ResponseEntity<ApiErrorResponse> handleClienteService(ClienteServiceException ex) {
        log.error("Error en el servicio de clientes", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, MENSAJE_GENERICO_500);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errores = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() == null ? "Valor inválido" : fe.getDefaultMessage(),
                        (a, b) -> a + "; " + b,
                        LinkedHashMap::new));
        log.debug("Validación fallida: {}", errores);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errores);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedBody(HttpMessageNotReadableException ex) {
        log.debug("Cuerpo de la solicitud ilegible: {}", ex.getMessage());
        return build(HttpStatus.BAD_REQUEST, "El cuerpo de la solicitud es inválido o está mal formado");
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.debug("Parámetro con tipo inválido: {}", ex.getName());
        return build(HttpStatus.BAD_REQUEST, "El parámetro '" + ex.getName() + "' tiene un valor inválido");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex) {
        // Errores 4xx propios de Spring (ruta inexistente, método no soportado, etc.)
        if (ex instanceof ErrorResponse errorResponse) {
            HttpStatus status = HttpStatus.resolve(errorResponse.getStatusCode().value());
            if (status != null && status.is4xxClientError()) {
                log.warn("Solicitud rechazada ({}): {}", status.value(), ex.getMessage());
                return build(status, status.getReasonPhrase());
            }
        }
        log.error("Error inesperado", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, MENSAJE_GENERICO_500);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(new ApiErrorResponse(status, mensaje));
    }
}
