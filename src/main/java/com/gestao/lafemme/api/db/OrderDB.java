// Caminho: com.gestao.lafemme.api.db/OrderDB.java
package com.gestao.lafemme.api.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class OrderDB {

    public static final class OrderItem {
        private final String campo;
        private final boolean asc;

        public OrderItem(String campo, boolean asc) {
            if (campo == null || campo.isBlank()) {
                throw new IllegalArgumentException("campo do ORDER BY não pode ser nulo/vazio");
            }
            this.campo = campo.trim();
            this.asc = asc;
        }

        public String getCampo() { return campo; }
        public boolean isAsc() { return asc; }
    }

    private final List<OrderItem> itens = new ArrayList<>();

    public OrderDB addAsc(String campo) {
        itens.add(new OrderItem(campo, true));
        return this;
    }

    public OrderDB addDesc(String campo) {
        itens.add(new OrderItem(campo, false));
        return this;
    }

    public boolean isEmpty() {
        return itens.isEmpty();
    }

    public List<OrderItem> getItens() {
        return Collections.unmodifiableList(itens);
    }
}
