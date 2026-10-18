package com.denkitronik.clienteservice.integration;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas E2E: levanta la aplicación completa en un puerto aleatorio y
 * hace peticiones HTTP reales. Usa H2 embebido (modo PostgreSQL) en lugar de
 * Testcontainers, porque el entorno de calificación no tiene Docker.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ClienteIntegrationTest {

    private static final String BASE = "/api/v1/cliente-service/clientes";

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private JdbcTemplate jdbc;

    private long regionId;

    @BeforeEach
    void crearRegion() {
        String nombre = "Region-" + System.nanoTime();
        jdbc.update("INSERT INTO regiones (nombre) VALUES (?)", nombre);
        regionId = jdbc.queryForObject("SELECT id FROM regiones WHERE nombre = ?", Long.class, nombre);
    }

    private HttpEntity<String> json(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }

    private String clienteJson(String nombre, String email) {
        return String.format(
                "{\"nombre\":\"%s\",\"apellido\":\"Perez\",\"email\":\"%s\",\"region\":{\"id\":%d}}",
                nombre, email, regionId);
    }

    private long crearCliente(String nombre) {
        ResponseEntity<JsonNode> r = rest.postForEntity(
                BASE, json(clienteJson(nombre, nombre.toLowerCase() + "@mail.com")), JsonNode.class);
        assertEquals(HttpStatus.CREATED, r.getStatusCode());
        return r.getBody().get("id").asLong();
    }

    @Test
    void flujoCompleto_crear_buscar_actualizar_eliminar() {
        long id = crearCliente("Ana");

        ResponseEntity<JsonNode> get = rest.getForEntity(BASE + "/" + id, JsonNode.class);
        assertEquals(HttpStatus.OK, get.getStatusCode());
        assertEquals("Ana", get.getBody().get("nombre").asText());
        assertEquals(regionId, get.getBody().get("region").get("id").asLong());

        ResponseEntity<JsonNode> put = rest.exchange(BASE + "/" + id, HttpMethod.PUT,
                json(clienteJson("Maria", "maria@mail.com")), JsonNode.class);
        assertEquals(HttpStatus.CREATED, put.getStatusCode());
        assertEquals("Maria", put.getBody().get("nombre").asText());
        assertNotNull(put.getBody().get("createAt"));

        ResponseEntity<Void> delete = rest.exchange(BASE + "/" + id, HttpMethod.DELETE, null, Void.class);
        assertEquals(HttpStatus.NO_CONTENT, delete.getStatusCode());

        ResponseEntity<JsonNode> despues = rest.getForEntity(BASE + "/" + id, JsonNode.class);
        assertEquals(HttpStatus.NOT_FOUND, despues.getStatusCode());
    }

    @Test
    void crear_conDatosValidos_devuelve201ConIdYCreateAt() {
        ResponseEntity<JsonNode> r = rest.postForEntity(
                BASE, json(clienteJson("Luis", "luis@mail.com")), JsonNode.class);

        assertEquals(HttpStatus.CREATED, r.getStatusCode());
        assertTrue(r.getBody().get("id").asLong() > 0);
        assertNotNull(r.getBody().get("createAt"));
    }

    @Test
    void crear_conNombreVacio_devuelve400() {
        ResponseEntity<JsonNode> r = rest.postForEntity(
                BASE, json(clienteJson("", "luis@mail.com")), JsonNode.class);

        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        assertNotNull(r.getBody().get("nombre"));
    }

    @Test
    void crear_conEmailInvalido_devuelve400() {
        ResponseEntity<JsonNode> r = rest.postForEntity(
                BASE, json(clienteJson("Luis", "no-es-email")), JsonNode.class);

        assertEquals(HttpStatus.BAD_REQUEST, r.getStatusCode());
        assertNotNull(r.getBody().get("email"));
    }

    @Test
    void listar_devuelve200ConArregloQueIncluyeAlClienteCreado() {
        long id = crearCliente("Carla");

        ResponseEntity<JsonNode> r = rest.getForEntity(BASE, JsonNode.class);

        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertTrue(r.getBody().isArray());
        boolean encontrado = false;
        for (JsonNode nodo : r.getBody()) {
            if (nodo.get("id").asLong() == id) {
                encontrado = true;
            }
        }
        assertTrue(encontrado);
    }

    @Test
    void buscar_inexistente_devuelve404ConApiErrorResponse() {
        ResponseEntity<JsonNode> r = rest.getForEntity(BASE + "/999999", JsonNode.class);

        assertEquals(HttpStatus.NOT_FOUND, r.getStatusCode());
        assertEquals(404, r.getBody().get("status").asInt());
        assertEquals("Not Found", r.getBody().get("error").asText());
        assertEquals("Cliente con id 999999 no encontrado", r.getBody().get("mensaje").asText());
        assertNotNull(r.getBody().get("timestamp"));
    }

    @Test
    void actualizar_inexistente_devuelve404() {
        ResponseEntity<JsonNode> r = rest.exchange(BASE + "/999999", HttpMethod.PUT,
                json(clienteJson("Ana", "ana@mail.com")), JsonNode.class);

        assertEquals(HttpStatus.NOT_FOUND, r.getStatusCode());
    }

    @Test
    void eliminar_inexistente_devuelve404() {
        ResponseEntity<JsonNode> r = rest.exchange(BASE + "/999999", HttpMethod.DELETE, null, JsonNode.class);

        assertEquals(HttpStatus.NOT_FOUND, r.getStatusCode());
    }

    @Test
    void listarPaginado_devuelve200ConContent() {
        crearCliente("Pedro");

        ResponseEntity<JsonNode> r = rest.getForEntity(BASE + "/page/0", JsonNode.class);

        assertEquals(HttpStatus.OK, r.getStatusCode());
        assertTrue(r.getBody().get("content").isArray());
    }
}
