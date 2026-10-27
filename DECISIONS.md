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

## D-004 · Casos de uso planificación
Fecha: 22/10 · It. 0 · Acta: 22/10
Contexto: al tener muchos requisitos , a la hora de planificar no podriamos hacer una iteracion por caso de uso por tema de tiempo.
Decision: agrupar varios casos de uso en una iteración.

## D-005 · Atributos de la entidad Paciente
Fecha: 24/10 · It. 1 · Issue: 20
Contexto: el enunciado no define los datos de un paciente y CU22, CU23 y CU24 comparten la entidad.
Decisión: Paciente tiene id, nombre, apellidos, dni, fechaNacimiento, telefono, email, direccion, numeroTarjetaSanitaria, fechaAlta y activo.
Alternativas: modelar historia clínica y datos de seguro. Motivo: se evita sobreingeniería, eso queda para la integración con HCE.
Consecuencias: CU23 y CU24 usan esta misma entidad; la baja es lógica con activo.

## D-006 · Identificación única por DNI/NIE y tarjeta sanitaria
Fecha: 24/10 · It. 1 · Issue: 20
Contexto: hay que evitar pacientes duplicados.
Decisión: dni y numeroTarjetaSanitaria son únicos; el alta falla si ya existen.
Consecuencias: el servicio lanza PacienteDuplicadoException.

## D-007 · El alta de paciente no crea cuenta de acceso
Fecha: 24/10 · It. 1 · Issue: 20
Contexto: los pacientes también son usuarios del sistema, pero el RF22 solo habla de dar de alta al paciente.
Decisión: CU22 solo registra los datos del paciente; el acceso se resolverá con SSO / gestión de usuarios.

## D-008 · Baja lógica
Fecha:26/10 · It. 2 · Issue: 19
Contexto: Baja lógica en lugar de borrado físico, para no romper el historial de pruebas. *(propuesta)*

## D-009· Nombre de tipo unico
Fecha: 26/10 · It. 2 · Issue: 19
Contexto: El nombre del tipo es único. *(propuesta)*

## D-011 · Relación entre protocolo y tipo de prueba
Fecha: 26/10 · It. 2 · Acta: 22/10 · Issue: #25
Contexto: CU33 crea la entidad TipoPrueba y CU34 gestiona los protocolos de pruebas.
Decisión: cada ProtocoloPrueba estará asociado a un TipoPrueba existente. *(propuesta)*
Consecuencias: no se podrá crear un protocolo para un tipo de prueba inexistente y se reutilizará la información definida en CU33.

## D-012 · Separación de información entre tipo de prueba y protocolo
Fecha: 26/10 · It. 2 · Acta: 22/10 · Issue: #25
Contexto: TipoPrueba ya contiene los datos generales definidos en CU33 y CU34 introduce la entidad ProtocoloPrueba.
Decisión: los datos generales de la prueba permanecen en TipoPrueba y ProtocoloPrueba contiene la información específica de realización, como instrucciones de realización, uso de contraste, consentimiento informado y requisitos de seguridad. *(propuesta)*
Consecuencias: se evita duplicar información y CU34 depende de la entidad TipoPrueba creada en CU33.

## D-013 · Validación de formato en cliente y de negocio en servidor

Fecha: 27/10 · It. 2 · Issue: #19 Contexto: la interfaz de CU33 (tipos de pruebas diagnósticas) puede comprobar datos antes de enviarlos, pero las reglas de negocio (nombre único, duración válida, permisos) también las aplica el servicio. Decisión: el cliente solo valida el formato (campos obligatorios vacíos, duración mayor que 0); la validación de negocio es siempre la del servidor, y el cliente muestra los errores que este devuelve. Alternativas: duplicar las reglas de negocio en el cliente. Motivo: evita reglas repetidas que se desincronicen y garantiza que ninguna regla se salte llamando al servicio directamente. Consecuencias: cualquier cambio en una regla de negocio se hace solo en el servidor; el cliente debe gestionar y mostrar los errores del servicio. (propuesta)

## D-014 · Confirmación explícita al desactivar un tipo de prueba

Fecha: 27/10 · It. 2 · Issue: #19 Contexto: desactivar un tipo de prueba es una baja lógica (D-008) que lo retira de la prescripción, y puede haber pruebas pendientes de ese tipo. Decisión: la interfaz pide confirmación en un diálogo antes de desactivar, y avisa si el servicio indica que hay pruebas pendientes. Alternativas: desactivar directamente con un solo clic; permitir deshacer después. Motivo: evita desactivaciones por error y deja claro el efecto antes de aplicarlo. Consecuencias: el cliente necesita el aviso de pruebas pendientes que devuelve el servidor; la desactivación requiere dos pasos. (propuesta)

## D-015 · Textos de los mensajes de error de la interfaz

Fecha: 27/10 · It. 2 · Issue: #19 Contexto: el servidor devuelve errores identificables (nombre duplicado, duración inválida, tipo no encontrado, sin permiso) y el cliente tiene que mostrarlos al administrador. Decisión: pendiente de cerrar con el equipo la redacción exacta de cada mensaje y si se muestran en el propio formulario o en un aviso aparte. Alternativas: mostrar el mensaje técnico del servidor tal cual; usar textos propios del cliente por cada código de error. Motivo: pendiente. Consecuencias: hasta decidirlo, los mensajes son provisionales y puede cambiar el cliente sin afectar al contrato del servidor. (pendiente)

