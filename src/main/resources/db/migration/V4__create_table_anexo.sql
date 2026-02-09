-- V4__create_table_anexo.sql

CREATE TABLE anexo (
    anex_id BIGSERIAL PRIMARY KEY,
    anex_data_cadastro TIMESTAMP NOT NULL,
    anex_tipo VARCHAR(40) NOT NULL,
    anex_nome VARCHAR(160) NOT NULL,
    anex_mime_type VARCHAR(120) NOT NULL,
    anex_tamanho_bytes BIGINT NOT NULL,
    anex_arquivo BYTEA NOT NULL,

    usu_id BIGINT,
    prod_id BIGINT,
    uni_id BIGINT,

    CONSTRAINT fk_anexo_usuario
        FOREIGN KEY (usu_id)
        REFERENCES usuario (usu_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_anexo_produto
        FOREIGN KEY (prod_id)
        REFERENCES produto (prod_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_anexo_unidade
        FOREIGN KEY (uni_id)
        REFERENCES unidade (uni_id)
        ON DELETE CASCADE,

    CONSTRAINT ck_anexo_dono_unico CHECK (
        (CASE WHEN usu_id IS NOT NULL THEN 1 ELSE 0 END) +
        (CASE WHEN prod_id IS NOT NULL THEN 1 ELSE 0 END) +
        (CASE WHEN uni_id IS NOT NULL THEN 1 ELSE 0 END)
        = 1
    )
);

CREATE INDEX idx_anexo_usuario ON anexo (usu_id);
CREATE INDEX idx_anexo_produto ON anexo (prod_id);
CREATE INDEX idx_anexo_unidade ON anexo (uni_id);
CREATE INDEX idx_anexo_tipo ON anexo (anex_tipo);
