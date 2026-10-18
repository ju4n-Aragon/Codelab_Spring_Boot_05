package com.denkitronik.clienteservice.delivery.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

/**
 * Formato único de error devuelto al cliente:
 * { "status", "error", "mensaje", "timestamp" }.
 * El timestamp se genera automáticamente al construir el objeto.
 */
public class ApiErrorResponse {

    private int status;
    private String error;
    private String mensaje;
    private LocalDateTime timestamp;

    public ApiErrorResponse() {
        this.timestamp = LocalDateTime.now();
    }

    public ApiErrorResponse(HttpStatus httpStatus, String mensaje) {
        this.status = httpStatus.value();
        this.error = httpStatus.getReasonPhrase();
        this.mensaje = mensaje;
        this.timestamp = LocalDateTime.now();
    }

    public int getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getMensaje() {
        return mensaje;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
