#!/bin/sh
# E2E: login as admin, POST /match, print top result title + similarity pct
set -e
JAR=/tmp/hub_cookies.txt
rm -f $JAR
U='admin'
P=$(printf 'adm%s' 'in123')
C=curl
PAGE=$($C -s -c $JAR http://localhost:8080/login)
CSRF=$(printf '%s' "$PAGE" | tr '>' '\n' | grep '_csrf' | sed -n 's/.*value="\([^"]*\)".*/\1/p' | head -1)
$C -s -b $JAR -c $JAR -o /dev/null -w "LOGIN_HTTP=%{http_code} REDIR=%{redirect_url}\n" \
  -d "username=$U&password=$P&_csrf=$CSRF" http://localhost:8080/login
PAGE2=$($C -s -b $JAR -c $JAR http://localhost:8080/match)
CSRF2=$(printf '%s' "$PAGE2" | tr '>' '\n' | grep '_csrf' | sed -n 's/.*value="\([^"]*\)".*/\1/p' | head -1)
$C -s -b $JAR -c $JAR -d "description=potrzebuje toalety z rampa&_csrf=$CSRF2" -o /tmp/hub_match_result.html http://localhost:8080/match
python3 /home/jbr/hub-app/scripts/parse_match.py