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
Decisión: incluir Adecuación funcional, Seguridad y Fiabilidad. Adecuación funcional se comprueba en todas las iteraciones; además cada iteración tiene una característica de foco (columna Calidad de la planificación) y la evaluación final se hace en una iteración ItQ siguiendo los cinco pasos de ISO 25040. (propuesta)
Alternativas: Capacidad de uso; Compatibilidad (interoperabilidad); Eficacia de desempeño, Mantenibilidad y Portabilidad.
Motivo: Adecuación funcional, porque trabajamos por casos de uso y esta característica comprueba que cada CU cubre su RF y lo hace correctamente (completitud y corrección funcional). Seguridad, porque se manejan datos de pacientes, informes e imágenes clínicas y hay requisitos que son justo sus subcaracterísticas: roles y permisos (RF38, RF39), auditoría de actividades y accesos a datos clínicos (RF40, RF41) y validación de identidad con SSO (RF45). Fiabilidad, porque el sistema gestiona agendas y citas (RF14, RF25, RF26), intercambia información con sistemas externos (RF46, RF48) y ejecuta procesos automáticos con el temporizador (RF49, RF50), y un fallo afecta directamente a la atención al paciente (disponibilidad, tolerancia a fallos y capacidad de recuperación).
Consecuencias: la planificación añade la columna Calidad, un plan de calidad, las iteraciones It13 (integraciones externas) e It14 (procesos automáticos) para RF45 a RF50, y la iteración ItQ de evaluación; el total pasa a 511 horas, 34 días y 15.775 €. Al terminar el proyecto hay que medir estas tres características en el producto entregado.
