# Monitoreo de Humedad en Suelos de Cultivos (Caso 1)
## Ejecutar (3 formas)

### A) Rapido, sin Docker (Java 21 + Maven, base de datos en memoria)
    mvn spring-boot:run -Dspring-boot.run.profiles=dev

### B) Docker Compose (app + PostgreSQL)
    docker compose up --build

### C) Kubernetes (Docker Desktop, minikube o kind)
    docker build -t monitoreo-humedad:1.0.0 .
    # minikube:  minikube image load monitoreo-humedad:1.0.0
    # kind:      kind load docker-image monitoreo-humedad:1.0.0
    kubectl apply -f k8s/
    kubectl -n agro get pods
    kubectl -n agro port-forward svc/monitoreo-humedad 8080:80

(El HPA necesita metrics-server: `minikube addons enable metrics-server`.)
Si algun pod se reinicia una vez en el primer arranque (crean la tabla a la vez), es normal.

## Probar
    ./scripts/probar.sh            # recorre todos los endpoints con curl

Todas las rutas /api/** exigen la cabecera `X-Captcha-Token`.
En modo DEMO el token es `AGRO-CAPTCHA-DEMO`. Con `CAPTCHA_SECRET` (clave de Google reCAPTCHA)
se valida contra Google.

| Metodo | Ruta | Que hace |
|---|---|---|
| GET | /api/humedad/parcelas | Parcelas con datos |
| GET | /api/humedad/{parcela}/ultima | Ultima lectura (cache y, si falta, BD) |
| GET | /api/humedad/{parcela}/resumen | Promedio/min/max calculado con Fork/Join |
| GET | /api/humedad/{parcela}/informe | Informe completo: 3 tareas en paralelo (CompletableFuture) |
| GET | /api/humedad/{parcela}/historial?limite=20 | Ultimas lecturas |
| GET | /api/humedad/stream | Lecturas en vivo (SSE) |
| GET | /api/humedad/alertas | Solo CRITICO o EXCESO en vivo (SSE) |
| GET | /api/humedad/estado | Nucleos, actor, contadores |
| POST | /api/humedad/simular?sensores=5&lecturas=10 | Lanza sensores simulados |
| GET | /actuator/health | Salud (sin Captcha) |

Niveles (editables en FuncionesHumedad): CRITICO <20 · BAJO 20-39 · OPTIMO 40-69 · EXCESO >=70.
