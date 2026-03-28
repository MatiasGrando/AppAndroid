# ReservasApp Technical Notes

## Sesion y guards

- `ReservasApp` intenta bootstrapear sesion al iniciar la app.
- `UserSession` maneja 5 estados: `Uninitialized`, `LoggedOut`, `AuthenticatedPendingRole`, `AuthenticatedUser`, `AuthenticatedAdmin`.
- `SessionBootstrap` usa `FirebaseAuth.currentUser` para decidir si hay sesion, limpia cache de `ReservasRepository` cuando cambia el usuario y resuelve el rol real via `PerfilRepository.resolverEstadoSesionActual()`.
- `BaseActivity.ensureAuthenticatedSession()` deja pasar estados autenticados, rebootstrapea si el estado aun no se inicializo pero Firebase tiene usuario, y redirige a `LoginActivity` si no hay sesion valida.
- `BaseActivity.ensureAdminAccess()` solo deja entrar a `AuthenticatedAdmin`; un usuario autenticado comun vuelve a `MainActivity`.
- `LoginActivity` reusa bootstrap si ya existe usuario Firebase; si el login Google completa bien sincroniza perfil, recarga reservas y fuerza un refresh del estado antes de abrir `MainActivity`.

## Crear y editar reserva

- `ReservarActivity` exige sesion valida y perfil completo antes de permitir reservar.
- La ventana operativa ahora sale de `BookingAvailabilityRepository`: el admin define `initialDelayDays` (margen inicial desde hoy) y `windowLengthDays` (cantidad total de dias reservables desde ese inicio).
- `ReservasRepository.puedeCrearReservaEnFecha()` cruza dos reglas: caer dentro de la ventana configurable de creacion y en un dia de semana habilitado por admin.
- `ReservasRepository.puedeEditarReservaExistenteEnFecha()` ahora tambien respeta el `initialDelayDays`: si la reserva cae antes del inicio configurable de la ventana, no se puede editar aunque ya exista.
- En calendario, una fecha reservable sin reserva habilita `Continuar`; una fecha ya reservada habilita `Editar reserva`.
- Crear reserva: `ReservarActivity` -> `DetalleReservaActivity.createIntent()` -> seleccion por secciones de menu -> `ConfirmacionReservaActivity` -> `ReservasRepository.agregarReserva()`.
- Editar reserva: `ReservarActivity` o `MisReservasActivity` -> `DetalleReservaActivity.editIntent(reserva)` -> `ConfirmacionReservaActivity` con `EXTRA_ES_EDICION=true` -> `ReservasRepository.actualizarReserva()`.
- `DetalleReservaActivity` valida el punto de entrada antes de renderizar. Si la fecha o la reserva ya no son reservables, muestra error y sale.
- `ConfirmacionReservaActivity` sanitiza selecciones, revalida fecha/reserva, y solo confirma si el request sigue siendo consistente.

## Dias habilitados para encargos

- Que hace: `AdminActivity` permite definir que dias de la semana aceptan nuevos encargos/reservas. El comportamiento por defecto sigue siendo lunes a viernes.
- Como lo usa admin: en el panel admin hay checkboxes por dia, dos atajos (`Lunes a viernes` y `Toda la semana`), dos campos numericos (`margen inicial` y `longitud de ventana`) y un guardado explicito.
- Impacto en usuarios: `ReservarActivity` solo habilita nuevas fechas que cumplan las dos reglas al mismo tiempo: estar dentro de la ventana configurable de creacion y caer en un dia habilitado. Si un dia queda deshabilitado o queda antes del margen inicial, no se pueden crear reservas nuevas para ese dia.
- Edicion de reservas existentes: `ReservarActivity`, `DetalleReservaActivity` y `ConfirmacionReservaActivity` bloquean la edicion si la fecha cae antes del margen inicial configurado. La validacion sigue sin depender de `enabledWeekdays`: el bloqueo nuevo alinea margen inicial + longitud de ventana para crear y editar, pero no oculta la reserva existente de listados proximos.
- Persistencia/configuracion: `BookingAvailabilityRepository` guarda `enabledWeekdays`, `initialDelayDays` y `windowLengthDays` en Firestore (`configuracion_app/dias_habilitados_reservas`) y mantiene cache local en `SharedPreferences` (`booking_availability_prefs`). Si no hay config valida, hace fallback seguro a lunes-viernes, margen 0 y ventana total de 7 dias.
- Resiliencia: cuando Firestore falla al cargar, la app usa la ultima configuracion local conocida para no bloquear por completo el flujo.

## Tests que existen hoy

- `app/src/test/java/com/example/reservasapp/UserSessionTest.kt`: transiciones basicas de `UserSession`.
- `app/src/test/java/com/example/reservasapp/BaseActivityGuardsTest.kt`: reglas de guards autenticado/admin.
- `app/src/test/java/com/example/reservasapp/SessionBootstrapTest.kt`: bootstrap, cache por uid y logout cuando cambia el usuario.
- `app/src/test/java/com/example/reservasapp/ReservasRepositoryTest.kt`: ventana reservable, sanitizacion y validacion de selecciones contra menu.
- `app/src/test/java/com/example/reservasapp/DetalleReservaNavigationContractTest.kt`: contrato de navegacion crear/editar.
- `app/src/test/java/com/example/reservasapp/ConfirmacionReservaHelpersTest.kt`: helpers de resumen e imagenes.
- `app/src/test/java/com/example/reservasapp/ConfirmacionReservaRequestResolverTest.kt`: validacion del request antes de confirmar.
- `app/src/test/java/com/example/reservasapp/BookingAvailabilityRepositoryTest.kt`: sanitizacion, fallback seguro y evaluacion del dia habilitado.

## Nota operativa

- En este trabajo no se hizo build ni corrida de tests; el inventario de tests sale del repo actual.
