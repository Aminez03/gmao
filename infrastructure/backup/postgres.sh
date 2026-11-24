#!/bin/sh
set -eu
umask 077
timestamp=$(date -u +%Y%m%dT%H%M%SZ)
pg_dump --format=custom --no-owner --file="/backups/gmao-${timestamp}.dump"
printf 'Sauvegarde PostgreSQL terminée : %s\n' "$timestamp"
