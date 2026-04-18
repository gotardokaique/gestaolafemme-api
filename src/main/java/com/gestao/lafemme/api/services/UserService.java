package com.gestao.lafemme.api.services;

import java.security.SecureRandom;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestao.lafemme.api.bo.EmailBO;
import com.gen.core.context.UserContext;
import com.gestao.lafemme.api.context.ApiUserContext;
import com.gestao.lafemme.api.controllers.dto.CheckTrocarSenhaResponseDTO;
import com.gestao.lafemme.api.controllers.dto.CriarNovoUsuarioRequestDTO;
import com.gestao.lafemme.api.controllers.dto.CriarNovoUsuarioResponseDTO;
import com.gestao.lafemme.api.controllers.dto.TrocarSenhaRequestDTO;
import com.gestao.lafemme.api.controllers.dto.UserMeResponseDTO;
import com.gestao.lafemme.api.controllers.dto.UsuarioUnidadeDTO;
import com.gen.core.db.Condicao;
import com.gen.core.db.DAOController;
import com.gen.core.db.TransactionDB;
import com.gestao.lafemme.api.entity.PerfilUsuario;
import com.gestao.lafemme.api.entity.Unidade;
import com.gestao.lafemme.api.entity.Usuario;
import com.gestao.lafemme.api.entity.UsuarioUnidade;
import com.gestao.lafemme.api.security.controller.UsuarioRepository;
import com.gestao.lafemme.api.services.exceptions.BusinessException;

@Service
public class UserService {

    private final DAOController dao;
    private final TransactionDB trans;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioRepository usuarioRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final EmailBO emailBO;

    public UserService(DAOController dao, TransactionDB trans,
            PasswordEncoder passwordEncoder, UsuarioRepository usuarioRepository, EmailBO emailBO) {
        this.dao = dao;
        this.trans = trans;
        this.passwordEncoder = passwordEncoder;
        this.usuarioRepository = usuarioRepository;
        this.emailBO = emailBO;
    }

    @Transactional(readOnly = true)
    public UserMeResponseDTO getMe() {
        try {
            Usuario usuario = dao.select()
                    .from(Usuario.class)
                    .join("perfilUsuario")
                    .id(UserContext.getIdUsuario());

            // Carrega unidade explicitamente na transação atual para evitar
            // LazyInitException com objeto do contexto
            Unidade unidade = dao.select()
                    .from(Unidade.class)
                    .id(UserContext.getIdUnidade());

            usuario.setUnidadeAtiva(unidade);

            return UserMeResponseDTO.from(usuario);
        } catch (Exception e) {
            return UserMeResponseDTO.from(ApiUserContext.getUsuario());
        }
    }

    @Transactional
    public CriarNovoUsuarioResponseDTO criarNovoUsuario(CriarNovoUsuarioRequestDTO request) {
        Usuario admin = trans.selectById(Usuario.class, UserContext.getIdUsuario());
        PerfilUsuario perfilAdmin = admin.getPerfilUsuario();
        if (perfilAdmin == null || !"ADMIN".equalsIgnoreCase(perfilAdmin.getNome())) {
            throw new BusinessException("Apenas administradores podem criar novos usuários.");
        }

        String email = request.email().trim().toLowerCase();
        String nome = request.nome().trim();

        if (email.isEmpty() || nome.isEmpty()) {
            throw new IllegalArgumentException("Nome e email são obrigatórios");
        }

        if (usuarioRepository.findByEmail(email) != null) {
            throw new IllegalArgumentException("Email já cadastrado no sistema");
        }

        String senhaTemporaria = gerarSenhaTemporaria();
        String senhaHash = passwordEncoder.encode(senhaTemporaria);

        Long unidadeIdAdmin = UserContext.getIdUnidade();

        PerfilUsuario perfilPadrao = trans.selectById(PerfilUsuario.class, 1L);
        if (perfilPadrao == null) {
            throw new IllegalStateException("Perfil padrão não encontrado");
        }

        Usuario novoUsuario = new Usuario(nome, email, senhaHash, perfilPadrao);
        novoUsuario.setTrocarSenha(true);
        novoUsuario.setAtivo(true);
        trans.insert(novoUsuario);

        UsuarioUnidade usuarioUnidade = new UsuarioUnidade();
        usuarioUnidade.setUsuario(novoUsuario);
        Unidade unidade = trans.selectById(Unidade.class, unidadeIdAdmin);
        usuarioUnidade.setUnidade(unidade);
        trans.insert(usuarioUnidade);

        String corpoHtml = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 32px; background: #fff;">
                    <div style="text-align: center; margin-bottom: 32px;">
                        <p style="color: #999; font-size: 13px; margin: 4px 0 0;">Sistema de Gestão</p>
                    </div>
                    <h2 style="color: #333; font-size: 20px;">Bem-vinda(o), <strong>%s</strong>! 🎉</h2>
                    <p style="color: #555; line-height: 1.6;">Sua conta foi criada com sucesso. Use as credenciais abaixo para acessar o sistema pelo primeiro acesso:</p>
                    <div style="background: #fdf4f8; border-left: 4px solid #b5477a; padding: 16px 20px; border-radius: 6px; margin: 24px 0;">
                        <p style="margin: 0 0 8px;"><span style="color: #888; font-size: 12px; text-transform: uppercase;">E-mail</span><br><strong style="color: #333;">%s</strong></p>
                        <p style="margin: 0;"><span style="color: #888; font-size: 12px; text-transform: uppercase;">Senha temporária</span><br><strong style="color: #333; letter-spacing: 2px;">%s</strong></p>
                    </div>
                    <div style="background: #fff8e1; border: 1px solid #ffe082; padding: 12px 16px; border-radius: 6px; margin-bottom: 24px;">
                        <p style="margin: 0; color: #795548; font-size: 13px;">⚠️ <strong>Senha temporária.</strong> Você será solicitada(o) a alterá-la no primeiro login.</p>
                    </div>
                    <p style="color: #aaa; font-size: 12px; text-align: center; margin-top: 32px; border-top: 1px solid #eee; padding-top: 16px;">
                        Este é um e-mail automático. Caso não reconheça esta mensagem ou não saiba o que é, ignore-o.
                    </p>
                </div>
                """
                .formatted(nome, email, senhaTemporaria);

        try {
            emailBO.criar()
                    .remetente(admin.getId())
                    .destinatario(email)
                    .mensagem("Suas credenciais de acesso - Lá Femme", corpoHtml)
                    .enviar();

        } catch (Exception e) {
            throw new BusinessException("Erro ao enviar e-mail. Contate o dev.");
        }

        return new CriarNovoUsuarioResponseDTO(email);
    }

    private String gerarSenhaTemporaria() {
        String caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder senha = new StringBuilder(16);

        for (int i = 0; i < 8; i++) {
            int index = secureRandom.nextInt(caracteres.length());
            senha.append(caracteres.charAt(index));
        }

        return senha.toString();
    }

    @Transactional(readOnly = true)
    public CheckTrocarSenhaResponseDTO checkTrocarSenha() {
        Long usuarioId = UserContext.getIdUsuario();

        Usuario usuario = dao.select()
                .from(Usuario.class)
                .id(usuarioId);

        if (usuario == null) {
            throw new IllegalStateException("Usuário não encontrado");
        }

        return new CheckTrocarSenhaResponseDTO(usuario.isTrocarSenha());
    }

    @Transactional
    public void trocarSenhaObrigatoria(TrocarSenhaRequestDTO request) {
        Long usuarioId = UserContext.getIdUsuario();
        Usuario usuario = trans.selectById(Usuario.class, usuarioId);

        if (usuario == null) {
            throw new IllegalStateException("Usuário não encontrado");
        }

        if (passwordEncoder.matches(request.senhaAtual(), usuario.getSenha()) == false) {
            throw new IllegalArgumentException("Senha atual incorreta");
        }

        if (request.senhaNova().equals(request.senhaNovaConfirmacao()) == false) {
            throw new IllegalArgumentException("As senhas novas não coincidem");
        }

        if (validarSenhaForte(request.senhaNova()) == false) {
            throw new IllegalArgumentException(
                    "Senha fraca. A senha deve ter no mínimo 8 caracteres, " +
                            "incluindo letras maiúsculas, minúsculas e números");
        }

        if (passwordEncoder.matches(request.senhaNova(), usuario.getSenha())) {
            throw new IllegalArgumentException("A nova senha deve ser diferente da senha atual");
        }

        usuario.setSenha(passwordEncoder.encode(request.senhaNova()));
        usuario.setTrocarSenha(false);
        usuario.setSenhaTrocadaEm(new java.util.Date());

        trans.update(usuario);
    }

    private boolean validarSenhaForte(String senha) {
        if (senha == null || senha.length() < 8) {
            return false;
        }

        boolean temMaiuscula = senha.matches(".*[A-Z].*");
        boolean temMinuscula = senha.matches(".*[a-z].*");
        boolean temNumero = senha.matches(".*[0-9].*");

        if (temMaiuscula == false || temMinuscula == false || temNumero == false) {
            return false;
        }

        // Verifica se tem 3 ou mais caracteres repetidos consecutivos
        if (senha.matches(".*(.)\\1{2,}.*")) {
            return false;
        }

        return true;
    }

    @Transactional(readOnly = true)
    public java.util.List<UsuarioUnidadeDTO> listarUsuariosDaUnidade() {
        Long unidadeId = UserContext.getIdUnidade();

        java.util.List<UsuarioUnidade> vinculos = dao.select()
                .from(UsuarioUnidade.class)
                .join("usuario")
                .join("usuario.perfilUsuario")
                .join("unidade")
                .where("unidade.id", Condicao.EQUAL, unidadeId)
                .list();

        return vinculos.stream()
                .map(vinculo -> {
                    Usuario usuario = vinculo.getUsuario();
                    PerfilUsuario perfil = usuario.getPerfilUsuario();

                    return new UsuarioUnidadeDTO(
                            usuario.getId(),
                            usuario.getNome(),
                            usuario.getEmail(),
                            usuario.getDataCriacao(),
                            perfil != null ? perfil.getNome() : "Sem perfil",
                            perfil != null ? perfil.getDescricao() : "",
                            usuario.isAtivo());
                })
                .collect(java.util.stream.Collectors.toList());
    }
}
