package com.gestao.lafemme.api.services;

import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestao.lafemme.api.bo.EmailBO;
import com.gestao.lafemme.api.utils.StringEncryptUtils;
import com.gestao.lafemme.api.utils.StringUtils;
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
        String email = StringUtils.normalize(request.email());

        if (usuarioRepository.findByEmail(email) != null) {
            throw new IllegalArgumentException("O email informado já está em uso.");
        }

        // 1. Criar Unidade
        Unidade unidade = new Unidade();
        unidade.setNome(request.nomeUnidade());
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

        Usuario novoUsuario = new Usuario(request.nomeUsuario(), email, senhaHash, perfilAdmin);
        novoUsuario.setTrocarSenha(true);
        novoUsuario.setAtivo(true);
        trans.insert(novoUsuario);

        // 4. Vincular Usuario e Unidade
        UsuarioUnidade usuarioUnidade = new UsuarioUnidade();
        usuarioUnidade.setUsuario(novoUsuario);
        usuarioUnidade.setUnidade(unidade);
        trans.insert(usuarioUnidade);

        enviarEmailCredenciais(request.nomeUsuario(), email, senhaTemporaria);

        return new CriarUnidadeResponseDTO(UUID.nameUUIDFromBytes(unidade.getId().toString().getBytes()));
    }

    private void enviarEmailCredenciais(String nome, String email, String senhaTemporaria) {
        String corpoHtml = """
                <table width="100%%" cellpadding="0" cellspacing="0" border="0"
                       style="font-family:'Helvetica Neue',Arial,sans-serif;background:#F0F2F5;padding:40px 16px;">
                  <tr>
                    <td align="center">
                      <table width="600" cellpadding="0" cellspacing="0" border="0"
                             style="max-width:600px;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);">

                        <!-- HEADER -->
                        <tr>
                          <td style="background:#111827;padding:32px 40px;border-radius:16px 16px 0 0;">
                            <table width="100%%" cellpadding="0" cellspacing="0" border="0">
                              <tr>
                                <td>
                                  <span style="color:#FFFFFF;font-size:17px;font-weight:700;letter-spacing:0.3px;">Gestão Lá Femme</span>
                                </td>
                                <td align="right">
                                  <span style="background:rgba(255,255,255,0.12);color:#E5E7EB;font-size:10px;font-weight:600;letter-spacing:1.2px;text-transform:uppercase;padding:5px 12px;border-radius:20px;border:1px solid rgba(255,255,255,0.18);">Novo Acesso</span>
                                </td>
                              </tr>
                            </table>
                          </td>
                        </tr>

                        <!-- ACCENT LINE -->
                        <tr>
                          <td style="height:3px;background:linear-gradient(90deg,#2563EB,#38BDF8);"></td>
                        </tr>

                        <!-- BODY -->
                        <tr>
                          <td style="padding:44px 40px 36px;">

                            <p style="margin:0 0 4px;font-size:12px;color:#9CA3AF;text-transform:uppercase;letter-spacing:1.2px;font-weight:600;">Olá,</p>
                            <h1 style="margin:0 0 20px;font-size:24px;color:#111827;font-weight:700;line-height:1.2;">%s &#128075;</h1>

                            <p style="margin:0 0 32px;font-size:14px;color:#4B5563;line-height:1.75;">
                              Sua conta foi criada com sucesso na plataforma. Utilize as credenciais abaixo para realizar seu primeiro acesso.
                            </p>

                            <!-- CREDENTIALS -->
                            <table width="100%%" cellpadding="0" cellspacing="0" border="0"
                                   style="background:#F8FAFC;border:1px solid #E2E8F0;border-radius:12px;margin-bottom:16px;overflow:hidden;">
                              <tr>
                                <td style="padding:20px 24px;border-bottom:1px solid #E2E8F0;">
                                  <p style="margin:0 0 5px;font-size:11px;color:#9CA3AF;text-transform:uppercase;letter-spacing:1px;font-weight:600;">E-mail de acesso</p>
                                  <p style="margin:0;font-size:15px;color:#111827;font-weight:600;">%s</p>
                                </td>
                              </tr>
                              <tr>
                                <td style="padding:20px 24px;">
                                  <p style="margin:0 0 5px;font-size:11px;color:#9CA3AF;text-transform:uppercase;letter-spacing:1px;font-weight:600;">Senha temporária</p>
                                  <p style="margin:0;font-size:17px;color:#111827;font-weight:700;letter-spacing:4px;font-family:'Courier New',monospace;">%s</p>
                                </td>
                              </tr>
                            </table>

                            <!-- ALERT -->
                            <table width="100%%" cellpadding="0" cellspacing="0" border="0"
                                   style="background:#FFFBEB;border:1px solid #FDE68A;border-radius:12px;margin-bottom:36px;">
                              <tr>
                                <td style="padding:14px 20px;">
                                  <table cellpadding="0" cellspacing="0" border="0">
                                    <tr>
                                      <td style="vertical-align:top;padding-right:10px;font-size:15px;line-height:1;">&#9888;&#65039;</td>
                                      <td>
                                        <p style="margin:0;font-size:13px;color:#92400E;line-height:1.65;">
                                          <strong>Senha temporária.</strong> Você será solicitado a cadastrar uma nova senha no primeiro acesso.
                                        </p>
                                      </td>
                                    </tr>
                                  </table>
                                </td>
                              </tr>
                            </table>

                            <!-- CTA -->
                            <table cellpadding="0" cellspacing="0" border="0">
                              <tr>
                                <td style="background:#111827;border-radius:10px;">
                                  <a href="https://gestaolafemme.com.br/login"
                                     style="display:inline-block;padding:14px 32px;font-size:13px;font-weight:700;color:#ffffff;text-decoration:none;letter-spacing:0.4px;">
                                    Acessar o Sistema &rarr;
                                  </a>
                                </td>
                              </tr>
                            </table>

                          </td>
                        </tr>

                        <!-- DIVIDER -->
                        <tr><td style="height:1px;background:#F3F4F6;"></td></tr>

                        <!-- FOOTER -->
                        <tr>
                          <td style="padding:24px 40px 32px;background:#FAFAFA;border-radius:0 0 16px 16px;">
                            <p style="margin:0 0 4px;font-size:12px;color:#9CA3AF;line-height:1.6;">
                              Este é um e-mail automático. Não responda a esta mensagem.
                            </p>
                            <p style="margin:0;font-size:12px;color:#D1D5DB;">
                              Se você não solicitou este acesso, ignore este e-mail ou entre em contato com o suporte.
                            </p>
                          </td>
                        </tr>

                      </table>
                    </td>
                  </tr>
                </table>
                """
                .formatted(nome, email, senhaTemporaria);

        try {
            Long adminId = UserContext.getIdUsuario();
            emailBO.criar()
                    .remetente(adminId)
                    .destinatario(email)
                    .mensagem("Credenciais de acesso — Sistema de Gestão", corpoHtml)
                    .enviar();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Falha ao enviar e-mail com credenciais. A criação da unidade foi abortada.");
        }
    }
}
