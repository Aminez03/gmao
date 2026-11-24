"""Rehearse a PostgreSQL dump/restore only in the isolated verification project."""
import json
import subprocess
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CONTAINER = 'gmao-verification-postgres-1'


def execute(script):
    result = subprocess.run(['docker', 'exec', CONTAINER, 'sh', '-ec', script],
                            text=True, capture_output=True, check=True)
    return result.stdout.strip()


def run():
    started = time.monotonic()
    name = 'gmao_restore_' + str(int(time.time()))
    dump = '/tmp/' + name + '.dump'
    execute(f'pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc -f {dump}')
    execute(f'createdb -U "$POSTGRES_USER" {name}')
    try:
        execute(f'pg_restore -U "$POSTGRES_USER" --exit-on-error -d {name} {dump}')
        query = 'select (select count(*) from work_order), (select count(*) from stock_movement), (select count(*) from audit_event), (select count(*) from attachment)'
        source = execute(f'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Atc "{query}"')
        restored = execute(f'psql -U "$POSTGRES_USER" -d {name} -Atc "{query}"')
        if source != restored:
            raise AssertionError('Les volumes de donnees source/restauration different.')
        result = {'environment': 'gmao-verification', 'dumpRestorePassed': True,
                  'counts': source.split('|'), 'seconds': round(time.monotonic()-started, 2),
                  'scope': 'PostgreSQL only; object backup and off-host recovery remain operational prerequisites'}
        (ROOT/'.cache/backup-result.json').write_text(json.dumps(result, indent=2), encoding='utf-8')
        print('Sauvegarde et restauration PostgreSQL verifiees dans une base temporaire isolee.')
    finally:
        execute(f'dropdb -U "$POSTGRES_USER" {name}')
        execute(f'rm -- {dump}')


if __name__ == '__main__':
    run()
