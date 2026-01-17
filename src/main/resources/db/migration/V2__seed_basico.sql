-- V2__seed_basico.sql
-- Seed inicial: perfis + usuario admin

-- 1) Perfis (idempotente por nome)
INSERT INTO perfil_usuario (nome, descricao, ativo)
SELECT 'ADMIN', 'Acesso total ao sistema', TRUE
WHERE NOT EXISTS (
  SELECT 1 FROM perfil_usuario WHERE nome = 'ADMIN'
);

INSERT INTO perfil_usuario (nome, descricao, ativo)
SELECT 'OPERADOR', 'Acesso operacional padrão', TRUE
WHERE NOT EXISTS (
  SELECT 1 FROM perfil_usuario WHERE nome = 'OPERADOR'
);

-- 2) Usuario admin (idempotente por email)
-- Senha BCrypt: admin123
-- Hash gerado (BCrypt) - pode trocar depois
--INSERT INTO usuarios (nome, email, senha, ativo, data_criacao, perfil_usuario_id)
--SELECT
  --'Administrador',
 -- 'admin@lafemme.com',
 -- '$2a$10$9qX8c6q3f9rG8G2s2Jx9Ue5Zl3a9g9O2t9pV8gCqv8bqvYxGqv7qK',
 -- TRUE,
  --CURRENT_DATE,
 -- (SELECT id FROM perfil_usuario WHERE nome = 'ADMIN')
--WHERE NOT EXISTS (
 -- SELECT 1 FROM usuarios WHERE email = 'admin@lafemme.com'
--);
