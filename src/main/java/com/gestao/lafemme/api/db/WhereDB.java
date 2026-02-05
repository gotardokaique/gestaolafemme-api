// Caminho: com.gestao.lafemme.api.db/WhereDB.java
package com.gestao.lafemme.api.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WhereDB {

    public static final class WhereItem {
        private final String campo;
        private final Condicao condicao;
        private final Object[] valores;

        public WhereItem(String campo, Condicao condicao, Object... valores) {
            if (campo == null || campo.isBlank()) {
                throw new IllegalArgumentException("campo não pode ser nulo/vazio");
            }
            if (condicao == null) {
                throw new IllegalArgumentException("condicao não pode ser nula");
            }
            this.campo = campo.trim();
            this.condicao = condicao;
            this.valores = (valores == null ? new Object[0] : Arrays.copyOf(valores, valores.length));
        }

        public String getCampo() { return campo; }
        public Condicao getCondicao() { return condicao; }
        public Object[] getValores() { return Arrays.copyOf(valores, valores.length); }
    }

    private final List<WhereItem> itens = new ArrayList<>();

    public WhereDB add(String campo, Condicao condicao, Object... valores) {
        itens.add(new WhereItem(campo, condicao, valores));
        return this;
    }

    public boolean isEmpty() {
        return itens.isEmpty();
    }

    public List<WhereItem> getItens() {
        return Collections.unmodifiableList(itens);
    }
}
