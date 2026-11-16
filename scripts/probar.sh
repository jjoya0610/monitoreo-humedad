#!/bin/sh
# Uso: ./scripts/probar.sh [URL_BASE]      (por defecto http://localhost:8080)
BASE="${1:-http://localhost:8080}"
TOKEN="AGRO-CAPTCHA-DEMO"
H="X-Captcha-Token: $TOKEN"

echo "--- 1. Sin Captcha (debe dar 403) ---"
curl -s -o /dev/null -w "HTTP %{http_code}\n" "$BASE/api/humedad/parcelas"

echo "--- 2. Salud (sin Captcha, 200) ---"
curl -s "$BASE/actuator/health"; echo

echo "--- 3. Lanzar simulacion de sensores ---"
curl -s -X POST -H "$H" "$BASE/api/humedad/simular?sensores=10&lecturas=10"; echo
sleep 6

echo "--- 4. Parcelas con datos ---"
curl -s -H "$H" "$BASE/api/humedad/parcelas"; echo

echo "--- 5. Ultima lectura de PARCELA-1 ---"
curl -s -H "$H" "$BASE/api/humedad/PARCELA-1/ultima"; echo

echo "--- 6. Resumen estadistico (Fork/Join) de PARCELA-1 ---"
curl -s -H "$H" "$BASE/api/humedad/PARCELA-1/resumen"; echo

echo "--- 7. Historial (5) de PARCELA-1 ---"
curl -s -H "$H" "$BASE/api/humedad/PARCELA-1/historial?limite=5"; echo

echo "--- 8. Estado del sistema (nucleos, actor) ---"
curl -s -H "$H" "$BASE/api/humedad/estado"; echo

echo "--- 9. Alertas en vivo durante 10 s (SSE) ---"
curl -s -N --max-time 10 -H "$H" "$BASE/api/humedad/alertas"; echo
