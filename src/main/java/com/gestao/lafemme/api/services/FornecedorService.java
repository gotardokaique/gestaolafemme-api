package com.gestao.lafemme.api.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestao.lafemme.api.controllers.dto.FornecedorRequestDTO;
import com.gestao.lafemme.api.controllers.dto.FornecedorResponseDTO;
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
    public void  criarFornecedor(FornecedorRequestDTO dto) {
        if (dto.nome() == null || dto.nome().isBlank()) {
            throw new BusinessException("Nome do fornecedor é obrigatório.");
        }

        Fornecedor fornBean = new Fornecedor();
        fornBean.setNome(dto.nome().trim());
        fornBean.setTelefone(dto.telefone());
        fornBean.setEmail(dto.email());
        fornBean.setAtivo(true);

        dao.insert(fornBean);
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
    public List<FornecedorResponseDTO> listarTodos() {
    	List<Fornecedor> fornList;
    	
    	try {
    		fornList = dao.select()
    				.from(Fornecedor.class)
    				.orderBy("nome", true)
    				.list();
    		
    	} catch (NotFoundException no) {
    		fornList = new ArrayList<Fornecedor>();
    	}
    	return FornecedorResponseDTO.refactor(fornList);
    }

    // ===================== BUSCAR =====================

    @Transactional(readOnly = true)
    public Fornecedor buscarPorId(Integer id) {
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
    public void atualizar(Integer id, FornecedorRequestDTO dto) {
        Fornecedor fornBean = buscarPorId(id);

        if (dto.nome() != null ) fornBean.setNome(dto.nome().trim());
        if (dto.telefone() != null) fornBean.setTelefone(dto.telefone());
        if (dto.email() != null) fornBean.setEmail(dto.email());
        if (dto.ativo() != null) fornBean.setAtivo(dto.ativo());

        dao.update(fornBean);
    }

    // ===================== DESATIVAR =====================

    @Transactional
    public void ativarDesativar(Integer id) {
        Fornecedor fornBean = buscarPorId(id);
        if (fornBean.isAtivo()) {
        	fornBean.setAtivo(false);        	
        } else {
        	fornBean.setAtivo(true);
        }
        
        dao.update(fornBean);
    }
}
