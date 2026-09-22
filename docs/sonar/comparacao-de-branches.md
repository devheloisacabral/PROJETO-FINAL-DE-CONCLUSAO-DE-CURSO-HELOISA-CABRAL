# Comparação das branches no SonarCloud

O workflow **SonarCloud - comparar branches** consulta as análises que já existem no SonarCloud e produz uma tabela na qual cada branch é comparada à branch anterior. Ele não executa uma nova análise nem modifica o histórico do projeto.

## Como executar

1. Confirme em `Settings > Secrets and variables > Actions` que o secret `SONAR_TOKEN` existe. O token precisa de permissão para navegar pelo projeto.
2. Abra `Actions > SonarCloud - comparar branches > Run workflow`.
3. Em **branches**, informe de uma a dez branches na ordem do experimento, separadas por vírgulas, ponto e vírgula ou quebras de linha. Exemplo: `prompt-01, prompt-02, prompt-03`. Use `ALL` para incluir todas as branches analisadas, em ordem natural do nome.
4. Opcionalmente, informe uma **baseline branch**. Ela não vira uma linha: é usada apenas para calcular o impacto da primeira branch selecionada.
5. Escolha se o job deve falhar quando alguma branch tiver o Quality Gate reprovado.

Ao final, a tabela aparece no **Summary** da execução. Um artefato retido por 90 dias contém:

- `impacto-branches.md`: tabela pronta para leitura;
- `impacto-branches.csv`: valores e deltas de todas as métricas, adequado para Excel, LibreOffice, R ou Python;
- `impacto-branches.json`: dados estruturados e metadados da coleta.

## Métricas

A tabela apresenta bugs, vulnerabilidades, hotspots de segurança, code smells, cobertura, cobertura de branches, duplicação, complexidade, complexidade cognitiva e linhas de código. O CSV também inclui cobertura de linhas, blocos duplicados, testes e densidade de sucesso dos testes.

O número entre parênteses é o delta (`branch atual - branch anterior`). A cobertura maior é considerada melhora; para defeitos, duplicação e complexidade, valores menores são considerados melhora. A cor é apenas um auxílio visual e não substitui a interpretação do experimento.

## Observações importantes

- A branch precisa ter sido analisada no mesmo projeto do SonarCloud antes da coleta.
- A ordem digitada é preservada. Com `ALL`, nomes são ordenados naturalmente (`prompt-2` antes de `prompt-10`).
- Métricas indisponíveis aparecem como `N/D`, sem serem convertidas incorretamente para zero.
- Um snapshot mostra o estado da branch na última análise disponível; ele não prova causalidade isoladamente.
- O token nunca é gravado nos relatórios ou enviado como parâmetro de URL.
