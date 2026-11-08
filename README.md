# Own Tempo

Aplicação Android nativa (Kotlin) para registar treinos dia a dia, com vista mensal e semanal, sem obrigar a um plano fixo.

**Autor:** Carlos Leandro León Pérez, n.º 52813 — Programação de Dispositivos Móveis, Universidade da Beira Interior, 2026/27

## Como abrir, compilar e testar

**Ambiente:** Android Studio Ladybug (2024.2.1), Android Gradle Plugin 9.4.1, Gradle 9.6.0. `minSdk` 29; `compileSdk` e `targetSdk` 37. As versões das dependências estão em `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties` e `app/build.gradle.kts`.

1. Extrair o ZIP e abrir a pasta no Android Studio (*File > Open*). Aguardar o Gradle sync (na primeira vez precisa de Internet para descarregar as dependências; a aplicação em si não usa Internet).
2. Executar com *Run* num emulador ou dispositivo com Android 10 (API 29) ou superior.
3. O APK de depuração instalável (`app-debug.apk`) é entregue à parte. Para o instalar, arrastar o ficheiro para o emulador ou usar `adb install app-debug.apk`.
4. Testes unitários: *Build > Clean and Assemble Project with Tests* ou, no painel do projeto, clique direito em `app/src/test` e *Run Tests*.
5. Teste manual rápido:
   - Tocar num dia para abrir a visão geral do dia (`DayOverviewActivity`), tocar em `+` (ou no botão "+ Adicionar outro treino ou rotina") para escolher *Gym*, escrever a rotina `Legs` e guardar.
   - Tocar no treino criado para abrir o detalhe (`DayDetailActivity`), adicionar um exercício (definindo o seu nome, duração opcional em horas/minutos/segundos, séries, repetições e peso com decimais como 0.5 kg).
   - Ajustar o passo de peso diretamente no ecrã da rutina (0.5 kg, 1 kg, 2.5 kg, 5 kg) e usar os botões −/+ em tempo real.
   - Experimentar o cronómetro global de treino (com serviço em primeiro plano e **notificação persistente com botões de ação na barra de notificações** que continua ativo ao mudar de rotina).
   - Alternar entre *Month* e *Week*, e experimentar os ajustes (engrenagem): modo dia/noite/automático, vista inicial e meta semanal.
   - Fechar a aplicação por completo e voltar a abrir: os dados mantêm-se. Funciona também em modo avião.

## Descrição

O Own Tempo serve quem treina de forma variável (por exemplo, ginásio, boxe, futebol, ténis ou corrida) e não se identifica com apps que assumem uma semana-tipo fixa. Em cada dia regista-se o que foi feito, com suporte a múltiplos treinos no mesmo dia, cronómetro global persistente com **notificações interativas**, exercícios com séries, repetições, peso próprio, tempo por exercício e preenchimento automático da rotina anterior.

## Funcionalidades

- **Ecrã principal** com vista **mensal** por defeito, no mês atual segundo a data do dispositivo, e vista **semanal**, na semana atual. Botões ‹ ›, *Today* e seletor *Month | Week*.
- **Visão geral do dia (`DayOverviewActivity`):** ecrã intermédio ao tocar em qualquer data, permitindo registar e gerir múltiplos treinos/rotinas de diferentes categorias no mesmo dia.
- **Cronómetro global com notificações persistentes:** temporizador com serviço em primeiro plano (*Foreground Service*) e **notificação interativa na barra de estado do Android** (com ações de Pausa/Retomar e Parar), permitindo acompanhar e controlar o tempo de treino em tempo real mesmo com a aplicação em segundo plano ou o ecrã bloqueado.
- **Categorias predefinidas:** Gym, Running, Boxing, Soccer, Tennis, Hiking, Basketball e Pilates. Cada categoria define os campos que pede (Gym e Boxing com exercícios e rotinas; Running e Hiking com distância, tempo em horas/minutos e calorias; etc.).
- **Exercícios avançados:** cada exercício permite registar o seu tempo de duração (horas, minutos e segundos), séries individuais com repetições, peso próprio (com suporte a decimais como 0.5 kg) e reordenação por arrasto (*drag & drop*).
- **Passo de peso personalizável:** seletor rápido (0.5 kg, 1 kg, 2.5 kg, 5 kg) diretamente no ecrã da rutina.
- **Rotinas:** ao criar um dia com a mesma categoria e rotina, copiam-se os exercícios e as cargas da última sessão.
- **Sequência semanal:** sequência atual, melhor sequência e barra de progresso da semana, com meta configurável.
- **Ajustes:** modo dia, noite ou automático (segue o dispositivo); vista inicial (mês ou semana); meta semanal (1 a 7 treinos).
- **Ciclo completo de dados:** criar, listar, ver detalhe, editar e eliminar (dias, treinos, exercícios e séries), com confirmação e validação.
- **Adaptação da barra de sistema (`SystemBars`):** contraste automático da barra de estado (bateria, relógio, notificações) em modo claro e escuro.
- **Sem Internet:** todas as funcionalidades funcionam offline.

## Tecnologia de armazenamento

**Room sobre SQLite** (base de dados `own_tempo_db`, versão 5), com migrações incrementais (1→2, 2→3, 3→4 e 4→5) que preservam todos os dados existentes. As definições ficam em `SharedPreferences`.

## Estrutura dos dados

| Tabela | Colunas |
|---|---|
| `training_day` | `id` (chave primária), `date` (*epoch day*), `category` (chave da categoria), `routine`, `notes`, `durationMinutes`, `distanceKm`, `calories` |
| `exercise` | `id` (chave primária), `trainingDayId` (→ `training_day`, `ON DELETE CASCADE`), `name`, `displayOrder`, `durationSeconds` |
| `exercise_set` | `id` (chave primária), `exerciseId` (→ `exercise`, `ON DELETE CASCADE`), `setNumber`, `reps`, `weightKg` |

Um dia tem vários treinos/sessões, cada treino tem vários exercícios, e cada exercício tem várias séries. Eliminar um dia ou treino elimina em cascata os respetivos registos.

## Estrutura do código

```
pt.ubi.clp.owntempo
├── MainActivity.kt             calendário mensal/semanal e sequência
├── DayOverviewActivity.kt      visão geral e lista de treinos de um dia
├── DayDetailActivity.kt        detalhe de um treino, exercícios, séries e cronómetro
├── SettingsActivity.kt         ajustes
├── OwnTempoApp.kt              aplica o modo dia/noite ao arrancar
├── data    Category, TrainingDay, Exercise, ExerciseSet, DAO, DB, TrainingRepository, AppSettings
├── logic   StreakCalculator, Periods, WorkoutTimerManager, WorkoutTimerService, WorkoutTimerState
└── ui      TrainingViewModel, MonthAdapter, DayAdapter, DayOverviewAdapter, ExerciseAdapter, Dialogs, Formatting, SystemBars
```

## Decisões de implementação

- Camadas Activity → ViewModel → Repositório → DAO, para separar interface, lógica e dados. As consultas do Room devolvem `Flow`, atualizando a interface de forma reativa.
- **Múltiplos treinos por dia:** suportados através da entidade `TrainingDay` por data e do ecrã intermédio `DayOverviewActivity`.
- **Cronómetro em Foreground Service com Notificações:** implementado como um gestor global (`WorkoutTimerManager`) com serviço em primeiro plano e **notificação persistente na barra de estado** (`NotificationCompat.Builder`, canais de notificação e `PendingIntent` para botões de pausa e paragem), garantindo persistência e controlo fora da aplicação.
- **Séries numa tabela própria** (`exercise_set`), permitindo repetições e pesos individuais.
- **Rotina = última sessão copiada** numa transação Room (`db.withTransaction`).
- **Sequência semanal** (e não diária), considerando os dias de descanso planeados.
- **Barras de sistema adaptativas:** controlo programático de `isAppearanceLightStatusBars` para legibilidade perfeita da bateria e relógio.
- Todos os textos visíveis estão em `res/values/strings.xml`.

## Dificuldades

- **Gradle:** o projeto usa um Android Gradle Plugin com Kotlin integrado, incompatível com os plugins `kotlin-android` e `kotlin-kapt`. Resolvido com `com.android.legacy-kapt` (mesma versão do AGP) e atualizando o Room para 2.8.4.
- **Notificações e Foreground Services no Android 13/14+:** gestão de permissões de tempo de execução (`POST_NOTIFICATIONS`), especificação de tipos de serviços especiais (`foregroundServiceType="specialUse"`) e atualização em tempo real da notificação com ações.
- **Ecrãs pequenos e orientação horizontal:** utilização de `NestedScrollView` para evitar cortes visuais.
- **Gestão reativa do incremento de peso:** avaliação dinâmica do passo de peso no `ExerciseAdapter` para evitar atrasos no primeiro toque.

## Funcionalidades adicionais (face ao enunciado)

- Suporte a múltiplos treinos na mesma data com ecrã de resumo do dia.
- Cronómetro global de treino com **notificação persistente interativa** na barra de estado.
- Duração detalhada por exercício (horas, minutos e segundos).
- Seletor de passo de peso personalizável (0.5kg, 1kg, 2.5kg, 5kg) por rotina.
- Modo claro e escuro adaptativo com cores de status bar contrastantes.
- Testes automatizados unitários (`StreakCalculatorTest`, etc.).

## Bibliotecas externas

Todas oficiais (Google/JetBrains) e necessárias ao funcionamento:

- **Room** (`room-runtime`, `room-ktx`, `room-compiler`): armazenamento local.
- **AndroidX Lifecycle ViewModel & Runtime** (`lifecycle-viewmodel-ktx`): gestão de estado reativa.
- **Kotlin Coroutines e Flow**: operações assíncronas.
- **Material Components** (`material`): componentes Material 3 e RecyclerView.
- **AndroidX AppCompat, Core KTX, Activity KTX, ConstraintLayout.**
- **JUnit:** testes unitários.
- **Plugin `com.android.legacy-kapt`:** processador de anotações do Room.

## Utilização de ferramentas de IA

Foi utilizado o assistente **Claude (Anthropic)**, em conversa, entre setembro e novembro de 2026, para:
- discutir e refinar a proposta e arquitetura;
- gerar código para as camadas `data`, `logic` e `ui`, Activities e layouts;
- implementar o serviço de cronómetro em primeiro plano com notificações interativas e adaptação das barras de sistema;
- diagnosticar erros de configuração e redigir esta documentação.

A conceção global, funcionalidades, arquitetura e validação final foram executadas pelo autor no Android Studio.

## Limitações conhecidas

- A categoria de um dia não pode ser alterada depois de criado (é preciso eliminá-lo e criá-lo de novo).
- A interface está em inglês.
