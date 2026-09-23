# Comparação das branches no SonarCloud

O workflow **SonarCloud - comparar branches** analisa cada checkout experimental, em ordem cronológica, no projeto SonarCloud principal. Cada execução recebe o commit como `sonar.projectVersion`, e o script coleta o snapshot logo após o processamento. Assim o histórico permanece rastreável sem consumir a cota de linhas criando um projeto independente para cada branch.

## Pré-requisitos

Em `Settings > Secrets and variables > Actions`, crie o secret `SONAR_TOKEN`. Além da permissão de executar análises, o token deve pertencer a um usuário autorizado a **Create Projects** na organização `devheloisacabral`. O token não é incluído nos relatórios.

O workflow usa a chave original como prefixo e gera uma chave determinística para cada branch, por exemplo:

```text
devheloisacabral_PROJETO-FINAL-DE-CONCLUSAO-DE-CURSO-ESPORTIVA-PLUS__experiment__prompt-01-a1b2c3d4
```

O projeto principal é reutilizado em todas as análises. Projetos experimentais criados por versões anteriores do script não são removidos automaticamente, pois a remoção exige uma decisão explícita após a exportação dos relatórios.

## Como executar

1. Faça o merge do workflow na branch padrão; o GitHub só exibe **Run workflow** para arquivos presentes nela.
2. Abra `Actions > SonarCloud - comparar branches > Run workflow`.
3. Em **branches**, informe de uma a vinte branches na ordem do experimento, separadas por vírgula, ponto e vírgula ou quebra de linha. Exemplo: `prompt-01, prompt-02, prompt-03`.
4. `ALL` seleciona automaticamente apenas branches experimentais com prefixo `prompt-` ou `manutencao-`, até vinte referências, em ordem natural. Branches auxiliares são ignoradas.
5. A **baseline branch** é opcional: ela também ganha um projeto, mas serve somente para calcular o primeiro delta.
6. Escolha se um Quality Gate reprovado deve falhar o job.

Para cada branch, o workflow faz checkout detached, executa `mvn verify`, gera o relatório JaCoCo, executa o SonarScanner no projeto principal, aguarda o processamento no SonarCloud e só então coleta as métricas. Nenhuma análise usa o recurso de branch secundária do SonarCloud.

## Resultados

O Summary contém uma tabela com uma linha por branch e links para os projetos independentes. O artefato retido por 90 dias contém:

- `impacto-branches.md`: tabela para leitura;
- `impacto-branches.csv`: métricas e deltas para Excel, LibreOffice, R ou Python;
- `impacto-branches.json`: dados estruturados, commits e chaves dos projetos.

A tabela apresenta bugs, vulnerabilidades, hotspots, code smells, cobertura, cobertura de branches, duplicação, complexidade, complexidade cognitiva, linhas de código, débito técnico e métricas detalhadas de testes. O CSV também contém cobertura de linhas, blocos duplicados, falhas, erros, testes ignorados, tempo de execução e sucesso dos testes. A data registrada é a data oficial retornada pelo SonarCloud; valores ausentes aparecem como `N/D`, nunca como zero.

O delta é `branch atual - branch anterior`. Cobertura maior é marcada como melhora; para defeitos, duplicação e complexidade, valores menores são marcados como melhora. A cor é somente um auxílio visual e não prova causalidade.

## Limpeza

Os projetos são mantidos para garantir rastreabilidade e permitir repetição do experimento. Se precisar removê-los, faça isso pelo SonarCloud somente depois de exportar os artefatos; filtre projetos cuja chave contenha `__experiment__`. O workflow nunca remove projetos automaticamente.
