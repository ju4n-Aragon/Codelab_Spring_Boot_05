package com.denkitronik.clienteservice.delivery.rest;

import com.denkitronik.clienteservice.domain.entities.Cliente;
import com.denkitronik.clienteservice.domain.entities.Region;
import com.denkitronik.clienteservice.domain.exception.ClienteNotFoundException;
import com.denkitronik.clienteservice.domain.exception.ClienteServiceException;
import com.denkitronik.clienteservice.domain.services.IClienteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ClienteRestController.class)
class ClienteRestControllerTest {

    private static final String BASE = "/api/v1/cliente-service/clientes";
    private static final String JSON_VALIDO =
            "{\"nombre\":\"Ana\",\"apellido\":\"Perez\",\"email\":\"ana@mail.com\",\"region\":{\"id\":1}}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IClienteService clienteService;

    private Cliente cliente(Long id, String nombre) {
        Region region = new Region();
        region.setId(1L);
        region.setNombre("Sudamerica");

        Cliente c = new Cliente();
        c.setId(id);
        c.setNombre(nombre);
        c.setApellido("Perez");
        c.setEmail(nombre.toLowerCase() + "@mail.com");
        c.setCreateAt(LocalDate.of(2025, 4, 20));
        c.setRegion(region);
        return c;
    }

    // ---------- GET /clientes ----------

    @Test
    void index_devuelve200ConLaLista() throws Exception {
        when(clienteService.findAll()).thenReturn(List.of(cliente(1L, "Ana"), cliente(2L, "Luis")));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].nombre").value("Ana"));
    }

    @Test
    void index_sinClientes_devuelve200ConArregloVacio() throws Exception {
        when(clienteService.findAll()).thenReturn(List.of());

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void indexPaginado_devuelve200ConContent() throws Exception {
        Pageable pageable = PageRequest.of(0, 5);
        when(clienteService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(cliente(1L, "Ana")), pageable, 1));

        mockMvc.perform(get(BASE + "/page/0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    // ---------- GET /clientes/{id} ----------

    @Test
    void show_existente_devuelve200ConElCliente() throws Exception {
        when(clienteService.findById(1L)).thenReturn(cliente(1L, "Ana"));

        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.apellido").value("Perez"))
                .andExpect(jsonPath("$.email").value("ana@mail.com"));
    }

    @Test
    void show_inexistente_devuelve404ConApiErrorResponse() throws Exception {
        when(clienteService.findById(999L)).thenThrow(new ClienteNotFoundException(999L));

        mockMvc.perform(get(BASE + "/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.mensaje").value("Cliente con id 999 no encontrado"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ---------- POST /clientes ----------

    @Test
    void create_conDatosValidos_devuelve201() throws Exception {
        when(clienteService.save(any(Cliente.class))).thenReturn(cliente(10L, "Ana"));

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.nombre").value("Ana"));
    }

    @Test
    void create_conNombreVacio_devuelve400YNoLlamaAlServicio() throws Exception {
        String json = "{\"nombre\":\"\",\"apellido\":\"Perez\",\"email\":\"ana@mail.com\",\"region\":{\"id\":1}}";

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.nombre").exists());

        verify(clienteService, never()).save(any(Cliente.class));
    }

    @Test
    void create_conNombreDeUnCaracter_devuelve400() throws Exception {
        String json = "{\"nombre\":\"A\",\"apellido\":\"Perez\",\"email\":\"ana@mail.com\",\"region\":{\"id\":1}}";

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.nombre").exists());
    }

    @Test
    void create_conEmailInvalido_devuelve400() throws Exception {
        String json = "{\"nombre\":\"Ana\",\"apellido\":\"Perez\",\"email\":\"no-es-email\",\"region\":{\"id\":1}}";

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists());
    }

    @Test
    void create_sinRegion_devuelve400() throws Exception {
        String json = "{\"nombre\":\"Ana\",\"apellido\":\"Perez\",\"email\":\"ana@mail.com\"}";

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.region").exists());
    }

    @Test
    void create_conJsonMalFormado_devuelve400() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{nombre:"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // ---------- PUT /clientes/{id} ----------

    @Test
    void update_existente_devuelve201ConElClienteActualizado() throws Exception {
        when(clienteService.update(eq(1L), any(Cliente.class))).thenReturn(cliente(1L, "Ana"));

        mockMvc.perform(put(BASE + "/1").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void update_inexistente_devuelve404() throws Exception {
        when(clienteService.update(eq(999L), any(Cliente.class)))
                .thenThrow(new ClienteNotFoundException(999L));

        mockMvc.perform(put(BASE + "/999").contentType(MediaType.APPLICATION_JSON).content(JSON_VALIDO))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Cliente con id 999 no encontrado"));
    }

    // ---------- DELETE /clientes/{id} ----------

    @Test
    void delete_existente_devuelve204SinBody() throws Exception {
        mockMvc.perform(delete(BASE + "/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(clienteService).delete(1L);
    }

    @Test
    void delete_inexistente_devuelve404() throws Exception {
        doThrow(new ClienteNotFoundException(999L)).when(clienteService).delete(999L);

        mockMvc.perform(delete(BASE + "/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ---------- Errores 500 ----------

    @Test
    void errorDeServicio_devuelve500GenericoSinExponerDetalles() throws Exception {
        when(clienteService.findById(1L))
                .thenThrow(new ClienteServiceException("detalle interno secreto"));

        mockMvc.perform(get(BASE + "/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.mensaje").value("Error interno del servidor"));
    }

    @Test
    void errorInesperado_devuelve500Generico() throws Exception {
        when(clienteService.findById(2L)).thenThrow(new IllegalStateException("boom"));

        mockMvc.perform(get(BASE + "/2"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.mensaje").value("Error interno del servidor"));
    }
}
