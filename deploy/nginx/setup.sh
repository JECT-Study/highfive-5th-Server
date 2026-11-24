#!/usr/bin/env bash
# dev 서버(EC2)에 Nginx를 설치하고 설정을 적용한다. 서버에서 직접 실행한다 (배포 훅에는 넣지 않음)
#
#   sudo bash /home/ubuntu/app/nginx/setup.sh                  # HTTP만: http://<탄력적 IP>로 접속
#   sudo CERTBOT_EMAIL=me@example.com \
#     bash /home/ubuntu/app/nginx/setup.sh dev-api.example.com   # 도메인 연결 + HTTPS
#
# 같은 명령을 다시 실행해도 된다 (설정 파일을 다시 복사하고, 인증서는 있으면 재사용한다)
# 도메인을 넣으려면 DNS에 도메인 → 서버 IP A 레코드가 먼저 걸려 있어야 한다
# 설정 파일을 레포에서 고쳤다면 새 배포가 끝난 뒤 이 스크립트를 다시 실행해 적용한다
set -euo pipefail

DOMAIN="${1:-}"
SRC_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CONF_DEST=/etc/nginx/conf.d/highfive.conf

if [[ $EUID -ne 0 ]]; then
  echo "sudo로 실행해 주세요." >&2
  exit 1
fi

if [[ -n "$DOMAIN" && -z "${CERTBOT_EMAIL:-}" ]]; then
  echo "도메인을 넣으면 CERTBOT_EMAIL(인증서 만료 알림을 받을 이메일) 환경변수가 필요합니다." >&2
  exit 1
fi

export DEBIAN_FRONTEND=noninteractive
apt-get update -y
apt-get install -y nginx
if [[ -n "$DOMAIN" ]]; then
  apt-get install -y certbot python3-certbot-nginx
fi

# 기본 사이트가 80번 default_server를 잡고 있어서 충돌하므로 끈다
rm -f /etc/nginx/sites-enabled/default

install -m 644 "$SRC_DIR/highfive.conf" "$CONF_DEST"
if [[ -n "$DOMAIN" ]]; then
  sed -i "s/server_name _;/server_name $DOMAIN;/" "$CONF_DEST"
fi

nginx -t
systemctl enable nginx
systemctl reload nginx || systemctl restart nginx

if [[ -n "$DOMAIN" ]]; then
  # --redirect: HTTP 요청을 HTTPS로 넘기게 설정. 갱신은 certbot 타이머가 알아서 한다
  certbot --nginx -d "$DOMAIN" --non-interactive --agree-tos \
    -m "$CERTBOT_EMAIL" --redirect
  systemctl list-timers certbot.timer --no-pager || true
fi

echo "Nginx 적용 완료"
if [[ -n "$DOMAIN" ]]; then
  echo "확인: curl -sI https://$DOMAIN/api/v1/ (502만 아니면 앱까지 연결된 것)"
else
  echo "확인: curl -sI http://127.0.0.1/api/v1/ (502만 아니면 앱까지 연결된 것)"
fi
