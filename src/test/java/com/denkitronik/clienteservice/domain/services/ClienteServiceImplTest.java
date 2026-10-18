package com.denkitronik.clienteservice.domain.services;

import com.denkitronik.clienteservice.domain.entities.Cliente;
import com.denkitronik.clienteservice.domain.entities.Region;
import com.denkitronik.clienteservice.domain.exception.ClienteNotFoundException;
import com.denkitronik.clienteservice.domain.exception.ClienteServiceException;
import com.denkitronik.clienteservice.domain.repositories.IClienteDao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceImplTest {

    @Mock
    private IClienteDao clienteDao;

    @InjectMocks
    private ClienteServiceImpl service;

    private Cliente cliente(Long id, String nombre) {
        Region region = new Region();
        region.setId(1L);
        region.setNombre("Sudamerica");

        Cliente c = new Cliente();
        c.setId(id);
        c.setNombre(nombre);
        c.setApellido("Perez");
        c.setEmail(nombre.toLowerCase() + "@mail.com");
        c.setRegion(region);
        return c;
    }

    // ---------- findAll ----------

    @Test
    void findAll_devuelveLaListaDelDao() {
        List<Cliente> clientes = List.of(cliente(1L, "Ana"), cliente(2L, "Luis"));
        when(clienteDao.findAll()).thenReturn(clientes);

        assertEquals(2, service.findAll().size());
    }

    @Test
    void findAll_cuandoFallaLaBd_lanzaClienteServiceException() {
        when(clienteDao.findAll()).thenThrow(new DataAccessResourceFailureException("bd caida"));

        assertThrows(ClienteServiceException.class, () -> service.findAll());
    }

    @Test
    void findAll_paginado_devuelveLaPaginaDelDao() {
        Pageable pageable = PageRequest.of(0, 5);
        Page<Cliente> pagina = new PageImpl<>(List.of(cliente(1L, "Ana")), pageable, 1);
        when(clienteDao.findAll(pageable)).thenReturn(pagina);

        Page<Cliente> resultado = service.findAll(pageable);

        assertEquals(1, resultado.getTotalElements());
    }

    // ---------- findById ----------

    @Test
    void findById_existente_devuelveElCliente() {
        Cliente ana = cliente(1L, "Ana");
        when(clienteDao.findById(1L)).thenReturn(Optional.of(ana));

        assertSame(ana, service.findById(1L));
    }

    @Test
    void findById_inexistente_lanzaClienteNotFoundException() {
        when(clienteDao.findById(999L)).thenReturn(Optional.empty());

        ClienteNotFoundException ex =
                assertThrows(ClienteNotFoundException.class, () -> service.findById(999L));

        assertEquals("Cliente con id 999 no encontrado", ex.getMessage());
    }

    // ---------- save ----------

    @Test
    void save_persisteYDevuelveElClienteGuardado() {
        Cliente nuevo = cliente(null, "Ana");
        Cliente guardado = cliente(10L, "Ana");
        when(clienteDao.save(nuevo)).thenReturn(guardado);

        Cliente resultado = service.save(nuevo);

        assertEquals(10L, resultado.getId());
    }

    @Test
    void save_cuandoFallaLaBd_lanzaClienteServiceException() {
        when(clienteDao.save(any(Cliente.class)))
                .thenThrow(new DataAccessResourceFailureException("bd caida"));

        assertThrows(ClienteServiceException.class, () -> service.save(cliente(null, "Ana")));
    }

    // ---------- update ----------

    @Test
    void update_existente_copiaCamposYPreservaIdYCreateAt() {
        LocalDate creado = LocalDate.of(2025, 4, 20);
        Cliente actual = cliente(1L, "Viejo");
        actual.setCreateAt(creado);

        Cliente cambios = cliente(null, "Nuevo");
        cambios.setApellido("Gomez");
        cambios.setFoto("foto.png");

        when(clienteDao.findById(1L)).thenReturn(Optional.of(actual));
        when(clienteDao.saveAndFlush(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        Cliente resultado = service.update(1L, cambios);

        assertEquals(1L, resultado.getId());
        assertEquals("Nuevo", resultado.getNombre());
        assertEquals("Gomez", resultado.getApellido());
        assertEquals("foto.png", resultado.getFoto());
        assertEquals(creado, resultado.getCreateAt());
    }

    @Test
    void update_inexistente_lanzaClienteNotFoundExceptionYNoGuarda() {
        when(clienteDao.findById(42L)).thenReturn(Optional.empty());

        assertThrows(ClienteNotFoundException.class, () -> service.update(42L, cliente(null, "Ana")));

        verify(clienteDao, never()).saveAndFlush(any(Cliente.class));
    }

    // ---------- delete ----------

    @Test
    void delete_existente_eliminaPorId() {
        when(clienteDao.existsById(1L)).thenReturn(true);

        service.delete(1L);

        verify(clienteDao).deleteById(1L);
    }

    @Test
    void delete_inexistente_verificaExistsByIdYNoElimina() {
        when(clienteDao.existsById(5L)).thenReturn(false);

        assertThrows(ClienteNotFoundException.class, () -> service.delete(5L));

        verify(clienteDao, never()).deleteById(anyLong());
    }

    @Test
    void delete_cuandoFallaLaBd_lanzaClienteServiceException() {
        when(clienteDao.existsById(1L)).thenReturn(true);
        doThrow(new DataAccessResourceFailureException("bd caida")).when(clienteDao).deleteById(1L);

        assertThrows(ClienteServiceException.class, () -> service.delete(1L));
    }
}
