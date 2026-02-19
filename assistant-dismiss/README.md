# Assistant Dismiss — Fix bolla Assistente Google

## Il problema

Quando dici **"Hey Google, metti [canzone]"**, YouTube Music ReVanced riproduce il brano ma la **bolla dell'Assistente Google resta aperta** sullo schermo. Questo succede perché il server di Google non manda il comando di chiusura quando la musica parte tramite microG.

## La soluzione

Una piccola app separata che funziona come servizio di accessibilità:

1. Rileva quando si apre la bolla dell'Assistente
2. Aspetta che la musica parta
3. La chiude automaticamente

Se **non** stai ascoltando musica (es. fai una domanda all'Assistente), la bolla **non** viene toccata.

> Perché un'app separata e non dentro GmsCore? Perché GmsCore è troppo grande — Android blocca i servizi di accessibilità di app con troppi componenti. Un'app leggera a parte funziona senza problemi.

---

## Guida passo-passo

Prerequisiti: telefono collegato via USB con **debug USB attivo**, e `adb` installato sul PC.

### Passo 1 — Compila GmsCore

```bash
cd GmsCore
./gradlew :play-services-core:assembleHmsDefaultRelease
```

L'APK esce in:
```
play-services-core/build/outputs/apk/hmsDefault/release/app.revanced.android.gms-*.apk
```

### Passo 2 — Compila Assistant Dismiss

```bash
./gradlew :assistant-dismiss:assembleRelease
```

L'APK esce in:
```
assistant-dismiss/build/outputs/apk/release/assistant-dismiss-release-unsigned.apk
```

### Passo 3 — Firma entrambe le APK

Servono firmate per poterle installare. Qui usiamo il keystore di debug (va bene per uso personale):

```bash
# Firma GmsCore
jarsigner -keystore ~/.android/debug.keystore -storepass android \
  play-services-core/build/outputs/apk/hmsDefault/release/app.revanced.android.gms-*.apk \
  androiddebugkey

# Firma Assistant Dismiss
jarsigner -keystore ~/.android/debug.keystore -storepass android \
  assistant-dismiss/build/outputs/apk/release/assistant-dismiss-release-unsigned.apk \
  androiddebugkey
```

### Passo 4 — Installa le APK sul telefono

```bash
# Installa GmsCore
adb install -r play-services-core/build/outputs/apk/hmsDefault/release/app.revanced.android.gms-*.apk

# Installa Assistant Dismiss
adb install -r assistant-dismiss/build/outputs/apk/release/assistant-dismiss-release-unsigned.apk
```

### Passo 5 — Attiva il servizio di accessibilità

```bash
adb shell settings put secure enabled_accessibility_services \
  "app.revanced.android.gms.assistant/org.microg.gms.assistant.AssistantDismissService"
adb shell settings put secure accessibility_enabled 1
```

Oppure dal telefono: **Impostazioni > Accessibilità > Assistant Dismiss > Attiva**.

### Passo 6 — Verifica

```bash
adb shell dumpsys accessibility | grep assistant
```

Se vedi `Enabled services` con `app.revanced.android.gms.assistant` dentro, è tutto OK.

---

## Test

| Comando vocale | Risultato atteso |
|---|---|
| "Hey Google, metti una canzone" | La bolla si chiude dopo qualche secondo |
| "Hey Google, che tempo fa?" | La bolla resta aperta (nessuna musica = nessuna azione) |
