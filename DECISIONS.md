# Decisiones del proyecto
## D-001 · Diversificación de requisito
Fecha: 15/10 · It. 0 · Acta: 15/10
Contexto: detectamos que los requisitos que utilizaban la palabra gestión quedaba muy ambigua.
Decisión: dividir dichos requisitos en 4 casos de uso (consultar, modificar, eliminar, añadir).

## D-002 · Notificaciones de las citas
Fecha: 15/10 · It. 0 · Acta: 15/10
Contexto: detectamos que las notificaciones y los recordatorios de los pacientes son un requisito que tienen éstos.
Decisión: añadir dicho requisito a los pacientes

## D-003 · Estructura Maven multimódulo
Fecha: 19/10 · It. 0 · Acta: 19/10 · Issue: #12
Contexto: hay dos aplicaciones, servidor y
cliente, en un mismo repositorio.
Decisión: pom padre radiodiagnostico con dos
módulos: servidor y cliente.
Alternativas: dos repositorios; un solo módulo.
Motivo: un mvn verify construye todo y las
versiones se fijan una vez en el padre.
Consecuencias: cada módulo con su versión; al
cambiar la del padre, actualizar <parent>.

## D-004 · Unificación de requisitos
Fecha: 20/10 · It. 0 · Acta: 20/10
Contexto: al tener 64 requisitos, esto nos impedía hacer una planificación que nos permitiera llegar a tiempo.
Decisión: unificar los requisitos que dividimos de gestión en solo uno, es decir, aquellos que dividimos en 4 ahora pasan a ser solo 1.
Consecuencias: cambiar de nuevo los requisitos de la wiki y el diagrama de casos de uso del cliente.

## D-005 · Atributos de la entidad Paciente
Fecha: 22/10 · It. 1 · Acta: 22/10 · Issue: #20, #23, #26
Contexto: CU22 crea la entidad Paciente y CU23 y CU24 la reutilizan, por lo que los nombres de los campos deben ser los mismos en los tres.
Decisión: `id`, `nombre`, `apellidos`, `dni`, `fechaNacimiento`, `telefono`, `email`, `direccion`, `numeroTarjetaSanitaria`, `fechaAlta` y `activo`. El campo se llama `dni` (DNI/NIE) y no `documentoIdentidad`.
Consecuencias: #23 debe usar `dni` y añadir `numeroTarjetaSanitaria`. CU23 y CU24 no añaden atributos.

## D-006 · Identificación única de pacientes
Fecha: 22/10 · It. 1 · Acta: 22/10 · Issue: #20, #23
Contexto: hay que evitar pacientes duplicados al dar de alta y al modificar.
Decisión: el DNI/NIE y el número de tarjeta sanitaria son únicos. Al modificar el DNI se valida que no pertenezca a otro paciente.
Consecuencias: el servicio lanza `PacienteDuplicadoException`.

## D-007 · El alta de paciente no crea cuenta de acceso
Fecha: 22/10 · It. 1 · Acta: 22/10 · Issue: #20
Contexto: el paciente es un actor del sistema, pero el alta la hace el personal administrativo.
Decisión: dar de alta a un paciente solo crea el registro del paciente, no su cuenta de acceso.
Consecuencias: la cuenta de acceso se trata con la gestión de roles y permisos (It. 12).

## D-008 · Baja de paciente: borrado lógico
Fecha: 22/10 · It. 1 · Acta: 22/10 · Issue: #26
Contexto: la baja no debe perder el historial de pruebas del paciente.
Decisión: la baja pone `activo = false` y no borra datos. Un paciente inactivo no aparece en los listados por defecto ni recibe citas nuevas. No hay reactivación en este caso de uso.
Consecuencias: errores `PacienteNoEncontradoException` y `PacienteYaInactivoException`. CU23 no permite modificar pacientes dados de baja.

## D-009 · Baja lógica de los tipos de prueba
Fecha: 22/10 · It. 2 · Acta: 22/10 · Issue: #19, #29
Contexto: desactivar un tipo no debe romper el historial de pruebas ya realizadas.
Decisión: `desactivar(id)` pone `activo = false` y no hay borrado físico. El tipo deja de aparecer para prescribir pero se conserva en las pruebas ya realizadas. Si hay pruebas pendientes, hay que avisar al administrador y pedir confirmación.
Consecuencias: el mismo criterio se aplica a los protocolos (D-011).

## D-010 · Nombre único de los tipos de prueba
Fecha: 22/10 · It. 2 · Acta: 22/10 · Issue: #19, #29
Contexto: dos tipos con el mismo nombre confundirían al prescribir.
Decisión: el nombre es único sin distinguir mayúsculas ni espacios sobrantes.
Consecuencias: un nombre duplicado se rechaza con un error identificable, que la interfaz muestra sin borrar lo escrito (#28).

## D-011 · Relación entre protocolo y tipo de prueba
Fecha: 22/10 · It. 2 · Acta: 22/10 · Issue: #25
Contexto: CU34 crea `ProtocoloPrueba` y CU33 crea `TipoPrueba`.
Decisión: cada protocolo se asocia a un `TipoPrueba` existente mediante `tipoPruebaId`. Se rechaza un tipo inexistente. La baja del protocolo es lógica, como en D-009.
Consecuencias: el listado de protocolos filtra por tipo de prueba y por estado.

## D-012 · Reparto de información entre tipo de prueba y protocolo
Fecha: 22/10 · It. 2 · Acta: 22/10 · Issue: #25
Contexto: hay datos que podrían estar tanto en el tipo como en el protocolo.
Decisión: la duración estimada y la preparación previa del paciente se quedan en `TipoPrueba`. `ProtocoloPrueba` guarda las instrucciones de realización, el contraste, el consentimiento y los requisitos de seguridad.
Consecuencias: ningún dato se duplica entre las dos entidades.

## D-013 · Control de rol sencillo hasta It. 12
Fecha: 22/10 · It. 1 · Acta: 22/10 · Issue: #20, #23, #24, #25, #26, #29
Contexto: la gestión de roles y permisos (CU38 y CU39) llega en la It. 12.
Decisión: hasta entonces cada servicio comprueba el rol del usuario de forma simple: Personal administrativo para pacientes, Administrador del Sistema para tipos y protocolos y Médico para prescribir. Los demás roles solo consultan.
Consecuencias: un usuario sin el rol recibe un error de permiso. En la It. 12 se sustituye por la gestión de roles.

## D-014 · Auditoría de las modificaciones
Fecha: 22/10 · It. 1 · Acta: 22/10 · Issue: #23, #29
Contexto: la auditoría completa (CU40 y CU41) llega en la It. 12.
Decisión: cada modificación guarda quién la hizo y cuándo. Modificar un paciente no cambia su historial de pruebas ni de citas.
Consecuencias: en la It. 12 estos datos alimentan la auditoría.

## D-015 · Validaciones y confirmaciones en la interfaz
Fecha: 22/10 · It. 2 · Acta: 22/10 · Issue: #28
Contexto: el cliente no debe duplicar la lógica de negocio.
Decisión: el cliente valida el formato y el servidor valida siempre las reglas de negocio. La desactivación pide confirmación explícita en un diálogo. Los botones de crear, modificar y desactivar solo se muestran al rol Administrador.
Consecuencias: el cliente muestra los errores que devuelve el servicio.

## D-016 · Solicitud de prueba y reparto entre prescripción y cita
Fecha: 22/10 · It. 3 · Acta: 22/10 · Issue: #24
Contexto: CU11, CU12 y CU13 trabajan con las mismas solicitudes.
Decisión: la solicitud es la entidad `SolicitudPrueba`, compartida por los tres. La indicación clínica, la justificación y la prioridad (urgente, preferente u ordinaria) son obligatorias. El médico prescribe y el personal administrativo gestiona la cita, como procesos separados.
Consecuencias: el estado inicial de la solicitud y los campos definitivos se cierran con el equipo en la It. 3.
