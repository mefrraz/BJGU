# Bro, Just Get Up (BJGU) 🔔

**Alarme anti-soneca para Android — acorda mesmo o teu cérebro.**

Ao contrário dos alarmes normais que desligas ainda meio a dormir, o BJGU obriga-te a **resolver um desafio matemático** antes de poderes desligar o alarme.  
O teu cérebro tem de estar mesmo ativo para desligar aquilo.

---

## ✨ Funcionalidades (v1.0 — MVP)

| Funcionalidade | Descrição |
|---|---|
| 📋 **Lista de alarmes** | Cria, liga/desliga e apaga alarmes facilmente |
| ⏰ **Agendamento fiável** | Usa `setAlarmClock()` — toca mesmo em modo Doze |
| 🧮 **Desafio matemático** | 3 níveis de dificuldade (fácil/médio/difícil) |
| 🔒 **Ecrã inquebrável** | Não consegues fechar com "voltar", swipe ou gestos |
| 🔊 **Som + vibração** | Som do sistema em loop com vibração contínua |
| 🌍 **2 idiomas** | Português (PT-PT) e Inglês |
| 💾 **Persistência local** | Room database — alarmes guardados no dispositivo |

---

## 📸 Ecrãs

| Principal | Criar Alarme | Alarme a Tocar |
|---|---|---|
| Lista de alarmes com switch on/off | TimePicker, dias, som, dificuldade | Desafio matemático full-screen |

---

## 🚀 Compilar e Instalar

### Android Studio
```bash
# Windows (PowerShell / CMD)
studio64.exe "C:\Users\andre\OneDrive\Documentos\alarmes"

# Ou abre o Android Studio e faz File > Open > seleciona esta pasta
```

### Gerar APK
1. **Build > Build Bundle(s) / APK(s) > Build APK(s)**
2. O APK fica em: `app/build/outputs/apk/debug/app-debug.apk`

### Instalar via ADB
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

📖 Instruções detalhadas em [BUILD_INSTRUCTIONS.md](BUILD_INSTRUCTIONS.md).

---

## 🏗️ Arquitetura

```
alarmes/
├── app/src/main/java/com/bjgu/app/
│   ├── alarm/           # AlarmScheduler, AlarmReceiver, BootReceiver, PermissionManager
│   ├── challenges/       # ChallengeGenerator (3 níveis de dificuldade)
│   ├── data/alarm/       # Room: AlarmEntity, AlarmDao, AlarmDatabase, AlarmRepository
│   ├── ui/alarms/        # MainActivity, AlarmAdapter, AlarmViewModel
│   ├── ui/create/        # CreateAlarmActivity
│   ├── ui/ringing/       # AlarmRingingActivity (full-screen, não fecha)
│   └── BJGUApplication.kt
├── app/src/main/res/
│   ├── layout/           # XMLs dos ecrãs
│   ├── values/           # strings.xml (EN), cores, temas
│   └── values-pt/        # strings.xml (PT-PT)
└── BUILD_INSTRUCTIONS.md
```

---

## 📋 Roadmap

### ✅ v1.0 — MVP (atual)
Alarme fiável + desafio matemático + persistência + 2 idiomas.

### 🔜 v2.0 — Melhorias (futuro)
- Modo escalada: se desligar demasiado rápido, novo alarme 2 min depois
- Shake to wake: agitar o telemóvel antes do desafio
- Estatísticas: gráfico de tempo médio de resposta
- Snooze limitado: máximo 1 snooze de 5 min

### 🔮 v3.0 — Extras (futuro)
- Código fora da cama: QR code para escanear noutra divisão
- Modo accountability: notificação a contacto se não desligar
- Mais idiomas: Espanhol, Francês, PT-BR

---

## 📄 Licença

MIT

---

**Feito com ☕ e raiva de manhã cedo.**
