package com.gestao.lafemme.api.services;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestao.lafemme.api.bo.EmailBO;
import com.gestao.lafemme.api.utils.StringEncryptUtils;
import com.gestao.lafemme.api.context.UserContext;
import com.gestao.lafemme.api.controllers.dto.CriarUnidadeRequestDTO;
import com.gestao.lafemme.api.controllers.dto.CriarUnidadeResponseDTO;
import com.gestao.lafemme.api.db.TransactionDB;
import com.gestao.lafemme.api.entity.PerfilUsuario;
import com.gestao.lafemme.api.entity.Unidade;
import com.gestao.lafemme.api.entity.Usuario;
import com.gestao.lafemme.api.entity.UsuarioUnidade;
import com.gestao.lafemme.api.security.controller.UsuarioRepository;

@Service
public class AdminUnitService {

    private final TransactionDB trans;
    private final PasswordEncoder passwordEncoder;
    private final UsuarioRepository usuarioRepository;
    private final EmailBO emailBO;

    public AdminUnitService(TransactionDB trans, PasswordEncoder passwordEncoder,
            UsuarioRepository usuarioRepository, EmailBO emailBO) {
        this.trans = trans;
        this.passwordEncoder = passwordEncoder;
        this.usuarioRepository = usuarioRepository;
        this.emailBO = emailBO;
    }

    @Transactional
    public CriarUnidadeResponseDTO criarUnidade(CriarUnidadeRequestDTO request) {
        String email = request.email().trim().toLowerCase();

        if (usuarioRepository.findByEmail(email) != null) {
            throw new IllegalArgumentException("O email informado já está em uso.");
        }

        // 1. Criar Unidade
        Unidade unidade = new Unidade();
        unidade.setNome(request.nome().trim());
        unidade.setAtivo(true);
        trans.insert(unidade);

        // 2. Criar PerfilUsuario ADMIN para a Unidade
        PerfilUsuario perfilAdmin = new PerfilUsuario();
        perfilAdmin.setNome("ADMIN");
        perfilAdmin.setDescricao("Perfil administrador da unidade");
        perfilAdmin.setAtivo(true);
        perfilAdmin.setUnidade(unidade);

        trans.insert(perfilAdmin);

        // 3. Criar Usuário Admin associado ao Perfil
        String senhaTemporaria = StringEncryptUtils.generateRandomToken(10);
        String senhaHash = passwordEncoder.encode(senhaTemporaria);

        Usuario novoUsuario = new Usuario(request.nome().trim(), email, senhaHash, perfilAdmin);
        novoUsuario.setTrocarSenha(true);
        novoUsuario.setAtivo(true);
        trans.insert(novoUsuario);

        // 4. Vincular Usuario e Unidade
        UsuarioUnidade usuarioUnidade = new UsuarioUnidade();
        usuarioUnidade.setUsuario(novoUsuario);
        usuarioUnidade.setUnidade(unidade);
        trans.insert(usuarioUnidade);

        // 5. Enviar Email
        enviarEmailCredenciais(request.nome().trim(), email, senhaTemporaria, request.plano());

        // Retorna UUID determinístico baseado no ID para evitar modificações em banco
        // via Migration
        return new CriarUnidadeResponseDTO(UUID.nameUUIDFromBytes(unidade.getId().toString().getBytes()));
    }

    private void enviarEmailCredenciais(String nome, String email, String senhaTemporaria, String plano) {
        String corpoHtml = """
                <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 32px; background: #fff;">
                    <div style="text-align: center; margin-bottom: 32px;">
                        <p style="color: #999; font-size: 13px; margin: 4px 0 0;">Sistema de Gestão</p>
                    </div>
                    <h2 style="color: #333; font-size: 20px;">Bem-vinda(o), <strong>%s</strong>! 🎉</h2>
                    <p style="color: #555; line-height: 1.6;">Sua nova unidade com o plano <strong>%s</strong> foi criada com sucesso. Use as credenciais abaixo para acessar o sistema:</p>
                    <div style="background: #fdf4f8; border-left: 4px solid #b5477a; padding: 16px 20px; border-radius: 6px; margin: 24px 0;">
                        <p style="margin: 0 0 8px;"><span style="color: #888; font-size: 12px; text-transform: uppercase;">E-mail</span><br><strong style="color: #333;">%s</strong></p>
                        <p style="margin: 0;"><span style="color: #888; font-size: 12px; text-transform: uppercase;">Senha temporária</span><br><strong style="color: #333; letter-spacing: 2px;">%s</strong></p>
                    </div>
                    <div style="background: #fff8e1; border: 1px solid #ffe082; padding: 12px 16px; border-radius: 6px; margin-bottom: 24px;">
                        <p style="margin: 0; color: #795548; font-size: 13px;">⚠️ <strong>Senha temporária.</strong> Você será solicitada(o) a alterá-la no primeiro login.</p>
                    </div>
                </div>
                """
                .formatted(nome, plano, email, senhaTemporaria);

        try {
            Long adminId = UserContext.getIdUsuario();
            emailBO.criar()
                    .remetente(adminId)
                    .destinatario(email)
                    .mensagem("Sua nova unidade - Sistema de Gestão", corpoHtml)
                    .enviar();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Falha ao enviar e-mail com credenciais. A criação da unidade foi abortada.");
        }
    }
}
