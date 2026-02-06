-- =========================
-- 1) UNIDADE PADRÃO
-- =========================
INSERT INTO unidade (uni_nome, uni_ativo, uni_data_cadastro)
SELECT 'Gestão LaFemme', TRUE, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM unidade WHERE uni_nome = 'Gestão LaFemme'
);

-- =========================
-- 2) PERFIL ADMIN (COM uni_id)
-- =========================
INSERT INTO perfil_usuario (per_nome, per_descricao, per_ativo, per_data_cadastro, uni_id)
SELECT
  'ADMIN',
  'Perfil administrador do sistema',
  TRUE,
  NOW(),
  (SELECT uni_id FROM unidade WHERE uni_nome = 'Gestão LaFemme')
WHERE NOT EXISTS (
  SELECT 1 FROM perfil_usuario
  WHERE per_nome = 'ADMIN'
    AND uni_id = (SELECT uni_id FROM unidade WHERE uni_nome = 'Gestão LaFemme')
);

-- =========================
-- 3) USUÁRIO ADMIN (ADMIN)
-- =========================
INSERT INTO usuario (
  usu_nome,
  usu_email,
  usu_senha,
  usu_ativo,
  usu_data_criacao,
  perfil_usuario_id
)
SELECT
  'Shamia Andressa',
  'shamiapbo@gmail.com',
  '$argon2id$v=19$m=65536,t=5,p=2$etg/LXOVRuQk/WVU9Lj0bw$6uzcQ665URvEOdzue3aTabk6Am4FmFNqYLvWOCwO2cwtLKFh29OafsMl/X2AW8f254s',
  TRUE,
  NOW(),
  (SELECT per_id
     FROM perfil_usuario
    WHERE per_nome = 'ADMIN'
      AND uni_id = (SELECT uni_id FROM unidade WHERE uni_nome = 'Gestão LaFemme')
    ORDER BY per_id
    LIMIT 1
  )
WHERE NOT EXISTS (
  SELECT 1 FROM usuario WHERE usu_email = 'shamiapbo@gmail.com'
);
