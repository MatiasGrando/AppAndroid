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
- La ventana actual de reserva sale de `ReservasRepository.esFechaReservable()`: hoy + 6 dias.
- En calendario, una fecha reservable sin reserva habilita `Continuar`; una fecha ya reservada habilita `Editar reserva`.
- Crear reserva: `ReservarActivity` -> `DetalleReservaActivity.createIntent()` -> seleccion por secciones de menu -> `ConfirmacionReservaActivity` -> `ReservasRepository.agregarReserva()`.
- Editar reserva: `ReservarActivity` o `MisReservasActivity` -> `DetalleReservaActivity.editIntent(reserva)` -> `ConfirmacionReservaActivity` con `EXTRA_ES_EDICION=true` -> `ReservasRepository.actualizarReserva()`.
- `DetalleReservaActivity` valida el punto de entrada antes de renderizar. Si la fecha o la reserva ya no son reservables, muestra error y sale.
- `ConfirmacionReservaActivity` sanitiza selecciones, revalida fecha/reserva, y solo confirma si el request sigue siendo consistente.

## Tests que existen hoy

- `app/src/test/java/com/example/reservasapp/UserSessionTest.kt`: transiciones basicas de `UserSession`.
- `app/src/test/java/com/example/reservasapp/BaseActivityGuardsTest.kt`: reglas de guards autenticado/admin.
- `app/src/test/java/com/example/reservasapp/SessionBootstrapTest.kt`: bootstrap, cache por uid y logout cuando cambia el usuario.
- `app/src/test/java/com/example/reservasapp/ReservasRepositoryTest.kt`: ventana reservable, sanitizacion y validacion de selecciones contra menu.
- `app/src/test/java/com/example/reservasapp/DetalleReservaNavigationContractTest.kt`: contrato de navegacion crear/editar.
- `app/src/test/java/com/example/reservasapp/ConfirmacionReservaHelpersTest.kt`: helpers de resumen e imagenes.
- `app/src/test/java/com/example/reservasapp/ConfirmacionReservaRequestResolverTest.kt`: validacion del request antes de confirmar.

## Nota operativa

- En este trabajo no se hizo build ni corrida de tests; el inventario de tests sale del repo actual.
