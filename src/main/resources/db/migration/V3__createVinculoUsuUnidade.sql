-- =========================
-- V3__vincula_admin_unidade.sql
-- Vincula o usuário admin1@lafemme.com à unidade "Gestão LaFemme"
-- =========================
INSERT INTO usuario_unidade (
  usu_id,
  uni_id,
  usn_data_cadastro
)
SELECT
  u.usu_id,
  un.uni_id,
  NOW()
FROM usuario u
JOIN unidade un
  ON un.uni_nome = 'Gestão LaFemme'
WHERE u.usu_email = 'shamiapbo@gmail.com'
AND NOT EXISTS (
  SELECT 1
  FROM usuario_unidade uu
  WHERE uu.usu_id = u.usu_id
    AND uu.uni_id = un.uni_id
);
