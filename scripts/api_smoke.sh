#!/bin/sh
# Smoke-test all /api/v1 endpoints against localhost:8080
C=curl
B=http://localhost:${APP_PORT:-8083}/api/v1
echo "--- GET /areas"; $C -s $B/areas | head -c 200; echo
echo "--- GET /innovations?q=toalety"; $C -s "$B/innovations?q=lazienki" | head -c 200; echo
echo "--- GET /innovations/4"; $C -s $B/innovations/4 | head -c 150; echo
echo "--- GET /innovations/999 (expect 404)"; $C -s -o /dev/null -w '%{http_code}\n' $B/innovations/999
echo "--- GET /resources"; $C -s $B/resources | head -c 120; echo
echo "--- POST /matches"; $C -s -X POST -H 'Content-Type: application/json' \
  -d '{"description":"moj brat ma 19 lat depresje i autyzm co robic"}' $B/matches \
  | python3 -c "import json,sys; d=json.load(sys.stdin); print('reportId',d['reportId'],'area',(d['area'] or {}).get('name')); [print(f\"  {m['similarity']:.2f} {m['innovation']['title']} | {m['why'][:60]}\") for m in d['matches'][:3]]"
echo "--- POST /ideas"; $C -s -o /dev/null -w '%{http_code}\n' -X POST -H 'Content-Type: application/json' \
  -d '{"title":"test api fiszka"}' $B/ideas
echo "--- POST /ideas (invalid, expect 400)"; $C -s -o /dev/null -w '%{http_code}\n' -X POST -H 'Content-Type: application/json' \
  -d '{"essence":"bez tytułu"}' $B/ideas
echo "--- GET /ideas"; $C -s $B/ideas | head -c 150; echo
echo "--- POST /ideas/assistant"; $C -s -X POST -H 'Content-Type: application/json' \
  -d '{"message":"jak sprawdzic czy pomysl ma sens?"}' $B/ideas/assistant | head -c 200; echo
echo "--- GET /reports"; $C -s $B/reports | head -c 120; echo
echo "--- POST /admin/innovations (anon, expect 401/403)"; $C -s -o /dev/null -w '%{http_code}\n' -X POST -H 'Content-Type: application/json' \
  -d '{"title":"x"}' $B/admin/innovations
echo "--- swagger"; $C -s -o /dev/null -w 'ui:%{http_code} ' http://localhost:${APP_PORT:-8083}/swagger-ui.html
$C -s http://localhost:${APP_PORT:-8083}/v3/api-docs | python3 -c "import json,sys; d=json.load(sys.stdin); print('paths:', len(d['paths']), 'title:', d['info']['title'])"