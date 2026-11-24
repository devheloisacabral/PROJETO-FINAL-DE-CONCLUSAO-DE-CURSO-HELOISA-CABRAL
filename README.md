# Esportiva Plus

API REST para gestão de aulas, turmas e atividades de uma escola esportiva. Este repositório também é a base documental do Trabalho de Conclusão de Curso (TCC) de Heloísa Cabral, graduanda, que investiga a evolução de indicadores de qualidade interna do sistema ao longo de 20 incrementos desenvolvidos com assistência de inteligência artificial e especificados a partir de requisitos humanos em BDD/Gherkin.

> Este repositório contém o backend e os artefatos do experimento. Não há, neste projeto, uma aplicação React Native/Expo ou uma interface web separada.

## Sumário

- [Sobre o projeto](#sobre-o-projeto)
- [Autoria e contexto acadêmico](#autoria-e-contexto-acadêmico)
- [Tecnologias](#tecnologias)
- [Como navegar pelo repositório](#como-navegar-pelo-repositório)
- [Hierarquia das pastas](#hierarquia-das-pastas)
- [Arquitetura da aplicação](#arquitetura-da-aplicação)
- [Domínio funcional da API](#domínio-funcional-da-api)
- [Configuração e execução](#configuração-e-execução)
- [O experimento do TCC](#o-experimento-do-tcc)
- [Prompts, BDD e registros](#prompts-bdd-e-registros)
- [Base consolidada e análise longitudinal](#base-consolidada-e-análise-longitudinal)
- [Workflow de comparação SonarCloud](#workflow-de-comparação-sonarcloud)
- [Relatórios gerados](#relatórios-gerados)
- [Limitações metodológicas](#limitações-metodológicas)
- [Próximos passos](#próximos-passos)

## Sobre o projeto

O Esportiva Plus é uma API de gestão de uma escola esportiva. O sistema organiza informações de alunos, professores, turmas, aulas, matrículas, presenças, planos, mensalidades e indicadores administrativos.

O desenvolvimento foi organizado em incrementos P01 a P20. A sequência começa pela fundação técnica e autenticação, passa pelo cadastro e pela operação acadêmica/financeira e termina com manutenções evolutivas, como vencimento individual, pagamentos parciais, estornos, cancelamentos, transferências, lista de espera, conflitos de horário e reagendamento de aulas.

O repositório tem dois objetivos relacionados, mas distintos:

1. manter uma API executável para representar o objeto de estudo;
2. preservar uma trilha documental para analisar como os indicadores de qualidade interna variaram durante a evolução do sistema.

O segundo objetivo é o foco atual do repositório como base do TCC. Por isso, a pasta docs/ é parte essencial do projeto e não apenas documentação auxiliar.

## Autoria e contexto acadêmico

**Autora:** Heloísa Cabral, graduanda.

Este repositório integra o Trabalho de Conclusão de Curso sobre a evolução da qualidade de software gerado por inteligência artificial ao longo de 20 incrementos. O estudo acompanha estados sucessivos do mesmo sistema, desenvolvidos a partir de especificações humanas em BDD/Gherkin e avaliados por testes, métricas de cobertura e análise estática.

O estudo é descritivo e exploratório. A documentação não afirma que a inteligência artificial ou o BDD causaram qualquer melhora ou piora. O conjunto atual não possui grupo de controle, repetições independentes ou baseline sem uso de IA suficiente para sustentar uma conclusão causal.

Para conhecer o planejamento acadêmico, consulte [docs/tcc/00-planejamento/proposta-inicial.md](docs/tcc/00-planejamento/proposta-inicial.md). Para conhecer a organização da escrita do TCC, consulte [docs/tcc/README.md](docs/tcc/README.md) e [docs/tcc/03-capitulos/README.md](docs/tcc/03-capitulos/README.md).

## Tecnologias

### Aplicação

- Java 21;
- Spring Boot 4.1.1;
- Spring MVC para a API REST;
- Spring Validation;
- Spring Security;
- JWT com assinatura RSA/RS256;
- Spring Data JPA/Hibernate;
- PostgreSQL;
- Flyway;
- Maven e Maven Wrapper.

### Testes e qualidade

- JUnit 5, Spring Boot Test, Mockito e MockMvc;
- Testcontainers para persistência PostgreSQL quando o Docker está disponível;
- JaCoCo para relatório de cobertura;
- SonarCloud para métricas e comparação longitudinal;
- GitHub Actions para workflows manuais de comparação.

## Como navegar pelo repositório

Se você quer entender a aplicação, comece por:

1. [pom.xml](pom.xml), para Java, Spring Boot e dependências;
2. [src/main/java/devheloisacabral/tcc/com/esportiva/EsportivaApplication.java](src/main/java/devheloisacabral/tcc/com/esportiva/EsportivaApplication.java), classe de inicialização;
3. [src/main/java/devheloisacabral/tcc/com/esportiva/presentation/](src/main/java/devheloisacabral/tcc/com/esportiva/presentation/), controllers e contratos HTTP;
4. [src/main/java/devheloisacabral/tcc/com/esportiva/application/](src/main/java/devheloisacabral/tcc/com/esportiva/application/), serviços e casos de uso;
5. [src/main/java/devheloisacabral/tcc/com/esportiva/domain/](src/main/java/devheloisacabral/tcc/com/esportiva/domain/), entidades e tipos;
6. [src/main/resources/db/migration/](src/main/resources/db/migration/), migrations Flyway;
7. [src/test/java/](src/test/java/), testes automatizados.

Se você quer entender o TCC, siga esta ordem:

1. [docs/tcc/00-planejamento/proposta-inicial.md](docs/tcc/00-planejamento/proposta-inicial.md), para problema, pergunta, objetivos e escopo;
2. [docs/experimento/resultado-20-prompts.md](docs/experimento/resultado-20-prompts.md), para a tabela consolidada dos 20 incrementos;
3. [docs/experimento/prompts/](docs/experimento/prompts/), para as especificações BDD/Gherkin;
4. [docs/experimento/registros/](docs/experimento/registros/), para execução e validação;
5. [docs/experimento/analise-longitudinal/](docs/experimento/analise-longitudinal/), para dados normalizados, gráficos e conferências;
6. [docs/tcc/03-capitulos/05-resultados-e-avaliacao.md](docs/tcc/03-capitulos/05-resultados-e-avaliacao.md), para o texto-base dos resultados;
7. [docs/tcc/01-referencias/](docs/tcc/01-referencias/), para a matriz e a estratégia bibliográfica.

## Hierarquia das pastas

~~~text
.
├── .github/workflows/                 # workflows manuais do SonarCloud
├── .mvn/                               # suporte do Maven Wrapper
├── docs/
│   ├── experimento/
│   │   ├── prompts/v1/                 # prompts P01-C/P01-T a P10-C/P10-T
│   │   ├── prompts/v2/                 # prompts P11-C/P11-T a P20-C/P20-T
│   │   ├── registros/v1/               # registros dos incrementos P01-P10
│   │   ├── registros/v2/               # registros dos incrementos P11-P20
│   │   ├── resultado-20-prompts.md      # tabela consolidada
│   │   └── analise-longitudinal/        # derivados, gráficos e auditoria
│   └── tcc/
│       ├── 00-planejamento/             # proposta e decisões de escopo
│       ├── 01-referencias/              # busca, matriz e referências
│       └── 03-capitulos/                # capítulos e resultados
├── scripts/
│   ├── sonar/                            # comparador SonarCloud e testes
│   ├── generate_analise_tcc.py           # análise longitudinal
│   ├── generate_capitulo_5.py             # derivados do Capítulo 5
│   ├── generate_capitulo_5_docx.py        # DOCX do Capítulo 5
│   ├── generate_resultado_pdf.py          # PDF de resultados
│   ├── generate_resumo_orientador.py      # resumo textual
│   ├── generate_resumo_orientador_docx.py # resumo em DOCX
│   └── requirements-capitulo-5.txt        # versões de Matplotlib e NumPy
├── src/
│   ├── main/
│   │   ├── java/.../esportiva/
│   │   │   ├── application/              # casos de uso e serviços
│   │   │   ├── config/                   # configuração de segurança
│   │   │   ├── domain/                   # entidades e regras de domínio
│   │   │   ├── persistence/              # repositórios JPA
│   │   │   ├── presentation/             # controllers REST
│   │   │   └── security/                 # filtro e serviço JWT
│   │   └── resources/
│   │       ├── application.properties    # configuração por ambiente
│   │       └── db/migration/              # migrations Flyway V1 a V15
│   └── test/java/                         # testes unitários, contrato e persistência
├── output/                                # artefatos documentais gerados
├── target/                                # saída do Maven
├── tmp/                                   # arquivos temporários locais
├── mvnw / mvnw.cmd                        # Maven Wrapper
├── pom.xml                                # configuração Maven
└── README.md                              # este guia
~~~

output/, tmp/ e target/ podem conter artefatos gerados durante a pesquisa ou validação local. A fonte documental principal é docs/, e o código executável é src/.

## Arquitetura da aplicação

O backend usa uma organização por responsabilidades:

| Camada | Local | Responsabilidade |
| --- | --- | --- |
| Apresentação | presentation/ | Endpoints REST, validação de requests e respostas HTTP. |
| Aplicação | application/ | Casos de uso e regras de negócio. |
| Domínio | domain/ | Entidades, enums e conceitos centrais. |
| Persistência | persistence/ | Repositórios e consultas JPA. |
| Segurança | security/ e config/ | JWT, filtro de autenticação e autorização. |
| Banco | resources/db/migration/ | Evolução versionada por Flyway. |

Fluxo típico:

~~~text
HTTP request
    ↓
Controller em presentation/
    ↓
Service em application/
    ↓
Entidades e regras em domain/
    ↓
Repository em persistence/
    ↓
PostgreSQL
~~~

## Domínio funcional da API

| Grupo | Prefixo | Responsabilidade |
| --- | --- | --- |
| Saúde | /health | Verificação do processo HTTP. |
| Autenticação | /auth | Login, renovação, logout e usuário autenticado. |
| Alunos | /alunos | Cadastro, consulta, plano e vencimento. |
| Professores | /professores | Cadastro, consulta, atualização e status. |
| Planos | /planos | Cadastro de planos. |
| Mensalidades | /mensalidades | Emissão, consulta, recebimento, cancelamento e estorno. |
| Turmas | /turmas | Cadastro, consulta, atualização e status. |
| Matrículas | /matriculas | Matrícula, cancelamento, transferência e lista de espera. |
| Aulas | /aulas | Geração, consulta e reagendamento. |
| Presenças | /presencas | Registro e consulta. |
| Dashboard | /admin/dashboard | Indicadores administrativos mensais. |

Para o contrato exato de cada rota, consulte os controllers em [src/main/java/devheloisacabral/tcc/com/esportiva/presentation/](src/main/java/devheloisacabral/tcc/com/esportiva/presentation/). O README não substitui o código como fonte do contrato HTTP.

## Configuração e execução

### Pré-requisitos

Para compilar e executar:

- JDK 21;
- PostgreSQL acessível;
- Git;
- Maven Wrapper, incluído no repositório.

Para executar também o teste de persistência:

- Docker em execução, para o Testcontainers iniciar um PostgreSQL temporário.

Sem Docker, a maior parte da suíte pode ser executada, mas o teste marcado com Testcontainers pode ser ignorado. Isso não equivale a validar a persistência contra PostgreSQL real.

Para reproduzir os relatórios:

- Python compatível com o script escolhido;
- dependências de [scripts/requirements-capitulo-5.txt](scripts/requirements-capitulo-5.txt);
- em alguns geradores, pacotes adicionais como ReportLab e pypdf.

### Banco e variáveis de ambiente

Crie um banco vazio, por exemplo esportiva. As tabelas são criadas pelas migrations Flyway na inicialização.

O arquivo [src/main/resources/application.properties](src/main/resources/application.properties) espera:

| Variável | Finalidade |
| --- | --- |
| DB_URL | URL JDBC, por exemplo jdbc:postgresql://localhost:5432/esportiva. |
| DB_USERNAME | Usuário do PostgreSQL. |
| DB_PASSWORD | Senha do PostgreSQL. |
| JWT_PRIVATE_KEY | Chave privada RSA em PEM PKCS#8, com pelo menos 2048 bits. |

Exemplo para PowerShell; substitua os valores localmente e não os versione:

~~~powershell
$env:DB_URL = "jdbc:postgresql://localhost:5432/esportiva"
$env:DB_USERNAME = "usuario_local"
$env:DB_PASSWORD = "senha_local"
$env:JWT_PRIVATE_KEY = Get-Content -Raw -LiteralPath "C:\caminho\seguro\jwt-private-key.pem"
~~~

O servidor usa a porta 8081. O access token possui TTL padrão de 15 minutos e o refresh token possui TTL de 7 dias.

Não coloque senhas, chaves privadas ou tokens no README, no código, em URLs, em commits ou em logs.

### Executar a aplicação

Na raiz:

~~~powershell
.\mvnw.cmd clean verify
.\mvnw.cmd spring-boot:run
~~~

Alternativamente:

~~~powershell
.\mvnw.cmd clean package
java -jar target\esportiva-0.0.1-SNAPSHOT.jar
~~~

A API estará em http://localhost:8081, desde que o PostgreSQL e as variáveis estejam configurados.

### Executar os testes

~~~powershell
.\mvnw.cmd test
.\mvnw.cmd clean verify
~~~

Para uma classe específica:

~~~powershell
.\mvnw.cmd -Dtest=HealthControllerTest test
~~~

Testes do comparador:

~~~powershell
python scripts/sonar/test_compare_branches.py
~~~

Os testes Java cobrem controllers, serviços, mapeamentos, autorização, estrutura e persistência. O resultado deve ser interpretado junto da indicação de testes ignorados.

### Verificar a API

Com o servidor em execução:

~~~powershell
Invoke-RestMethod -Method Get -Uri "http://localhost:8081/health"
~~~

Resposta esperada:

~~~json
{
  "status": "operational"
}
~~~

/health confirma que o processo HTTP respondeu. Não substitui validação de banco, migrations, autenticação ou regras de negócio.

### Autenticação e segurança

Os perfis presentes no domínio são ADMIN, PROFESSOR e ALUNO. As rotas públicas atuais são:

- GET /health;
- POST /auth/login;
- POST /auth/refresh.

As demais rotas exigem autenticação, com autorização configurada em [SecurityConfig.java](src/main/java/devheloisacabral/tcc/com/esportiva/config/SecurityConfig.java). O fluxo inclui login, access token RS256, refresh token, logout e GET /auth/me.

As senhas são armazenadas como hash. Este repositório não contém o frontend; qualquer cliente deve tratar tokens com armazenamento seguro apropriado à plataforma.

### Banco e migrations

As migrations estão em [src/main/resources/db/migration/](src/main/resources/db/migration/) e seguem V<número>__<descrição>.sql. Atualmente a sequência vai de V1 a V15 e cobre usuários, refresh tokens, alunos, planos, mensalidades, professores, turmas, matrículas, aulas, presenças, histórico de planos, pagamentos, estornos, status de matrícula e lista de espera.

O Hibernate está configurado com ddl-auto=validate: valida o schema, mas não deve criar ou alterar tabelas automaticamente. Alterações estruturais devem usar uma nova migration Flyway.

## O experimento do TCC

O experimento acompanha 20 incrementos:

- P01–P10: criação inicial e expansão da base;
- P11–P20: manutenção e evolução do sistema existente.

Cada incremento representa um estado sucessivo do mesmo projeto; as observações não são 20 experimentos independentes.

### Pergunta de pesquisa

> Como evoluem os indicadores de qualidade interna do Esportiva Plus ao longo de 20 incrementos gerados por inteligência artificial a partir de especificações BDD/Gherkin?

### Indicadores observados

A tabela consolidada reúne tamanho, complexidade ciclomática e cognitiva, testes, cobertura, cobertura de condições, bugs, vulnerabilidades, security hotspots, code smells, duplicação, débito técnico, falhas, erros, testes ignorados, sucesso dos testes e Quality Gate.

### Panorama numérico

| Estado | Linhas | Ciclomática | Cognitiva | Testes | Cobertura | Condições | Bugs | Vulnerabilidades | Smells | Duplicação | Gate |
| --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: | --- |
| P01 | 212 | 12 | 2 | 8 | 40,7% | 50,0% | 0 | 0 | 0 | 0,0% | NONE |
| P10 | 2.009 | 355 | 88 | 64 | 70,2% | 67,7% | 0 | 1 | 0 | 1,1% | OK |
| P20 | 2.492 | 469 | 137 | 106 | 74,1% | 71,7% | 1 | 1 | 2 | 1,7% | OK |

A base fechada contém 20 versões e 16 métricas numéricas por versão, totalizando 320 medições. A análise derivada confere 304 deltas consecutivos. Isso descreve uma série documental; não representa 320 execuções independentes.

## Prompts, BDD e registros

### Onde encontrar os prompts

Os prompts estão em [docs/experimento/prompts/](docs/experimento/prompts/):

| Pasta | Conteúdo |
| --- | --- |
| [v1/](docs/experimento/prompts/v1/) | Especificações P01–P10, fase de criação inicial. |
| [v2/](docs/experimento/prompts/v2/) | Especificações P11–P20, fase de manutenção e evolução. |

Sufixos:

- PXX-C.md: prompt da funcionalidade ou manutenção;
- PXX-T.md: prompt voltado à implementação ou ampliação dos testes.

Os prompts usam a estrutura BDD/Gherkin em português, com contexto, Dado, Quando, Então e E. A existência de um cenário no prompt documenta a especificação solicitada; não prova, sozinha, que o cenário foi automatizado ou que todos os requisitos foram validados em produção.

Exemplos:

- [P01-C — fundação](docs/experimento/prompts/v1/P01-C.md);
- [P01-T — testes da fundação](docs/experimento/prompts/v1/P01-T.md);
- [P10-C — dashboard](docs/experimento/prompts/v1/P10-C.md);
- [P11-C — vencimento individual](docs/experimento/prompts/v2/P11-C.md);
- [P20-C — reagendamento](docs/experimento/prompts/v2/P20-C.md).

### Registros de implementação e testes

Os registros estão em:

- [docs/experimento/registros/v1/](docs/experimento/registros/v1/): P01–P10;
- [docs/experimento/registros/v2/](docs/experimento/registros/v2/): P11–P20.

Cada incremento normalmente possui PXX-C.md, associado à implementação, e PXX-T.md, associado aos testes e validações. Os registros podem informar branch, commit, arquivos, testes Maven, PostgreSQL, Flyway, /health e SonarCloud.

Quando uma validação não foi executada, ela deve permanecer explícita como pendente. Compilação local não é prova de validação em produção.

## Base consolidada e análise longitudinal

A fonte numérica principal é [docs/experimento/resultado-20-prompts.md](docs/experimento/resultado-20-prompts.md), que apresenta a série P01–P20 e seus deltas.

A análise derivada está em [docs/experimento/analise-longitudinal/](docs/experimento/analise-longitudinal/):

- [analise-longitudinal.md](docs/experimento/analise-longitudinal/analise-longitudinal.md): relatório de apoio ao TCC;
- dados-normalizados.json: dados extraídos e indicadores derivados;
- manifesto.json: hashes, referências locais e metadados;
- conferencia.json: controles numéricos;
- [LEIA-ME.md](docs/experimento/analise-longitudinal/LEIA-ME.md): instruções e limites;
- graficos/: PNG e SVG.

O material do Capítulo 5 está em [docs/tcc/03-capitulos/05-resultados/](docs/tcc/03-capitulos/05-resultados/), com tabelas, figuras, dados derivados e auditoria.

### Regerar a análise

~~~powershell
python scripts/generate_analise_tcc.py
~~~

Consulte o LEIA-ME antes de executar: o gerador confere a fonte, recalcula os deltas e pode atualizar docs/ e output/.

Para o material do Capítulo 5:

~~~powershell
python scripts/generate_capitulo_5.py
~~~

Instale as dependências em ambiente isolado. O [LEIA-ME do Capítulo 5](docs/tcc/03-capitulos/05-resultados/LEIA-ME.md) registra as dependências e controles de reprodução.

## Workflow de comparação SonarCloud

A comparação é feita por [scripts/sonar/compare_branches.py](scripts/sonar/compare_branches.py), chamado por:

- [.github/workflows/sonar-branch-comparison.yml](.github/workflows/sonar-branch-comparison.yml);
- [.github/workflows/sonar-commits.yml](.github/workflows/sonar-commits.yml).

Os dois workflows usam workflow_dispatch: devem ser iniciados manualmente em **Actions > Run workflow**.

### Passo a passo

1. Abra o repositório no GitHub.
2. Acesse **Actions**.
3. Selecione o workflow de comparação.
4. Clique em **Run workflow**.
5. Informe:
   - branches: ALL ou uma lista separada por vírgula/quebra de linha;
   - baseline_branch: opcional, para a primeira comparação;
   - fail_on_quality_gate: true para falhar se algum gate for ERROR.
6. Aguarde a execução.
7. Consulte o Summary.
8. Baixe o artefato sonar-comparacao-branches-<run_number>, que contém sonar/reports/.

A seleção ALL considera branches com nomes prompt-* e manutencao-* e o comparador limita a seleção a 20 branches.

### Secret e configurações

O workflow usa Java 21, Python 3.13, fetch-depth 0 e cache Maven. A organização e a chave do SonarCloud estão no workflow. O token deve existir apenas como secret:

~~~text
SONAR_TOKEN
~~~

Não copie o token para código, README, URL, arquivo versionado ou log.

### O que o script faz

1. identifica branches experimentais no remoto;
2. seleciona ou preserva a ordem indicada;
3. faz checkout dos commits;
4. executa Maven verify e o scanner Maven do SonarCloud;
5. aguarda a análise;
6. consulta métricas e Quality Gate;
7. calcula deltas;
8. grava JSON, CSV e Markdown;
9. opcionalmente falha quando FAIL_ON_QUALITY_GATE=true e algum gate é ERROR.

O script faz checkout forçado e limpa target/ durante a análise. Use uma cópia de trabalho destinada à coleta e confirme o estado do Git antes de iniciar.

### Executar localmente

A execução local exige Java 21, Python, Git com referências remotas, Maven, rede e token válido. O script usa ./mvnw quando o wrapper está presente; por isso, o caminho mais previsível é Linux, WSL, Git Bash ou GitHub Actions.

~~~bash
export SONAR_HOST_URL="https://sonarcloud.io"
export SONAR_ORGANIZATION="devheloisacabral"
export SONAR_PROJECT_KEY="devheloisacabral_PROJETO-FINAL-DE-CONCLUSAO-DE-CURSO-ESPORTIVA-PLUS"
export SONAR_TOKEN="<token-fornecido-fora-do-repositorio>"
export SELECTED_BRANCHES="ALL"
export BASELINE_BRANCH=""
export FAIL_ON_QUALITY_GATE="false"
export REPORTS_DIR="sonar/reports"

python3 scripts/sonar/compare_branches.py
~~~

Para branches específicas:

~~~bash
export SELECTED_BRANCHES="prompt-01-fundacao,prompt-02-autenticacao,manutencao-11-vencimento-individual-por-aluno"
python3 scripts/sonar/compare_branches.py
~~~

Antes da coleta:

~~~bash
git fetch --all --prune
git branch -r
~~~

Testes do comparador:

~~~bash
python3 scripts/sonar/test_compare_branches.py
~~~

## Relatórios gerados

A comparação pode gerar sonar/reports/:

| Arquivo | Conteúdo |
| --- | --- |
| impacto-branches.md | Tabela para o Summary. |
| impacto-branches.csv | Métricas e deltas em formato tabular. |
| impacto-branches.json | Snapshots, métricas, commits e datas. |

Preserve data, número do workflow, commit, configuração do SonarCloud e proveniência da coleta. O token nunca deve aparecer nos relatórios.

## Como interpretar os resultados

- cobertura maior não significa, isoladamente, ausência de bugs ou vulnerabilidades;
- Quality Gate OK significa atendimento às condições configuradas, não certificação de ausência de defeitos;
- complexidade maior pode acompanhar crescimento do escopo e não é, sozinha, prova de deterioração;
- quantidade de testes e cobertura medem dimensões diferentes;
- contagem estável não prova que os mesmos apontamentos permaneceram;
- P01–P20 são estados dependentes no tempo do mesmo sistema;
- a sequência não representa intervalos iguais de tempo, esforço ou tamanho funcional.

Os indicadores devem ser lidos em conjunto, preservando tendências, oscilações, resultados desfavoráveis e divergências.

## Limitações metodológicas

1. Ainda não há vínculo comprovado entre cada medição, o commit efetivamente analisado e a execução correspondente do SonarCloud.
2. Os links disponíveis não confirmam, por si só, a configuração histórica completa de cada análise.
3. Há divergência entre P10-T/P11-T e a tabela consolidada quanto a testes e testes ignorados: os registros informam 61 testes e um ignorado em determinadas execuções, enquanto a tabela registra 64 testes e zero ignorados para P10/P11.
4. Quality Gate aprovado não prova ausência de defeitos, segurança ou conformidade geral.
5. Um apontamento do SonarCloud exige análise adicional para interpretação funcional ou de explorabilidade.
6. As 20 observações não são uma amostra de 20 projetos independentes.
7. O desenho atual não permite afirmar causalidade da IA ou do BDD.
8. P30, P40 e P50 são possibilidades de extensão, não previsões nem valores estimados.

Essas limitações não impedem descrever a série, mas delimitam o alcance das conclusões.

## Próximos passos

O planejamento atual concentra-se em:

- integrar a análise de resultados ao texto completo do TCC;
- revisar o Capítulo 5 com o orientador;
- alinhar metodologia, terminologia, tabelas e figuras;
- recuperar, quando possível, os artefatos originais de análise e execução;
- relacionar medições, commits e análises efetivas;
- conciliar divergências entre registros de testes e tabela consolidada;
- manter explícitas as limitações de proveniência, dependência temporal e causalidade.

O escopo atual não inclui criar novos incrementos P21 ou ampliar a série para P30, P40 ou P50 sem decisão metodológica explícita.

## Uso acadêmico

Este repositório é mantido como projeto acadêmico e base documental do TCC. Ao reutilizar código, prompts, métricas ou figuras, preserve a autoria, a origem dos dados, a data da análise e as limitações metodológicas registradas no projeto.
