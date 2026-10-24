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
Fecha: 24/10 · It. 0 · Issue: 20
Contexto: el enunciado no define los datos de un paciente y CU22, CU23 y CU24 comparten la entidad.
Decisión: Paciente tiene id, nombre, apellidos, dni, fechaNacimiento, telefono, email, direccion, numeroTarjetaSanitaria, fechaAlta y activo.
Alternativas: modelar historia clínica y datos de seguro. Motivo: se evita sobreingeniería, eso queda para la integración con HCE.
Consecuencias: CU23 y CU24 usan esta misma entidad; la baja es lógica con activo.

## D-006 · Identificación única por DNI/NIE y tarjeta sanitaria
Fecha: 24/10 · It. 0 · Issue: 20
Contexto: hay que evitar pacientes duplicados.
Decisión: dni y numeroTarjetaSanitaria son únicos; el alta falla si ya existen.
Consecuencias: el servicio lanza PacienteDuplicadoException.

## D-007 · El alta de paciente no crea cuenta de acceso
Fecha: 24/10 · It. 0 · Issue: 20
Contexto: los pacientes también son usuarios del sistema, pero el RF22 solo habla de dar de alta al paciente.
Decisión: CU22 solo registra los datos del paciente; el acceso se resolverá con SSO / gestión de usuarios.

