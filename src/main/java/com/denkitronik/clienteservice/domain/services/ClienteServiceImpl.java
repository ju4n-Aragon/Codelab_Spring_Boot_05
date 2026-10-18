package com.denkitronik.clienteservice.domain.services;

import com.denkitronik.clienteservice.domain.entities.Cliente;
import com.denkitronik.clienteservice.domain.exception.ClienteNotFoundException;
import com.denkitronik.clienteservice.domain.exception.ClienteServiceException;
import com.denkitronik.clienteservice.domain.repositories.IClienteDao;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ClienteServiceImpl implements IClienteService {

    private final IClienteDao clienteDao;

    public ClienteServiceImpl(IClienteDao clienteDao) {
        this.clienteDao = clienteDao;
    }

    @Override
    public List<Cliente> findAll() {
        try {
            return clienteDao.findAll();
        } catch (DataAccessException e) {
            throw new ClienteServiceException("Error al consultar los clientes", e);
        }
    }

    @Override
    public Page<Cliente> findAll(Pageable pageable) {
        try {
            return clienteDao.findAll(pageable);
        } catch (DataAccessException e) {
            throw new ClienteServiceException("Error al consultar los clientes", e);
        }
    }

    @Override
    public Cliente findById(Long id) {
        return clienteDao.findById(id)
                .orElseThrow(() -> new ClienteNotFoundException(id));
    }

    @Override
    public Cliente save(Cliente cliente) {
        try {
            return clienteDao.save(cliente);
        } catch (DataAccessException e) {
            throw new ClienteServiceException("Error al guardar el cliente", e);
        }
    }

    @Override
    public Cliente update(Long id, Cliente cliente) {
        // Carga el cliente actual (lanza ClienteNotFoundException si no existe)
        Cliente actual = clienteDao.findById(id)
                .orElseThrow(() -> new ClienteNotFoundException(id));

        // Copia los campos editables, preservando id y createAt
        actual.setNombre(cliente.getNombre());
        actual.setApellido(cliente.getApellido());
        actual.setEmail(cliente.getEmail());
        actual.setFoto(cliente.getFoto());
        actual.setRegion(cliente.getRegion());

        try {
            return clienteDao.saveAndFlush(actual);
        } catch (DataAccessException e) {
            throw new ClienteServiceException("Error al actualizar el cliente", e);
        }
    }

    @Override
    public void delete(Long id) {
        if (!clienteDao.existsById(id)) {
            throw new ClienteNotFoundException(id);
        }
        try {
            clienteDao.deleteById(id);
        } catch (DataAccessException e) {
            throw new ClienteServiceException("Error al eliminar el cliente", e);
        }
    }
}
