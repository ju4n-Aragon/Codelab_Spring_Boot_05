package com.denkitronik.clienteservice.domain.exception;

/**
 * Se lanza cuando se busca, actualiza o elimina un cliente que no existe.
 * Sin dependencias de frameworks web: es una excepción de negocio pura.
 */
public class ClienteNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ClienteNotFoundException(Long id) {
        super("Cliente con id " + id + " no encontrado");
    }
}
