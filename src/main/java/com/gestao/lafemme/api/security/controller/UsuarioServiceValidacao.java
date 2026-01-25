package com.gestao.lafemme.api.security.controller;

import java.util.List;

import org.springframework.stereotype.Service;

import com.gestao.lafemme.api.db.TransactionDB;

@Service
public class UsuarioServiceValidacao {

    private final TransactionDB trans;

    public UsuarioServiceValidacao(TransactionDB trans) {
        this.trans = trans;
    }
	
	public Boolean validarEmailJaCadastrado (String email) {
        String jpql = 
                "   select usu                   " +
                "   FROM Usuarios usu             " +
                "   WHERE 1 = 1                  " +
                "   AND usu.email ='" + email + "'";
		 
	        List<?> lista = trans.select(jpql);
	        
	        if (lista.isEmpty() == false) {
	        	return true;
	        }	

		return false;
	}

}
