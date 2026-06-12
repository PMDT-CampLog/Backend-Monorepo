# CampLog - Backend API

Esta é a API central de autenticação e gerenciamento de usuários do ecossistema **CampLog**, desenvolvida em **Java 17** com **Spring Boot 3**.

---

## 🛠️ Tecnologias Utilizadas

- **Framework**: Spring Boot 3.2.5
- **Segurança**: Spring Security & Stateless JWT (JJWT)
- **Persistência**: Spring Data JPA & Hibernate
- **Banco de Dados**: PostgreSQL
- **Orquestração de Eventos**: ApplicationEventPublisher (Event-driven assíncrono para e-mails e telemetria)
- **Monitoramento**: Spring Boot Actuator & Prometheus

---

## 🔑 Fluxo de Autenticação OAuth2 (Simulado)

Para facilitar o desenvolvimento e testes locais sem a necessidade de chaves e credenciais reais do Google ou GitHub, o backend expõe uma simulação ponta a ponta do fluxo OAuth2:

1. **Início do Fluxo**:
   O frontend redireciona o usuário para o endpoint `/oauth2/authorization/{provider}` (onde `{provider}` é `google` ou `github`).
2. **Redirecionamento Simulado**:
   O controlador `OAuth2RedirectController` intercepta a chamada de forma pública e responde com um redirecionamento HTTP 302 de volta para a rota de callback do frontend (`http://localhost:3000/oauth2/callback`), adicionando os parâmetros `provider` e um `code` de autorização mockado.
3. **Validação do Callback**:
   O frontend recebe os dados e faz uma requisição HTTP `POST /api/v1/auth/oauth2/callback/{provider}` enviando o código mockado.
4. **Verificação & Fallback**:
   Na camada de serviço (`AuthService`), se o provedor for Google ou GitHub, a API resolve o código mockado usando fallbacks locais. Ela gera um perfil fictício baseado no hash do código e registra/efetua o login do usuário retornando um token JWT.

---

## ⚙️ Configurações Importantes (`application.yml`)

- **Porta padrão**: `3333`
- **Banco de Dados**: Configurado para se conectar ao PostgreSQL local via `DATABASE_URL` (porta `5432`).
- **Segurança**:
  - `app.jwt.secret`: Chave de assinatura HS256 para o JWT.
  - `app.jwt.expiration`: Expiração do token (padrão 24h).

---

## 🚀 Como Executar

### Pré-requisitos
- JDK 17+ instalado
- Maven instalado
- Banco de dados PostgreSQL ativo (porta 5432, banco `camplog`, usuário/senha `postgres`/`postgres` por padrão)

### Comandos de Build e Inicialização

```bash
# Compilar o projeto e rodar testes unitários
mvn clean install

# Executar a aplicação
mvn spring-boot:run
```
