# SonarQube por commit

O workflow `.github/workflows/sonar-commits.yml` executa no `push` de qualquer branch e no `workflow_dispatch`.

Para cada commit incluído no evento `push`, ele:

1. faz checkout do commit;
2. executa o build Maven e o SonarScanner for Maven;
3. usa o SHA completo como `sonar.projectVersion` e `sonar.scm.revision`;
4. aguarda o processamento no SonarQube;
5. registra as métricas e a diferença para o commit anterior;
6. publica `impacto-commits.md` no resumo da execução e arquiva `sonar/reports/`.

O build executa os testes com `mvn verify`. O JaCoCo é executado pelo Maven e gera `target/site/jacoco/jacoco.xml`; esse arquivo é enviado ao SonarQube por meio de `sonar.coverage.jacoco.xmlReportPaths`.

Quando o push contém mais de um commit, todos são analisados em ordem. Quando existe um commit anterior com `pom.xml`, ele é analisado como baseline técnico para que o primeiro commit do push também tenha comparação.

## Configuração no GitHub

Em `Settings > Secrets and variables > Actions`, crie:

- Secret `SONAR_TOKEN`: token de usuário do SonarQube com permissão para analisar o projeto.

O workflow está configurado diretamente para o SonarQube Cloud:

- `SONAR_HOST_URL`: `https://sonarcloud.io`.
- `SONAR_PROJECT_KEY`: `devheloisacabral_PROJETO-FINAL-DE-CONCLUSAO-DE-CURSO-ESPORTIVA-PLUS`.
- `SONAR_ORGANIZATION`: `devheloisacabral`.

O token não deve ser colocado no repositório, no workflow, na URL ou nos relatórios.

Os testes também precisam estar commitados e enviados ao GitHub. Um arquivo de teste apenas no computador local não participa da análise do Action.

## Execução manual

O workflow pode ser executado pela aba `Actions`. Nesse caso, o commit selecionado em `workflow_dispatch` será analisado; se houver pai com `pom.xml`, ele servirá como baseline.

## Limite da comparação

O SonarQube fornece as métricas do código analisado. A tabela registra a diferença numérica entre análises consecutivas; ela não atribui causalidade automaticamente a um commit. Para causalidade, deve-se complementar o relatório com o diff do Git e os arquivos alterados.
