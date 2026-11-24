"""Read-only validation of the local Grafana/Prometheus deployment."""
import base64
import json
import time
import urllib.error
import urllib.parse
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
settings = {}
for line in (ROOT/'.env').read_text(encoding='utf-8-sig').splitlines():
    if '=' in line and not line.startswith('#'):
        key, value = line.split('=', 1)
        settings[key] = value
authorization = 'Basic ' + base64.b64encode(
    ('admin:' + settings['GRAFANA_PASSWORD']).encode()).decode()


def query(path):
    request = urllib.request.Request('http://localhost:3001' + path,
                                     headers={'Authorization': authorization})
    with urllib.request.urlopen(request, timeout=10) as response:
        return json.load(response)


def run():
    deadline = time.monotonic() + 120
    expression = urllib.parse.urlencode({'query': 'up{job="gmao"}'})
    while True:
        try:
            health = query('/api/health')
            dashboard = query('/api/dashboards/uid/gmao-operations')
            scrape = query('/api/datasources/proxy/uid/gmao-prometheus/api/v1/query?' + expression)
            values = scrape.get('data', {}).get('result', [])
            if health.get('database') == 'ok' and any(row['value'][1] == '1' for row in values):
                break
        except (urllib.error.URLError, TimeoutError):
            pass
        if time.monotonic() >= deadline:
            raise AssertionError('Monitoring local indisponible ou API non collectee apres 120 secondes.')
        time.sleep(3)
    result = {'grafanaHealthy': True, 'dashboardProvisioned': dashboard['dashboard']['uid'],
              'prometheusBackendUp': True, 'panels': len(dashboard['dashboard']['panels'])}
    (ROOT/'.cache/monitoring-result.json').write_text(json.dumps(result, indent=2), encoding='utf-8')
    print('Grafana, tableau provisionne et collecte Prometheus verifies.')


if __name__ == '__main__':
    run()
