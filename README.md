# 🛡️ Sistema de Gestão de Usuários

Bem-vindo ao repositório do **Sistema de Gestão de Usuários**. Este projeto é uma aplicação Web Full-Stack (Front-end e Back-end) construída com rigor arquitetural para gerenciar o controle de acesso, autenticação e perfis de usuários em um ambiente seguro.

Se você é um avaliador ou recrutador, este guia foi escrito especialmente para você. Mesmo que não tenha familiaridade prévia com Java, Spring Boot ou React, os passos abaixo o guiarão perfeitamente para rodar a aplicação em sua máquina.

---

## 📌 Índice
- [Links Úteis](#-links-úteis)
- [Sobre o Projeto](#-sobre-o-projeto)
- [Funcionalidades Principais](#-funcionalidades-principais)
- [Tecnologias Utilizadas](#️-tecnologias-utilizadas)
- [Arquitetura](#-arquitetura)
- [Instalação e Execução](#️-instalação-e-execução)
- [Demonstração](#-demonstração)
- [Exemplo de saída no Terminal](#-exemplo-de-saída-no-terminal-para-back-end-api-cli)
- [Autores e Agradecimentos](#️-autores)

---

## 🔗 Links Úteis
* **Repositório GitHub:** [https://github.com/LuizFMoreira/Sistema-de-Gestao-de-Usuarios](https://github.com/LuizFMoreira/Sistema-de-Gestao-de-Usuarios/tree/main)
* **LinkedIn do Desenvolvedor:** [Luiz Fernando Batista Moreira](https://www.linkedin.com/in/luiz-fernando-batista-moreira-987834218/)

---

## 📖 Sobre o Projeto
O sistema visa resolver o desafio técnico de gestão de usuários. Ele fornece uma interface moderna onde pessoas podem se cadastrar, fazer login de forma segura e acessar um painel administrativo (Dashboard). Por baixo dos panos, o sistema utiliza criptografia de ponta para senhas e tokens de sessão (JWT) para garantir que apenas usuários autorizados acessem os dados.

---

## ✨ Funcionalidades Principais
* **Autenticação Segura:** Login de usuários com verificação de credenciais criptografadas.
* **Cadastro com Validação:** Criação de conta com medidor visual de força de senha e recurso mostrar/ocultar senha.
* **Dashboard Privado:** Painel de gestão protegido contra acessos não logados.
* **Gestão do Próprio Perfil:** O usuário logado pode alterar seu nome, e-mail e redefinir a senha.
* **Gestão Administrativa:** Adição de novos usuários diretamente pelo painel.
* **Proteção de Rotas:** Redirecionamento automático e expulsão da sessão caso o token expire ou seja adulterado.

---

## 🛠️ Tecnologias Utilizadas

**Back-end (O "Motor" da aplicação)**
* **Java 21:** Linguagem de programação robusta e orientada a objetos.
* **Spring Boot (v4.0.3):** Framework que facilita a criação da API REST.
* **Spring Security & JWT (Auth0):** Bibliotecas para blindar as rotas e gerar os "crachás" de acesso (Tokens).

**Front-end (A Interface visual)**
* **React com Vite:** Biblioteca JavaScript para criar telas rápidas e dinâmicas.
* **Tailwind CSS:** Framework de estilização para um design moderno e responsivo.
* **Axios:** Cliente HTTP para conectar a interface ao Back-end.

**Banco de Dados (A "Memória")**
* **PostgreSQL:** Banco de dados relacional poderoso e open-source.
* **Hibernate/JPA:** Ferramenta do Java que traduz o código para comandos do banco de dados automaticamente.

---

## 🏗️ Arquitetura
O projeto segue a arquitetura **Cliente-Servidor (Stateless)** e princípios de **Clean Architecture**:
1. O Front-end (Cliente) coleta os dados e envia requisições HTTP (JSON).
2. A API REST em Java (Servidor) recebe, valida as regras de negócio e processa a segurança.
3. O Banco de Dados armazena as informações persistentes. 

Nenhuma sessão fica presa na memória do servidor, tornando a aplicação altamente escalável.

---

## ⚙️ Instalação e Execução

### Pré-requisitos
Para rodar este projeto na sua máquina, você precisará ter instalado:
* **Node.js:** [Baixe aqui](https://nodejs.org/)
* **Java JDK 21:** [Baixe aqui](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
* **PostgreSQL:** [Baixe aqui](https://www.postgresql.org/download/)

### 1. Inicialização do Banco de Dados (PostgreSQL)
Abra a sua ferramenta de banco de dados (pgAdmin ou DBeaver) e crie um banco de dados vazio rodando o comando SQL abaixo:
```sql
CREATE DATABASE gestao_usuarios;
```
Nota: Graças à configuração ddl-auto=update do Spring Boot, você não precisa criar tabelas manualmente. O Java criará as tabelas de usuários assim que a aplicação for iniciada.

O arquivo de configuração do Java (backend/src/main/resources/application.properties) já está pronto e conectará no banco local esperando a seguinte senha:
```
Properties
spring.datasource.url=jdbc:postgresql://localhost:5432/gestao_usuarios
spring.datasource.username=postgres
spring.datasource.password=12345
(Se o seu banco possuir uma senha diferente de 12345, basta alterá-la no arquivo acima antes de rodar).
```
2. Ligando o Back-end (Spring Boot)
Você não precisa ter o Maven instalado. O script empacotador (mvnw) baixará tudo o que o Java precisa automaticamente.

Abra um terminal, entre na pasta backend e inicie o servidor da API:

No Windows:
```
Bash
cd backen
mvnw.cmd spring-boot:run
```
No Mac/Linux:
```
Bash
cd backend
./mvnw spring-boot:run
```
3. Ligando o Front-end (React)
Abra um novo terminal, entre na pasta frontend e rode os comandos abaixo para baixar as bibliotecas e iniciar a interface:
```
Bash
cd frontend
npm install
npm run dev
```
Acesse http://localhost:5173 no seu navegador para utilizar o sistema.

🐳 Execução Alternativa com Docker Compose
(Sessão bônus para execução em contêineres)

Caso possua o Docker instalado, você pode subir o banco de dados sem instalar o PostgreSQL na máquina. Crie um arquivo docker-compose.yml na raiz:

YAML
version: '3.8'
services:
  db:
    image: postgres:latest
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: 12345
      POSTGRES_DB: gestao_usuarios
    ports:
      - "5432:5432"
Basta rodar docker-compose up -d para subir o banco, e em seguida executar os passos 2 e 3 detalhados acima.

📱 Demonstração
Responsividade Mobile: Embora o projeto seja uma Aplicação Web (e não um aplicativo nativo), toda a interface (Tailwind CSS) é 100% responsiva, adaptando menus, formulários e a tabela administrativa para telas de celulares.

Tela de Acesso e Cadastro: Layout limpo com feedback visual em tempo real e medidores de força de senha.

Dashboard: Tabela administrativa para controle geral e botões de ação restritos.

💻 Exemplo de saída no Terminal (para Back-end, API, CLI)
Ao iniciar corretamente, o Spring Boot imprimirá no terminal logs semelhantes a este:
```
Plaintext
  .   ____          _            __ _ _
 /\\ / ___'_ __ _ _(_)_ __  __ _ \ \ \ \
( ( )\___ | '_ | '_| | '_ \/ _` | \ \ \ \
 \\/  ___)| |_)| | | | | || (_| |  ) ) ) )
  '  |____| .__|_| |_|_| |_\__, | / / / /
 =========|_|==============|___/=/_/_/_/
 :: Spring Boot ::                (v3.1.2)

2026-03-17 12:00:00.000  INFO 12345 --- [main] c.l.gestaousuarios.Application         : Starting Application...
2026-03-17 12:00:02.500  INFO 12345 --- [main] o.s.b.w.embedded.tomcat.TomcatWebServer  : Tomcat initialized with port(s): 8081 (http)
2026-03-17 12:00:03.100  INFO 12345 --- [main] c.l.gestaousuarios.Application         : Started Application in 3.5 seconds
```
✍️ Autores
Luiz Fernando Batista Moreira - Desenvolvimento Full-Stack - GitHub
