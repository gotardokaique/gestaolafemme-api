package com.gestao.lafemme.api.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gen.core.constants.SitId;
import com.gen.core.db.DAOController;
import com.gen.core.db.exception.NotFoundException;
import com.gestao.lafemme.api.entity.Situacao;

@Component
public class SituacaoDefaultData implements ApplicationRunner {

    private final DAOController dao;

    public SituacaoDefaultData(DAOController dao) {
        this.dao = dao;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        for (SitId.Situacao sit : SitId.TODOS) {
            sincronizar(sit.id(), sit.nome(), sit.descricao());
        }
    }

    private void sincronizar(Integer id, String nome, String descricao) {
        Situacao sitBean;
        try {
            sitBean = dao.select()
                    .from(Situacao.class)
                    .id(id);

            sitBean.setNome(nome);
            sitBean.setDescricao(descricao);

            dao.update(sitBean);
        } catch (NotFoundException e) {
            sitBean = new Situacao(id, nome, descricao);
            dao.insert(sitBean);
        } catch (Exception e) {
            throw new RuntimeException("Erro ao sincronizar situação: " + nome);
        }
    }
}