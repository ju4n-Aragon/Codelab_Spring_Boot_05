package com.denkitronik.clienteservice.domain.services;

import com.denkitronik.clienteservice.domain.entities.Cliente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IClienteService {

    List<Cliente> findAll();

    Page<Cliente> findAll(Pageable pageable);

    /** @throws com.denkitronik.clienteservice.domain.exception.ClienteNotFoundException si no existe */
    Cliente findById(Long id);

    Cliente save(Cliente cliente);

    /** @throws com.denkitronik.clienteservice.domain.exception.ClienteNotFoundException si no existe */
    Cliente update(Long id, Cliente cliente);

    /** @throws com.denkitronik.clienteservice.domain.exception.ClienteNotFoundException si no existe */
    void delete(Long id);
}
