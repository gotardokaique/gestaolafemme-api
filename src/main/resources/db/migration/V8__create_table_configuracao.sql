CREATE TABLE configuracao (
    conf_id SERIAL PRIMARY KEY,
    conf_user_id BIGINT NOT NULL UNIQUE,
    conf_api_token TEXT NOT NULL,
    conf_ativo BOOLEAN DEFAULT true,
    conf_created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    conf_updated_at TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT fk_configuracao_usuario FOREIGN KEY (conf_user_id) REFERENCES usuario(usu_id)
);
