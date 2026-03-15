ALTER TABLE configuracao ADD COLUMN uni_id BIGINT;
ALTER TABLE configuracao ADD COLUMN conf_mp_access_token TEXT;
ALTER TABLE configuracao ADD COLUMN conf_mp_refresh_token TEXT;
ALTER TABLE configuracao ADD COLUMN conf_mp_public_key VARCHAR(255);
ALTER TABLE configuracao ADD COLUMN conf_mp_user_id VARCHAR(255);
ALTER TABLE configuracao ADD COLUMN conf_mp_expires_at TIMESTAMP;

ALTER TABLE configuracao ADD CONSTRAINT fk_configuracao_unidade FOREIGN KEY (uni_id) REFERENCES unidade(uni_id);
