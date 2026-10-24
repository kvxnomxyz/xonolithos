#!/usr/bin/env bash
set -u
root=${XOS_ROOT:-/home/kvxnomxyz/Work/XonolithOS}
while :; do
  clear
  printf 'XonolithOS / xOS Samsung SM-T725 build\n'
  printf 'Updated: '; date '+%F %T %Z'
  printf '\nStatus:\n'
  if [[ -f "$root/.state/status.json" ]]; then sed -n '1,12p' "$root/.state/status.json"; else echo 'waiting for build status'; fi
  printf '\nResources:\n'; free -h | sed -n '1,3p'; df -h "$root" | tail -1
  printf '\nArtifacts:\n'
  find "$root/artifacts" -maxdepth 2 -type f -printf '%TY-%Tm-%Td %TH:%TM  %s bytes  %p\n' 2>/dev/null | sort | tail -12 || true
  printf '\nRecent log:\n'; tail -12 "$root/.state/build.log" 2>/dev/null || true
  sleep 5
done
