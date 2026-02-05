package com.gestao.lafemme.api.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestao.lafemme.api.context.UserContext;
import com.gestao.lafemme.api.controllers.dto.MovimentacaoEstoqueResponseDTO;
import com.gestao.lafemme.api.db.Condicao;
import com.gestao.lafemme.api.db.DAOController;
import com.gestao.lafemme.api.entity.MovimentacaoEstoque;
import com.gestao.lafemme.api.services.exceptions.NotFoundException;

@Service
public class MovimentacaoEstoqueService {

    private final DAOController dao;

    public MovimentacaoEstoqueService(DAOController dao) {
        this.dao = dao;
    }

    @Transactional(readOnly = true)
    public List<MovimentacaoEstoqueResponseDTO> listarMovimentacoes() {
        List<MovimentacaoEstoque> listMov;

        try {
            listMov = dao.select()
                    .from(MovimentacaoEstoque.class)
                    .join("estoque")
                    .join("estoque.produto") // Garante que o produto venha junto
                    .join("unidade")
                    .where("unidade.id", Condicao.EQUAL, UserContext.getIdUnidade())
                    .orderBy("dataMovimentacao", false) // Mais recentes primeiro
                    .orderBy("id", false)
                    .list();

        } catch (NotFoundException not) {
            listMov = new ArrayList<>();
        }

        return MovimentacaoEstoqueResponseDTO.refactor(listMov);
    }
}
