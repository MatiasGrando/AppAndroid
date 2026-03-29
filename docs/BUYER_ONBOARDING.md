## Onboarding rápido de un nuevo comprador

Esta app ya soporta white-label por **buyer profile runtime** + **identidad visible install-time**. Para sumar un comprador nuevo, seguí este orden y NO mezcles responsabilidades.

---

## Mapa mental corto

- **Branding visual runtime**: textos, colores, fondos, logo dentro de la app.
- **Firebase config runtime**: credenciales que usa `FirebaseProvider` según el buyer activo.
- **Identidad visible install-time**: nombre e ícono con los que la app se instala y aparece en el launcher.
- **Features opcionales**: flags por buyer para prender/apagar comportamiento puntual.

La selección del buyer sale del manifest meta-data `com.example.reservasapp.CLIENT_PROFILE`, que hoy se alimenta desde `manifestPlaceholders`.

---

## Orden recomendado

1. Crear el profile Kotlin del nuevo comprador.
2. Agregar resources de branding (strings + colores + assets si hacen falta).
3. Registrar el profile en el resolver.
4. Configurar la identidad visible install-time (label + íconos + `clientProfileId`).
5. Ajustar features opcionales.
6. Hacer una revisión manual de consistencia.

---

## 1) Crear el profile runtime del comprador

### Crear archivo

Crear un archivo nuevo en:

- `app/src/main/java/com/example/reservasapp/branding/NuevoBuyerProfile.kt`

Tomá como referencia:

- `app/src/main/java/com/example/reservasapp/branding/LunchPointProfile.kt`
- `app/src/main/java/com/example/reservasapp/branding/SuperViandasProfile.kt`

### Qué tiene que definir sí o sí

- `id`: identificador estable del buyer. Ejemplo: `super-viandas-app`
- `buyerName`: nombre humano
- `branding: BrandingConfig`
- `featureFlags: FeatureFlags`
- `firebase: FirebaseConfig`

### Esqueleto mínimo

```kotlin
object NuevoBuyerProfile : ClientProfile {
    override val id: String = "super-viandas-app"
    override val buyerName: String = "SuperViandasAPP"

    override val branding: BrandingConfig = BrandingConfig(
        appNameRes = R.string.branding_super_viandas_app_app_name,
        loginTitleRes = R.string.branding_super_viandas_app_login_title,
        homeTitleRes = R.string.branding_super_viandas_app_home_title,
        homePrimaryActionRes = R.string.branding_super_viandas_app_home_primary_action,
        homeSecondaryActionRes = R.string.branding_super_viandas_app_home_secondary_action,
        appLogoRes = R.mipmap.ic_launcher_super_viandas_app,
        loginDecorRes = R.drawable.landing_background_super_viandas_app,
        homeBackgroundRes = R.drawable.bg_main_soft_super_viandas_app,
        confirmationBackgroundRes = R.drawable.bg_asian_food_super_viandas_app,
        loginBackgroundColorRes = R.color.branding_super_viandas_app_login_background,
        loginOverlayColorRes = R.color.branding_super_viandas_app_login_overlay,
        homeOverlayColorRes = R.color.branding_super_viandas_app_home_overlay,
        primaryActionColorRes = R.color.branding_super_viandas_app_primary_action,
        secondaryActionColorRes = R.color.branding_super_viandas_app_secondary_action,
        actionTextColorRes = R.color.branding_super_viandas_app_action_text,
        confirmationTitleColorRes = R.color.branding_super_viandas_app_confirmation_title,
        confirmationBodyTextColorRes = R.color.branding_super_viandas_app_confirmation_body,
        confirmationCardBackgroundColorRes = R.color.branding_super_viandas_app_confirmation_card_background,
        confirmationCardStrokeColorRes = R.color.branding_super_viandas_app_confirmation_card_stroke
    )

    override val featureFlags: FeatureFlags = FeatureFlags(
        brandedConfirmationScreen = true
    )

    override val firebase: FirebaseConfig = FirebaseConfig(
        applicationId = "REEMPLAZAR",
        apiKey = "REEMPLAZAR",
        projectId = "REEMPLAZAR",
        storageBucket = "REEMPLAZAR",
        googleWebClientId = "REEMPLAZAR"
    )
}
```

### Regla importante

El `id` del profile tiene que coincidir EXACTAMENTE con el `clientProfileId` que pongas después en `manifestPlaceholders`. Si no coincide, `ClientProfileResolver` hace fallback a `LunchPointProfile`.

---

## 2) Cargar branding visual runtime

### Strings

Modificar:

- `app/src/main/res/values/strings.xml`

Agregar al menos:

- `branding_<buyer>_app_name`
- `branding_<buyer>_login_title`
- `branding_<buyer>_home_title`
- `branding_<buyer>_home_primary_action`
- `branding_<buyer>_home_secondary_action`

Tomá el patrón ya usado por:

- `branding_lunch_point_*`
- `branding_super_viandas_*`

### Colores

Modificar:

- `app/src/main/res/values/colors.xml`

Agregar el set completo:

- `branding_<buyer>_login_background`
- `branding_<buyer>_login_overlay`
- `branding_<buyer>_home_overlay`
- `branding_<buyer>_primary_action`
- `branding_<buyer>_secondary_action`
- `branding_<buyer>_action_text`
- `branding_<buyer>_confirmation_title`
- `branding_<buyer>_confirmation_body`
- `branding_<buyer>_confirmation_card_background`
- `branding_<buyer>_confirmation_card_stroke`

### Assets visuales

Crear o reutilizar resources según necesidad:

- `app/src/main/res/drawable/*`
- `app/src/main/res/drawable-night/*`
- `app/src/main/res/mipmap-*/*`
- `app/src/main/res/mipmap-anydpi-v26/*`

Podés arrancar reutilizando fondos existentes si querés salir rápido. El punto es que el `BrandingConfig` nuevo siempre apunte a resources válidos.

---

## 3) Registrar el nuevo buyer en el resolver

Modificar:

- `app/src/main/java/com/example/reservasapp/branding/ClientProfileResolver.kt`

Agregar el profile nuevo en `profilesById`.

Ejemplo:

```kotlin
private val profilesById: Map<String, ClientProfile> = listOf(
    LunchPointProfile,
    SuperViandasProfile,
    NuevoBuyerProfile
).associateBy(ClientProfile::id)
```

Si te olvidás este paso, el manifest puede pedir el buyer nuevo pero la app igual cae en el default.

---

## 4) Configurar Firebase runtime

La configuración de Firebase del buyer NO sale del manifest. Sale del `FirebaseConfig` adentro del `ClientProfile` y la consume:

- `app/src/main/java/com/example/reservasapp/firebase/FirebaseProvider.kt`
- `app/src/main/java/com/example/reservasapp/ReservasApp.kt`

### Qué completar

Dentro de `NuevoBuyerProfile.kt`:

- `applicationId`
- `apiKey`
- `projectId`
- `storageBucket`
- `googleWebClientId`

### Regla operativa

- Si dejás credenciales dummy, login/firestore/storage van a apuntar mal.
- Para un buyer nuevo, la fuente de verdad es el `FirebaseConfig` del profile, no múltiples `google-services.json` por buyer.
- El `google-services.json` actual del módulo se mantiene como base del proyecto/plugin, pero la resolución runtime del buyer ya está centralizada en `FirebaseProvider`.

---

## 5) Configurar identidad visible install-time

Esto define cómo se instala y cómo se ve la app FUERA del runtime: nombre bajo el ícono, ícono principal y `clientProfileId` inicial.

### Archivo a modificar

- `app/build.gradle.kts`

### Campos a revisar en `defaultConfig.manifestPlaceholders`

- `clientProfileId`
- `launcherLabel`
- `launcherIcon`
- `launcherRoundIcon`

Ejemplo:

```kotlin
manifestPlaceholders += mapOf(
    "clientProfileId" to "super-viandas-app",
    "launcherLabel" to "@string/branding_super_viandas_app_app_name",
    "launcherIcon" to "@mipmap/ic_launcher_super_viandas_app",
    "launcherRoundIcon" to "@mipmap/ic_launcher_super_viandas_app_round"
)
```

### NO tocar salvo que cambie la arquitectura

- `app/src/main/AndroidManifest.xml`

Ese archivo ya está bien: consume `${clientProfileId}`, `${launcherLabel}`, `${launcherIcon}` y `${launcherRoundIcon}`. La identidad install-time se cambia desde placeholders, no hardcodeando el manifest.

---

## 6) Features opcionales por buyer

Archivo relevante:

- `app/src/main/java/com/example/reservasapp/branding/FeatureFlags.kt`

Hoy existe:

- `brandedConfirmationScreen`

### Regla

- Si el buyer nuevo solo necesita el comportamiento actual, configurá el flag y listo.
- Si necesitás una feature nueva por buyer, primero agregá la propiedad en `FeatureFlags`, después consumila desde la UI/caso de uso que corresponda.
- No metas diferencias visuales simples como flags si ya se resuelven con `BrandingConfig`.

---

## 7) Checklist manual de consistencia (sin build)

Antes de dar por terminado el onboarding, revisá esto a mano:

- [ ] Existe `NuevoBuyerProfile.kt`
- [ ] El `id` del profile coincide con `clientProfileId` en `app/build.gradle.kts`
- [ ] `ClientProfileResolver.kt` registra el profile nuevo
- [ ] Todos los `R.string.*` usados por el profile existen en `strings.xml`
- [ ] Todos los `R.color.*` usados por el profile existen en `colors.xml`
- [ ] Todos los drawables/mipmap referenciados existen de verdad
- [ ] `FirebaseConfig` tiene valores reales del buyer
- [ ] `launcherLabel` apunta al nombre correcto del buyer
- [ ] `launcherIcon` y `launcherRoundIcon` apuntan a assets del buyer o a un fallback aceptado

---

## Archivos que normalmente tocás

### Siempre

- `app/src/main/java/com/example/reservasapp/branding/NuevoBuyerProfile.kt` **(nuevo)**
- `app/src/main/java/com/example/reservasapp/branding/ClientProfileResolver.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values/colors.xml`
- `app/build.gradle.kts`

### Según branding visual

- `app/src/main/res/drawable/*`
- `app/src/main/res/drawable-night/*`
- `app/src/main/res/mipmap-*/*`
- `app/src/main/res/mipmap-anydpi-v26/*`

### Solo si aparece una necesidad nueva

- `app/src/main/java/com/example/reservasapp/branding/FeatureFlags.kt`
- Pantallas que consumen `AppRuntime.branding` o `AppRuntime.featureFlags`

---

## Errores comunes que esta arquitectura deja explícitos

- **Mismatch entre `clientProfileId` e `id`** → la app cae en `LunchPointProfile`.
- **Cambiar `AndroidManifest.xml` a mano** en vez de placeholders → duplicás configuración y rompés el flujo actual.
- **Meter branding visual en lógica de negocio** → está mal; eso va en `BrandingConfig` o `FeatureFlags`.
- **Confiar en strings o colores que no existen** → rompés referencias de resources.
- **Usar Firebase dummy** y darlo por listo → la app arranca, pero auth/firestore/storage van a fallar.

---

## Ejemplo corto: SuperViandasAPP

Si mañana entra `SuperViandasAPP`, el flujo correcto sería:

1. Crear `SuperViandasAppProfile.kt`.
2. Agregar `branding_super_viandas_app_*` en `strings.xml`.
3. Agregar `branding_super_viandas_app_*` en `colors.xml`.
4. Subir/reutilizar íconos y fondos del buyer.
5. Registrar `SuperViandasAppProfile` en `ClientProfileResolver.kt`.
6. Completar `FirebaseConfig` real de ese buyer.
7. Cambiar `clientProfileId`, `launcherLabel`, `launcherIcon` y `launcherRoundIcon` en `app/build.gradle.kts`.

Es así de simple: **runtime profile adentro de la app, identidad visible desde placeholders, Firebase resuelto por profile**.
