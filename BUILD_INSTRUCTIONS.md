# BUILD INSTRUCTIONS — Bro, Just Get Up (BJGU)

## 📱 Como compilar e instalar o APK

### Pré-requisitos
- **Android Studio** (Hedgehog 2023.1.1 ou superior recomendado)
- **JDK 17** (incluído no Android Studio)
- **Dispositivo Android 8.0+** (API 26+) ou emulador

---

### 1. Abrir o projeto no Android Studio
1. Abre o Android Studio
2. Clica em **File > Open**
3. Navega até à raiz deste projeto (onde está `settings.gradle.kts`)
4. Clica **OK** e espera o Gradle sincronizar (primeira vez demora ~3–5 min)

---

### 2. Gerar APK de debug
1. No menu: **Build > Build Bundle(s) / APK(s) > Build APK(s)**
2. Espera a build terminar (ver barra de progresso em baixo)
3. O APK estará em:  
   `app/build/outputs/apk/debug/app-debug.apk`

---

### 3. Instalar no dispositivo via ADB
```bash
# Verificar se o dispositivo está ligado
adb devices

# Instalar o APK
adb install app/build/outputs/apk/debug/app-debug.apk

# Se der erro "INSTALL_FAILED_UPDATE_INCOMPATIBLE", desinstala primeiro:
adb uninstall com.bjgu.app
```

---

## 🔐 Permissões que o utilizador TEM de conceder manualmente

A app vai pedir estas permissões ao abrir pela primeira vez.
Se as negaste, tens de as ativar manualmente:

### a) Alarme exato (Android 12+)
- **Definições > Apps > BJGU > Alarmes e lembretes > Permitir alarmes exatos**
- Ou: **Definições > Apps > Apps especiais > Alarmes e lembretes > BJGU**

### b) Ignorar otimização de bateria
- **Definições > Apps > BJGU > Bateria > Não otimizar**
- Ou: **Definições > Bateria > Otimização de bateria > BJGU > Não otimizar**

---

## ⚠️ Notas importantes sobre fiabilidade

### Modo Doze / Economia de bateria
O Android pode adiar alarmes em modo Doze (ecrã desligado, parado).  
Para **garantir** que os alarmes tocam sempre:

1. **Xiaomi (MIUI):** Desativar "Restrição de atividade em segundo plano" e "Poupança de energia" para a app
2. **Samsung (One UI):** Desativar "Colocar app em pausa" em Definições > Bateria > Limites de utilização
3. **Huawei (EMUI):** Adicionar BJGU às "Apps protegidas" em Otimizador > Apps protegidas
4. **OPPO/OnePlus (ColorOS):** Permitir "Execução em segundo plano" nas Definições da app
5. **Pixel/Android stock:** Desativar "Otimização de bateria" (a app já pede)

### Ecrã de bloqueio
Alguns fabricantes bloqueiam Activities por cima do ecrã de bloqueio.  
Se o alarme não aparecer por cima do ecrã de bloqueio, verifica:
- **Definições > Apps > BJGU > Mostrar por cima de outras apps** (ativar)
- **Definições > Ecrã de bloqueio > Notificações** (ativar para a BJGU)

---

## 🧪 Testar o alarme
1. Cria um alarme para daqui a 1 minuto (ex: se são 14:05, cria para 14:06)
2. Bloqueia o ecrã e espera
3. O alarme deve abrir a Activity por cima do ecrã de bloqueio
4. Resolve o desafio matemático
5. O botão verde "DESLIGAR ALARME" deve aparecer

---

## 📦 Estrutura do projeto
```
alarmes/
├── app/
│   ├── src/main/
│   │   ├── java/com/bjgu/app/
│   │   │   ├── alarm/          # AlarmScheduler, AlarmReceiver, BootReceiver, PermissionManager
│   │   │   ├── challenges/     # ChallengeGenerator, MathChallenge
│   │   │   ├── data/alarm/     # AlarmEntity, AlarmDao, AlarmDatabase, AlarmRepository
│   │   │   ├── ui/
│   │   │   │   ├── alarms/     # MainActivity, AlarmAdapter, AlarmViewModel
│   │   │   │   ├── create/     # CreateAlarmActivity
│   │   │   │   └── ringing/    # AlarmRingingActivity
│   │   │   └── BJGUApplication.kt
│   │   ├── res/
│   │   │   ├── values/         # strings.xml (EN), colors.xml, themes.xml
│   │   │   ├── values-pt/      # strings.xml (PT-PT)
│   │   │   ├── layout/         # Todos os layouts XML
│   │   │   ├── drawable/       # Ícones e backgrounds
│   │   │   └── mipmap-*/       # Ícones do launcher
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── gradle/wrapper/
```
