-- =====================================================
-- TABELA: unidade
-- =====================================================
CREATE TABLE unidade (
  uni_id            BIGSERIAL PRIMARY KEY,
  uni_nome          VARCHAR(140) NOT NULL,
  uni_ativo         BOOLEAN NOT NULL DEFAULT TRUE,
  uni_data_cadastro TIMESTAMP NOT NULL DEFAULT NOW()
);

-- =====================================================
-- TABELA: perfil_usuario
-- =====================================================
CREATE TABLE perfil_usuario (
  per_id             BIGSERIAL PRIMARY KEY,
  per_nome           VARCHAR(120) NOT NULL,
  per_descricao      VARCHAR(255),
  per_ativo          BOOLEAN NOT NULL DEFAULT TRUE,
  per_data_cadastro  TIMESTAMP NOT NULL DEFAULT NOW(),

  uni_id             BIGINT NOT NULL,

  CONSTRAINT fk_perfil_usuario_unidade
    FOREIGN KEY (uni_id)
    REFERENCES unidade (uni_id)
);

CREATE INDEX idx_perfil_usuario_uni_id
  ON perfil_usuario (uni_id);

-- =====================================================
-- TABELA: usuario
-- =====================================================
CREATE TABLE usuario (
  usu_id            BIGSERIAL PRIMARY KEY,
  usu_nome          VARCHAR(120) NOT NULL,
  usu_email         VARCHAR(180) NOT NULL,
  usu_senha         VARCHAR(255) NOT NULL,
  usu_ativo         BOOLEAN NOT NULL DEFAULT TRUE,
  usu_data_criacao  TIMESTAMP NOT NULL DEFAULT NOW(),
  usu_trocarsenha BOOLEAN NOT NULL DEFAULT FALSE,
  usu_senhatrocadaem TIMESTAMP,

  perfil_usuario_id BIGINT NOT NULL,

  CONSTRAINT uk_usuario_email
    UNIQUE (usu_email),

  CONSTRAINT fk_usuario_perfil_usuario
    FOREIGN KEY (perfil_usuario_id)
    REFERENCES perfil_usuario (per_id)   
);

CREATE INDEX idx_usuario_perfil_usuario_id
  ON usuario (perfil_usuario_id);

-- =====================================================
-- TABELA: usuario_unidade
-- =====================================================
CREATE TABLE usuario_unidade (
  usn_id            BIGSERIAL PRIMARY KEY,
  usu_id            BIGINT NOT NULL,
  uni_id            BIGINT NOT NULL,
  usn_data_cadastro TIMESTAMP NOT NULL DEFAULT NOW(),

  CONSTRAINT uk_usu_uni
    UNIQUE (usu_id, uni_id),

  CONSTRAINT fk_usuario_unidade_usuario
    FOREIGN KEY (usu_id)
    REFERENCES usuario (usu_id),

  CONSTRAINT fk_usuario_unidade_unidade
    FOREIGN KEY (uni_id)
    REFERENCES unidade (uni_id)
);

CREATE INDEX idx_usuario_unidade_usu_id
  ON usuario_unidade (usu_id);

CREATE INDEX idx_usuario_unidade_uni_id
  ON usuario_unidade (uni_id);

-- =====================================================
-- TABELA: categoria_produto
-- =====================================================
CREATE TABLE categoria_produto (
  catp_id            BIGSERIAL PRIMARY KEY,
  catp_nome          VARCHAR(120) NOT NULL,
  catp_descricao     VARCHAR(255),
  catp_ativo         BOOLEAN NOT NULL DEFAULT TRUE,
  catp_data_cadastro TIMESTAMP NOT NULL DEFAULT NOW(),

  uni_id             BIGINT NOT NULL,

  CONSTRAINT fk_categoria_produto_unidade
    FOREIGN KEY (uni_id)
    REFERENCES unidade (uni_id)
);

CREATE INDEX idx_categoria_produto_uni_id
  ON categoria_produto (uni_id);

-- =====================================================
-- TABELA: produto
-- =====================================================
CREATE TABLE produto (
  prod_id            BIGSERIAL PRIMARY KEY,
  prod_nome          VARCHAR(140) NOT NULL,
  prod_codigo        VARCHAR(60)  NOT NULL,
  prod_descricao     VARCHAR(255),
  prod_valor_custo   NUMERIC(19,2) NOT NULL,
  prod_valor_venda   NUMERIC(19,2) NOT NULL,
  prod_ativo         BOOLEAN NOT NULL DEFAULT TRUE,
  prod_data_cadastro TIMESTAMP NOT NULL DEFAULT NOW(),

  catp_id            BIGINT NOT NULL,
  uni_id             BIGINT NOT NULL,

  CONSTRAINT uk_produto_codigo
    UNIQUE (prod_codigo),

  CONSTRAINT fk_produto_categoria_produto
    FOREIGN KEY (catp_id)
    REFERENCES categoria_produto (catp_id),

  CONSTRAINT fk_produto_unidade
    FOREIGN KEY (uni_id)
    REFERENCES unidade (uni_id)
);

CREATE INDEX idx_produto_catp_id
  ON produto (catp_id);

CREATE INDEX idx_produto_uni_id
  ON produto (uni_id);

-- =====================================================
-- TABELA: estoque
-- =====================================================
CREATE TABLE estoque (
  estq_id            BIGSERIAL PRIMARY KEY,
  estq_quantidade_atual INTEGER NOT NULL,
  estq_estoque_minimo  INTEGER NOT NULL,
  estq_data_cadastro TIMESTAMP NOT NULL DEFAULT NOW(),

  prod_id            BIGINT NOT NULL,
  uni_id             BIGINT NOT NULL,

  CONSTRAINT uk_estoque_prod_id
    UNIQUE (prod_id),

  CONSTRAINT fk_estoque_produto
    FOREIGN KEY (prod_id)
    REFERENCES produto (prod_id),

  CONSTRAINT fk_estoque_unidade
    FOREIGN KEY (uni_id)
    REFERENCES unidade (uni_id)
);

CREATE INDEX idx_estoque_uni_id
  ON estoque (uni_id);

-- =====================================================
-- TABELA: fornecedor
-- =====================================================
CREATE TABLE fornecedor (
  forn_id            BIGSERIAL PRIMARY KEY,
  forn_nome          VARCHAR(140) NOT NULL,
  forn_telefone      VARCHAR(30),
  forn_email         VARCHAR(180),
  forn_ativo         BOOLEAN NOT NULL DEFAULT TRUE,
  forn_data_cadastro TIMESTAMP NOT NULL DEFAULT NOW(),

  usu_id             BIGINT NOT NULL,
  uni_id             BIGINT NOT NULL,

  CONSTRAINT fk_fornecedor_usuario
    FOREIGN KEY (usu_id)
    REFERENCES usuario (usu_id),

  CONSTRAINT fk_fornecedor_unidade
    FOREIGN KEY (uni_id)
    REFERENCES unidade (uni_id)
);

CREATE INDEX idx_fornecedor_usu_id
  ON fornecedor (usu_id);

CREATE INDEX idx_fornecedor_uni_id
  ON fornecedor (uni_id);

-- =====================================================
-- TABELA: compra
-- =====================================================
CREATE TABLE compra (
  comp_id            BIGSERIAL PRIMARY KEY,
  comp_data_compra   DATE NOT NULL DEFAULT CURRENT_DATE,
  comp_data_cadastro TIMESTAMP NOT NULL DEFAULT NOW(),
  comp_valor_total   NUMERIC(19,2) NOT NULL,
  comp_forma_pagamento VARCHAR(60) NOT NULL,

  forn_id            BIGINT NOT NULL,
  usu_id             BIGINT NOT NULL,
  uni_id             BIGINT NOT NULL,

  CONSTRAINT fk_compra_fornecedor
    FOREIGN KEY (forn_id)
    REFERENCES fornecedor (forn_id),

  CONSTRAINT fk_compra_usuario
    FOREIGN KEY (usu_id)
    REFERENCES usuario (usu_id),

  CONSTRAINT fk_compra_unidade
    FOREIGN KEY (uni_id)
    REFERENCES unidade (uni_id)
);

CREATE INDEX idx_compra_forn_id
  ON compra (forn_id);

CREATE INDEX idx_compra_usu_id
  ON compra (usu_id);

CREATE INDEX idx_compra_uni_id
  ON compra (uni_id);

CREATE INDEX idx_compra_data_compra
  ON compra (comp_data_compra);

-- =====================================================
-- TABELA: venda
-- =====================================================
CREATE TABLE venda (
  vend_id            BIGSERIAL PRIMARY KEY,
  vend_data_venda    DATE NOT NULL DEFAULT CURRENT_DATE,
  vend_data_cadastro TIMESTAMP NOT NULL DEFAULT NOW(),
  vend_valor_total   NUMERIC(19,2) NOT NULL,
  vend_forma_pagamento VARCHAR(60) NOT NULL,

  usu_id             BIGINT NOT NULL,
  uni_id             BIGINT NOT NULL,

  CONSTRAINT fk_venda_usuario
    FOREIGN KEY (usu_id)
    REFERENCES usuario (usu_id),

  CONSTRAINT fk_venda_unidade
    FOREIGN KEY (uni_id)
    REFERENCES unidade (uni_id)
);

CREATE INDEX idx_venda_usu_id
  ON venda (usu_id);

CREATE INDEX idx_venda_uni_id
  ON venda (uni_id);

CREATE INDEX idx_venda_data_venda
  ON venda (vend_data_venda);

-- =====================================================
-- TABELA: movimentacao_estoque
-- =====================================================
CREATE TABLE movimentacao_estoque (
  mvst_id               BIGSERIAL PRIMARY KEY,
  mvst_data_movimentacao DATE NOT NULL DEFAULT CURRENT_DATE,
  mvst_data_cadastro     TIMESTAMP NOT NULL DEFAULT NOW(),
  mvst_tipo_movimentacao VARCHAR(20) NOT NULL,
  mvst_quantidade        INTEGER NOT NULL,
  mvst_observacao        VARCHAR(255),

  estq_id               BIGINT NOT NULL,
  prod_id               BIGINT NOT NULL,
  comp_id               BIGINT,
  vend_id               BIGINT,
  usu_id                BIGINT NOT NULL,
  uni_id                BIGINT NOT NULL,

  CONSTRAINT fk_mov_estoque_estoque
    FOREIGN KEY (estq_id)
    REFERENCES estoque (estq_id),

  CONSTRAINT fk_mov_estoque_produto
    FOREIGN KEY (prod_id)
    REFERENCES produto (prod_id),

  CONSTRAINT fk_mov_estoque_compra
    FOREIGN KEY (comp_id)
    REFERENCES compra (comp_id),

  CONSTRAINT fk_mov_estoque_venda
    FOREIGN KEY (vend_id)
    REFERENCES venda (vend_id),

  CONSTRAINT fk_mov_estoque_usuario
    FOREIGN KEY (usu_id)
    REFERENCES usuario (usu_id),

  CONSTRAINT fk_mov_estoque_unidade
    FOREIGN KEY (uni_id)
    REFERENCES unidade (uni_id)
);

CREATE INDEX idx_mov_estoque_estq_id
  ON movimentacao_estoque (estq_id);

CREATE INDEX idx_mov_estoque_prod_id
  ON movimentacao_estoque (prod_id);

CREATE INDEX idx_mov_estoque_comp_id
  ON movimentacao_estoque (comp_id);

CREATE INDEX idx_mov_estoque_vend_id
  ON movimentacao_estoque (vend_id);

CREATE INDEX idx_mov_estoque_usu_id
  ON movimentacao_estoque (usu_id);

CREATE INDEX idx_mov_estoque_uni_id
  ON movimentacao_estoque (uni_id);

CREATE INDEX idx_mov_estoque_data_mov
  ON movimentacao_estoque (mvst_data_movimentacao);

-- =====================================================
-- TABELA: lancamento_financeiro
-- =====================================================
CREATE TABLE lancamento_financeiro (
  lanf_id              BIGSERIAL PRIMARY KEY,
  lanf_data_lancamento DATE NOT NULL DEFAULT CURRENT_DATE,
  lanf_data_cadastro   TIMESTAMP NOT NULL DEFAULT NOW(),
  lanf_tipo            VARCHAR(20) NOT NULL,
  lanf_valor           NUMERIC(19,2) NOT NULL,
  lanf_descricao       VARCHAR(255) NOT NULL,

  comp_id              BIGINT,
  vend_id              BIGINT,
  usu_id               BIGINT NOT NULL,
  uni_id               BIGINT NOT NULL,

  CONSTRAINT fk_lanf_compra
    FOREIGN KEY (comp_id)
    REFERENCES compra (comp_id),

  CONSTRAINT fk_lanf_venda
    FOREIGN KEY (vend_id)
    REFERENCES venda (vend_id),

  CONSTRAINT fk_lanf_usuario
    FOREIGN KEY (usu_id)
    REFERENCES usuario (usu_id),

  CONSTRAINT fk_lanf_unidade
    FOREIGN KEY (uni_id)
    REFERENCES unidade (uni_id)
);

CREATE INDEX idx_lanf_comp_id
  ON lancamento_financeiro (comp_id);

CREATE INDEX idx_lanf_vend_id
  ON lancamento_financeiro (vend_id);

CREATE INDEX idx_lanf_usu_id
  ON lancamento_financeiro (usu_id);

CREATE INDEX idx_lanf_uni_id
  ON lancamento_financeiro (uni_id);

CREATE INDEX idx_lanf_data_lancamento
  ON lancamento_financeiro (lanf_data_lancamento);
