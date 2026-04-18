# 🔒 Relatório de Auditoria de Segurança — API LaFemme

**Data:** 2026-02-24 (atualizado 2026-04-17)  
**Versão da API:** 4.0.1  
**Padrão:** OWASP Top 10 (2021) + ASVS Nível 2  
**Auditor:** Antigravity Security Audit

---

## 📊 Resumo Executivo

| Criticidade | Quantidade |
|-------------|-----------|
| 🔴 Crítica   | 3         |
| 🟠 Alta      | 6         |
| 🟡 Média     | 8         |
| 🔵 Baixa     | 5         |
| **Total**   | **22**    |

### 🏆 Top 3 Riscos Mais Críticos

1. **Dependências com CVEs conhecidos (JJWT 0.11.5)** — Biblioteca JWT desatualizada com vulnerabilidades conhecidas e APIs depreciadas (OWASP A06)
2. **Senha temporária exposta no Response Body** — Senha em texto claro retornada na resposta HTTP do endpoint de criação de usuário (OWASP A02)
3. **Ausência de autorização por recurso (IDOR)** — Endpoints que recebem IDs não validam se o recurso pertence ao usuário autenticado em todos os casos (OWASP A01)

---

## 📋 Vulnerabilidades Identificadas

---

### #1 — 🔴 CRÍTICA | A06 — Vulnerable and Outdated Components — ✅ CORRIGIDO

**Arquivo:** `pom.xml` — Linhas 23-24, 99-117  
**Componente:** JJWT 0.11.5

```xml
<jjwt.version>0.11.5</jjwt.version>

<dependency>
  <groupId>io.jsonwebtoken</groupId>
  <artifactId>jjwt-api</artifactId>
  <version>${jjwt.version}</version>
</dependency>
```

**Explicação:**  
A versão 0.11.5 do JJWT é de **2022** e utiliza APIs depreciadas (`SignatureAlgorithm.HS512`, `Jwts.builder().setSubject()`, `Jwts.parserBuilder()`). A versão atual é 0.12.6+ que traz correções de segurança e APIs modernas. Além disso, o `pom.xml` declara `<java.version>17</java.version>` mas o `Dockerfile` usa `eclipse-temurin:21`, indicando inconsistência.

**Correção Proposta:**
```xml
<jjwt.version>0.12.6</jjwt.version>
<java.version>21</java.version>
```

E atualizar `JwtTokenProvider.java` para usar a API nova:
```java
// ANTES (depreciado)
Jwts.builder().setSubject(username).signWith(getSigningKey(), SignatureAlgorithm.HS512)
Jwts.parserBuilder().setSigningKey(getSigningKey()).build()

// DEPOIS (0.12.x)
Jwts.builder().subject(username).signWith(getSigningKey(), Jwts.SIG.HS512)
Jwts.parser().verifyWith(getSigningKey()).build()
```

---

### #2 — 🔴 CRÍTICA | A02 — Cryptographic Failures — ✅ CORRIGIDO (senha removida do response, enviada por e-mail)

**Arquivo:** `UserService.java` — Linhas 121-122  
**Método:** `criarNovoUsuario()`

```java
return new CriarNovoUsuarioResponseDTO(email, senhaTemporaria, mensagem);
```

**Explicação:**  
A senha temporária em texto claro é retornada no response body HTTP. Qualquer proxy, logging middleware, WAF ou cache intermediário pode registrar essa senha. A `CriarNovoUsuarioResponseDTO` inclui o campo `senhaTemporaria` que é serializado diretamente como JSON.

**Correção Proposta:**  
A senha temporária deveria ser enviada por um canal seguro separado (e-mail ou notificação) e **nunca** retornada no response body da API. Se não houver sistema de e-mail:

```java
// Opção 1: Retornar senha apenas uma vez, logando a ação
logger.info("Senha temporária gerada para {}. Senha será exibida apenas uma vez.", email);
// Manter retorno mas garantir que headers anti-cache estão presentes

// Opção 2 (preferível): Enviar por e-mail e não retornar
return new CriarNovoUsuarioResponseDTO(email, null, 
    "Senha temporária enviada para o e-mail do usuário.");
```

Adicionar headers anti-cache no endpoint:
```java
return ResponseEntity.ok()
    .header("Cache-Control", "no-store, no-cache, must-revalidate")
    .header("Pragma", "no-cache")
    .body(response);
```

---

### #3 — 🔴 CRÍTICA | A09 — Security Logging and Monitoring Failures — ✅ CORRIGIDO

**Arquivo:** `RegisterUserBO.java` — Linhas 256-257  
**Método:** `extrairIpCliente()`

```java
logger.warn("[PROXY-DEBUG] remoteAddr={} | X-Forwarded-For={} | trustedProxies={} | isTrusted={}",
        remoteAddr, xForwardedFor, trustedProxies, trustedProxies.contains(remoteAddr));
```

**Explicação:**  
O log de debug temporário para diagnóstico de proxy **NÃO FOI REMOVIDO** e está executando em PRODUÇÃO no nível `warn`. Ele expõe em logs:
- IP real do cliente (`remoteAddr`)
- Header `X-Forwarded-For` completo
- Lista de proxies confiáveis
- Status de confiança do IP

Isso é um vazamento de informação sensível que pode facilitar ataques direcionados e violações de privacidade (LGPD/GDPR).

**Correção Proposta:**  
Remover a linha de log imediatamente:

```java
private String extrairIpCliente(jakarta.servlet.http.HttpServletRequest request) {
    String remoteAddr = request.getRemoteAddr();
    // Sem log de debug — informação sensível

    Set<String> trustedProxies = parseTrustedProxies();
    
    if (trustedProxies.contains(remoteAddr)) {
        // ...
    }
```

---

### #4 — 🟠 ALTA | A01 — Broken Access Control (IDOR) — ✅ CORRIGIDO

**Arquivos:** Múltiplos controllers e services  
**Padrão afetado:** Endpoints que recebem IDs numéricos

**Exemplos:**

`FornecedorController.java` — Linha 36:
```java
@GetMapping("/{id}")
public ResponseEntity<?> buscarPorId(@PathVariable Integer id) {
    return ResponseEntity.ok(service.buscarPorId(id));
}
```

`FornecedorService.java` — Linha 92-101:
```java
public Fornecedor buscarPorId(Integer id) {
    try {
        return dao.select()
                .from(Fornecedor.class)
                .join("unidade")
                .where("unidade.id", Condicao.EQUAL, UserContext.getIdUnidade())
                .id(id);
    } catch (Exception e) {
        throw new NotFoundException("Fornecedor não encontrado: " + id);
    }
}
```

**Explicação:**  
Embora os services filtrem por `unidade.id`, o método `id(id)` do `QueryBuilder` (linhas 593-629) faz um `entityManager.find(clazz, pk)` que **IGNORA** o filtro `where("unidade.id", ...)` porque o `id()` não usa a query JPQL construída — ele usa diretamente o `EntityManager.find()`. Isso significa que um atacante pode acessar recursos de **OUTRAS UNIDADES** simplesmente adivinhando IDs sequenciais.

**Impacto:** Um usuário da Unidade A pode ver/editar fornecedores, produtos, categorias e compras da Unidade B.

**Correção Proposta:**  
O método `id()` no `QueryBuilder` deve incorporar os filtros `where` na busca:

```java
@Transactional(readOnly = true)
public <T> T id(Number id) {
    // Em vez de entityManager.find(), usar a query construída com where
    this.where("id", Condicao.EQUAL, (id instanceof Long) ? id : Long.valueOf(id.longValue()));
    try {
        return this.one();
    } catch (Exception e) {
        String entidade = (entityClass != null ? entityClass.getSimpleName() : "Entidade");
        throw new EntityNotFoundException(entidade + " não encontrada para o id " + id);
    }
}
```

---

### #5 — 🟠 ALTA | A05 — Security Misconfiguration — ✅ CORRIGIDO

**Arquivo:** `application.properties` — Linha 5

```properties
server.forward-headers-strategy=framework
```

**Explicação:**  
`server.forward-headers-strategy=framework` faz o Spring confiar nos headers `X-Forwarded-*` para determinar protocolo, host e porta. Se a API estiver exposta diretamente (sem proxy reverso — como confirmado na sessão anterior), **qualquer cliente pode spoofar** esses headers e fazer o Spring pensar que a requisição veio por HTTPS quando foi HTTP, ou modificar o host percebido. Isso pode causar:
- Bypass de `requiresSecure()` 
- URL rewriting incorreto
- SSRF via `X-Forwarded-Host`

**Correção Proposta:**  
Como confirmado que não há proxy reverso:

```properties
# Sem proxy => não confiar em headers forwarded
server.forward-headers-strategy=none
```

---

### #6 — 🟠 ALTA | A01 — Broken Access Control — ✅ CORRIGIDO

**Arquivo:** `FilePreviewController.java` — Todo o controller  
**Endpoint:** `/api/v1/preview/*`

```java
@GetMapping("/produto/{id}/foto")
public ResponseEntity<?> fotoProduto(@PathVariable Long id) throws Exception {
    Produto prod = dao.select()
            .from(Produto.class)
            .id(id);  // SEM filtro por unidade!
    // ...
}
```

**Explicação:**  
O `FilePreviewController` não filtra por unidade do usuário logado. O uso de `.id(id)` sem `.where("unidade.id", ...)` permite que qualquer usuário autenticado acesse fotos de produtos de **qualquer unidade**.

**Correção Proposta:**  
Adicionar filtro de unidade:
```java
Produto prod = dao.select()
        .from(Produto.class)
        .join("unidade")
        .where("unidade.id", Condicao.EQUAL, UserContext.getIdUnidade())
        .id(id);
```

---

### #7 — 🟠 ALTA | A04 — Insecure Design — ✅ CORRIGIDO

**Arquivo:** `UserService.java` — Linhas 72-122  
**Método:** `criarNovoUsuario()`

```java
public CriarNovoUsuarioResponseDTO criarNovoUsuario(CriarNovoUsuarioRequestDTO request) {
    String email = request.email().trim().toLowerCase();
    String nome = request.nome().trim();
    // ... sem validação @Valid no DTO, sem limite de tamanho
```

**Explicação:**  
1. O `CriarNovoUsuarioRequestDTO` **não tem nenhuma validação Bean Validation** (`@NotBlank`, `@Email`, `@Size`). Qualquer string passa.
2. Não há verificação se o **usuário logado tem permissão de admin** para criar outros usuários. O endpoint em `UserController` está em `/api/v1/user` que requer apenas `authenticated()`, sem verificação de role/perfil.
3. A senha temporária usa apenas maiúsculas e números (36 caracteres possíveis, 8 posições = ~2.8 trilhões), o que é razoável mas poderia incluir caracteres especiais.

**Correção Proposta:**
```java
// 1. Adicionar validações ao DTO:
public record CriarNovoUsuarioRequestDTO(
    @NotBlank @Size(min = 2, max = 120) String nome,
    @NotBlank @Email @Size(max = 180) String email
) {}

// 2. Verificar permissão no service:
public CriarNovoUsuarioResponseDTO criarNovoUsuario(CriarNovoUsuarioRequestDTO request) {
    PerfilUsuario perfilLogado = UserContext.getUsuarioAutenticado().getPerfilUsuario();
    if (perfilLogado == null || !"ADMIN".equals(perfilLogado.getNome())) {
        throw new BusinessException("Apenas administradores podem criar usuários.");
    }
    // ...
}
```

---

### #8 — 🟠 ALTA | A02 — Cryptographic Failures — ✅ CORRIGIDO

**Arquivo:** `UserController.java` — Endpoint de criação de usuário  
**Arquivo:** `TrocarSenhaRequestDTO.java`

```java
public record TrocarSenhaRequestDTO(
    String senhaAtual,
    String senhaNova,
    String senhaNovaConfirmacao
) {}
```

**Explicação:**  
O DTO de troca de senha não possui validações Bean Validation, permitindo senhas nulas ou vazias chegarem ao service. Embora o `trocarSenhaObrigatoria()` valide programaticamente, a ausência de `@NotBlank` e `@Size` no DTO é uma defesa em profundidade faltante.

**Correção Proposta:**
```java
public record TrocarSenhaRequestDTO(
    @NotBlank(message = "Senha atual é obrigatória")
    String senhaAtual,
    
    @NotBlank(message = "Nova senha é obrigatória") 
    @Size(min = 8, max = 120, message = "Senha deve ter entre 8 e 120 caracteres")
    String senhaNova,
    
    @NotBlank(message = "Confirmação de senha é obrigatória")
    String senhaNovaConfirmacao
) {}
```

---

### #9 — 🟠 ALTA | A07 — Identification and Authentication Failures

**Arquivo:** `SecurityConfig.java` — Linha 75

```java
.requestMatchers("/api/v1/auth/**").permitAll()
```

**Explicação:**  
O pattern `/api/v1/auth/**` com `permitAll()` abre **todos** os endpoints sob `/api/v1/auth/`, incluindo o `/api/v1/auth/logout`. Isso significa que o endpoint de logout é acessível sem autenticação, o que embora não seja diretamente explorável, é inconsistente. Mais importante: qualquer novo endpoint adicionado sob `/api/v1/auth/` automaticamente será público.

Além disso, o `JwtAuthenticationFilter.isPublicAuthPath()` lista apenas login, register e refresh — mas o `SecurityConfig` permite tudo sob `auth/**`.

**Correção Proposta:**
```java
.requestMatchers(
    "/api/v1/auth/login",
    "/api/v1/auth/register",
    "/api/v1/auth/refresh"
).permitAll()
```

---

### #10 — 🟡 MÉDIA | A05 — Security Misconfiguration

**Arquivo:** `SecurityConfig.java` — Linha 56

```java
.csrf(AbstractHttpConfigurer::disable)
```

**Explicação:**  
CSRF está desabilitado. Em uma API stateless com JWT via cookie, o cookie `SameSite=Strict` já oferece proteção contra CSRF em navegadores modernos. No entanto, `SameSite=Strict` não é suportado por todos os clientes (bots, clientes antigos). Para uma API que usa cookies para autenticação, considere implementar proteção CSRF adicional ou documentar que `SameSite=Strict` é o mecanismo de proteção assumido.

**Correção Proposta:**  
Para APIs stateless com cookie SameSite=Strict, isso é aceitável, porém documente a decisão:
```java
// CSRF desabilitado: proteção via SameSite=Strict no cookie auth_token
// + Stateless API sem formulários HTML server-side
.csrf(AbstractHttpConfigurer::disable)
```

---

### #11 — 🟡 MÉDIA | A05 — Security Misconfiguration — ✅ CORRIGIDO

**Arquivo:** `SecurityConfig.java` — Linhas 93-100

```java
.headers(headers -> headers
    .frameOptions(frame -> frame.deny())
    .contentTypeOptions(Customizer.withDefaults())
    .httpStrictTransportSecurity(hsts -> hsts
        .includeSubDomains(true)
        .maxAgeInSeconds(31536000)
    )
)
```

**Explicação:**  
Faltam os seguintes security headers:
- `Content-Security-Policy` — previne XSS e injeção de conteúdo
- `X-Permitted-Cross-Domain-Policies: none` — previne Adobe/Flash cross-domain
- `Referrer-Policy: strict-origin-when-cross-origin` — limita informação no Referer
- `Permissions-Policy` — limita APIs do navegador

**Correção Proposta:**
```java
.headers(headers -> headers
    .frameOptions(frame -> frame.deny())
    .contentTypeOptions(Customizer.withDefaults())
    .httpStrictTransportSecurity(hsts -> hsts
        .includeSubDomains(true)
        .maxAgeInSeconds(31536000)
        .preload(true)
    )
    .referrerPolicy(ref -> ref.policy(
        org.springframework.security.headers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN
    ))
    .permissionsPolicy(perm -> perm.policy("geolocation=(), camera=(), microphone=()"))
)
```

---

### #12 — 🟡 MÉDIA | A03 — Injection — ✅ CORRIGIDO

**Arquivo:** `ApiResponseWrapperAdvice.java` — Linhas 42-45

```java
private String toJsonString(String s) {
    if (s == null) return "null";
    String escaped = s.replace("\\", "\\\\").replace("\"", "\\\"");
    return "\"" + escaped + "\"";
}
```

**Explicação:**  
O método `toJsonString()` faz escape manual de JSON, mas não escapa:
- Caracteres de controle (`\n`, `\r`, `\t`)
- Barra `/` (escape opicional)
- Caracteres Unicode (`\u0000` - `\u001F`)

Isso pode causar JSON injection se uma mensagem de erro contiver newlines ou caracteres de controle.

**Correção Proposta:**  
Use Jackson ao invés de escape manual:
```java
import com.fasterxml.jackson.databind.ObjectMapper;

private static final ObjectMapper mapper = new ObjectMapper();

private String toJsonString(String s) {
    if (s == null) return "null";
    try {
        return mapper.writeValueAsString(s);
    } catch (Exception e) {
        return "\"erro de serialização\"";
    }
}
```

---

### #13 — 🟡 MÉDIA | A04 — Insecure Design

**Arquivo:** `ProdutoService.java` — Linhas 290-321  
**Método:** `salvarFotoProduto()`

```java
byte[] arquivoBytes = Base64.getDecoder().decode(fotoDTO.arquivo());
```

**Explicação:**  
1. **Sem limite de tamanho:** O Base64 pode ter qualquer tamanho, permitindo upload de arquivos gigantescos que causam OOM (Out of Memory).
2. **Sem validação de MIME type real:** O `mimeType` vem do DTO sem validação. Um atacante pode fazer upload de um executável dizendo que é `image/jpeg`.
3. **Sem validação de conteúdo:** Não há verificação dos magic bytes do arquivo para confirmar que é realmente uma imagem.

**Correção Proposta:**
```java
private void salvarFotoProduto(Produto produto, FotoDTO fotoDTO) {
    if (fotoDTO.arquivo() == null || fotoDTO.arquivo().isBlank()) return;
    
    // Limite de 5MB em Base64 (~6.7MB encoded)
    if (fotoDTO.arquivo().length() > 7_000_000) {
        throw new BusinessException("Foto excede o tamanho máximo de 5MB.");
    }
    
    byte[] arquivoBytes = Base64.getDecoder().decode(fotoDTO.arquivo());
    
    // Validar magic bytes
    String mimeReal = detectarMimeType(arquivoBytes);
    Set<String> MIME_PERMITIDOS = Set.of("image/jpeg", "image/png", "image/webp");
    if (!MIME_PERMITIDOS.contains(mimeReal)) {
        throw new BusinessException("Tipo de arquivo não permitido. Apenas JPEG, PNG e WebP.");
    }
    // ...
}

private String detectarMimeType(byte[] bytes) {
    if (bytes.length >= 3 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8) return "image/jpeg";
    if (bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == 0x50) return "image/png";
    if (bytes.length >= 4 && bytes[0] == 0x52 && bytes[1] == 0x49 
        && bytes[2] == 0x46 && bytes[3] == 0x46) return "image/webp";
    return "unknown";
}
```

---

### #14 — 🟡 MÉDIA | A08 — Software and Data Integrity Failures

**Arquivo:** `Dockerfile` — Linhas 1-21

```dockerfile
FROM maven:3.9.6-eclipse-temurin-21 AS build
# ...
FROM eclipse-temurin:21-jre
```

**Explicação:**  
1. **Imagens sem tag fixa de versão:** `eclipse-temurin:21-jre` aponta para a última versão do JRE 21, que pode mudar sem aviso. Use tags com hash ou versão exata.
2. **Container roda como root:** Não há instrução `USER` para criar/usar um usuário não-root.
3. **Sem `HEALTHCHECK`** definido no Dockerfile.
4. **Sem `.dockerignore`** para evitar copiar arquivos sensíveis.

**Correção Proposta:**
```dockerfile
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q clean package -DskipTests

FROM eclipse-temurin:21.0.5_11-jre
RUN groupadd -r appuser && useradd -r -g appuser appuser
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
RUN chown appuser:appuser /app/app.jar
USER appuser

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
```

---

### #15 — 🟡 MÉDIA | A05 — Security Misconfiguration — ✅ CORRIGIDO

**Arquivo:** `pom.xml` — Linhas 32-37

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-devtools</artifactId>
    <scope>runtime</scope>
    <optional>true</optional>
</dependency>
```

**Explicação:**  
`spring-boot-devtools` inclui funcionalidades como:
- LiveReload server
- Remote debugging
- Restart automático
- Cache desabilitado para templates

Embora tenha `<optional>true</optional>`, se o profile Maven não excluir corretamente a dependência, ela pode acabar no artefato final (JAR de produção).

**Correção Proposta:**  
Verificar que não está no JAR final. Idealmente, mover para um profile Maven:
```xml
<profiles>
    <profile>
        <id>dev</id>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-devtools</artifactId>
                <scope>runtime</scope>
                <optional>true</optional>
            </dependency>
        </dependencies>
    </profile>
</profiles>
```

---

### #16 — 🟡 MÉDIA | A07 — Identification and Authentication Failures

**Arquivo:** `JwtAuthenticationFilter.java` — Linhas 63-68

```java
// 2️⃣ Se já existe auth no contexto, não sobrescreve
if (SecurityContextHolder.getContext().getAuthentication() != null) {
    filterChain.doFilter(request, response);
    return;
}
```

**Explicação:**  
Se por qualquer razão houver uma autenticação residual no `SecurityContext` (thread pool reuse, bug em outro filter), a validação JWT será completamente ignorada. O filter deveria sempre validar o token quando presente.

**Correção Proposta:**
```java
// Sempre valida o JWT quando presente, mesmo se já houver auth no contexto
// (previne problemas de thread pool reuse)
String jwt = getJwtFromRequest(request);
if (!StringUtils.hasText(jwt)) {
    filterChain.doFilter(request, response);
    return;
}
```

---

### #17 — 🟡 MÉDIA | A04 — Insecure Design — ✅ CORRIGIDO

**Arquivo:** `VendaService.java` — Linhas 44-47

```java
BigDecimal valorTotal = dto.valorTotal();
if (valorTotal == null || valorTotal.compareTo(BigDecimal.ZERO) <= 0) {
    valorTotal = produto.getValorVenda().multiply(new BigDecimal(dto.quantidade()));
}
```

**Explicação:**  
O valor total da venda pode ser definido pelo cliente via DTO. Um atacante pode enviar `valorTotal = 0,01` para comprar produtos ao preço que quiser. O valor deveria **sempre** ser calculado no servidor.

**Correção Proposta:**
```java
// NUNCA confiar no valorTotal do cliente
BigDecimal valorTotal = produto.getValorVenda().multiply(new BigDecimal(dto.quantidade()));
```

---

### #18 — 🔵 BAIXA | A05 — Security Misconfiguration

**Arquivo:** `application.properties` — Linha 3

```properties
management.endpoints.web.exposure.include=health
```

**Explicação:**  
Embora apenas o endpoint `health` esteja exposto, as melhores práticas recomendam definir explicitamente a base path do actuator e considerar proteger com autenticação:

```properties
management.server.port=9090  # porta separada
management.endpoints.web.base-path=/internal/actuator
```

---

### #19 — 🔵 BAIXA | A09 — Security Logging and Monitoring Failures

**Arquivo:** `GlobalExceptionHandler.java` — Linhas 49-53

```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
    logWithStack(ex, HttpStatus.INTERNAL_SERVER_ERROR);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(fail("Ops, algo deu errado!"));
}
```

**Explicação:**  
A mensagem genérica "Ops, algo deu errado!" é boa (não expõe detalhes internos), mas falta:
1. Um **correlation ID** para vincular o erro no log com o response do cliente
2. Log de informações de contexto (endpoint, IP, usuário autenticado)

**Correção Proposta:**
```java
@ExceptionHandler(Exception.class)
public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception ex) {
    String correlationId = java.util.UUID.randomUUID().toString().substring(0, 8);
    logger.error("[{}] Erro interno não tratado", correlationId, ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(fail("Erro interno. Referência: " + correlationId));
}
```

---

### #20 — 🔵 BAIXA | A04 — Insecure Design

**Arquivo:** `UserService.java` — Linhas 129-139  
**Método:** `gerarSenhaTemporaria()`

```java
private String gerarSenhaTemporaria() {
    String caracteres = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    StringBuilder senha = new StringBuilder(8);
    for (int i = 0; i < 8; i++) {
        int index = secureRandom.nextInt(caracteres.length());
        senha.append(caracteres.charAt(index));
    }
    return senha.toString();
}
```

**Explicação:**  
Embora use `SecureRandom` (bom), o charset tem apenas 36 caracteres e o comprimento é 8. Entropia: log2(36^8) ≈ 41 bits. Para senhas temporárias, o NIST SP 800-63B recomenda pelo menos 20 bits de entropia para segredos gerados, então está acima do mínimo, mas poderia ser mais forte.

**Correção Proposta (opcional):**
```java
private String gerarSenhaTemporaria() {
    String caracteres = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789!@#$%";
    // Removidos 0, O, I, l, 1 para evitar confusão visual
    StringBuilder senha = new StringBuilder(12);
    for (int i = 0; i < 12; i++) {
        senha.append(caracteres.charAt(secureRandom.nextInt(caracteres.length())));
    }
    return senha.toString();
}
```

---

### #21 — 🔵 BAIXA | A01 — Broken Access Control — ✅ CORRIGIDO

**Arquivo:** `JwtAuthenticationFilter.java` — Linhas 52-55

```java
if ("HEAD".equalsIgnoreCase(method)) {
    response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
    return;
}
```

**Explicação:**  
O bloqueio de HEAD requests no JWT filter retorna 405 **sem executar a filter chain** e **sem headers de segurança**. A resposta não terá HSTS, X-Frame-Options, etc. Este é um edge case, mas é melhor deixar o Spring Security gerenciar métodos permitidos.

**Correção Proposta:**  
Remover do filter e configurar nos controllers com `@RequestMapping(method = {...})` ou no `HttpSecurity`:
```java
// No SecurityConfig:
.authorizeHttpRequests(auth -> auth
    .requestMatchers(HttpMethod.HEAD, "/**").denyAll()
    // ...
)
```

---

### #22 — 🔵 BAIXA | A05 — Security Misconfiguration — ✅ CORRIGIDO

**Arquivo:** `SecurityConfig.java` — Linha 178

```java
config.setAllowedHeaders(List.of("Content-Type", "Accept", "Origin", "X-Requested-With"));
```

**Explicação:**  
A lista de headers CORS permitidos não inclui `Authorization`, embora a API use cookies. Se no futuro a API precisar aceitar tokens via header, isso bloquearia. Mais importante: a **ausência** de headers como `X-Content-Type-Options` na whitelist CORS não é um problema de segurança em si, mas limitar headers expostos é uma boa prática.

**Correção Proposta (informativo):**  
Manter a lista restrita é correto. Apenas documentar que não se usa `Authorization` header porque a autenticação é via cookie.

---

## ✅ Checklist ASVS Nível 2

| Controle | Status | Observação |
|----------|--------|------------|
| V2.1 — Password Security | ✅ | Argon2id com parâmetros adequados |
| V2.2 — General Authenticator | ✅ | Senha temporária via e-mail exclusivamente |
| V2.3 — Authenticator Lifecycle | ✅ | Troca de senha obrigatória no primeiro login |
| V2.4 — Credential Storage | ✅ | Argon2id (salt=16, hash=50, p=2, m=65536, t=5) |
| V2.7 — Session Management | ✅ | JWT via HttpOnly/Secure/SameSite cookie |
| V2.8 — Rate Limiting | ⚠️ | Redis increment + expire separados (race condition) |
| V3.1 — Session Management | ✅ | Stateless com JWT |
| V3.4 — Cookie Security | ✅ | HttpOnly, Secure, SameSite=Strict |
| V4.1 — Access Control | ✅ | IDOR corrigido — QueryBuilder.id() agora usa where-clauses |
| V4.2 — Operation Level Access | ⚠️ | Admin check em criarUsuario ✅, ausente em ConfiguracaoService ❌ |
| V5.1 — Input Validation | ⚠️ | Bean Validation nos DTOs ✅, upload sem validação de tamanho/tipo ❌ |
| V5.2 — Sanitization | ✅ | SecuritySanitizer + QueryBuilder regex |
| V5.3 — Output Encoding | ✅ | Jackson substituiu escape manual |
| V6.1 — Data Classification | ❌ | Tokens MP em plaintext no banco; credenciais no VCS |
| V6.2 — Algorithms | ✅ | HS512 para JWT, Argon2id para senhas |
| V8.1 — Data Protection | ⚠️ | HTTPS forçado, HSTS, mas credenciais commitadas |
| V9.1 — Communication Security | ✅ | TLS obrigatório |
| V10.1 — Code Integrity | ⚠️ | Dockerfile hardened ✅, sem .dockerignore ❌ |
| V14.1 — Build | ✅ | devtools removido do POM |
| V14.2 — Dependency | ✅ | JJWT atualizado para 0.12.6 |

### Legenda:
- ✅ Conforme
- ⚠️ Parcialmente conforme / melhorias necessárias
- ❌ Não conforme / vulnerabilidade identificada

---

## 🔧 Prioridade de Correção

### Imediatas (Sprint atual):
1. ~~**#3** — Remover log PROXY-DEBUG (1 linha)~~ ✅
2. ~~**#4** — Corrigir QueryBuilder.id() para usar where-clauses (IDOR crítico)~~ ✅
3. ~~**#5** — Mudar `server.forward-headers-strategy` para `none`~~ ✅
4. ~~**#9** — Restringir paths públicos em `SecurityConfig`~~ ✅
5. **#28** — 🔴 NOVA — Mover credenciais para application-local.properties + rotacionar
6. **#29** — 🔴 NOVA — Trocar `ddl-auto=update` → `validate`, reativar Flyway
7. **#23** — 🔴 — Remover fallback `buscarPrimeiraConfiguracaoValida` → retornar 400
8. **#30** — 🟠 NOVA — Variável `isPayment` → local no método

### Curto prazo (próxima sprint):
9. ~~**#1** — Atualizar JJWT para 0.12.6~~ ✅
10. ~~**#2** — Remover senha temporária do response body~~ ✅
11. ~~**#7** — Adicionar validação Bean Validation aos DTOs + verificação de admin~~ ✅
12. ~~**#6** — Adicionar filtro de unidade no FilePreviewController~~ ✅
13. **#25** — Encrypt/decrypt nos tokens MP (usar StringEncryptUtils)
14. **#26** — Adicionar verificação de admin no ConfiguracaoService
15. **#24** — Implementar validação de state no OAuth callback
16. **#13** — Validar uploads (tamanho, MIME, magic bytes)
17. ~~**#17** — Calcular valorTotal sempre no servidor~~ ✅
18. **#31** — 🟡 NOVA — Tratar falha de e-mail em criarNovoUsuario

### Médio prazo:
19. ~~**#8** — Adicionar Bean Validation ao TrocarSenhaRequestDTO~~ ✅
20. ~~**#11** — Adicionar security headers faltantes~~ ✅
21. ~~**#12** — Substituir escape manual por Jackson~~ ✅
22. **#14** — Hardening do Dockerfile (criar .dockerignore)
23. ~~**#15** — Mover devtools para profile Maven~~ ✅ (removido do POM)
24. **#16** — Remover verificação de auth existente no filter
25. **#32** — 🟡 NOVA — Substituir e.printStackTrace() por logger

### Monitoramento:
26. **#10** — Documentar decisão sobre CSRF
27. **#18** — Considerar porta separada para actuator
28. **#19** — Adicionar correlation ID nos erros
29. **#20** — Fortalecer senha temporária (opcional)
30. ~~**#21** — Mover bloqueio HEAD para SecurityConfig~~ ✅
31. ~~**#22** — Documentar política de CORS headers~~ ✅

---

## 🛡️ Atualização (2026-03-20) - Varredura Completa e Validação de Achados

### Validação dos Achados Anteriores

1. **Redis rate limiting race condition**
   - **Status:** ❌ AINDA VULNERÁVEL
   - **Evidência:** Em `RegisterUserBO.java`, `incrementarAtomico` chama o Spring Data Redis para incrementar a chave (`increment(key)`) e numa operação separada faz `expire(key)`. Não é atômico (como num Lua script). Um crash entre incremento e expiração deixa a chave presa, resultando em bloqueios eternos indevidos.
2. **IP spoofing via X-Forwarded-For**
   - **Status:** ✅ CORRIGIDO
   - **Evidência:** `extrairIpCliente` varre os proxies aprovados em `app.security.trusted-proxies`. Apenas confia no X-Forwarded-For se a origem primária TCP (`remoteAddr`) coincidir com os IPs do Nginx/LoadBalancer local em deploy real.
3. **Silent account lock bypass**
   - **Status:** ✅ CORRIGIDO
   - **Evidência:** O fluxo de `processarLogin` valida os bloqueios do Redis antes de executar `.authenticate()`. Não há APIs de resgate (reset password) expostas com falha e sem login prévio atuando como token-bypass.
4. **IDOR risk no QueryBuilder multi-tenant**
   - **Status:** ✅ CORRIGIDO
   - **Evidência:** Em `QueryBuilder.java`, o método sintático `.id(Long)` foi refatorado. Agora previne saltos multi-tenant, sendo construído interligado `.where("id", Condicao.EQUAL, pk)`, mantendo integralmente o `where("unidade.id", ...)` declarado nos Services.
5. **Conflito entre dois sistemas de rate limiting (dead code)**
   - **Status:** ✅ CORRIGIDO
   - **Evidência:** Verificação geral por `Filters` desativados concluída com sucesso. Nenhum código paralelo concorre com a restrição explícita do Controller `RegisterUserBO`.

---

### Novos Achados (Avaliação Multi-Tenant, MP & Deploy)

### [#23] 🔴 CRÍTICA | A04 Insecure Design / SSRF Multi-Tenant - Atribuição Global e Aleatória de Webhooks
**Arquivo:** `MercadoPagoWebhookController.java` e `ConfiguracaoService.java`
**Onde/Evidência:** 
Na ausência do parâmetro `data.userId` ou falha de leitura, a aplicação executa o método fallback cego `configuracaoService.buscarPrimeiraConfiguracaoValida()`. Em essência, este busca varrerá todos os dados ignorando inquilinos (`dao.select().from(Configuracao.class).list();`) e atrelará à primeira configurada válida.
**Impacto:** Permite que atacantes ou atrasos lógicos do MP redirecionem e confirmem faturas operacionais (Vendas Concluídas) pagas externamente para ateliês _aleatórios_ ou o primário absoluto em risco isolado global e severo.
**Recomendação:** Destruir de imediato a chamada e método arquitetural `buscarPrimeiraConfiguracaoValida`. Falhas no callback da identificação isolada exigem retorno `400 Bad Request` na ponta externa, jamais redirecionamento aleatório.

---

### [#24] 🟠 ALTA | A04 Insecure Design & CSRF - Webhook OAuth Callback Sem Proteção
**Arquivo:** `MercadoPagoCallbackController.java`
**Onde/Evidência:**
A rota `mp/callback` faz acesso irrestrito (`.permitAll()`). A seguir invoca `configuracaoService.salvarMercadoPagoConfig(tokenResponse)`, solicitando `UserContext.getIdUsuario()`. Entretanto, o estrito formato `SameSite=Strict` dos cookies anulam o tráfego do token em redirects top-level cruzados na jornada do MP, produzindo o colapso e `Error 500`. Ademais, a rota desconsidera verificações reais matemáticas e lógicas do token local contra a _Hash_ injetada no `State` (`state`).
**Impacto:** Impede locatários originais de finalizarem emparelhamentos autênticos. Exponencia riscos do sequestro CSRF, viabilizando que agressores sobreponham carteiras e Tokens de controle e liquidação em suas origens logadas caso explorem o clique do inquilino.
**Recomendação:** Cacheamento e conferência persistente entre Hash da Sessão original de `/mp/autorizar` até o `State Parameter` emitido em retorno. Para resgatar o contexto funcional, aplique `SameSite=Lax` na criação em `TokenService` ou armazene chaves pendentes isoladas pelo `State` e re-hidrate o registro de configuração com base em um mapeamento seguro (em memória).

---

### [#25] 🟠 ALTA | A02 Cryptographic Failures - Armazenamento de Tokens MP em Texto Aberto
**Arquivo:** `Configuracao.java`
**Onde/Evidência:**
As credenciais máximas de cobrança e saque não encontram defesas preventivas no banco relacional.
```java
@Column(name = "conf_mp_access_token", columnDefinition = "TEXT")
private String mpAccessToken;
```
**Impacto:** Risco financeiro drástico entre dezenas de clientes ativos. A quebra mínima do banco através de uma exploração paralela, injeção ou backup vazado abre margem para tomada massiva monetária (transações abertas) dos inquilinos integrados pela falta de blindagem _at-rest_.
**Recomendação:** Reaproveitar a mecânica prévia elaborada no utilitário de Senhas App/E-mails do SaaS, invocando de forma assíncrona o `StringEncryptUtils.encrypt()` e `.decrypt()` nos Getters e Setters dos Refresh e Access Tokens ao se comunicar com a MercadoPago.

---

### [#26] 🟠 ALTA | A01 Broken Access Control - Omissão de Permissão Sistêmica no Painel
**Arquivo:** `ConfiguracaoService.java` (atualizarTipoPagamentoMercadoPago)
**Onde/Evidência:** 
```java
config = dao.select().from(Configuracao.class).where("unidade.id", Condicao.EQUAL, unidade.getId()).one();
```
**Impacto:** Nenhum escrutínio de credencial restrita ("ADMIN"). Um funcionário de baixo prestígio logado legalmente no Ateliê consegue explorar APIs em `Configuracoes` da entidade (ex. alterando o formato ou Pix base da plataforma inteira na Unidade), pois o método não isola sub-entidades da mesa de locatário.
**Recomendação:** Acessos que alteram configurações essenciais unificadas como transições MP devem incluir programativamente e isoladamente avaliações hierárquicas (`"ADMIN".equalsIgnoreCase(usuarioAux.getPerfilUsuario().getNome())`) assim como estabelecido no cadastro.

---

### [#27] 🟡 MÉDIA | A08 Software & Data Integrity Failures - Injeção Base Cativa no Deploy
**Arquivo:** `db/migration/V6__setupTestUser.sql`
**Onde/Evidência:**
A migração imutável empurra dados globais massivos da modelagem de inicialização base contendo nomes e chaves com permissões plenas de Administrador. (`kaiquecgotardo@gmail.com`)
**Impacto:** Backdoors padrão no host. Em deploy isolados da produção, credenciais abertas representam portas cegas para criminosos que assumem as raízes hardcoded do projeto (CWE-259).
**Recomendação:** A migração do perfil e base fixa de Unidades não devem ser efetuadas obrigatoriamente acopladas ao Flyway. Remova e faça o versionamento exclusivo entre esquemas ou desassociando aos `data.sql` dedicados a `@Profile("dev")`. Gere uma Migration subjacente em V14 limpando este usuário caso o SaaS já possua instâncias validades hospedadas.

---

## 🛡️ Atualização (2026-04-17) — Revalidação Completa do Relatório

### Resumo Executivo

- **17 de 27 achados originais confirmados como ✅ CORRIGIDOS**
- **6 achados permanecem ❌ VULNERÁVEIS** (#13, #16, #23, #24, #25, #26)
- **5 novos achados identificados** (2 críticos, 1 alto, 2 médios)
- Total ativo: **17 achados abertos** (5 críticos, 5 altos, 5 médios, 2 baixos)

### Correção de Status — #16 (JWT filter auth check)

> ⚠️ O item #16 foi marcado como ✅ CORRIGIDO nas atualizações anteriores, porém o código **não foi alterado**.

**Evidência atual:** `JwtAuthenticationFilter.java` linhas 73-76 ainda contém:
```java
if (SecurityContextHolder.getContext().getAuthentication() != null) {
    filterChain.doFilter(request, response);
    return;  // ← pula validação JWT se já houver auth no contexto
}
```
**Status corrigido:** ❌ AINDA VULNERÁVEL (risco: thread pool reuse pode preservar auth residual)

---

### Novos Achados

### [#28] 🔴 CRÍTICA | A02 Cryptographic Failures — Credenciais Commitadas em application.properties

**Arquivo:** `application.properties`  
**Violação direta da regra:** *"NUNCA commitar credenciais em nenhum arquivo"*

Credenciais expostas no arquivo rastreado pelo VCS:

| Linha | Tipo | Risco |
|:---:|---|---|
| 12 | API Public Secret | Médio |
| 15-16 | **MP Client ID + Client Secret** | Crítico |
| 38 | **JWT Secret** (128 chars base64) | Crítico |
| 39-41 | **DB URL, username, password** | Crítico |

**Impacto:** Qualquer pessoa com acesso ao repositório (GitLab, backup, clone, fork) obtém acesso completo: capacidade de forjar JWTs, acessar o banco de dados diretamente, e usar credenciais OAuth do Mercado Pago.

**Correção:**
1. Mover **todas** as credenciais para `application-local.properties` (gitignored)
2. Em `application.properties`, manter apenas placeholders:
```properties
api.security.jwt.secret=${JWT_SECRET}
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USER}
spring.datasource.password=${DB_PASS}
mp.oauth.client-secret=${MP_CLIENT_SECRET}
```
3. **Rotacionar imediatamente:** JWT secret, DB password, MP client-secret
4. Executar `git log --all --full-history -- application.properties` para verificar histórico de exposição

---

### [#29] 🔴 CRÍTICA | A05 Security Misconfiguration — `ddl-auto=update` Ativo + Flyway Desabilitado

**Arquivo:** `application.properties` — Linhas 19, 30  
**Violação direta de DUAS regras do projeto:**
- *"NUNCA usar spring.jpa.hibernate.ddl-auto=update ou create"*
- *"SEMPRE usar Flyway para qualquer alteração no schema"*

```properties
spring.flyway.enabled=false           # Flyway DESABILITADO
spring.jpa.hibernate.ddl-auto=update  # Hibernate gerenciando schema livremente
```

**Impacto:**
- Hibernate pode alterar colunas, dropar índices, ou mudar tipos silenciosamente
- Sem histórico auditável de alterações no schema
- Em produção, remover um campo da entidade pode causar `ALTER TABLE DROP COLUMN` acidental
- Deploy inconsistente entre ambientes

**Correção:**
```properties
spring.flyway.enabled=true
spring.jpa.hibernate.ddl-auto=validate
```

---

### [#30] 🟠 ALTA | A04 Insecure Design — Thread-Safety Bug no MercadoPagoWebhookController

**Arquivo:** `MercadoPagoWebhookController.java` — Linha 26

```java
private boolean isPayment = false;  // variável de instância em singleton Spring
```

**Explicação:** Controllers Spring são **singletons por padrão**. A variável `isPayment` como campo de instância é compartilhada entre todas as threads. Se dois webhooks chegarem simultaneamente, um pode sobrescrever o valor do outro, causando:
- Pagamento válido ignorado (isPayment=false sobrescreve isPayment=true)
- Pagamento indevido confirmado

**Correção:** Tornar variável local dentro do método `receiveWebhook()`:
```java
@PostMapping
public ResponseEntity<String> receiveWebhook(...) {
    boolean isPayment = "payment".equals(payload.getType())
        || (payload.getAction() != null && payload.getAction().startsWith("payment."));
    // ...
}
```

---

### [#31] 🟡 MÉDIA | A09 Security Logging and Monitoring Failures — Falha Silenciosa no Envio de E-mail

**Arquivo:** `UserService.java` — Linhas 136-138

```java
} catch (Exception e) {
    /* bora bil */
}
```

**Explicação:** Após a correção do #2, a senha temporária é enviada **exclusivamente** por e-mail e **não é retornada** na resposta HTTP. Se o envio de e-mail falhar silenciosamente, a senha é **perdida para sempre**. O novo usuário fica impossibilitado de acessar o sistema sem intervenção manual, e o administrador não é notificado da falha.

**Correção:**
```java
} catch (Exception e) {
    logger.error("Falha ao enviar credenciais para {}: {}", email, e.getMessage());
    throw new BusinessException(
        "Usuário criado, porém não foi possível enviar o e-mail com as credenciais. "
        + "Verifique as configurações de e-mail e tente novamente.");
}
```

---

### [#32] 🟡 MÉDIA | A09 Security Logging and Monitoring Failures — `e.printStackTrace()` em Produção

**Arquivo:** `ConfiguracaoService.java` — Linhas 258, 319

```java
} catch (Exception e) {
    e.printStackTrace();  // stdout não-estruturado
    return ...;
}
```

**Explicação:** `e.printStackTrace()` escreve stacktraces não-estruturados em `stderr`, que pode não ser capturado pelo logging driver do Docker. Em produção, isso impossibilita monitoramento e detecção de falhas nas configurações do Mercado Pago.

**Correção:**
```java
} catch (Exception e) {
    log.error("Erro ao buscar configuração MP: {}", e.getMessage(), e);
    return ...;
}
```
