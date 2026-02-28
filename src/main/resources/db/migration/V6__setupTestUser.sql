-- =====================================================
-- 1) NOVA UNIDADE DE TESTE
-- =====================================================
INSERT INTO unidade (uni_nome, uni_ativo, uni_data_cadastro)
SELECT 'Teste', TRUE, NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM unidade WHERE uni_nome = 'Teste'
);

-- =====================================================
-- 2) PERFIL ADMIN PARA A UNIDADE TESTE
-- =====================================================
INSERT INTO perfil_usuario (per_nome, per_descricao, per_ativo, per_data_cadastro, uni_id)
SELECT 
  'ADMIN', 
  'Perfil administrador da unidade de teste', 
  TRUE, 
  NOW(), 
  (SELECT uni_id FROM unidade WHERE uni_nome = 'Teste')
WHERE NOT EXISTS (
  SELECT 1 FROM perfil_usuario 
  WHERE per_nome = 'ADMIN' 
    AND uni_id = (SELECT uni_id FROM unidade WHERE uni_nome = 'Teste')
);

INSERT INTO usuario (
  usu_nome, 
  usu_email, 
  usu_senha, 
  usu_ativo, 
  usu_data_criacao, 
  usu_trocarsenha, 
  perfil_usuario_id
)
SELECT 
  'Kaique Gotardo', 
  'kaiquecgotardo@gmail.com', 
  '$argon2id$v=19$m=65536,t=5,p=2$XjN6TndWbXFpMk1YeGllcg$7V5A6Wc6R9nQ4S7rM8M1Yk4L5h2U3i8O9P0Q1R2S3T4', 
  TRUE, 
  NOW(), 
  TRUE, 
  (SELECT per_id 
     FROM perfil_usuario 
    WHERE per_nome = 'ADMIN' 
      AND uni_id = (SELECT uni_id FROM unidade WHERE uni_nome = 'Teste')
  )
WHERE NOT EXISTS (
  SELECT 1 FROM usuario WHERE usu_email = 'kaiquecgotardo@gmail.com'
);

-- =====================================================
-- 4) VÍNCULO DO USUÁRIO COM A UNIDADE
-- =====================================================
INSERT INTO usuario_unidade (usu_id, uni_id, usn_data_cadastro)
SELECT 
  (SELECT usu_id FROM usuario WHERE usu_email = 'kaiquecgotardo@gmail.com'),
  (SELECT uni_id FROM unidade WHERE uni_nome = 'Teste'),
  NOW()
WHERE NOT EXISTS (
  SELECT 1 FROM usuario_unidade 
  WHERE usu_id = (SELECT usu_id FROM usuario WHERE usu_email = 'kaiquecgotardo@gmail.com')
    AND uni_id = (SELECT uni_id FROM unidade WHERE uni_nome = 'Teste')
);
