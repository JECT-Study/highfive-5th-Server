#!/usr/bin/env bash
# GitHub 이벤트를 Discord 임베드 메시지로 보낸다 (.github/workflows/discord-notify.yml에서 호출)
# 입력은 모두 환경변수로 받는다. DRY_RUN=1이면 보내지 않고 JSON만 출력한다
#
#   KIND=pr_opened | pr_merged : PR_NUMBER, PR_TITLE, PR_URL, PR_AUTHOR, HEAD_BRANCH, BASE_BRANCH
#   KIND=ci_failed             : BRANCH, COMMIT_MESSAGE, ACTOR, RUN_URL
set -euo pipefail

JIRA_BROWSE_URL="https://ject04.atlassian.net/browse"

# 제목에서 첫 번째 JECT-123을 찾아 Jira 링크로 만든다. 없으면 "-"
jira_link() {
  local key
  key=$(grep -oE 'JECT-[0-9]+' <<<"$1" | head -n 1 || true)
  if [[ -n "$key" ]]; then
    echo "[$key]($JIRA_BROWSE_URL/$key)"
  else
    echo "-"
  fi
}

# 임베드: 위에 이벤트(header), 그 아래 링크가 걸린 제목(title → url), 그리고 필드들
# PR 제목의 [JECT-123]이 마크다운 링크를 깨뜨리지 않도록 제목은 임베드 title로 넣는다 (최대 256자)
embed() {
  jq -n --arg header "$1" --argjson color "$2" --arg title "${3:0:256}" --arg url "$4" \
    --argjson fields "$5" \
    '{embeds: [{author: {name: $header}, title: $title, url: $url, color: $color,
       fields: $fields}],
      allowed_mentions: {parse: []}}'
}

field() {
  jq -n --arg name "$1" --arg value "$2" '{name: $name, value: $value, inline: true}'
}

case "${KIND:-}" in
  pr_opened | pr_merged)
    if [[ "$KIND" == "pr_opened" ]]; then
      header="🆕 PR이 열렸습니다"
      color=5793266 # 파랑
    else
      header="✅ PR이 머지됐습니다"
      color=3066993 # 초록
    fi
    fields=$(jq -s . \
      <(field "작성자" "$PR_AUTHOR") \
      <(field "브랜치" "\`$HEAD_BRANCH\` → \`$BASE_BRANCH\`") \
      <(field "Jira" "$(jira_link "$PR_TITLE")"))
    payload=$(embed "$header" "$color" "#$PR_NUMBER $PR_TITLE" "$PR_URL" "$fields")
    ;;
  ci_failed)
    # 커밋 메시지는 첫 줄만 쓴다 (머지 커밋이면 PR 제목)
    subject=$(head -n 1 <<<"$COMMIT_MESSAGE")
    fields=$(jq -s . \
      <(field "푸시한 사람" "$ACTOR") \
      <(field "Jira" "$(jira_link "$subject")"))
    payload=$(embed "🚨 $BRANCH CI가 실패했습니다" 15158332 "$subject" "$RUN_URL" "$fields")
    ;;
  *)
    echo "알 수 없는 KIND: ${KIND:-}" >&2
    exit 1
    ;;
esac

if [[ "${DRY_RUN:-}" == "1" ]]; then
  echo "$payload"
  exit 0
fi

# 알림이 실패해도 PR·CI 자체를 막지는 않도록 시크릿이 없으면 건너뛴다
if [[ -z "${DISCORD_WEBHOOK_URL:-}" ]]; then
  echo "::notice::DISCORD_WEBHOOK_URL 시크릿이 없어 알림을 건너뜁니다"
  exit 0
fi

curl -fsS -X POST -H "Content-Type: application/json" -d "$payload" "$DISCORD_WEBHOOK_URL"
echo "Discord 알림 전송: $KIND"
