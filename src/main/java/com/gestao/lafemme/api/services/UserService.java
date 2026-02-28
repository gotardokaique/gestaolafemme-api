package com.gestao.lafemme.api.services;

import java.security.SecureRandom;

import org.springframework.security.crypto.password.PasswordEncoder;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.gestao.lafemme.api.context.UserContext;
import com.gestao.lafemme.api.controllers.dto.CheckTrocarSenhaResponseDTO;
import com.gestao.lafemme.api.controllers.dto.CriarNovoUsuarioRequestDTO;
import com.gestao.lafemme.api.controllers.dto.CriarNovoUsuarioResponseDTO;
import com.gestao.lafemme.api.controllers.dto.TrocarSenhaRequestDTO;
import com.gestao.lafemme.api.controllers.dto.UserMeResponseDTO;
import com.gestao.lafemme.api.controllers.dto.UsuarioUnidadeDTO;
import com.gestao.lafemme.api.db.Condicao;
import com.gestao.lafemme.api.db.DAOController;
import com.gestao.lafemme.api.db.TransactionDB;
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

    public UserService(DAOController dao, TransactionDB trans, 
                      PasswordEncoder passwordEncoder, UsuarioRepository usuarioRepository) {
        this.dao = dao;
        this.trans = trans;
        this.passwordEncoder = passwordEncoder;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public UserMeResponseDTO getMe() {
        try {
            Usuario usuario = dao.select()
                    .from(Usuario.class)
                    .join("perfilUsuario")
                    .id(UserContext.getIdUsuario());

            // Carrega unidade explicitamente na transação atual para evitar LazyInitException com objeto do contexto
            Unidade unidade = dao.select()
                    .from(Unidade.class)
                    .id(UserContext.getIdUnidade());

            usuario.setUnidadeAtiva(unidade);

            return UserMeResponseDTO.from(usuario);
        } catch (Exception e) {
            // Fallback se falhar ao recarregar, tenta usar o do contexto direto
            return UserMeResponseDTO.from(UserContext.getUsuario());
        }
    }

    /**
     * Cria um novo usuário com senha temporária aleatória de 8 dígitos.
     * O usuário criado terá a flag trocarSenha=true, forçando a troca no primeiro login.
     * 
     * @param request DTO contendo nome e email do novo usuário
     * @return DTO com email e senha temporária gerada
     */
    @Transactional
    public CriarNovoUsuarioResponseDTO criarNovoUsuario(CriarNovoUsuarioRequestDTO request) {
        // Verificação de autorização: apenas admins podem criar usuários
        Usuario admin = UserContext.getUsuarioAutenticado();
        PerfilUsuario perfilAdmin = admin.getPerfilUsuario();
        if (perfilAdmin == null || !"ADMIN".equalsIgnoreCase(perfilAdmin.getNome())) {
            throw new BusinessException("Apenas administradores podem criar novos usuários.");
        }

        String email = request.email().trim().toLowerCase();
        String nome = request.nome().trim();
        
        // Validações básicas
        if (email.isEmpty() || nome.isEmpty()) {
            throw new IllegalArgumentException("Nome e email são obrigatórios");
        }
        
        // Verifica se email já existe
        if (usuarioRepository.findByEmail(email) != null) {
            throw new IllegalArgumentException("Email já cadastrado no sistema");
        }
        
        // Gera senha temporária de 8 dígitos
        String senhaTemporaria = gerarSenhaTemporaria();
        String senhaHash = passwordEncoder.encode(senhaTemporaria);
        
        // Obtém a unidade do usuário logado (admin)
        Long unidadeIdAdmin = UserContext.getIdUnidade();
        
        // Busca o perfil padrão (ID 1 - assumindo que é o perfil básico)
        // TODO: Ajustar para buscar perfil correto baseado na lógica de negócio
        PerfilUsuario perfilPadrao = trans.selectById(PerfilUsuario.class, 1L);
        
        if (perfilPadrao == null) {
            throw new IllegalStateException("Perfil padrão não encontrado");
        }
        
        // Cria o novo usuário
        Usuario novoUsuario = new Usuario(nome, email, senhaHash, perfilPadrao);
        novoUsuario.setTrocarSenha(true); // Força troca de senha no primeiro login
        novoUsuario.setAtivo(true);
        
        trans.insert(novoUsuario);
        
        // Vincula o usuário à mesma unidade do admin
        UsuarioUnidade usuarioUnidade = new UsuarioUnidade();
        usuarioUnidade.setUsuario(novoUsuario);
        
        Unidade unidade = trans.selectById(Unidade.class, unidadeIdAdmin);
        usuarioUnidade.setUnidade(unidade);
        
        trans.insert(usuarioUnidade);
        
        String mensagem = "⚠️ IMPORTANTE: Esta senha é temporária e deve ser alterada no primeiro login. " +
                         "Envie estas credenciais de forma segura ao novo usuário.";
        
        return new CriarNovoUsuarioResponseDTO(email, senhaTemporaria, mensagem);
    }
    
    /**
     * Gera uma senha temporária aleatória de 8 dígitos alfanuméricos.
     * 
     * @return String com 8 caracteres aleatórios
     */
    private String gerarSenhaTemporaria() {
        String caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder senha = new StringBuilder(8);
        
        for (int i = 0; i < 8; i++) {
            int index = secureRandom.nextInt(caracteres.length());
            senha.append(caracteres.charAt(index));
        }
        
        return senha.toString();
    }

    /**
     * Verifica se o usuário logado precisa trocar a senha.
     * 
     * @return DTO indicando se precisa trocar senha
     */
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

    /**
     * Troca a senha do usuário logado de forma obrigatória.
     * Valida senha atual, força senha forte e atualiza flag trocarSenha.
     * 
     * @param request DTO com senha atual, nova senha e confirmação
     */
    @Transactional
    public void trocarSenhaObrigatoria(TrocarSenhaRequestDTO request) {
        Long usuarioId = UserContext.getIdUsuario();
        
        // Busca o usuário logado
        Usuario usuario = trans.selectById(Usuario.class, usuarioId);
        
        if (usuario == null) {
            throw new IllegalStateException("Usuário não encontrado");
        }
        
        // Validação 1: Verifica se a senha atual está correta
        if (!passwordEncoder.matches(request.senhaAtual(), usuario.getSenha())) {
            throw new IllegalArgumentException("Senha atual incorreta");
        }
        
        // Validação 2: Verifica se as senhas novas coincidem
        if (!request.senhaNova().equals(request.senhaNovaConfirmacao())) {
            throw new IllegalArgumentException("As senhas novas não coincidem");
        }
        
        // Validação 3: Verifica se a senha nova é forte
        if (!validarSenhaForte(request.senhaNova())) {
            throw new IllegalArgumentException(
                "Senha fraca. A senha deve ter no mínimo 8 caracteres, " +
                "incluindo letras maiúsculas, minúsculas e números"
            );
        }
        
        // Validação 4: Verifica se a senha nova é diferente da atual
        if (passwordEncoder.matches(request.senhaNova(), usuario.getSenha())) {
            throw new IllegalArgumentException("A nova senha deve ser diferente da senha atual");
        }
        
        // Atualiza a senha
        usuario.setSenha(passwordEncoder.encode(request.senhaNova()));
        usuario.setTrocarSenha(false);
        usuario.setSenhaTrocadaEm(new java.util.Date());
        
        trans.update(usuario);
    }

    /**
     * Valida se a senha é forte seguindo os critérios:
     * - Mínimo 8 caracteres
     * - Pelo menos uma letra maiúscula
     * - Pelo menos uma letra minúscula
     * - Pelo menos um número
     * - Não pode ter 3 ou mais caracteres repetidos consecutivos
     * 
     * @param senha Senha a ser validada
     * @return true se a senha é forte, false caso contrário
     */
    private boolean validarSenhaForte(String senha) {
        if (senha == null || senha.length() < 8) {
            return false;
        }

        boolean temMaiuscula = senha.matches(".*[A-Z].*");
        boolean temMinuscula = senha.matches(".*[a-z].*");
        boolean temNumero = senha.matches(".*[0-9].*");

        if (!temMaiuscula || !temMinuscula || !temNumero) {
            return false;
        }

        // Verifica se tem 3 ou mais caracteres repetidos consecutivos
        if (senha.matches(".*(.)\\1{2,}.*")) {
            return false;
        }

        return true;
    }

    /**
     * Lista todos os usuários da mesma unidade do usuário logado.
     * 
     * @return Lista de usuários com informações de perfil
     */
    @Transactional(readOnly = true)
    public java.util.List<UsuarioUnidadeDTO> listarUsuariosDaUnidade() {
        Long unidadeId = UserContext.getIdUnidade();
        
        // Busca todos os vínculos de usuário-unidade da unidade atual
        java.util.List<UsuarioUnidade> vinculos = dao.select()
                .from(UsuarioUnidade.class)
                .join("usuario")
                .join("usuario.perfilUsuario")
                .join("unidade")
                .where("unidade.id", Condicao.EQUAL, unidadeId)
                .list();
        
        // Mapeia para DTO
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
                        usuario.isAtivo()
                    );
                })
                .collect(java.util.stream.Collectors.toList());
    }
}
