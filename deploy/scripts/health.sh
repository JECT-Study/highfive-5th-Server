#!/usr/bin/env bash
# CodeDeploy ValidateService: /actuator/health가 UP이 될 때까지 기다린다 (5초 간격, 최대 150초)
# 끝내 UP이 아니면 배포를 실패로 처리하고 앱 로그를 남긴다
set -uo pipefail

URL=http://127.0.0.1:8080/actuator/health

for _ in $(seq 1 30); do
  if curl -fsS "$URL" 2>/dev/null | grep -q '"status":"UP"'; then
    echo "health check passed"
    exit 0
  fi
  sleep 5
done

echo "health check failed: $URL" >&2
docker logs --tail 100 highfive-app-1 >&2 || true
exit 1
