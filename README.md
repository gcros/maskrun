# MaskRun

Jogo arcade Android inspirado em labirintos de movimento rápido, com identidade visual própria em neon/pixel art.

## Recursos iniciais
- Movimento por deslize ou setas na tela.
- Labirinto, moedas, armadilhas, vidas e pontuação.
- Fases progressivas e pausa.
- Tema escuro com detalhes neon.
- Build de APK debug pelo GitHub Actions.

## Gerar APK
1. Abra a aba **Actions** deste repositório.
2. Selecione **Build MaskRun APK** e aguarde a execução terminar.
3. Baixe o artefato **MaskRun-debug-apk** na execução concluída.
4. Extraia o ZIP e instale `app-debug.apk` no Android.

Também é possível disparar a compilação manualmente em **Actions → Build MaskRun APK → Run workflow**.

## Compilar localmente
Requer JDK 17, Android SDK com plataforma 35 e Gradle 8.9:
```bash
gradle --no-daemon assembleDebug
```

O projeto é um protótipo inicial; jogabilidade, equilíbrio das fases e compatibilidade devem ser testados no aparelho.
