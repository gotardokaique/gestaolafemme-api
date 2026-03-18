ALTER TABLE configuracao
ADD COLUMN conf_tipo_pagamento_mp VARCHAR(20) NOT NULL DEFAULT 'CHECKOUT';

ALTER TABLE venda
ADD COLUMN vend_mp_qr_code VARCHAR(1000),
ADD COLUMN vend_mp_qr_code_base64 TEXT;
