# AP-U1 · TurnoJusto: turnos de atención distribuidos

Aplicaciones Distribuidas (ISR-701) · UTEQ · Erwin Daniel Bueno Troya

> **Versión incompleta (práctica).** Busca los `TODO` en `src/` y en `docs/informe.tex`.
> Hecho: Parte A completa, Parte B en modo normal (TURNO, LLAMAR, ESTADO, cupo, token).
> Pendiente: B2 PruebaCarga, B3 modo `--inseguro`, Parte C (latidos), Parte D (reglas de Lamport y orden de OrdenTotal), mitad del informe.

Java 21 con sockets TCP, sin bibliotecas externas. Un coordinador (id 0) y tres ventanillas (V1, V2, V3), cada una en su propio proceso.

## Estructura

```
src/                Coordinador, Ventanilla, PruebaCarga, OrdenTotal y clases de apoyo
config.properties   cupo, token, host, puerto y tiempos de latido
logs/               un .log por proceso y orden_total.log
docs/               informe.tex, entrega_SGA.tex y capturas en docs/img/
```

## Compilar

Desde la carpeta raíz del repositorio (donde está `config.properties`), en PowerShell:

```powershell
javac -encoding UTF-8 -d out src\*.java
```

Todos los programas se ejecutan desde la carpeta raíz, porque leen `config.properties` y escriben en `logs\`.

## Ejecutar

Orden de arranque: primero el coordinador y después las ventanillas, cada una en su propia terminal.

```powershell
java -cp out Coordinador              # modo normal
java -cp out Coordinador --inseguro   # modo inseguro, solo para E3
java -cp out Ventanilla 1
java -cp out Ventanilla 2
java -cp out Ventanilla 3
java -cp out PruebaCarga              # con el coordinador recién iniciado
java -cp out OrdenTotal               # al final, une los cuatro logs
```

Órdenes de la ventanilla:

| Orden | Qué hace |
|---|---|
| `turno` | pide un número nuevo para una persona que llega |
| `llamar` | pide el siguiente turno en espera |
| `estado` | muestra emitidos, cupo, cola de espera y ventanillas activas o caídas |
| `token-malo` | envía un TURNO con un token incorrecto (escenario E2) |
| `sin-token` | envía un TURNO sin token (escenario E2) |
| `salir` | termina la ventanilla |

El contador de turnos vive en memoria, así que para empezar desde 1 hay que reiniciar el coordinador. PruebaCarga necesita un coordinador recién iniciado en cada corrida.

## Configuración (`config.properties`)

| Clave | Valor | Uso |
|---|---|---|
| `cupo` | 10 | turnos máximos por jornada |
| `token` | TJ-UTEQ-2026 | token que debe llevar toda solicitud |
| `host`, `puerto` | 127.0.0.1, 5000 | dirección del coordinador |
| `latido.intervalo.ms` | 2000 | cada cuánto envía latido la ventanilla |
| `latido.limite.ms` | 6000 | silencio tras el cual el coordinador marca CAIDA |
| `socket.timeout.ms` | 3000 | espera máxima de la ventanilla por una respuesta |
| `registrar.latidos` | true | si es false, los latidos actualizan el reloj pero no se escriben en el log |

## Formato de los mensajes

Cada mensaje es una línea de texto UTF-8 terminada en `\n`. El salto de línea es el delimitador: el coordinador lee con `readLine()`, así que varias solicitudes enviadas seguidas por la misma conexión se leen una por una sin mezclarse. Los campos van separados por `|`.

```
Solicitud:  TIPO|token|lamport|origen|dato
Respuesta:  TIPO|lamport|dato
```

| Solicitud | Respuestas posibles |
|---|---|
| `TURNO` | `ASIGNADO|<lamport>|<número>` o `RECHAZADA|<lamport>|cupo agotado (10 turnos)` |
| `LLAMAR` | `LLAMADO|<lamport>|<número>` o `RECHAZADA|<lamport>|no hay turnos en espera` |
| `ESTADO` | `ESTADO|<lamport>|emitidos=.. cupo=.. ultimo_llamado=.. en_espera=[..] ventanillas=V1:ACTIVA,...` |
| `LATIDO` | `ACK|<lamport>|` |
| cualquiera con token vacío o incorrecto | `RECHAZADA|<lamport>|token ausente` o `token invalido` |

Ejemplo: `TURNO|TJ-UTEQ-2026|17|1|` → `ASIGNADO|19|1`

## Registros

Cada proceso escribe en `logs/<proceso>.log` (`coordinador.log`, `V1.log`, `V2.log`, `V3.log`) una línea por evento:

```
lamport;id_proceso;evento;detalle
```

El reloj se incrementa antes de cada evento local o envío y se fija en max(local, recibido)+1 al recibir. Si un proceso se reinicia, su reloj continúa desde la última marca de su log, así las marcas no se repiten entre escenarios. `OrdenTotal` ordena por marca de Lamport y desempata por identificador de proceso.

## Compilar el informe

Compilador: `pdflatex`. Archivos principales: `docs/informe.tex` y `docs/entrega_SGA.tex`.

```powershell
cd docs
pdflatex informe.tex
pdflatex informe.tex
pdflatex entrega_SGA.tex
```

`informe.tex` se compila dos veces para que salgan bien las referencias a figuras y tablas.

## Código reutilizado

No se reutilizó código.
