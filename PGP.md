

```
# PGP - Plano Geral do Projeto

## Sistema de Gestão de Aulas

**Versão:** 1.0  
**Data:** 11/09/2026  
**Objetivo do documento:** consolidar os requisitos conhecidos, organizar o escopo funcional e servir como contrato para a construção incremental em 10 prompts, usando BDD como única forma de especificação executável.

## 1. Visão do produto

O sistema será uma aplicação padrão e simples para gestão de alunos, professores, turmas, matrículas, aulas, presenças e mensalidades.

O produto deve ser didático, com regras de negócio objetivas, código orientado a objetos e uso consciente de padrões de projeto. O domínio não deve carregar regras, nomes ou fluxos específicos de nenhum projeto externo.

### 1.1 Tipo de aplicação e stack definida

O produto será uma API REST backend, sem frontend incluído no escopo dos dez prompts iniciais.

| Camada | Tecnologia definida |
|---|---|
| Linguagem | Java 21 LTS. |
| Framework | Spring Boot. A versão exata será fixada no Prompt 1 usando uma versão estável compatível com Java 21. |
| Build | Maven, preferencialmente com Maven Wrapper. |
| API | Spring Web MVC e Spring Validation. |
| Segurança | Spring Security com JWT, access token de curta duração e refresh token rotativo. |
| Persistência | Spring Data JPA, Hibernate e PostgreSQL Driver. |
| Banco | PostgreSQL. |
| Migrações | Flyway, para versionar a criação e a evolução das tabelas. |
| Testes unitários | `spring-boot-starter-test`, JUnit 5, AssertJ e Mockito. |
| Testes de integração | Spring Boot Test com Testcontainers para PostgreSQL. |

O projeto não terá, inicialmente, Cucumber, H2, Lombok, MapStruct, Swagger/OpenAPI ou outras bibliotecas auxiliares. A inclusão de qualquer dependência adicional deverá ser justificada por um requisito ou por uma necessidade técnica comprovada.

### 1.2 Objetivo de negócio

Permitir que uma instituição controle seus alunos e professores, organize turmas e aulas, acompanhe presenças e registre mensalidades em um único sistema.

### 1.3 Diretrizes do projeto

- O sistema terá os perfis `admin`, `professor` e `aluno`.
- O escopo deve permanecer simples e genérico.
- Cada incremento será implementado e validado por cenários BDD.
- Os requisitos funcionais dos prompts serão escritos em Gherkin (`Funcionalidade`, `Cenário`, `Dado`, `Quando`, `Então`, `E`).
- A implementação deverá respeitar princípios de POO e poderá utilizar padrões como Repository, Service, Factory e Strategy quando trouxerem benefício real.
- Nenhum comportamento deve ser criado apenas por conveniência técnica sem um cenário BDD correspondente.

## 2. Escopo

### 2.1 Incluído

- autenticação e autorização por perfil;
- cadastro e consulta de alunos;
- cadastro e consulta de professores;
- criação e consulta de turmas;
- matrícula de alunos em turmas;
- cadastro e consulta de aulas;
- registro e consulta de presenças;
- registro e consulta de mensalidades;
- ativação de aluno a partir do pagamento da primeira mensalidade;
- visão administrativa resumida dos dados do sistema.

### 2.2 Fora do escopo inicial

- cadastro de responsáveis;
- múltiplas organizações ou multi-tenancy;
- integração com gateway de pagamento;
- emissão de nota fiscal;
- notificações externas;
- aplicativo específico para celular;
- regras avançadas de cobrança, descontos ou multas;
- funcionalidades que não tenham sido aprovadas por um cenário BDD.

## 3. Perfis e responsabilidades

| Perfil | Responsabilidade principal |
|---|---|
| Admin | Administrar os cadastros e operações do sistema. |
| Professor | Consultar suas turmas e aulas e registrar presenças conforme autorização. |
| Aluno | Consultar seus próprios dados, turmas, aulas, presenças e mensalidades. |

### 3.1 Matriz inicial de permissões

| Recurso | Admin | Professor | Aluno |
|---|---:|---:|---:|
| Gerenciar alunos | Sim | Não | Não |
| Gerenciar professores | Sim | Não | Não |
| Gerenciar turmas | Sim | Não | Não |
| Gerenciar matrículas | Sim | Não | Não |
| Gerenciar planos | Sim | Não | Não |
| Registrar mensalidades | Sim | Não | Não |
| Consultar suas turmas/aulas | Sim | Sim | Sim, quando matriculado |
| Registrar presença | Sim | Sim, nas aulas autorizadas | Não |
| Consultar mensalidades próprias | Sim | Não | Sim |
| Consultar dados próprios | Sim | Sim | Sim |

Essa matriz é a referência inicial de autorização. Permissões específicas ainda não definidas deverão ser confirmadas antes do prompt que as utilizar.

## 4. Requisitos funcionais

### RF-01 - Autenticação

O sistema deve permitir que um usuário autenticado acesse as funcionalidades compatíveis com seu perfil utilizando JWT com access token e refresh token.

Admin, professor e aluno devem possuir senha no cadastro. O login de qualquer perfil deve utilizar e-mail e senha.

A senha nunca deve ser armazenada em texto puro.

### RF-02 - Cadastro de aluno

O admin deve poder cadastrar um aluno com os seguintes campos mínimos:

- nome;
- e-mail;
- telefone;
- data de nascimento;
- senha.

O status não deve ser informado na criação. Ele é controlado pelo sistema.

### RF-03 - Estado inicial do aluno

Todo aluno deve ser criado com status `inativo`. O cadastro não deve aceitar status como dado de entrada.

### RF-04 - Ativação por primeira mensalidade

O aluno somente pode ser alterado para `ativo` quando uma mensalidade pendente do mês for recebida pelo admin e mudar para `paga`.

### RF-05 - Cadastro de professor

O admin deve poder cadastrar professores com os seguintes campos mínimos:

- nome;
- e-mail;
- data de nascimento;
- número do CREF.

O cadastro do professor deve incluir senha para autenticação.

O professor deve possuir uma situação cadastral `ativo` ou `inativo`.

### RF-06 - Turmas

O admin deve poder criar e consultar turmas com os seguintes dados mínimos:

- nome;
- dia da semana;
- horário de início;
- horário de fim;
- capacidade máxima;
- um ou mais professores associados;
- um professor responsável.

Uma turma deve possuir status `ativa` ou `inativa`. Ela pode ter um ou vários professores associados, mas deve ter exatamente um professor responsável entre os professores associados.

### RF-07 - Matrículas

O admin deve poder matricular alunos em turmas, respeitando o status do aluno, o status da turma e a capacidade máxima.

Uma matrícula não possui status. O mesmo aluno pode estar matriculado em mais de uma turma.

### RF-08 - Aulas

As aulas devem ser criadas a partir de uma turma e somente podem ser geradas para os dias da semana cadastrados nessa turma. Uma aula deve possuir um indicador booleano de cancelamento.

### RF-09 - Presenças

O admin pode gerenciar presenças. O professor pode adicionar um aluno à presença somente em aulas de turmas às quais esteja associado. Aluno inativo não pode ser adicionado a uma presença.

O professor pode consultar os alunos, aulas e presenças somente das turmas às quais esteja associado.

### RF-10 - Mensalidades

O admin deve poder criar e gerenciar mensalidades. Cada mensalidade deve possuir vencimento no dia 10 do mês de referência, valor derivado do plano do aluno e status `pendente` ou `paga`.

Uma mensalidade paga pode ter seu recebimento cancelado pelo admin. Nesse caso, a mensalidade volta para `pendente` e pode ser recebida novamente. O cancelamento não é um status persistente nesse fluxo.

O aluno deve poder consultar apenas suas próprias mensalidades.

### RF-11 - Planos

O admin deve poder criar e gerenciar planos. Um plano possui apenas nome e valor.

Cada aluno deve estar associado a um único plano. O valor considerado no recebimento da mensalidade deve ser o valor vigente do plano associado ao aluno.

### RF-12 - Dashboard administrativo

O sistema deve apresentar ao admin uma visão resumida contendo:

- total de alunos ativos e inativos;
- total de professores ativos e inativos;
- total de turmas ativas e inativas;
- quantidade de matrículas ativas por turma;
- quantidade de mensalidades pendentes e pagas no mês selecionado;
- total financeiro pago e pendente no mês selecionado;
- próximas aulas previstas.

## 5. Regras de negócio confirmadas

### RN-01 - Status inicial

Um aluno recém-cadastrado sempre inicia com status `inativo`.

### RN-02 - Primeira mensalidade

A primeira mensalidade do aluno deve ser criada com status `pendente`.

### RN-03 - Aluno ativo

O aluno somente pode ficar `ativo` depois que o admin receber a mensalidade pendente do mês e ela passar para `paga`.

Se o admin inativar o aluno, ele somente poderá voltar a ficar ativo após o pagamento da mensalidade pendente do mês.

### RN-04 - Cadastro de responsáveis

O sistema não terá cadastro de responsáveis no escopo deste projeto.

### RN-05 - Perfis

Os únicos perfis de usuário do escopo são `admin`, `professor` e `aluno`.

### RN-06 - Unicidade de e-mail

O e-mail utilizado no cadastro de um usuário deve ser único em todo o sistema. O mesmo e-mail não pode pertencer a dois usuários, ainda que tenham perfis diferentes.

### RN-07 - Administração dos registros

O admin é o único perfil que pode criar, editar, excluir ou inativar registros do sistema.

O professor não possui permissão geral de criação, edição ou exclusão. Sua única operação de escrita no escopo é adicionar um aluno à presença de uma aula autorizada.

O aluno não possui operações de criação, edição ou exclusão.

### RN-08 - Professor responsável e professores associados

Uma turma deve possuir um ou mais professores associados e exatamente um professor responsável. O professor responsável deve fazer parte dos professores associados.

Um professor não pode consultar informações de turmas às quais não esteja associado.

### RN-09 - Capacidade da turma

Uma turma só pode receber matrícula enquanto a quantidade de alunos matriculados for menor ou igual à sua capacidade máxima. Quando a capacidade for atingida, novas matrículas devem ser impedidas.

### RN-10 - Status da turma

Uma turma pode estar `ativa` ou `inativa`. Turma inativa não aceita novas matrículas.

### RN-11 - Presença

Aluno inativo não pode ser adicionado à presença de uma aula.

### RN-12 - Mensalidade

Cada aluno pode ter no máximo uma mensalidade por mês de referência. O cancelamento do recebimento não cria uma nova mensalidade.

Toda mensalidade vence no dia 10 do mês de referência e deve iniciar com status `pendente`. Os status persistidos são `pendente` e `paga`.

Ao receber uma mensalidade, o admin deve alterá-la para `paga`, considerando o valor do plano associado ao aluno.

### RN-13 - Plano

Um plano possui somente nome e valor. Cada aluno deve possuir um único plano associado.

### RN-14 - Cancelamento de mensalidade

Quando o admin cancelar o recebimento de uma mensalidade paga, ela deve voltar para `pendente` e poderá ser recebida novamente.

`Cancelada` representa a operação realizada pelo admin sobre o recebimento, não um status persistido da mensalidade neste fluxo.

### RN-15 - Inativação administrativa

O admin pode inativar um aluno, professor ou turma a qualquer momento.

Um aluno inativado não pode ser adicionado a uma presença e não pode voltar a ficar ativo sem o pagamento da mensalidade pendente do mês.

Uma turma inativada não aceita novas matrículas.

## 6. Decisões consolidadas

As regras anteriormente pendentes foram definidas e incorporadas às seções de requisitos, regras de negócio, casos de uso e histórias de usuário.

O cancelamento de uma mensalidade paga foi definido como uma operação que retorna o registro ao status `pendente`. Portanto, não haverá status persistido `cancelada` nesse fluxo.

## 7. Entidades de domínio iniciais

As entidades abaixo representam o vocabulário inicial.

### Usuário

Entidade de autenticação com e-mail único, senha protegida e perfil (`admin`, `professor` ou `aluno`). Todos os perfis terão senha e acessarão o sistema com e-mail e senha.

### Aluno

`id`, `nome`, `e-mail`, `telefone`, `data de nascimento`, `status` e referência para um único plano.

### Professor

`id`, `nome`, `e-mail`, `data de nascimento`, `número do CREF`, credencial de acesso e situação cadastral.

### Turma

`id`, `nome`, `dia da semana`, `horário de início`, `horário de fim`, `capacidade máxima`, professores associados, professor responsável e situação.

### Matrícula

Relação entre aluno e turma, com data da matrícula. A matrícula não possui status.

### Aula

Registro de uma ocorrência de aula vinculada a uma turma, com data, horário e indicador booleano `cancelada`.

### Presença

Registro da situação de presença de um aluno em uma aula.

### Mensalidade

Registro financeiro associado a um aluno, incluindo mês de referência, valor, vencimento e status (`pendente` ou `paga`). O cancelamento do recebimento de uma mensalidade paga retorna seu status para `pendente`.

### Plano

`id`, `nome` e `valor`.

## 8. Casos de uso

### UC-01 - Autenticar usuário

**Ator principal:** qualquer usuário cadastrado.  
**Objetivo:** acessar o sistema conforme o perfil do usuário.

**Pré-condição:** o usuário possui credencial válida.  
**Pós-condição:** o usuário fica autenticado e pode acessar apenas as funcionalidades autorizadas.

**Fluxo principal:**

1. O usuário informa suas credenciais.
2. O sistema valida as credenciais.
3. O sistema identifica o perfil do usuário.
4. O sistema concede acesso às funcionalidades permitidas.

**Exceção:** credenciais inválidas não devem conceder acesso.

### UC-02 - Cadastrar aluno

**Ator principal:** admin.  
**Objetivo:** registrar um novo aluno.

**Pré-condição:** o admin está autenticado e autorizado.  
**Pós-condição:** o aluno é criado com status `inativo`.

**Fluxo principal:**

1. O admin informa nome, e-mail, telefone e data de nascimento.
2. O sistema valida os dados obrigatórios.
3. O sistema cria o aluno com status `inativo`.
4. O sistema disponibiliza o aluno para consulta.

### UC-03 - Registrar primeira mensalidade

**Ator principal:** admin.  
**Objetivo:** criar a primeira mensalidade, recebê-la e ativar o aluno após o pagamento.

**Pré-condição:** o aluno existe e está `inativo`.  
**Pós-condição:** a mensalidade é criada como `pendente`; após o recebimento, passa para `paga` e o aluno fica `ativo`.

**Fluxo principal:**

1. O admin seleciona o aluno.
2. O sistema considera o plano associado ao aluno.
3. O sistema cria a mensalidade com vencimento no dia 10 e status `pendente`.
4. O admin recebe a mensalidade.
5. O sistema altera a mensalidade para `paga`.
6. O sistema altera o status do aluno para `ativo`.

**Fluxo alternativo:** se o admin cancelar o recebimento, a mensalidade volta para `pendente` e pode ser recebida novamente.

### UC-04 - Gerenciar plano

**Ator principal:** admin.  
**Objetivo:** criar e manter planos com nome e valor para associação aos alunos.

### UC-05 - Cadastrar professor

**Ator principal:** admin.  
**Objetivo:** registrar um professor para uso no sistema.

**Dados obrigatórios:** nome, e-mail, data de nascimento e número do CREF.

### UC-06 - Criar turma

**Ator principal:** admin.  
**Objetivo:** criar uma turma ativa ou inativa, com um ou mais professores associados e exatamente um professor responsável.

**Dados obrigatórios:** nome, dia da semana, horário de início, horário de fim, capacidade máxima, professores associados e professor responsável.

### UC-07 - Matricular aluno em turma

**Ator principal:** admin.  
**Objetivo:** vincular um aluno a uma turma.

**Regras:** o aluno precisa estar `ativo`, a turma precisa estar `ativa` e a capacidade máxima não pode ser ultrapassada. Um aluno pode estar matriculado em mais de uma turma.

### UC-08 - Registrar aula

**Ator principal:** admin.  
**Objetivo:** gerar aulas a partir de uma turma somente nos dias da semana cadastrados para ela.

**Regra:** cada aula deve possuir um indicador booleano `cancelada`.

### UC-09 - Registrar presença

**Atores principais:** admin e professor autorizado.  
**Objetivo:** registrar a presença de alunos em uma aula.

**Regras:** o professor só pode adicionar alunos em aulas de turmas às quais esteja associado; aluno inativo não pode ser adicionado.

### UC-10 - Consultar informações do aluno

**Atores principais:** admin, professor nas suas turmas e o próprio aluno.  
**Objetivo:** consultar os dados permitidos do aluno.

### UC-11 - Consultar informações acadêmicas

**Atores principais:** admin, professor e aluno.  
**Objetivo:** consultar turmas, aulas e presenças de acordo com o perfil e a relação do usuário com o recurso.

**Regra:** o professor só pode consultar turmas às quais esteja associado; o aluno só pode consultar suas próprias matrículas, turmas, aulas e presenças.

### UC-12 - Consultar mensalidades

**Atores principais:** admin e aluno.  
**Objetivo:** consultar registros financeiros conforme a permissão do perfil.

### UC-13 - Consultar dashboard

**Ator principal:** admin.  
**Objetivo:** visualizar os indicadores operacionais e financeiros resumidos definidos no RF-12.

## 9. Histórias de usuário

As histórias abaixo representam o backlog inicial. Cada uma deve receber cenários BDD antes de ser implementada.

### Épico A - Acesso

**US-01 - Login**  
Como usuário, quero autenticar-me no sistema para acessar as funcionalidades permitidas pelo meu perfil.

**US-02 - Controle de acesso**  
Como administrador do sistema, quero que cada perfil veja somente as operações autorizadas para proteger os dados e as responsabilidades de cada usuário.

### Épico B - Alunos

**US-03 - Cadastro de aluno**  
Como admin, quero cadastrar um aluno com nome, e-mail, telefone, data de nascimento e senha para manter seus dados e permitir seu acesso ao sistema.

**US-04 - Status inicial do aluno**  
Como admin, quero que todo aluno novo seja criado como inativo para que sua ativação dependa do pagamento da primeira mensalidade.

**US-05 - Ativação do aluno**  
Como admin, quero receber a mensalidade pendente do mês para que ela fique paga e o sistema torne o aluno ativo.

**US-06 - Consulta do próprio cadastro**  
Como aluno, quero consultar meus próprios dados para acompanhar meu cadastro.

### Épico C - Professores e turmas

**US-07 - Cadastro de professor**  
Como admin, quero cadastrar professores com nome, e-mail, data de nascimento e número do CREF para associá-los às atividades do sistema.

**US-08 - Criação de turma**  
Como admin, quero criar uma turma com agenda, capacidade máxima, professores associados e um professor responsável para organizar a oferta de aulas.

**US-09 - Consulta de turmas**  
Como professor, quero consultar somente as turmas às quais estou associado para acompanhar minhas responsabilidades.

### Épico D - Matrículas e aulas

**US-10 - Matrícula de aluno ativo**  
Como admin, quero matricular um aluno ativo em uma turma ativa sem ultrapassar a capacidade máxima para registrar sua participação.

**US-11 - Bloqueio de matrícula de aluno inativo**  
Como admin, quero que o sistema impeça a matrícula de aluno inativo para respeitar a regra de ativação.

**US-12 - Consulta de agenda**  
Como aluno, quero consultar minhas turmas e aulas para saber onde e quando devo participar.

**US-13 - Registro de aula**  
Como admin, quero gerar aulas a partir dos dias da semana da turma para manter a agenda do sistema.

### Épico E - Presenças

**US-14 - Registro de presença pelo professor**  
Como professor, quero adicionar alunos ativos à presença de uma aula autorizada para manter o acompanhamento das participações.

**US-15 - Consulta de presença pelo aluno**  
Como aluno, quero consultar meu histórico de presenças para acompanhar minha participação.

### Épico F - Mensalidades

**US-16 - Registro de mensalidade**  
Como admin, quero criar mensalidades pendentes com vencimento no dia 10 e valor baseado no plano do aluno para manter o controle financeiro.

**US-17 - Consulta de mensalidades pelo aluno**  
Como aluno, quero consultar minhas mensalidades para acompanhar minha situação financeira.

**US-18 - Gerenciamento de planos**  
Como admin, quero criar planos com nome e valor e associar um único plano a cada aluno para definir o valor das mensalidades.

**US-19 - Dashboard administrativo**  
Como admin, quero visualizar indicadores de alunos, professores, turmas, matrículas, aulas e mensalidades para acompanhar a operação do sistema.

**US-20 - Credenciais de acesso**  
Como usuário, quero cadastrar e utilizar e-mail e senha de forma protegida para autenticar-me no sistema.

## 10. Critérios BDD essenciais já definidos

```gherkin
Funcionalidade: Cadastro de aluno

  Cenário: Criar aluno com status inativo
    Dado que o admin informou nome, e-mail, telefone, data de nascimento e senha válidos
    Quando o admin cadastrar o aluno
    Então o sistema deve criar o aluno com status "inativo"
    E o status não deve ser informado pelo admin na criação
```

```gherkin
Funcionalidade: Ativação de aluno

  Cenário: Criar primeira mensalidade pendente
    Dado que existe um aluno com status "inativo" e um plano associado
    Quando o admin criar a mensalidade do mês
    Então o sistema deve criar a mensalidade com status "pendente"
    E o vencimento deve ser o dia 10 do mês de referência
    E o valor deve ser o valor do plano do aluno

  Cenário: Ativar aluno após receber a mensalidade
    Dado que existe um aluno com status "inativo"
    E existe uma mensalidade pendente do mês para esse aluno
    Quando o admin receber a mensalidade
    Então o sistema deve alterar a mensalidade para "paga"
    E o sistema deve alterar o status do aluno para "ativo"
```

```gherkin
Funcionalidade: Matrícula

  Cenário: Impedir matrícula de aluno inativo
    Dado que existe um aluno com status "inativo"
    Quando o admin tentar matricular o aluno em uma turma
    Então o sistema deve impedir a matrícula
```

```gherkin
Funcionalidade: Capacidade da turma

  Cenário: Impedir matrícula quando a turma atingir a capacidade máxima
    Dado que existe uma turma ativa com sua capacidade máxima atingida
    E existe um aluno ativo sem matrícula nessa turma
    Quando o admin tentar matricular o aluno na turma
    Então o sistema deve impedir a matrícula
```

```gherkin
Funcionalidade: Unicidade de e-mail

  Cenário: Impedir e-mail já utilizado por outro usuário
    Dado que existe um usuário cadastrado com determinado e-mail
    Quando o admin tentar cadastrar outro usuário com o mesmo e-mail
    Então o sistema deve impedir o cadastro
```

```gherkin
Funcionalidade: Autenticação

  Cenário: Autenticar usuário com e-mail e senha válidos
    Dado que existe um usuário com e-mail e senha cadastrados
    Quando o usuário informar o e-mail e a senha corretos
    Então o sistema deve emitir um access token
    E o sistema deve emitir um refresh token
    E o usuário deve acessar somente as operações permitidas pelo seu perfil
```

```gherkin
Funcionalidade: Cancelamento de recebimento de mensalidade

  Cenário: Retornar mensalidade paga para pendente
    Dado que existe uma mensalidade com status "paga"
    Quando o admin cancelar o recebimento da mensalidade
    Então o sistema deve alterar o status da mensalidade para "pendente"
    E a mensalidade deve poder ser recebida novamente
```

Os demais cenários serão detalhados no prompt correspondente. Uma implementação só será considerada concluída quando seus cenários aprovados estiverem atendidos.

## 11. Arquitetura e orientação técnica

O sistema deve ser desenvolvido com orientação a objetos, mantendo responsabilidades separadas entre domínio, aplicação, persistência e apresentação da API REST.

A modelagem do PostgreSQL deve ser criada e evoluída por migrations Flyway. As tabelas, chaves, relacionamentos, índices e restrições necessários devem ser derivados dos requisitos BDD de cada incremento.

### 11.1 Estratégia de testes

- testes unitários devem validar entidades, regras de negócio e serviços sem depender do banco;
- testes de integração devem validar repositórios, migrations, autorização e endpoints relevantes;
- os testes de integração devem utilizar PostgreSQL em Testcontainers, evitando diferenças entre o banco de teste e o banco definido para produção;
- os cenários BDD serão a referência dos testes, usando a estrutura Dado/Quando/Então mesmo quando automatizados com JUnit;
- não será mantido um banco H2 paralelo para simular PostgreSQL.

### 11.2 Política de autenticação e tokens

- o access token terá validade de 15 minutos;
- o refresh token terá validade de 7 dias;
- o refresh token será rotacionado a cada utilização, invalidando o token anterior;
- logout e eventos de segurança devem revogar o refresh token correspondente;
- o servidor deve persistir somente o hash ou identificador seguro do refresh token, nunca o token em texto puro;
- o access token e o refresh token serão assinados com `RS256` usando chave RSA de, no mínimo, 2048 bits;
- a chave privada deverá permanecer fora do código-fonte e ser fornecida por configuração segura do ambiente;
- a chave pública poderá ser usada para validação dos tokens, e a rotação deve manter as chaves antigas disponíveis até a expiração dos tokens emitidos por elas;
- as senhas de todos os perfis serão armazenadas usando `BCryptPasswordEncoder`, nunca em texto puro.

Padrões candidatos, sujeitos à necessidade real:

- **Repository:** encapsular acesso a dados;
- **Service/Application Service:** coordenar casos de uso;
- **Factory:** centralizar criação de entidades com estado inicial obrigatório;
- **Strategy:** encapsular variações de regras que possam surgir;
- **Dependency Injection:** desacoplar casos de uso de implementações concretas.

Dependências técnicas mínimas esperadas:

- `spring-boot-starter-web`;
- `spring-boot-starter-validation`;
- `spring-boot-starter-security`;
- suporte OAuth2 Resource Server/Jose do Spring Security para validar JWT;
- `spring-boot-starter-data-jpa`;
- driver JDBC do PostgreSQL;
- Flyway;
- `spring-boot-starter-test` e `spring-security-test` com escopo de teste;
- Testcontainers PostgreSQL com escopo de teste.

Esses padrões são diretrizes de implementação. Eles não substituem requisitos BDD nem devem criar funcionalidades fora do escopo.

## 12. Plano dos 10 prompts

| Prompt | Incremento | Resultado esperado |
|---:|---|---|
| 1 | Fundação do projeto | API REST em Java/Spring Boot, Maven, PostgreSQL, Flyway, estrutura de domínio e primeiro fluxo BDD. |
| 2 | Autenticação e perfis | Login dos três perfis com e-mail e senha, access/refresh tokens, senha protegida e autorização por perfil. |
| 3 | Cadastro de alunos | Cadastro com campos mínimos e status inicial inativo. |
| 4 | Planos e mensalidades | Cadastro de planos, criação da mensalidade pendente, recebimento e ativação do aluno. |
| 5 | Professores | Cadastro com nome, e-mail, data de nascimento e CREF. |
| 6 | Turmas | Criação, capacidade, associação de professores e definição do responsável. |
| 7 | Matrículas | Matrícula de aluno ativo e bloqueios definidos em BDD. |
| 8 | Aulas | Geração de aulas nos dias da semana da turma e consulta. |
| 9 | Presenças | Adição de alunos ativos à presença por admin ou professor autorizado. |
| 10 | Consolidação | Dashboard, revisão de permissões, testes BDD e fechamento do escopo. |

Cada prompt deve conter somente:

1. objetivo do incremento;
2. contexto necessário;
3. regras de negócio;
4. cenários BDD;
5. critérios de conclusão;
6. restrições explícitas de escopo.

## 13. Critérios de aceite do projeto

O projeto estará concluído ao final do décimo prompt quando:

- os casos de uso aprovados estiverem implementados;
- as histórias de usuário tiverem cenários BDD correspondentes;
- os três perfis tiverem autorização coerente com a matriz aprovada;
- a API estiver estruturada em Java com Spring Boot e persistir em PostgreSQL;
- as tabelas e alterações de modelo estiverem versionadas por Flyway;
- access token e refresh token estiverem implementados com senha protegida;
- o aluno for criado como inativo;
- o status não for aceito como entrada no cadastro do aluno;
- a primeira mensalidade for criada como pendente e puder ativar o aluno após o recebimento;
- o e-mail for único em todo o sistema;
- planos, mensalidades, professores, turmas, aulas e presenças estiverem cobertos pelos respectivos cenários;
- a matrícula de aluno inativo, de turma inativa ou acima da capacidade for impedida;
- professores consultarem apenas turmas às quais estejam associados;
- alunos consultarem somente suas próprias matrículas e informações;
- os testes BDD dos incrementos estiverem executáveis;
- não existirem funcionalidades relevantes implementadas sem requisito aprovado;
- o décimo prompt registrar a revisão final e os itens deliberadamente deixados fora do escopo.

## 14. Decisões técnicas consolidadas

As decisões de fundação foram fechadas:

- tipo de aplicação: API REST backend;
- linguagem e framework: Java 21 LTS com Spring Boot;
- build: Maven;
- autenticação: JWT com access token e refresh token;
- persistência: PostgreSQL com JPA/Hibernate;
- evolução do banco: migrations Flyway;
- testes: JUnit 5, Spring Boot Test, AssertJ, Mockito, Spring Security Test e Testcontainers PostgreSQL.

Também estão definidos:

- todos os perfis acessam com e-mail e senha;
- access token com validade de 15 minutos;
- refresh token com validade de 7 dias;
- rotação do refresh token a cada utilização;
- revogação no logout e em eventos de segurança;
- persistência apenas do hash ou identificador seguro do refresh token;
- assinatura JWT com `RS256` e chave RSA de no mínimo 2048 bits;
- chave privada fornecida por configuração segura do ambiente;
- senhas protegidas com `BCryptPasswordEncoder`;
- mensalidade paga cancelada retorna para `pendente`, sem status persistido `cancelada` nesse fluxo.

O Prompt 1 deverá apenas fixar a versão concreta do Spring Boot compatível com Java 21 e gerar a estrutura inicial, sem alterar essas decisões.

```

