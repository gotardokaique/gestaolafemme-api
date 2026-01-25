package com.gestao.lafemme.api.services;

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

        Fornecedor f = new Fornecedor();
        f.setNome(nome.trim());
        f.setTelefone(telefone);
        f.setEmail(email);
        f.setAtivo(true);

        // Se Fornecedor tiver usuario (no seu diagrama de classe não tem; se tiver, set aqui)
        // f.setUsuario(UserContext.getUsuarioAutenticado());

        return dao.insert(f);
    }

    // ===================== LISTAR =====================

    @Transactional(readOnly = true)
    public List<Fornecedor> listarAtivos() {
        return dao.select()
                .from(Fornecedor.class)
                .where("ativo", Condicao.EQUAL, true)
                .orderBy("nome", true)
                .list();
    }

    @Transactional(readOnly = true)
    public List<Fornecedor> listarTodos() {
        return dao.select()
                .from(Fornecedor.class)
                .orderBy("nome", true)
                .list();
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
        Fornecedor f = buscarPorId(id);

        if (nome != null && !nome.isBlank()) f.setNome(nome.trim());
        if (telefone != null) f.setTelefone(telefone);
        if (email != null) f.setEmail(email);
        if (ativo != null) f.setAtivo(ativo);

        return dao.update(f);
    }

    // ===================== DESATIVAR =====================

    @Transactional
    public void desativar(Long id) {
        Fornecedor f = buscarPorId(id);
        f.setAtivo(false);
        dao.update(f);
    }
}
