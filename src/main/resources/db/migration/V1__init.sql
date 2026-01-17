-- V1__init.sql
-- PostgreSQL schema for Gestão La Femme (API)

-- =========================
-- 1) PERFIL_USUARIO
-- =========================
CREATE TABLE perfil_usuario (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nome VARCHAR(120) NOT NULL,
  descricao VARCHAR(255),
  ativo BOOLEAN NOT NULL DEFAULT TRUE
);

-- =========================
-- 2) USUARIOS
-- =========================
CREATE TABLE usuarios (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nome VARCHAR(120) NOT NULL,
  email VARCHAR(180) NOT NULL UNIQUE,
  senha VARCHAR(255) NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  data_criacao DATE NOT NULL DEFAULT CURRENT_DATE,
  perfil_usuario_id BIGINT NOT NULL,
  CONSTRAINT fk_usuarios_perfil
    FOREIGN KEY (perfil_usuario_id) REFERENCES perfil_usuario(id)
);

CREATE INDEX idx_usuarios_perfil_usuario_id ON usuarios(perfil_usuario_id);

-- =========================
-- 3) CATEGORIA_PRODUTO
-- =========================
CREATE TABLE categoria_produto (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nome VARCHAR(120) NOT NULL,
  descricao VARCHAR(255),
  ativo BOOLEAN NOT NULL DEFAULT TRUE
);

-- =========================
-- 4) PRODUTOS
-- =========================
CREATE TABLE produtos (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nome VARCHAR(140) NOT NULL,
  codigo VARCHAR(60) NOT NULL UNIQUE,
  descricao VARCHAR(255),
  valor_custo NUMERIC(19,2) NOT NULL,
  valor_venda NUMERIC(19,2) NOT NULL,
  ativo BOOLEAN NOT NULL DEFAULT TRUE,
  categoria_produto_id BIGINT NOT NULL,
  CONSTRAINT fk_produtos_categoria
    FOREIGN KEY (categoria_produto_id) REFERENCES categoria_produto(id),
  CONSTRAINT ck_produtos_valores
    CHECK (valor_custo >= 0 AND valor_venda >= 0)
);

CREATE INDEX idx_produtos_categoria_produto_id ON produtos(categoria_produto_id);

-- =========================
-- 5) ESTOQUES (1-1 com PRODUTO)
-- =========================
CREATE TABLE estoques (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  quantidade_atual INT NOT NULL DEFAULT 0,
  estoque_minimo INT NOT NULL DEFAULT 0,
  produto_id BIGINT NOT NULL UNIQUE,
  CONSTRAINT fk_estoques_produto
    FOREIGN KEY (produto_id) REFERENCES produtos(id),
  CONSTRAINT ck_estoques_quantidades
    CHECK (quantidade_atual >= 0 AND estoque_minimo >= 0)
);

CREATE INDEX idx_estoques_produto_id ON estoques(produto_id);

-- =========================
-- 6) FORNECEDORES
-- =========================
CREATE TABLE fornecedores (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nome VARCHAR(140) NOT NULL,
  telefone VARCHAR(30),
  email VARCHAR(180),
  ativo BOOLEAN NOT NULL DEFAULT TRUE
);

-- =========================
-- 7) COMPRAS
-- =========================
CREATE TABLE compras (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  data_compra DATE NOT NULL DEFAULT CURRENT_DATE,
  valor_total NUMERIC(19,2) NOT NULL,
  forma_pagamento VARCHAR(60) NOT NULL,
  fornecedor_id BIGINT NOT NULL,
  usuario_id BIGINT NOT NULL,
  CONSTRAINT fk_compras_fornecedor
    FOREIGN KEY (fornecedor_id) REFERENCES fornecedores(id),
  CONSTRAINT fk_compras_usuario
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
  CONSTRAINT ck_compras_valor_total
    CHECK (valor_total >= 0)
);

CREATE INDEX idx_compras_fornecedor_id ON compras(fornecedor_id);
CREATE INDEX idx_compras_usuario_id ON compras(usuario_id);
CREATE INDEX idx_compras_data_compra ON compras(data_compra);

-- =========================
-- 8) VENDAS
-- =========================
CREATE TABLE vendas (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  data_venda DATE NOT NULL DEFAULT CURRENT_DATE,
  valor_total NUMERIC(19,2) NOT NULL,
  forma_pagamento VARCHAR(60) NOT NULL,
  usuario_id BIGINT NOT NULL,
  CONSTRAINT fk_vendas_usuario
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
  CONSTRAINT ck_vendas_valor_total
    CHECK (valor_total >= 0)
);

CREATE INDEX idx_vendas_usuario_id ON vendas(usuario_id);
CREATE INDEX idx_vendas_data_venda ON vendas(data_venda);

-- =========================
-- 9) MOVIMENTACOES_ESTOQUE
-- =========================
CREATE TABLE movimentacoes_estoque (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  data_movimentacao DATE NOT NULL DEFAULT CURRENT_DATE,
  tipo_movimentacao VARCHAR(20) NOT NULL,
  quantidade INT NOT NULL,
  observacao VARCHAR(255),
  estoque_id BIGINT NOT NULL,
  compra_id BIGINT NULL,
  venda_id BIGINT NULL,
  usuario_id BIGINT NOT NULL,

  CONSTRAINT fk_mov_estoque_estoque
    FOREIGN KEY (estoque_id) REFERENCES estoques(id),
  CONSTRAINT fk_mov_estoque_compra
    FOREIGN KEY (compra_id) REFERENCES compras(id),
  CONSTRAINT fk_mov_estoque_venda
    FOREIGN KEY (venda_id) REFERENCES vendas(id),
  CONSTRAINT fk_mov_estoque_usuario
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),

  CONSTRAINT ck_mov_estoque_tipo
    CHECK (tipo_movimentacao IN ('ENTRADA','SAIDA','AJUSTE')),
  CONSTRAINT ck_mov_estoque_quantidade
    CHECK (quantidade > 0),

  -- regra de integridade simples: não permitir compra e venda ao mesmo tempo
  CONSTRAINT ck_mov_estoque_origem
    CHECK (NOT (compra_id IS NOT NULL AND venda_id IS NOT NULL))
);

CREATE INDEX idx_mov_estoque_data ON movimentacoes_estoque(data_movimentacao);
CREATE INDEX idx_mov_estoque_estoque_id ON movimentacoes_estoque(estoque_id);
CREATE INDEX idx_mov_estoque_compra_id ON movimentacoes_estoque(compra_id);
CREATE INDEX idx_mov_estoque_venda_id ON movimentacoes_estoque(venda_id);
CREATE INDEX idx_mov_estoque_usuario_id ON movimentacoes_estoque(usuario_id);

-- =========================
-- 10) LANCAMENTOS_FINANCEIROS
-- =========================
CREATE TABLE lancamentos_financeiros (
  id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  data_lancamento DATE NOT NULL DEFAULT CURRENT_DATE,
  tipo VARCHAR(20) NOT NULL,
  valor NUMERIC(19,2) NOT NULL,
  descricao VARCHAR(255) NOT NULL,
  compra_id BIGINT NULL,
  venda_id BIGINT NULL,
  usuario_id BIGINT NOT NULL,

  CONSTRAINT fk_lanc_fin_compra
    FOREIGN KEY (compra_id) REFERENCES compras(id),
  CONSTRAINT fk_lanc_fin_venda
    FOREIGN KEY (venda_id) REFERENCES vendas(id),
  CONSTRAINT fk_lanc_fin_usuario
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id),

  CONSTRAINT ck_lanc_fin_tipo
    CHECK (tipo IN ('ENTRADA','SAIDA')),
  CONSTRAINT ck_lanc_fin_valor
    CHECK (valor >= 0),

  -- não permitir compra e venda ao mesmo tempo
  CONSTRAINT ck_lanc_fin_origem
    CHECK (NOT (compra_id IS NOT NULL AND venda_id IS NOT NULL))
);

CREATE INDEX idx_lanc_fin_data ON lancamentos_financeiros(data_lancamento);
CREATE INDEX idx_lanc_fin_compra_id ON lancamentos_financeiros(compra_id);
CREATE INDEX idx_lanc_fin_venda_id ON lancamentos_financeiros(venda_id);
CREATE INDEX idx_lanc_fin_usuario_id ON lancamentos_financeiros(usuario_id);
