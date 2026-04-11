package com.gestao.lafemme.api.security.controller;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.gen.core.db.Condicao;
import com.gen.core.db.DAOController;
import com.gen.core.db.TransactionDB;
import com.gestao.lafemme.api.entity.Usuario;
import com.gestao.lafemme.api.services.exceptions.NotFoundException;

@Service
public class UsuarioServiceValidacao implements UserDetailsService{

    private final TransactionDB trans;
    private final DAOController dao;

    public UsuarioServiceValidacao (TransactionDB trans, DAOController dao) {
        this.trans = trans;
        this.dao = dao;
    }

    public Boolean validarEmailJaCadastrado(String email) throws Exception {
        try {
            Usuario user = dao.select()
                    .from(Usuario.class)
                    .where("email", Condicao.EQUAL, email)
                    .one();
            return true;
        } catch (NotFoundException e) {
            return false;
        }
    }
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        try {
            return dao.select()
                    .from(Usuario.class)
                    .where("email", Condicao.EQUAL, email)
                    .one();
        } catch (Exception e) {
            throw new UsernameNotFoundException("Usuário não encontrado: " + email);
        }
    }
}