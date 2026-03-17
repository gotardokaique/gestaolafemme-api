ALTER TABLE configuracao ADD COLUMN conf_mp_webhook_secret VARCHAR(255);
ALTER TABLE venda ADD COLUMN vend_mp_preference_id VARCHAR(255);
ALTER TABLE venda ADD COLUMN vend_mp_payment_link VARCHAR(500);
