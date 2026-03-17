# 🛡️ Sistema de Gestão de Usuários (UserFlow)

![Status](https://img.shields.io/badge/Status-Concluído-success)
![Java](https://img.shields.io/badge/Java-21-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.0.3-brightgreen)
![React](https://img.shields.io/badge/React-Vite-blue)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Database-blue)

Bem-vindo ao repositório do **Sistema de Gestão de Usuários**. Este projeto é uma aplicação Web Full-Stack (Front-end e Back-end) construída com rigor arquitetural para gerenciar o controle de acesso, autenticação e perfis de usuários em um ambiente seguro.

Se você é um avaliador ou recrutador, este guia foi escrito especialmente para você. Mesmo que não tenha familiaridade prévia com Java, Spring Boot ou React, os passos abaixo o guiarão perfeitamente para rodar a aplicação em sua máquina.

---

## 📌 Índice
- [Links Úteis](#links-úteis)
- [Sobre o Projeto](#sobre-o-projeto)
- [Funcionalidades Principais](#funcionalidades-principais)
- [Tecnologias Utilizadas](#tecnologias-utilizadas)
- [Arquitetura](#arquitetura)
- [Instalação e Execução](#instalação-e-execução)
  - [Pré-requisitos](#pré-requisitos)
  - [Inicialização do Banco de Dados (PostgreSQL)](#inicialização-do-banco-de-dados-postgresql)
  - [Variáveis de Ambiente](#variáveis-de-ambiente)
- [Como Executar a Aplicação](#como-executar-a-aplicação)
- [Estrutura de Pastas](#estrutura-de-pastas)
- [Demonstração](#demonstração)
- [Autores e Contribuição](#autores)
- [Licença](#licença)

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
* **Cadastro com Validação:** Criação de conta com medidor visual de força de senha.
* **Dashboard Privado:** Painel de gestão protegido contra acessos não logados.
* **Gestão do Próprio Perfil:** O usuário logado pode alterar seu nome, e-mail e senha.
* **Gestão Administrativa:** Adição de novos usuários diretamente pelo painel.
* **Proteção de Rotas:** Redirecionamento automático caso a sessão (token) expire ou seja adulterada.

---

## 🛠️ Tecnologias Utilizadas

### Back-end (O "Motor" da aplicação)
* **Java 21:** Linguagem de programação robusta e orientada a objetos.
* **Spring Boot (v4.0.3):** Framework que facilita a criação da API REST.
* **Spring Security & JWT (Auth0):** Bibliotecas para blindar as rotas e gerar os "crachás" de acesso (Tokens).
* **Maven:** Gerenciador de dependências (ele baixa automaticamente tudo o que o Java precisa para rodar).

### Front-end (A Interface visual)
* **React com Vite:** Biblioteca JavaScript para criar telas rápidas e dinâmicas.
* **Tailwind CSS:** Framework de estilização para um design moderno e responsivo.
* **Axios:** Cliente HTTP para conectar a interface ao Back-end.

### Banco de Dados (A "Memória")
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
1. **Node.js** (Para o Front-end): [Baixe aqui](https://nodejs.org/).
2. **Java JDK 21** (Para o Back-end): [Baixe aqui](https://adoptium.net/).
3. **PostgreSQL** (Banco de dados): [Baixe aqui](https://www.postgresql.org/download/).

### Inicialização do Banco de Dados (PostgreSQL)
Como o sistema salva usuários reais, precisamos de um banco de dados rodando.
1. Instale o PostgreSQL e abra o **pgAdmin** (ou a ferramenta de linha de comando `psql`).
2. Crie um novo banco de dados vazio chamado exatamente de: `gestao_usuarios`
3. Certifique-se de que a senha do usuário padrão (`postgres`) do seu banco de dados seja `12345` (ou altere no arquivo de configuração do Java, como mostrado no próximo passo).

### Variáveis de Ambiente

#### 1. Back-end (Spring Boot)
O arquivo de configuração já está pronto no repositório. Ele fica no caminho:
`backend/src/main/resources/application.properties`



Ele contém as chaves para conectar ao banco. Se o seu PostgreSQL tiver uma senha diferente de `12345`, basta alterar lá:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/gestao_usuarios
spring.datasource.username=postgres
spring.datasource.password=12345 # <- Mude aqui se a sua senha do Postgres for outra
server.port=8081
```

### 2. Front-end (React, Vite)
Crie um arquivo chamado .env na raiz da pasta frontend para apontar o React para o nosso servidor Java:

Snippet de código
VITE_API_URL=http://localhost:8081
3. Exemplos de Variáveis de Ambiente na Vercel
Caso o projeto seja hospedado na Vercel (serviço de nuvem), o painel exigirá a mesma configuração na aba Environment Variables:

Key: VITE_API_URL

Value: https://api-gestao-usuarios.onrender.com (Exemplo de URL do back-end em produção)

📦 Instalação de Dependências
Abra o terminal na raiz do projeto clonado.

Front-end (React)
Navegue até a pasta do cliente e instale as bibliotecas visuais:

Bash
cd frontend
npm install
Back-end (Spring Boot)
O Maven é o gerenciador do Java. Ele baixa todas as dependências automaticamente no momento da build:

Bash
cd backend
./mvnw clean install
(No Windows, utilize mvnw.cmd clean install)

🗄️ Inicialização do Banco de Dados (PostgreSQL)
Abra a sua ferramenta de banco de dados (pgAdmin ou DBeaver) e crie um banco de dados vazio utilizando o comando SQL abaixo:

SQL
CREATE DATABASE gestao_usuarios;
Nota: Graças à configuração ddl-auto=update do Spring Boot, você não precisa criar tabelas manualmente. O Java criará as tabelas de usuários assim que a aplicação for iniciada.

🚀 Como Executar a Aplicação
Terminal 1: Back-end (Spring Boot)
Na pasta backend, inicie o servidor da API:

Bash
./mvnw spring-boot:run
Terminal 2: Front-end (React, Vite)
Em um novo terminal, na pasta frontend, inicie a interface:

Bash
npm run dev
Acesse http://localhost:5173 no seu navegador.

Execução Local Completa com Docker Compose (Incluindo Banco de Dados)
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
Passos para build, inicialização e execução
Com o Docker em uso, basta rodar:

docker-compose up -d (Sobe o banco em segundo plano).

Execute o Front-end e o Back-end nos passos detalhados acima.

📱 Demonstração
Aplicativo Mobile
Embora o projeto seja uma Aplicação Web (e não um aplicativo nativo Android/iOS), toda a interface (Tailwind CSS) é 100% responsiva, adaptando menus, tabelas de listagem e botões de ação para a tela de dispositivos móveis.

Aplicação Web
Tela de Acesso: Layout limpo com feedback visual em tempo real.

Dashboard: Tabela administrativa para controle geral.

📱 Demonstração
Aplicativo Mobile
Embora o projeto seja uma Aplicação Web (e não um aplicativo nativo Android/iOS), toda a interface (Tailwind CSS) é 100% responsiva, adaptando menus, tabelas de listagem e botões de ação para a tela de dispositivos móveis.

Aplicação Web
Tela de Acesso: Layout limpo com feedback visual em tempo real.

Dashboard: Tabela administrativa para controle geral.

💻 Exemplo de saída no Terminal (para Back-end, API, CLI)
Ao iniciar corretamente, o Spring Boot imprimirá no terminal logs semelhantes a este:
```
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
🙌 Agradecimentos
Agradecimentos especiais aos instrutores e avaliadores que inspiraram este desafio técnico, permitindo colocar em prática os mais exigentes conceitos de engenharia de software, Clean Architecture e UX.


