# Decisiones del proyecto
## D-001 · Diversificación de requisito
Fecha: 15/10 · It. 0 · Acta: 15/10
Contexto: detectamos que los requisitos que utilizaban la palabra gestión quedaba muy ambigua.
Decisión: dividir dichos requisitos en 4 casos de uso (consultar, modificar, eliminar, añadir).
Consecuencias: sustituida por D-004, que los vuelve a unificar en uno solo.

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
Fecha: 24/10 · It. 1 · Acta: 22/10 · Issue: #20, #23, #26
Contexto: el enunciado no define los datos de un paciente y CU22, CU23 y CU24 comparten la entidad.
Decisión: Paciente tiene id, nombre, apellidos, dni, fechaNacimiento, telefono, email, direccion, numeroTarjetaSanitaria, fechaAlta y activo.
Alternativas: modelar historia clínica y datos de seguro. Motivo: se evita sobreingeniería, eso queda para la integración con HCE.
Consecuencias: CU23 y CU24 usan esta misma entidad; la baja es lógica con activo.

## D-006 · Identificación única por DNI/NIE y tarjeta sanitaria
Fecha: 24/10 · It. 1 · Acta: 22/10 · Issue: #20, #23
Contexto: hay que evitar pacientes duplicados.
Decisión: dni y numeroTarjetaSanitaria son únicos; el alta falla si ya existen y la modificación (CU23) rechaza un dni que pertenezca a otro paciente.
Consecuencias: el servicio lanza PacienteDuplicadoException.

## D-007 · El alta de paciente no crea cuenta de acceso
Fecha: 24/10 · It. 1 · Acta: 22/10 · Issue: #20
Contexto: los pacientes también son usuarios del sistema, pero el RF22 solo habla de dar de alta al paciente.
Decisión: CU22 solo registra los datos del paciente; el acceso se resolverá con SSO / gestión de usuarios.

## D-008 · Baja lógica de los tipos de prueba
Fecha: 26/10 · It. 2 · Acta: 22/10 · Issue: #19, #29
Contexto: desactivar un tipo no debe romper el historial de pruebas ya realizadas.
Decisión: desactivar(id) pone activo = false y no hay borrado físico. El tipo deja de aparecer para prescribir pero se conserva en las pruebas ya realizadas. (propuesta)
Motivo: se conserva el historial y se puede revertir.
Consecuencias: `listar` muestra por defecto solo los activos y el servicio no expone borrado. El mismo criterio se aplica a los protocolos (D-011).

## D-009 · Nombre único de los tipos de prueba
Fecha: 26/10 · It. 2 · Acta: 22/10 · Issue: #19, #29
Contexto: dos tipos con el mismo nombre confundirían al prescribir.
Decisión: el nombre es único, sin distinguir mayúsculas ni espacios sobrantes. (propuesta)
Consecuencias: el servicio normaliza el nombre y el alta o la modificación lanza `TipoPruebaDuplicadoException` si ya existe, igual que en D-006.

## D-010 · Baja de paciente: borrado lógico
Fecha: 27/10 · It. 1 · Acta: 22/10 · Issue: #26
Contexto: la baja no debe perder el historial de pruebas del paciente.
Decisión: la baja pone activo = false y no borra datos; el paciente inactivo no aparece en los listados por defecto ni recibe citas nuevas. No hay reactivación en este CU.
Consecuencias: errores PacienteNoEncontradoException y PacienteYaInactivoException; CU23 no modifica pacientes dados de baja.

## D-011 · Relación entre protocolo y tipo de prueba
Fecha: 26/10 · It. 2 · Acta: 22/10 · Issue: #25
Contexto: CU33 crea la entidad TipoPrueba y CU34 gestiona los protocolos de pruebas.
Decisión: cada ProtocoloPrueba estará asociado a un TipoPrueba existente. La baja del protocolo es lógica, como en D-008. *(propuesta)*
Consecuencias: no se podrá crear un protocolo para un tipo de prueba inexistente y se reutilizará la información definida en CU33.

## D-012 · Separación de información entre tipo de prueba y protocolo
Fecha: 26/10 · It. 2 · Acta: 22/10 · Issue: #25
Contexto: TipoPrueba ya contiene los datos generales definidos en CU33 y CU34 introduce la entidad ProtocoloPrueba.
Decisión: los datos generales de la prueba permanecen en TipoPrueba y ProtocoloPrueba contiene la información específica de realización, como instrucciones de realización, uso de contraste, consentimiento informado y requisitos de seguridad. *(propuesta)*
Consecuencias: se evita duplicar información y CU34 depende de la entidad TipoPrueba creada en CU33.

## D-013 · Validación de formato en cliente y de negocio en servidor
Fecha: 27/10 · It. 2 · Acta: 22/10 · Issue: #19, #28
Contexto: la interfaz de CU33 (tipos de pruebas diagnósticas) puede comprobar datos antes de enviarlos, pero las reglas de negocio (nombre único, duración válida, permisos) también las aplica el servicio.
Decisión: el cliente solo valida el formato (campos obligatorios vacíos, duración mayor que 0); la validación de negocio es siempre la del servidor, y el cliente muestra los errores que este devuelve.
Alternativas: duplicar las reglas de negocio en el cliente.
Motivo: evita reglas repetidas que se desincronicen y garantiza que ninguna regla se salte llamando al servicio directamente.
Consecuencias: cualquier cambio en una regla de negocio se hace solo en el servidor; el cliente debe gestionar y mostrar los errores del servicio. (propuesta)

## D-014 · Confirmación explícita al desactivar un tipo de prueba
Fecha: 27/10 · It. 2 · Acta: 22/10 · Issue: #19, #28
Contexto: desactivar un tipo de prueba es una baja lógica (D-008) que lo retira de la prescripción, y puede haber pruebas pendientes de ese tipo.
Decisión: la interfaz pide confirmación en un diálogo antes de desactivar, y avisa si el servicio indica que hay pruebas pendientes.
Alternativas: desactivar directamente con un solo clic; permitir deshacer después.
Motivo: evita desactivaciones por error y deja claro el efecto antes de aplicarlo.
Consecuencias: el cliente necesita el aviso de pruebas pendientes que devuelve el servidor; la desactivación requiere dos pasos. (propuesta)

## D-015 · Textos de los mensajes de error de la interfaz
Fecha: 27/10 · It. 2 · Acta: 22/10 · Issue: #19, #28
Contexto: el servidor devuelve errores identificables (nombre duplicado, duración inválida, tipo no encontrado, sin permiso) y el cliente tiene que mostrarlos al administrador.
Decisión: pendiente de cerrar con el equipo la redacción exacta de cada mensaje y si se muestran en el propio formulario o en un aviso aparte.
Alternativas: mostrar el mensaje técnico del servidor tal cual; usar textos propios del cliente por cada código de error.
Motivo: pendiente.
Consecuencias: hasta decidirlo, los mensajes son provisionales y puede cambiar el cliente sin afectar al contrato del servidor. (pendiente)

## D-016 · Control de rol sencillo hasta It. 12
Fecha: 27/10 · It. 1 · Acta: 22/10 · Issue: #19, #20, #23, #24, #25, #26, #29
Contexto: la gestión de roles y permisos (CU38 y CU39) llega en la It. 12.
Decisión: hasta entonces cada servicio comprueba el rol de forma simple: Personal administrativo para pacientes, Administrador del Sistema para tipos y protocolos y Médico para prescribir. Los demás roles solo consultan.
Consecuencias: un usuario sin el rol recibe un error de permiso. La comprobación está en un único punto del servicio, para sustituirla en la It. 12 por el control basado en permisos.

## D-017 · Auditoría de las modificaciones
Fecha: 27/10 · It. 1 · Acta: 22/10 · Issue: #23, #29
Contexto: la auditoría completa (CU40 y CU41) llega en la It. 12.
Decisión: cada modificación guarda quién la hizo y cuándo. Modificar un paciente no cambia su historial de pruebas ni de citas.
Consecuencias: en la It. 12 estos datos alimentan la auditoría.

## D-018 · Solicitud de prueba y reparto entre prescripción y cita
Fecha: 27/10 · It. 3 · Acta: 22/10 · Issue: #24
Contexto: CU11, CU12 y CU13 trabajan con las mismas solicitudes.
Decisión: la solicitud es la entidad SolicitudPrueba, compartida por los tres. La indicación clínica, la justificación y la prioridad (urgente, preferente u ordinaria) son obligatorias. El médico prescribe y el personal administrativo gestiona la cita, como procesos separados.
Consecuencias: el estado inicial de la solicitud y los campos definitivos se cierran con el equipo en la It. 3.

## D-019 · Casos de uso planificación
Fecha: 22/10 · It. 0 · Acta: 22/10
Contexto: al tener muchos requisitos , a la hora de planificar no podriamos hacer una iteracion por caso de uso por tema de tiempo.
Decision: agrupar varios casos de uso en una iteración.

## D-020 · Swing como tecnología de la interfaz del cliente de escritorio
Fecha: 27/10 · It. 1 · Acta: pendiente · Issue: #20, #37
Contexto: el enunciado fija Java pero no la biblioteca gráfica, y el cliente de escritorio necesita una para sus pantallas (CU22 y siguientes).
Decisión: la interfaz del cliente de escritorio se hace con Swing. (propuesta)
Alternativas: JavaFX; interfaz de consola.
Motivo: viene incluida en el JDK, no añade dependencias al pom y es multiplataforma (Windows, Linux, Mac).
Consecuencias: el cliente de escritorio no sirve para móvil ni web; esos clientes serán aplicaciones aparte contra el mismo servidor, lo que obliga a que la lógica no dependa de Swing (la validación y el contrato PacientesService no lo usan).

## D-021 · Campos obligatorios y validaciones del alta de paciente
Fecha: 27/10 · It. 1 · Acta: pendiente · Issue: #20, #36, #37
Contexto: D-005 define los atributos de Paciente pero no cuáles son obligatorios ni qué se valida, y D-013 reparte la validación entre cliente y servidor.
Decisión: son obligatorios nombre, apellidos, dni, fechaNacimiento y telefono; email, direccion y numeroTarjetaSanitaria son opcionales, y la tarjeta es única cuando se informa. El DNI/NIE debe tener formato válido (se comprueba el formato, no la letra de control), el email debe ser válido si se informa y la fecha de nacimiento no puede ser futura. id, fechaAlta y activo los asigna el servidor. (propuesta)
Motivo: los obligatorios son los de la issue #20; la tarjeta puede no estar disponible en el alta y la unicidad se exige solo cuando existe.
Consecuencias: cliente y servidor aplican las mismas reglas; el cliente impide el envío y el servidor lanza DatosInvalidosException aunque se le llame directamente.

## D-022 · Protocolo de comunicación entre cliente y servidor
Fecha: 27/10 · It. 1 · Acta: pendiente · Issue: #20, #36, #37
Contexto: el enunciado pide una arquitectura cliente-servidor con clientes de escritorio, móvil y web, pero no fija cómo se comunican.
Decisión: HTTP con JSON. El servidor expone POST /pacientes y responde 201 (paciente creado), 400 (datos inválidos, con la lista de errores), 403 (sin permiso) o 409 (duplicado). El usuario viaja en las cabeceras X-Usuario y X-Rol (D-023). Se usan HttpServer y HttpClient del JDK y Gson para el JSON. (propuesta)
Alternativas: sockets propios; RMI.
Motivo: HTTP es independiente del lenguaje y válido para clientes móviles y web; no necesita un framework.
Consecuencias: Gson es una dependencia nueva, fijada en el pom padre. Cliente y servidor comparten el formato de mensajes pero no código, así que Paciente sigue duplicado en cada módulo. El servidor solo escucha en localhost hasta que exista autenticación real.

## D-023 · Identificación del usuario y comprobación de rol
Fecha: 27/10 · It. 1 · Acta: pendiente · Issue: #20, #36, #37
Contexto: D-016 pide comprobar el rol de forma sencilla hasta la It. 12, y el alta de pacientes solo la puede hacer el Personal administrativo. Hace falta decidir cómo sabe el servicio quién llama.
Decisión: cada operación del servicio recibe como primer parámetro el usuario solicitante (con su rol: paciente, médico, personal administrativo o administrador). La comprobación está en un único punto del servidor, que lanza SinPermisoException si el rol no basta. El cliente conoce el usuario actual y no muestra las pantallas que su rol no permite. No hay autenticación real hasta la integración con SSO. (propuesta)
Alternativas: sesión o token de acceso desde ya; integrar el SSO ahora.
Motivo: es lo más simple que cumple D-016 y el SSO queda para su iteración.
Consecuencias: el contrato de PacientesService cambia a crear(solicitante, datos) y se añade SinPermisoException. Sin autenticación real, un cliente puede declarar el rol que quiera; es una limitación aceptada hasta sustituirla por permisos y SSO en la It. 12. El mismo patrón sirve para CU23, CU24 y los demás servicios.

## D-024 · Almacenamiento de los pacientes
Fecha: 27/10 · It. 1 · Acta: pendiente · Issue: #20, #36
Contexto: el alta debe dejar al paciente disponible para CU23 y CU24, y hoy se guarda solo en memoria dentro del servicio.
Decisión: el servicio guarda los pacientes a través de una interfaz de repositorio (PacientesRepositorio), con una implementación en memoria por ahora. La base de datos o el fichero concretos se deciden cuando se integre el servidor. (propuesta)
Alternativas: base de datos embebida con JDBC desde ya; ficheros JSON.
Motivo: separa la lógica de negocio del almacenamiento (programación orientada a interfaces, como pide el enunciado) y evita una dependencia que aún no es necesaria.
Consecuencias: los datos se pierden al reiniciar el servidor. CU23 y CU24 usan el mismo repositorio. Cambiar de almacenamiento solo requiere otra implementación de la interfaz.

## D-025 · Características de calidad ISO 25010 de la planificación
Fecha: 29/10 · It. 0 · Acta: pendiente
Contexto: la planificación debe incluir tres características del modelo de calidad del producto de ISO 25010.
Decisión: incluir Adecuación funcional, Seguridad y Fiabilidad. Adecuación funcional se comprueba en todas las iteraciones; además cada iteración tiene una característica de foco (columna Calidad de la planificación) y la evaluación final se hace en una iteración ItQ siguiendo los cinco pasos de ISO 25040. De ellas se derivan requisitos no funcionales (RNF-AF, RNF-SEG y RNF-FIA), cada uno con su iteración y su forma de medirlo. (propuesta)
Alternativas: Capacidad de uso; Compatibilidad (interoperabilidad); Eficacia de desempeño, Mantenibilidad y Portabilidad.
Motivo: Adecuación funcional, porque trabajamos por casos de uso y esta característica comprueba que cada CU cubre su RF y lo hace correctamente (completitud, corrección y pertinencia funcional). Seguridad, porque se manejan datos de pacientes, informes e imágenes clínicas y hay requisitos que son justo sus subcaracterísticas: roles y permisos (RF38, RF39), auditoría de actividades y accesos a datos clínicos (RF40, RF41) y validación de identidad con SSO (RF45). Fiabilidad, porque el sistema gestiona agendas y citas (RF14, RF25, RF26), intercambia información con sistemas externos (RF46, RF48) y ejecuta procesos automáticos con el temporizador (RF49, RF50), y un fallo afecta directamente a la atención al paciente (madurez, disponibilidad, tolerancia a fallos y recuperabilidad).
Consecuencias: la planificación añade la columna Calidad, un plan de calidad, las iteraciones It13 (integraciones externas) e It14 (procesos automáticos) para RF45 a RF50, y la iteración ItQ de evaluación; el total pasa a 511 horas, 25 días y 15.775 € (15.913 € con las licencias de IA, D-028). Algunos requisitos describen el producto final y en este proyecto se implementan de forma simplificada por D-016, D-023 y D-024 (control de rol sin autenticación real y almacenamiento en memoria). Al terminar el proyecto hay que medir estas tres características en el producto entregado.

## D-026 · Estado inicial de las solicitudes de prueba
Fecha: 08/11/2026 · It. 3 · Acta: pendiente · Issue: #24, #42, #43 · Relacionada con CU12, CU13, CU25 y CU27
Contexto: D-018 establece que CU11, CU12 y CU13 comparten la entidad `SolicitudPrueba`, pero deja pendiente definir su estado inicial. Es necesario distinguir entre la prescripción realizada por el médico y la posterior asignación de una cita por el personal administrativo.
Decisión: Toda nueva solicitud de prueba diagnóstica se registrará con el estado inicial `PENDIENTE_CITACION`. Este estado indica que la solicitud médica ya existe, pero todavía no tiene una cita asignada. Los estados se representan mediante el enumerado `EstadoSolicitud`, evitando el uso de cadenas de texto arbitrarias. El identificador y la fecha de creación son asignados por el servidor. El cliente no puede establecer directamente el estado inicial. Esta decisión ya está implementada en CU11, pendiente de ratificación formal por el equipo.
Alternativas: Utilizar `PENDIENTE` o `SOLICITADA` como estado inicial; representar los estados mediante `String`.
Motivo: `PENDIENTE_CITACION` distingue claramente el proceso de prescripción médica del proceso administrativo de citación. El enumerado evita estados no reconocidos y facilita las validaciones.
Consecuencias: El servidor de CU11 crea las solicitudes con `PENDIENTE_CITACION`, asigna la fecha de creación y guarda la solicitud mediante el repositorio correspondiente. El cliente utiliza el mismo enumerado y muestra el estado en la confirmación de la prescripción. Se han implementado y ejecutado pruebas JUnit del servicio y de la comunicación HTTP. CU12 reutilizará la misma entidad y CU13 deberá definir los estados posteriores y las transiciones permitidas. CU25 y CU27 podrán utilizar las solicitudes pendientes para gestionar citas y listas de espera.
Pendiente de acordar: La aprobación formal de esta decisión por el equipo, los demás estados y sus transiciones, los campos definitivos compartidos con CU12 y CU13, y la relación entre el identificador del médico y la identidad del usuario solicitante.


## D-027 · Tarifa y dedicación para la planificación
Fecha: 13/11 · It. 0 · Acta: pendiente
Contexto: el enunciado no fija las tarifas del equipo y propone una semana de 40 horas para un consultor humano; la planificación necesita una tarifa y una capacidad.
Decisión: todos los integrantes tienen una tarifa única de 25 €/h y una dedicación de 40 h/semana. La capacidad del equipo es 6 × 40 = 240 h/semana (48 h por día laborable) y los días de cada iteración son sus horas entre 48 h, redondeado hacia arriba. (propuesta)
Alternativas: tarifas distintas por rol (45, 40 y 35 €/h); dedicación de 25 h/semana por persona.
Motivo: simplifica el cálculo y sigue el supuesto del enunciado; es la tarifa de referencia del boletín E.01.02.
Consecuencias: con ninguna iteración por encima de 44 h, cada una ocupa 1 día (22 días en el plan inicial y 25 en el actualizado). Es una duración teórica: el calendario real depende de la dedicación efectiva.

## D-028 · Licencias de IA en el presupuesto
Fecha: 13/11 · It. 0 · Acta: pendiente
Contexto: el enunciado pide tener en cuenta el coste de las licencias de los modelos de IA.
Decisión: se imputan Claude Pro (22 €/mes) y ChatGPT Pro (24 €/mes), una cuenta de cada una durante tres meses (octubre a diciembre): 3 × (22 + 24) = 138 €. GitHub Copilot (licencia campus) y Visual Paradigm (licencia académica) no suponen coste. (propuesta)
Motivo: es un coste real de producción del proyecto.
Consecuencias: el presupuesto es 15.775 € de personal + 138 € de licencias = 15.913 €.

## D-029 · Fecha de finalización y calendario de liberaciones
Fecha: 13/11 · It. 0 · Acta: pendiente
Contexto: la planificación debe indicar una fecha de finalización y un calendario de liberaciones.
Decisión: con la capacidad teórica las 25 jornadas terminarían el 12/11/2026; como la dedicación efectiva es menor, la fecha de finalización es la de entrega, el 15/12/2026. Cada hito de fase genera una versión con Semantic Versioning: línea base de la arquitectura (It. 3) v0.3.0, capacidad operacional inicial (It. 14) v0.14.0 y entrega del producto final (It. N) v1.0.0 el 15/12/2026. (propuesta)
Consecuencias: las versiones de los módulos Maven deberán alinearse con este calendario.

## D-030 · Modelado de las pruebas de seguimiento
Fecha: 14/11/2026 · It. 3 · Acta: pendiente · Issue: #92 · Relacionada con CU11, CU13, CU25, CU27, CU09 y CU10
Contexto: CU12 permite al médico prescribir pruebas de seguimiento. D-018 establece que CU11, CU12 y CU13 comparten la entidad `SolicitudPrueba`, y D-026 fija el estado inicial `PENDIENTE_CITACION`, pero ninguna define cómo se expresa la periodicidad de un seguimiento ni cómo se añade a la entidad sin afectar al código existente de CU11 y CU13.
Decisión: (1) Una prueba de seguimiento es una `SolicitudPrueba`, no una clase nueva. (2) La periodicidad se expresa con un intervalo (número entero), una unidad (enumerado `UnidadPeriodicidad`: días, semanas o meses) y un número de repeticiones; el intervalo y las repeticiones deben ser mayores que 0, y los tres campos se informan juntos o ninguno. (3) Un seguimiento es una única solicitud con su periodicidad; CU25 asigna las citas y CU27 permite al personal administrativo consultar y filtrar la lista de espera, sin asignar citas. (4) La solicitud puede enlazarse a la prescripción original mediante `solicitudOrigenId`, opcional; si se informa, debe existir y pertenecer al mismo paciente. (5) Los campos nuevos pueden ser nulos, y nulo significa que no es un seguimiento; se mantiene el constructor actual de 9 argumentos y se añade otro más largo. (6) El cliente reutiliza `POST /solicitudes` con los campos opcionales, sin crear una ruta nueva. (propuesta)
Alternativas: crear una clase `SolicitudSeguimiento` aparte; expresar la periodicidad como texto libre; generar una solicitud por cada repetición; modificar el constructor existente; crear una ruta `/solicitudes/seguimiento`.
Motivo: reutilizar la entidad cumple D-018. El enumerado evita valores no reconocidos, como se hizo con `Prioridad` y `EstadoSolicitud`. Mantener el constructor actual evita cambiar el código de CU11 y CU13 y sus tests. Una sola solicitud mantiene la prescripción médica separada del proceso administrativo de citación, como establece D-026.
Consecuencias: `SolicitudPrueba` se amplía en el cliente y en el servidor, y `conId` debe copiar también los campos nuevos. Los tests de CU11 y CU13 no cambian. Los tests de CU12 irán en ficheros propios. CU13 gestionará las solicitudes de seguimiento igual que las demás. CU27 incluye en su lista las solicitudes en estado `PENDIENTE_CITACION`, ordenadas por prioridad, fecha e ID, y ofrece un filtro por prioridad; una solicitud deja de aparecer cuando CU25 la cambia a `CITADA`. CU09 y CU10 mostrarán más adelante los seguimientos al paciente. Las issues del CU12 y su sub-issue de servidor y de cliente deben citar esta decisión.
Pendiente de acordar: la aprobación formal del equipo, especialmente de Natalia (CU13) y Aitana (CU11), los valores máximos admitidos para el intervalo y las repeticiones, y si CU13 necesita estados propios para los seguimientos.

## D-031 · Gestión de las solicitudes por el médico: estados gestionables (CU13)
Fecha: 18/11/2026 · It. 4 · Acta: pendiente · Issue: #99, #109, #110 · Relacionada con CU11, CU12 y CU25
Contexto: CU13 permite al médico consultar, modificar y anular las solicitudes de prueba creadas en CU11 y CU12 (D-018). D-026 define los estados de la solicitud, pero no dice en cuáles se puede modificar o anular.
Decisión: (1) Solo se puede modificar o anular una solicitud en estado `PENDIENTE_CITACION`; en cualquier otro estado el servidor lanza `SolicitudNoGestionableException`. (2) Consultar, modificar y anular exigen el rol Médico (D-016 y D-023). (3) El cliente solo activa los botones Modificar y Anular con una solicitud pendiente de citación, pero la regla la aplica el servidor (D-013). (propuesta)
Alternativas: permitir modificar solicitudes ya citadas; permitir anular una solicitud citada cancelando también su cita.
Motivo: una solicitud citada ya está en manos de CU25; cambiarla desde CU13 dejaría la cita y la solicitud incoherentes.
Consecuencias: CU13 no toca las citas. Una solicitud `CITADA` vuelve a ser gestionable solo si CU25 cancela su cita y la devuelve a `PENDIENTE_CITACION` (D-032). `EstadoSolicitud.permiteGestion()` concentra esta regla.
Pendiente de acordar: la aprobación formal del equipo, especialmente de Natalia (CU13).


## D-032 · Gestión de las citas por el personal administrativo (CU25)
Fecha: 21/11/2026 · It. 5 · Acta: pendiente · Issue: #126, #127 · Relacionada con CU01, CU11, CU13, CU26 y CU29
Contexto: CU25 permite al personal administrativo dar cita a las solicitudes de prueba (D-026 deja la solicitud en `PENDIENTE_CITACION` hasta que tenga cita). CU01 creó la entidad `Cita`, pero una cita no sabe en qué hueco de agenda está ni de qué solicitud nace, y un hueco de CU29 solo puede estar bloqueado, no ocupado.
Decisión: (1) Un servicio nuevo `CitasAdministracionService`, en el paquete `cita` del servidor, con `asignar`, `reprogramar`, `cancelar`, `listarCitasDePaciente` y `consultarCita`; todas exigen el rol Personal administrativo (D-016 y D-023). (2) La `Cita` guarda `huecoId` y `solicitudId`; un hueco está ocupado si tiene una cita `PROGRAMADA` o `CONFIRMADA`, y «bloqueado» (CU29) no equivale a «ocupado». (3) `asignar` exige una solicitud en `PENDIENTE_CITACION`, un paciente activo (D-010) y un hueco libre (existente, no bloqueado, de una agenda activa); crea la cita `PROGRAMADA` con la fecha y el médico de la agenda y pasa la solicitud a `CITADA`. (4) `reprogramar` mueve una cita `PROGRAMADA` o `CONFIRMADA` a otro hueco libre y la deja `PROGRAMADA`, porque hay que volver a confirmarla. (5) `cancelar` pasa la cita a `CANCELADA`, libera el hueco y devuelve la solicitud a `PENDIENTE_CITACION` si estaba `CITADA`. (6) Una cita cancelada o realizada no se puede reprogramar ni cancelar. (propuesta)
Alternativas: deducir la ocupación por médico, día y hora sin añadir campos a `Cita`; dejar la solicitud en `CITADA` al cancelar; exponer también las rutas HTTP en la misma PR.
Motivo: el vínculo explícito entre cita, hueco y solicitud es más robusto que deducirlo, y devolver la solicitud a `PENDIENTE_CITACION` evita que un paciente se quede sin poder recibir otra cita tras una cancelación.
Consecuencias: `Cita` (CU01) gana dos campos y `CitasRepositorio` un método (`buscarActivaPorHueco`); `SolicitudPrueba` gana `conEstado`, como ya tiene `conId`. El servidor de CU25 no expone rutas HTTP todavía, así que el cliente usará un servicio simulado hasta que se acuerden. Los tests de CU01 no cambian.
Pendiente de acordar: la aprobación formal del equipo, especialmente de Adrián (CU01), Aitana (CU29) y Natalia (CU13); si hay que comprobar que la duración de la prueba cabe en el hueco; si se debe impedir citar en fechas pasadas; y las rutas HTTP.


## D-033 · Configuración de los horarios del servicio (CU35)
Fecha: 23/11/2026 · It. 4 · Acta: 6 · Issue: #134, #135, #136
Contexto: CU35 permite al Administrador del Sistema configurar el horario general del servicio, pero las issues no concretan sus atributos ni las reglas de validación.
Decisión: (1) El horario se representa para los siete días de la semana; cada día puede tener cero o más intervalos. Un día sin intervalos queda cerrado. (2) Cada intervalo tiene una hora de inicio y otra de fin; el inicio debe ser anterior al fin y los intervalos de un mismo día no pueden solaparse, aunque sí pueden ser consecutivos. (3) La configuración inicial contiene los siete días cerrados. (4) Cada modificación registra el nombre del usuario y la fecha y hora del cambio. (5) Solo el rol ADMINISTRADOR puede consultar o modificar el horario. (6) El servidor expone GET y PUT /horarios-servicio mediante HTTP/JSON. (7) El cliente Swing permite indicar intervalos como HH:mm-HH:mm, separados por comas; el cliente valida el formato y el servidor aplica las reglas de negocio.
Alternativas: limitar cada día a un único intervalo; validar también las reglas de negocio en el cliente.
Motivo: admitir horarios con pausas sin duplicar la validación de negocio; el servidor sigue siendo la autoridad para aceptar o rechazar la configuración.
Consecuencias: el horario se almacena en memoria y se pierde al reiniciar el servidor. El cliente y el servidor mantienen sus modelos en sus respectivos módulos. Las ramas de implementación son feature/CU35-servidor y feature/CU35-cliente.


## D-034 · Configuración de las políticas de citación (CU36)
Fecha: 23/11/2026 · It. 5 · Acta: 7 · Issue: #137, #138, #139
Contexto: CU36 permite al Administrador del Sistema configurar las políticas de citación, pero las issues no concretan sus atributos ni sus valores iniciales.
Decisión: (1) La política contiene la antelación mínima para reservar, en horas; la ventana máxima de reserva, en días; la antelación mínima para cancelar, en horas; y el máximo de citas activas por paciente. (2) Los valores iniciales son 0 horas, 90 días, 24 horas y 3 citas activas, respectivamente. (3) Las antelaciones no pueden ser negativas; el máximo de citas activas debe ser positivo; la ventana máxima, convertida a horas, debe superar la antelación mínima; y la antelación de cancelación no puede superar la ventana máxima. (4) Cada modificación registra el nombre del usuario y la fecha y hora del cambio. (5) Solo el rol ADMINISTRADOR puede consultar o modificar la política. (6) El servidor expone GET y PUT /politicas-citacion mediante HTTP/JSON. (7) El cliente valida el formato de los valores; el servidor aplica las reglas de negocio.
Alternativas: dejar los valores iniciales sin definir; aplicar las políticas directamente al flujo de gestión de citas de CU25 en esta misma implementación.
Motivo: disponer de una configuración consultable y válida desde el primer arranque, manteniendo la validación de negocio en el servidor.
Consecuencias: la política se almacena en memoria y se pierde al reiniciar el servidor. CU36 configura y guarda los valores; su aplicación al flujo de reservas y cancelaciones de CU25 queda fuera del alcance de estas issues y requerirá acordarse aparte. Las ramas de implementación son feature/CU36-servidor y feature/CU36-cliente.

## D-035 · Gestión de agendas propias de realización (CU14)
Fecha: 23/11/2026 · It. 4 · Acta: pendiente · Issue: #142, #143, #144
Contexto: CU14 permite al médico gestionar agendas de realización, mientras CU26 gestiona la agenda médica diaria y CU29 gestiona sus huecos.
Decisión: CU14 reutiliza `AgendaMedica` y sus datos (`medicoId`, fecha, intervalo horario, observaciones y estado activo), sin cambiar las operaciones administrativas de CU26 ni el contrato de CU29. El médico puede crear, consultar, modificar y eliminar lógicamente solo sus propias agendas. La propiedad la determina el `medicoId` explícito de `Usuario`, enviado en `X-Medico-Id` junto con `X-Usuario` y `X-Rol`; el servidor asigna el médico propietario al crear y lo conserva al modificar, sin confiar en el valor enviado en el cuerpo. (propuesta)
Motivo: se reutiliza el modelo ya consumido por CU29 y se evita duplicar el concepto de agenda; la identidad explícita permite comprobar en el servidor la propiedad de cada agenda.
Consecuencias: CU14 expone operaciones y pantalla separadas de las de CU26. El control de identidad sigue la simplificación temporal de D-023 y no sustituye la autenticación real prevista con SSO.
Pendiente de acordar: ratificación formal del equipo.

## D-036 · Modificación y anulación de solicitudes (CU13)
Fecha: 18/11/2026 · It. 4 · Acta: pendiente · Issue: #99, #109, #110 · Relacionada con CU11, CU12 y D-031
Contexto: D-031 fija qué solicitudes se pueden gestionar, pero falta definir qué datos puede cambiar el médico y qué se guarda al anular.
Decisión: (1) Al modificar solo cambian la indicación clínica, la justificación, la prioridad y, en un seguimiento, la periodicidad; el paciente, el médico, el tipo de prueba, la fecha, el estado y la solicitud original se conservan. (2) La indicación, la justificación y la prioridad son obligatorias; la periodicidad solo se informa en un seguimiento y completa, con intervalo y repeticiones mayores que 0 (D-030). (3) Anular exige un motivo no vacío; la solicitud pasa a `ANULADA` y se guardan el motivo (`motivoAnulacion`) y la fecha de anulación (`fechaAnulacion`). (4) La solicitud anulada se conserva y no se elimina. (propuesta)
Alternativas: permitir cambiar también el tipo de prueba o el paciente; anular sin motivo; borrar la solicitud al anularla.
Motivo: cambiar el paciente o el tipo de prueba equivale a una solicitud distinta, que debe crearse de nuevo; el motivo y la fecha dejan constancia de por qué se anuló.
Consecuencias: `SolicitudPrueba` gana los campos `motivoAnulacion` y `fechaAnulacion`, nulos si no está anulada, y un constructor más largo. El cliente valida el formato (D-013) y el servidor las reglas de negocio.
Pendiente de acordar: la aprobación formal del equipo, especialmente de Natalia (CU13).

## D-037 · Rutas HTTP de la gestión de solicitudes (CU13)
Fecha: 18/11/2026 · It. 4 · Acta: pendiente · Issue: #99, #109, #110 · Relacionada con CU11, CU12, D-022 y D-031
Contexto: CU11 y CU12 ya usan `POST /solicitudes`. CU13 necesita rutas para consultar, modificar y anular solicitudes.
Decisión: (1) `GET /solicitudes` devuelve las solicitudes, con los filtros opcionales y combinables `pacienteId`, `estado` y `prioridad`, ordenadas por prioridad, fecha de solicitud e id. (2) `PUT /solicitudes/{id}` modifica los datos permitidos por D-036. (3) `POST /solicitudes/{id}/anulacion` anula la solicitud con el cuerpo `{"motivo": "..."}`. (4) Errores: 403 `SIN_PERMISO`, 404 `NO_ENCONTRADO`, 409 `ESTADO_NO_PERMITIDO` si no está pendiente de citación (D-031) y 400 `DATOS_INVALIDOS`. (propuesta)
Alternativas: una ruta distinta para cada filtro; anular con `DELETE /solicitudes/{id}`.
Motivo: se reutiliza la ruta `/solicitudes` y su formato (D-022); `POST …/anulacion` deja claro que la solicitud se conserva.
Consecuencias: el cliente HTTP construye la consulta con `FiltroSolicitudes.comoParametros()`. CU27 consulta la lista de espera por su ruta propia, `/listas-espera`.
Pendiente de acordar: la aprobación formal del equipo, especialmente de Natalia (CU13).

## D-043 · Modelo y almacenamiento de imágenes de pruebas (CU17 y CU18)

Fecha: 28/11/2026 · It.: pendiente de confirmar · Acta: pendiente · Issue: #181, #182, #183, #184, #185, #186

Contexto: RF17 permite al médico consultar imágenes de pruebas y RF18 permite crearlas. Ambos casos de uso necesitan compartir un modelo de datos y un mecanismo de almacenamiento que permita recuperar las imágenes previamente registradas.

Decisión: (1) CU17 y CU18 comparten la entidad `ImagenPrueba`, formada por identificador, identificador de solicitud, nombre, tipo de contenido, contenido en Base64, identificador del médico que registra la imagen y fecha de creación. (2) Se utiliza `ResumenImagenPrueba` para devolver los listados sin incluir el contenido de las imágenes. (3) Las imágenes se almacenan mediante la interfaz `ImagenesRepositorio`, con una implementación en memoria denominada `ImagenesRepositorioMemoria`, siguiendo D-024. (4) Cada imagen debe estar asociada a una solicitud de prueba existente. Una solicitud puede contener varias imágenes. (5) El identificador y la fecha de creación son asignados por el servidor. El médico que registra la imagen se obtiene del usuario solicitante mediante `X-Medico-Id`. (propuesta)

Alternativas: almacenar los archivos directamente en el sistema de ficheros; utilizar una base de datos; almacenar las imágenes en un repositorio externo; devolver el contenido completo de todas las imágenes en cada consulta.

Motivo: reutilizar el modelo y el repositorio evita duplicar funcionalidades entre CU17 y CU18. La interfaz de repositorio permite sustituir el almacenamiento en memoria sin modificar la lógica de negocio. Separar el resumen del contenido completo reduce la cantidad de información transmitida durante las consultas.

Consecuencias: las imágenes se pierden al reiniciar el servidor. El cliente y el servidor mantienen sus respectivos modelos compatibles mediante HTTP/JSON, siguiendo D-022. Las imágenes creadas mediante CU18 quedan disponibles para su consulta mediante CU17.

Pendiente de acordar: la ratificación formal del equipo, la persistencia definitiva de las imágenes y su futura integración con los sistemas externos de información clínica.

## D-044 · Validación, permisos y comunicación de imágenes de pruebas (CU17 y CU18)

Fecha: 28/11/2026 · It.: pendiente de confirmar · Acta: pendiente · Issue: #181, #182, #183, #184, #185, #186

Contexto: CU17 y CU18 necesitan establecer las operaciones HTTP, los permisos de acceso y las condiciones que debe cumplir una imagen antes de registrarse.

Decisión: (1) Solo el rol Médico puede consultar y registrar imágenes, siguiendo D-016 y D-023. Para registrar imágenes se exige además un identificador de médico positivo, reutilizando el mecanismo de identificación de D-035. (2) Se admiten archivos PNG y JPEG con un tamaño máximo de 5 MiB y 16 millones de píxeles. (3) Se comprueba el contenido real de la imagen, su formato, sus dimensiones y la validez de su nombre. (4) El cliente realiza comprobaciones previas y el servidor aplica la validación definitiva, siguiendo D-013. (5) Se establecen las rutas `GET /imagenes?solicitudId={id}`, `GET /imagenes/{id}` y `POST /imagenes`. Las consultas correctas devuelven HTTP 200 y la creación devuelve HTTP 201. (6) Las pantallas se implementan mediante Swing, siguiendo D-020, y utilizan SwingWorker para evitar bloquear la interfaz durante la lectura de archivos y la comunicación HTTP. (7) El registro no modifica el estado de la solicitud. (propuesta)

Alternativas: admitir cualquier formato de archivo; validar únicamente la extensión; transmitir las imágenes mediante multipart/form-data; utilizar una ruta independiente para cada caso de uso; integrar directamente formatos clínicos como DICOM.

Motivo: limitar los formatos y las dimensiones reduce la posibilidad de registrar archivos incorrectos o excesivamente grandes. La comprobación del contenido real evita aceptar archivos que únicamente tienen una extensión de imagen. Reutilizar HTTP/JSON mantiene la coherencia con las decisiones arquitectónicas existentes.

Consecuencias: el servidor gestiona los errores mediante códigos HTTP 400, 403, 404, 405, 413 y 415. La creación permite seleccionar, previsualizar y registrar imágenes, mientras que la consulta permite obtener el listado y visualizar una imagen individual. El control de acceso es provisional y se basa en los datos declarados por el cliente, sin autenticación real ni comprobación de propiedad de la solicitud.

Pendiente de acordar: la aprobación de los formatos y límites propuestos, la necesidad de admitir DICOM, los estados de solicitud en los que se pueden registrar imágenes y las reglas definitivas de autorización y acceso a información clínica.