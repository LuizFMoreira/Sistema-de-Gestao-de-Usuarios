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
