package com.denkitronik.clienteservice.domain.repositories;

import com.denkitronik.clienteservice.domain.entities.Cliente;
import com.denkitronik.clienteservice.domain.entities.Region;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class IClienteDaoTest {

    @Autowired
    private IClienteDao clienteDao;

    @Autowired
    private TestEntityManager em;

    private Region nuevaRegion() {
        Region region = new Region();
        region.setNombre("Sudamerica");
        return em.persist(region);
    }

    private Cliente nuevoCliente(String nombre, Region region) {
        Cliente c = new Cliente();
        c.setNombre(nombre);
        c.setApellido("Perez");
        c.setEmail(nombre.toLowerCase() + "@mail.com");
        c.setRegion(region);
        return c;
    }

    @Test
    void save_generaIdYCreateAt() {
        Cliente guardado = clienteDao.save(nuevoCliente("Ana", nuevaRegion()));
        em.flush();

        assertNotNull(guardado.getId());
        assertNotNull(guardado.getCreateAt());
    }

    @Test
    void findById_existente_devuelveElClienteConSuRegion() {
        Region region = nuevaRegion();
        Cliente guardado = em.persistAndFlush(nuevoCliente("Ana", region));
        em.clear();

        Cliente encontrado = clienteDao.findById(guardado.getId()).orElseThrow();

        assertEquals("Ana", encontrado.getNombre());
        assertEquals(region.getId(), encontrado.getRegion().getId());
    }

    @Test
    void findById_inexistente_devuelveVacio() {
        assertTrue(clienteDao.findById(999999L).isEmpty());
    }

    @Test
    void findAll_devuelveTodosLosClientes() {
        Region region = nuevaRegion();
        em.persist(nuevoCliente("Ana", region));
        em.persist(nuevoCliente("Luis", region));
        em.flush();

        assertEquals(2, clienteDao.findAll().size());
    }

    @Test
    void findAll_paginado_respetaTamanioYTotal() {
        Region region = nuevaRegion();
        for (int i = 0; i < 5; i++) {
            em.persist(nuevoCliente("Cliente" + i, region));
        }
        em.flush();

        Page<Cliente> pagina = clienteDao.findAll(PageRequest.of(0, 2));

        assertEquals(2, pagina.getContent().size());
        assertEquals(5, pagina.getTotalElements());
        assertEquals(3, pagina.getTotalPages());
    }

    @Test
    void existsById_reflejaSiExisteONo() {
        Cliente guardado = em.persistAndFlush(nuevoCliente("Ana", nuevaRegion()));

        assertTrue(clienteDao.existsById(guardado.getId()));
        assertFalse(clienteDao.existsById(999999L));
    }

    @Test
    void deleteById_eliminaElCliente() {
        Cliente guardado = em.persistAndFlush(nuevoCliente("Ana", nuevaRegion()));

        clienteDao.deleteById(guardado.getId());
        em.flush();

        assertFalse(clienteDao.existsById(guardado.getId()));
    }
}
