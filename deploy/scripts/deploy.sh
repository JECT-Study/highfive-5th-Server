#!/usr/bin/env bash
# CodeDeploy ApplicationStart: ECR에서 새 이미지를 받아 컨테이너를 다시 띄운다
set -euo pipefail

APP_DIR=/home/ubuntu/app
CONFIG_ENV=/home/ubuntu/config/.env
AWS_REGION="${AWS_REGION:-ap-northeast-2}"

cd "$APP_DIR"

# ECR_REGISTRY, IMAGE_TAG (CI가 배포 묶음에 넣은 파일)
# shellcheck source=/dev/null
source ./image.env

aws ecr get-login-password --region "$AWS_REGION" |
  docker login --username AWS --password-stdin "$ECR_REGISTRY"

compose() {
  docker compose --env-file "$CONFIG_ENV" --env-file ./image.env "$@"
}

compose pull app
compose up -d --remove-orphans

# 이전 버전 이미지 정리 (실행 중인 컨테이너가 쓰는 이미지는 남는다)
docker image prune -af
