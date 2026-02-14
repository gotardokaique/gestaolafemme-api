# 💎 Gestão LaFemme API

API REST desenvolvida com **Spring Boot 3 + Java 21** para gerenciamento completo de produtos, estoque, vendas, fornecedores, usuários e controle operacional, com autenticação JWT, arquitetura modular e suporte a multi-unidade.

---

## 🚀 Visão Geral

A Gestão LaFemme API é uma solução backend robusta e escalável para sistemas de gestão comercial e operacional, oferecendo controle estruturado de dados, segurança e organização modular.

Principais capacidades:

- 🔐 Autenticação e autorização via JWT
- 👤 Gestão de usuários e perfis de acesso
- 🏬 Suporte a múltiplas unidades (multi-tenant)
- 📦 Cadastro e gerenciamento de produtos
- 🗂 Organização por categorias
- 📊 Controle de estoque
- 🔄 Movimentações de entrada e saída
- 🧾 Registro estruturado de vendas
- 🤝 Gestão de fornecedores
- 📈 Estrutura preparada para dashboards e relatórios
- ⚡ Arquitetura preparada para escalabilidade

---

## 🏗 Stack Tecnológica

- ☕ Java 21  
- 🌱 Spring Boot 3  
- 🔐 Spring Security  
- 🔑 JWT (Bearer Token)  
- 🐘 PostgreSQL  
- ⚡ Redis  
- 🛠 Flyway (versionamento de banco de dados)  
- 📦 Maven  
- 🐳 Docker / Docker Compose  

---

## 🧠 Arquitetura

A aplicação segue princípios de:

- Separação clara entre Controller, Service e camada de acesso a dados
- QueryBuilder customizado para consultas dinâmicas
- Multi-tenant baseado no contexto do usuário autenticado
- Uso de DTOs para proteção de entidades
- Tratamento global de exceções
- Estrutura modular e organizada para evolução contínua

---

## 📂 Estrutura do Projeto

```
src/main/java/com/gestao/lafemme/api
│
├── config/         # Configurações gerais
├── controllers/    # Endpoints REST
├── services/       # Regras de negócio
├── db/             # DAOController, QueryBuilder, TransactionDB
├── entities/       # Entidades JPA
├── dtos/           # Objetos de transferência
├── security/       # Filtros JWT e autenticação
├── enums/          # Enumerações
└── exceptions/     # Tratamento global de erros
```

## 📦 Principais Domínios

### 👤 Usuários
- Cadastro e autenticação
- Controle de permissões
- Associação à unidade

### 📦 Produtos
- Cadastro completo
- Status ativo/inativo
- Associação a categorias
- Filtros dinâmicos

### 📊 Estoque
- Controle de quantidade
- Movimentações registradas
- Histórico por produto

### 🧾 Vendas
- Registro estruturado
- Associação a múltiplos produtos
- Atualização automática de estoque

### 🤝 Fornecedores
- Cadastro e manutenção
- Status ativo/inativo
- Filtros personalizados

---

## 🛠 Configuração Local

### 1️⃣ Criar banco

```sql
CREATE DATABASE gestao_lafemme;
```

### 2️⃣ Configurar `application.properties`

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/gestao_lafemme
spring.datasource.username=postgres
spring.datasource.password=senha

spring.jpa.hibernate.ddl-auto=validate
spring.flyway.enabled=true

spring.redis.host=localhost
spring.redis.port=6379

jwt.secret=SUA_CHAVE_SECRETA
jwt.expiration=86400000
```

## 🔒 Segurança

- Autenticação baseada em JWT
- Isolamento por unidade (multi-tenant)
- Uso de DTOs para evitar exposição de entidades
- Tratamento global de exceções
- Estrutura preparada para CORS e HTTPS

---

## 📈 Escalabilidade

A arquitetura permite:

- Integração com gateways de pagamento
- Ampliação de relatórios e métricas
- Evolução modular
- Integração com front-end React / Next.js
- Estratégias de cache com Redis

---

## 📜 License

Proprietary – All Rights Reserved.

This software is publicly visible for evaluation and portfolio purposes only.  
Unauthorized use, modification, distribution, or deployment is strictly prohibited.

See the `LICENSE` file for full legal terms.
