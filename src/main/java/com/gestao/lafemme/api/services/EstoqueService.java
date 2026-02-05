package com.gestao.lafemme.api.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestao.lafemme.api.context.UserContext;
import com.gestao.lafemme.api.controllers.dto.EstoqueResponseDTO;
import com.gestao.lafemme.api.db.Condicao;
import com.gestao.lafemme.api.db.DAOController;
import com.gestao.lafemme.api.entity.Estoque;
import com.gestao.lafemme.api.services.exceptions.NotFoundException;

@Service
public class EstoqueService {

    private final DAOController dao;

    public EstoqueService(DAOController dao) {
        this.dao = dao;
    }

    @Transactional(readOnly = true)
    public List<EstoqueResponseDTO> listarEstoque() {
        List<Estoque> listEstq;

        try {
            listEstq = dao.select()
                    .from(Estoque.class)
                    .join("produto")
                    .join("produto.categoriaProduto")
                    .join("unidade")
                    .where("unidade.id", Condicao.EQUAL, UserContext.getIdUnidade())
                    .orderBy("produto.nome", true)
                    .list();

        } catch (NotFoundException not) {
            listEstq = new ArrayList<>();
        }

        return EstoqueResponseDTO.refactor(listEstq);
    }
}
