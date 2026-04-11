package com.gestao.lafemme.api.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gestao.lafemme.api.constants.SitId;
import com.gen.core.db.DAOController;
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
        sincronizar(SitId.PENDENTE,            "Pendente",                    "Aguardando ação ou confirmação.");
        sincronizar(SitId.CONCLUIDO,           "Concluído",                   "Processo finalizado com sucesso.");
        sincronizar(SitId.CANCELADO,           "Cancelado",                   "Operação abortada sem efeitos.");
        sincronizar(SitId.EM_ANDAMENTO,        "Em Andamento",                "Atividade em execução.");
        sincronizar(SitId.RASCUNHO,            "Rascunho",                    "Registro em criação, sem validade operacional.");
        sincronizar(SitId.ATIVO,               "Ativo",                       "Registro liberado para uso.");
        sincronizar(SitId.INATIVO,             "Inativo",                     "Registro bloqueado para novas operações.");
        sincronizar(SitId.BLOQUEADO,           "Bloqueado",                   "Acesso suspenso por infração ou segurança.");
        sincronizar(SitId.EM_ANALISE,          "Em Análise",                  "Aguardando auditoria ou aprovação.");
        sincronizar(SitId.EM_SEPARACAO,        "Em Separação",                "Estoque reservado, preparando pacote.");
        sincronizar(SitId.ENVIADO,             "Enviado",                     "Mercadoria despachada na transportadora.");
        sincronizar(SitId.RECEBIDO_PARCIAL,    "Recebido Parcial",            "Entrega realizada com itens faltantes.");
        sincronizar(SitId.DEVOLVIDO,           "Devolvido",                   "Mercadoria retornou fisicamente ao armazém.");
        sincronizar(SitId.PAGO_PARCIAL,        "Pago Parcial",                "Valor recebido é inferior ao total.");
        sincronizar(SitId.VENCIDO,             "Vencido",                     "Data limite ultrapassada, sujeito a juros.");
        sincronizar(SitId.RENEGOCIADO,         "Renegociado",                 "Substituído por uma nova negociação.");
        sincronizar(SitId.FATURADO,            "Faturado",                    "Nota fiscal emitida com sucesso.");
    }

    private void sincronizar(Integer id, String nome, String descricao) {    	
        try {
            Situacao sit = dao.select().from(Situacao.class).id(id);
            if (sit.getNome().equals(nome) == false || sit.getDescricao().equals(descricao) == false) {
                sit.setNome(nome);
                sit.setDescricao(descricao);
                dao.update(sit);
            }
        } catch (Exception e) {
            dao.insert(new Situacao(id, nome, descricao));
        } 
    }
}