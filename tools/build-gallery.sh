#!/usr/bin/env bash
# Compose Preview 스크린샷을 한 페이지 HTML로 모아본다.
#   사용법: ./gradlew :app:updateDebugScreenshotTest && bash tools/build-gallery.sh
# 결과: mockups/gallery.html (브라우저로 열기)
set -e
cd "$(dirname "$0")/.."

SRC="app/src/debug/screenshotTest/reference"
OUT_DIR="mockups"
OUT="$OUT_DIR/gallery.html"
IMG_DIR="$OUT_DIR/screens"

[ -d "$SRC" ] || { echo "스크린샷이 없습니다. 먼저 ./gradlew :app:updateDebugScreenshotTest 를 실행하세요."; exit 1; }

rm -rf "$IMG_DIR"; mkdir -p "$IMG_DIR"

{
  echo '<!doctype html><html lang="ko"><head><meta charset="utf-8">'
  echo '<title>Allowance Manager — 화면 갤러리</title><style>'
  echo 'body{margin:0;padding:32px;background:#12151b;color:#e8ecf3;font-family:"Malgun Gothic",system-ui,sans-serif}'
  echo 'h1{font-size:20px;font-weight:700;margin:0 0 4px}p.sub{margin:0 0 28px;color:#8A97AA;font-size:13px}'
  echo 'h2{font-size:14px;font-weight:700;margin:32px 0 14px;color:#10B981}'
  echo '.grid{display:flex;flex-wrap:wrap;gap:22px}'
  echo 'figure{margin:0;width:280px}'
  echo 'figure img{width:100%;border-radius:12px;border:1px solid #2a3140;display:block;background:#fff}'
  echo 'figcaption{margin-top:8px;font-size:12px;color:#A0AABB;text-align:center}'
  echo '</style></head><body>'
  echo '<h1>Allowance Manager — 화면 갤러리</h1>'
  echo "<p class=\"sub\">Compose Preview에서 실제 렌더링됨 · 생성 $(date '+%Y-%m-%d %H:%M')</p>"
} > "$OUT"

emit_group() {
  local title="$1" pattern="$2" found=0
  local body=""
  while IFS= read -r f; do
    [ -z "$f" ] && continue
    local base name safe
    base=$(basename "$f")
    # DesignGalleryKt.HomePreview_5. 홈_hash_hash_0.png → "5. 홈"
    name=$(echo "$base" | sed 's/^.*Kt\.[A-Za-z]*_//; s/_[0-9a-f]\{8\}_[0-9a-f]\{8\}_0\.png$//')
    safe=$(echo "$base" | tr ' ·' '__')
    cp "$f" "$IMG_DIR/$safe"
    body+="<figure><img src=\"screens/$safe\" alt=\"$name\"><figcaption>$name</figcaption></figure>"
    found=1
  done < <(find "$SRC" -name "$pattern" | sort)
  if [ $found -eq 1 ]; then
    echo "<h2>$title</h2><div class=\"grid\">$body</div>" >> "$OUT"
  fi
}

emit_group "화면 전체" "*Kt.[A-Z]*Preview_[0-9].*png"
emit_group "상태별 검수" "*Kt.*Preview_[!0-9]*png"

echo '</body></html>' >> "$OUT"
echo "생성 완료: $OUT ($(find "$IMG_DIR" -name '*.png' | wc -l)장)"
