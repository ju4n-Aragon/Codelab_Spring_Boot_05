package com.denkitronik.clienteservice.domain.exception;

/**
 * Error inesperado en la capa de servicio (p. ej. fallo de persistencia).
 * Se traduce a un 500 genérico: nunca se expone el detalle al cliente.
 */
public class ClienteServiceException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ClienteServiceException(String message) {
        super(message);
    }

    public ClienteServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
