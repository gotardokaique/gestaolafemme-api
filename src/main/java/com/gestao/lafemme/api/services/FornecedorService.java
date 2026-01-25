package com.gestao.lafemme.api.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestao.lafemme.api.context.UserContext;
import com.gestao.lafemme.api.db.Condicao;
import com.gestao.lafemme.api.db.DAOController;
import com.gestao.lafemme.api.entity.Fornecedor;
import com.gestao.lafemme.api.services.exceptions.BusinessException;
import com.gestao.lafemme.api.services.exceptions.NotFoundException;

@Service
public class FornecedorService {

    private final DAOController dao;

    public FornecedorService(DAOController dao) {
        this.dao = dao;
    }

    // ===================== CRIAR =====================

    @Transactional
    public Fornecedor criar(String nome, String telefone, String email) {
        if (nome == null || nome.isBlank()) {
            throw new BusinessException("Nome do fornecedor é obrigatório.");
        }

        Fornecedor fornBean = new Fornecedor();
        fornBean.setNome(nome.trim());
        fornBean.setTelefone(telefone);
        fornBean.setEmail(email);
        fornBean.setAtivo(true);

        return dao.insert(fornBean);
    }

    // ===================== LISTAR =====================
//
//    @Transactional(readOnly = true)
//    public List<Fornecedor> listarAtivos() {
//        return dao.select()
//                .from(Fornecedor.class)
//                .where("ativo", Condicao.EQUAL, true)
//                .orderBy("nome", true)
//                .list();
//    }

    @Transactional(readOnly = true)
    public List<Fornecedor> listarTodos() {
    	List<Fornecedor> fornList;
    	
    	try {
    		fornList = dao.select()
    				.from(Fornecedor.class)
    				.orderBy("nome", true)
    				.list();
    		
    	} catch (NotFoundException no) {
    		fornList = new ArrayList<Fornecedor>();
    	}
    	return fornList;
    }

    // ===================== BUSCAR =====================

    @Transactional(readOnly = true)
    public Fornecedor buscarPorId(Long id) {
        try {
            return dao.select()
                    .from(Fornecedor.class)
                    .id(id);
        } catch (Exception e) {
            throw new NotFoundException("Fornecedor não encontrado: " + id);
        }
    }

    // ===================== ATUALIZAR =====================

    @Transactional
    public Fornecedor atualizar(Long id, String nome, String telefone, String email, Boolean ativo) {
        Fornecedor fornBean = buscarPorId(id);

        if (nome != null ) fornBean.setNome(nome.trim());
        if (telefone != null) fornBean.setTelefone(telefone);
        if (email != null) fornBean.setEmail(email);
        if (ativo != null) fornBean.setAtivo(ativo);

        return dao.update(fornBean);
    }

    // ===================== DESATIVAR =====================

    @Transactional
    public void ativarDesativar(Long id) {
        Fornecedor fornBean = buscarPorId(id);
        if (fornBean.isAtivo()) {
        	fornBean.setAtivo(false);        	
        } else {
        	fornBean.setAtivo(true);
        }
        
        dao.update(fornBean);
    }
}
