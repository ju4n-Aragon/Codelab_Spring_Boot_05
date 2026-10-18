package com.denkitronik.clienteservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase raíz. Debe vivir en el paquete base para que el escaneo de
 * componentes alcance tanto {@code domain/} como {@code delivery/}.
 */
@SpringBootApplication
public class ClienteServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClienteServiceApplication.class, args);
    }
}
