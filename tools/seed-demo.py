"""Populate the local application with fictional examples through its business API.

Run: python tools/seed-demo.py
Existing records are retained. Demo identities and workflow state are reused on reruns.
Administrator credentials come from .env; they are never written to the result file.
"""
import json
import uuid
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timedelta, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
STATE = ROOT / '.cache/demo-seed.json'
DEMO_PASSWORD = 'TestClinique2026!'
TUNIS = timezone(timedelta(hours=1))
PREFIX = '[DEMO] '


class Demo:
    def __init__(self):
        self.settings = {}
        for line in (ROOT / '.env').read_text(encoding='utf-8-sig').splitlines():
            if '=' in line and not line.startswith('#'):
                key, value = line.split('=', 1)
                self.settings[key] = value
        self.base = self.settings['PUBLIC_ORIGIN'].rstrip('/')
        if urllib.parse.urlsplit(self.base).hostname not in ('localhost', '127.0.0.1'):
            raise ValueError('Ce script est reserve a une instance locale.')
        STATE.parent.mkdir(exist_ok=True)
        self.state = json.loads(STATE.read_text(encoding='utf-8')) if STATE.exists() else {}
        if self.state.get('baseUrl', self.base) != self.base:
            raise ValueError('Le fichier de reprise appartient a une autre instance.')
        self.state['baseUrl'] = self.base
        self.state.setdefault('records', {})
        self.now = datetime.fromisoformat(self.state.setdefault('referenceTime', datetime.now(TUNIS).isoformat()))
        self.admin = self.login(self.settings['BOOTSTRAP_EMAIL'], self.settings['BOOTSTRAP_PASSWORD'])
        self.cache = {}
        self.tokens = {}
        self.users = {}
        self.save()

    def save(self):
        STATE.write_text(json.dumps(self.state, ensure_ascii=False, indent=2), encoding='utf-8')

    def request(self, method, route, body=None, token=None, version=None, expected=200, raw=False, mime=None):
        headers = {'Origin': self.base}
        if token:
            headers['Authorization'] = 'Bearer ' + token
        if method in ('POST', 'PUT', 'DELETE'):
            headers['Idempotency-Key'] = str(uuid.uuid4())
        if version is not None:
            headers['If-Match'] = '"' + str(version) + '"'
        if body is not None:
            headers['Content-Type'] = mime or 'application/json'
            if not isinstance(body, bytes):
                body = json.dumps(body).encode('utf-8')
        req = urllib.request.Request(self.base + '/api/v1/' + route, data=body, headers=headers, method=method)
        try:
            with urllib.request.urlopen(req, timeout=60) as response:
                status, payload = response.status, response.read()
        except urllib.error.HTTPError as error:
            status, payload = error.code, error.read()
        if status != expected:
            problem = json.loads(payload) if payload.startswith(b'{') else {}
            raise RuntimeError(f'{method} {route}: HTTP {status}; {problem.get("code", "")} {problem.get("detail", "")}')
        return payload if raw else json.loads(payload) if payload else None

    def login(self, email, password):
        return self.request('POST', 'auth/login', {'email': email, 'password': password})['accessToken']

    def all(self, route, refresh=False):
        if route in self.cache and not refresh:
            return self.cache[route]
        rows, page = [], 0
        while True:
            result = self.request('GET', f'{route}?size=100&page={page}', token=self.admin)
            rows.extend(result['items'])
            if 'totalElements' not in result or len(rows) >= result['totalElements']:
                break
            page += 1
        self.cache[route] = rows
        return rows

    def ensure(self, route, fields, identity, token=None):
        existing = next((row for row in self.all(route) if all(row.get(k) == v for k, v in identity.items())), None)
        if existing:
            return existing
        result = self.request('POST', route, {'fields': fields}, token or self.admin, expected=201)
        self.cache[route].append(result)
        self.state['records'].setdefault(route, []).append(result['id'])
        self.save()
        return result

    def get(self, route, entity, token=None):
        return self.request('GET', route + '/' + entity['id'], token=token or self.admin)

    def act(self, route, entity, action, body=None, token=None, expected=200):
        token = token or self.admin
        latest = self.get(route, entity, token)
        result = self.request('POST', f'{route}/{entity["id"]}/{action}', body or {}, token,
                              latest['version'], expected=expected)
        self.cache.pop(route, None)
        return result

    def user(self, key, name, role, contractor=None):
        email = key + '@clinique.example'
        existing = next((u for u in self.all('users') if u.get('email') == email), None)
        if existing:
            if existing['role'] != role or not existing.get('active', True):
                raise ValueError('Compte existant incompatible : ' + email)
            account = existing
        else:
            account = self.request('POST', 'users', {'email': email, 'name': PREFIX + name, 'role': role,
                'active': True, 'contractorId': contractor, 'password': DEMO_PASSWORD}, self.admin)
            self.cache['users'].append(account)
        self.tokens[key] = self.login(email, DEMO_PASSWORD)
        self.users[key] = account
        self.state.setdefault('accounts', {})[key] = {'email': email, 'role': role, 'id': account['id']}
        self.save()
        return account

    def execute(self, work, technician, minutes, offset, checklist=False, before_finish=None):
        tech = self.tokens[technician]
        lead = self.tokens['responsable']
        work = self.get('work-orders', work)
        if work['status'] == 'VALIDEE':
            return work
        if work['status'] == 'NOUVELLE':
            work = self.act('work-orders', work, 'assign', {'technicianId': self.users[technician]['id']}, lead)
        if work['status'] == 'AFFECTEE':
            work = self.act('work-orders', work, 'start', {'safetyConfirmed': True}, tech)
        if before_finish:
            before_finish(work)
        work = self.get('work-orders', work)
        if work['status'] == 'EN_COURS':
            if checklist:
                self.act('work-orders', work, 'answers', {'answers': {'safety': True, 'fixations': True,
                    'leak': 'CONFORME', 'notes': 'Exemple fictif : controles effectues.'}}, tech)
            if not any(t['workOrderId'] == work['id'] for t in self.all('time-entries', True)):
                start = (self.now - timedelta(hours=offset)).replace(second=0, microsecond=0)
                self.act('work-orders', work, 'time', {'startsAt': start.isoformat(),
                    'endsAt': (start + timedelta(minutes=minutes)).isoformat()}, tech)
            work = self.act('work-orders', work, 'complete', {'diagnostic': 'Exemple fictif : controle de la pompe',
                'cause': 'Entretien ou usure du joint - DEMO', 'solution': 'Controle et remise en etat - DEMO',
                'report': 'DEMONSTRATION UNIQUEMENT : aucun acte technique reel atteste.'}, tech)
        if work['status'] == 'TERMINEE':
            work = self.act('work-orders', work, 'validate', token=lead)
        assert work['status'] == 'VALIDEE'
        return work

    def upload(self, route, entity, filename, content, mime):
        existing = next((a for a in self.all('attachments', True) if a['targetId'] == entity['id'] and a['name'] == filename), None)
        if existing:
            return existing
        boundary = 'demo-' + uuid.uuid4().hex
        body = b''
        for key, value in [('targetType', route), ('targetId', entity['id'])]:
            body += f'--{boundary}\r\nContent-Disposition: form-data; name="{key}"\r\n\r\n{value}\r\n'.encode()
        body += f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="{filename}"\r\nContent-Type: {mime}\r\n\r\n'.encode() + content + f'\r\n--{boundary}--\r\n'.encode()
        return self.request('POST', 'files', body, self.admin, mime='multipart/form-data; boundary=' + boundary)

    def run(self):
        contractor = self.ensure('subcontractors', {'code': 'PREST-001', 'name': PREFIX + 'Maintenance Technique',
            'email': 'contact@prestataire.example', 'skills': 'Hydraulique'}, {'code': 'PREST-001'})
        for key, name, role in [('responsable', 'Mohamed Ben Salah', 'RESPONSABLE_TECHNIQUE'),
                ('ali', 'Ali Mansour', 'TECHNICIEN'), ('sami', 'Sami Trabelsi', 'TECHNICIEN'),
                ('nour', 'Nour Hamdi', 'TECHNICIEN'), ('achats', 'Amira Saidi', 'SERVICE_ACHATS'),
                ('direction', 'Leila Ahmed', 'DIRECTION'), ('externe', 'Karim Maintenance', 'SOUS_TRAITANT')]:
            self.user(key, name, role, contractor['id'] if key == 'externe' else None)
        print('Comptes de demonstration disponibles.', flush=True)

        teams = {}
        for code, person in [('A', 'ali'), ('B', 'sami'), ('C', 'nour')]:
            team = self.ensure('teams', {'code': code, 'name': PREFIX + 'Equipe ' + code}, {'code': code})
            teams[code] = team
            self.ensure('team-members', {'teamId': team['id'], 'userId': self.users[person]['id'],
                'validFrom': self.now.date().isoformat()}, {'teamId': team['id'], 'userId': self.users[person]['id']})
        skill = self.ensure('skills', {'code': 'HYDRAULIQUE', 'name': PREFIX + 'Maintenance hydraulique'}, {'code': 'HYDRAULIQUE'})
        for person in ('ali', 'sami', 'nour'):
            self.ensure('user-skills', {'userId': self.users[person]['id'], 'skillId': skill['id'],
                'validUntil': f'{self.now.year + 1}-12-31'}, {'userId': self.users[person]['id'], 'skillId': skill['id']})

        if 'planningStart' not in self.state:
            day = self.now.date() + timedelta(days=1)
            shifts = self.all('shifts')
            while any(day.isoformat() <= s['startsAt'][:10] <= (day + timedelta(days=3)).isoformat() for s in shifts):
                day += timedelta(days=4)
            self.state['planningStart'] = day.isoformat()
            self.save()
        day = datetime.fromisoformat(self.state['planningStart']).date()
        self.request('POST', 'planning/generate', {'startDate': day.isoformat(), 'days': 3, 'cycleDays': 1}, self.admin)
        first = datetime.combine(day, datetime.min.time(), TUNIS).replace(hour=6)
        shifts = sorted((s for s in self.all('shifts', True) if first <= datetime.fromisoformat(s['startsAt'].replace('Z', '+00:00')) < first + timedelta(days=3)), key=lambda s: s['startsAt'])
        people = {teams[c]['id']: p for c, p in [('A', 'ali'), ('B', 'sami'), ('C', 'nour')]}
        for shift in shifts:
            person = people[shift['teamId']]
            self.ensure('shift-assignments', {'shiftId': shift['id'], 'userId': self.users[person]['id']},
                {'shiftId': shift['id'], 'userId': self.users[person]['id']})
            if self.get('shifts', shift)['status'] == 'BROUILLON':
                self.act('shifts', shift, 'publish')
        handover = self.ensure('handovers', {'fromShiftId': shifts[0]['id'], 'toShiftId': shifts[1]['id'],
            'priority': 'P2', 'comment': PREFIX + 'Surveiller la pompe apres reparation.'},
            {'fromShiftId': shifts[0]['id'], 'toShiftId': shifts[1]['id']}, self.tokens['ali'])
        if not self.get('handovers', handover).get('acknowledgedAt'):
            self.act('handovers', handover, 'acknowledge', token=self.tokens[people[shifts[1]['teamId']]])
        absence_start = first + timedelta(days=10)
        absence = self.ensure('absences', {'userId': self.users['sami']['id'], 'startsAt': absence_start.isoformat(),
            'endsAt': (absence_start + timedelta(days=1)).isoformat(), 'reason': PREFIX + 'Conge'},
            {'userId': self.users['sami']['id'], 'reason': PREFIX + 'Conge'}, self.tokens['sami'])
        if self.get('absences', absence)['status'] == 'SOUMISE':
            self.act('absences', absence, 'approve', token=self.tokens['responsable'])
        print('Equipes, habilitations, neuf postes, absence et consigne crees.', flush=True)

        parent = None
        for code, name, kind in [('BAT-A', 'Batiment principal', 'BATIMENT'), ('ET-SS', 'Sous-sol', 'ETAGE'),
                ('ZONE-TECH', 'Zone technique', 'ZONE'), ('LOCAL-POMPES', 'Local des pompes', 'LOCAL')]:
            fields = {'code': code, 'name': PREFIX + name, 'type': kind}
            if parent:
                fields['parentId'] = parent['id']
            parent = self.ensure('locations', fields, {'code': code})
        supplier = self.ensure('suppliers', {'code': 'FOUR-001', 'name': PREFIX + 'Fournisseur Technique',
            'email': 'contact@fournisseur.example'}, {'code': 'FOUR-001'}, self.tokens['achats'])
        asset = self.ensure('assets', {'code': 'POMPE-001', 'name': PREFIX + 'Pompe de circulation technique',
            'category': 'Hydraulique', 'locationId': parent['id'], 'brand': 'Marque Demo', 'model': 'PC-100',
            'serialNumber': 'DEMO-2026-001', 'commissionedOn': f'{self.now.year}-01-01',
            'supplierId': supplier['id'], 'criticality': 'C2', 'status': 'EN_SERVICE'}, {'code': 'POMPE-001'})
        checklist = self.ensure('checklists', {'name': PREFIX + 'Controle mensuel de la pompe', 'assetId': asset['id'],
            'steps': [{'key': 'safety', 'label': 'Controles de securite effectues', 'type': 'ATTESTATION', 'required': True},
                {'key': 'fixations', 'label': 'Verifier les fixations', 'type': 'CHECKBOX', 'required': True},
                {'key': 'leak', 'label': "Verifier l'absence de fuite", 'type': 'CONFORMITY', 'required': True},
                {'key': 'notes', 'label': 'Observations', 'type': 'TEXT', 'required': False},
                {'key': 'photo', 'label': 'Photo apres intervention', 'type': 'PHOTO', 'required': False}]},
            {'name': PREFIX + 'Controle mensuel de la pompe'})
        if not self.get('checklists', checklist)['published']:
            self.act('checklists', checklist, 'publish')
        plan = self.ensure('preventive-plans', {'name': PREFIX + 'Entretien mensuel POMPE-001', 'assetId': asset['id'],
            'checklistId': checklist['id'], 'teamId': teams['A']['id'], 'frequency': 'MONTHLY', 'intervalCount': 1,
            'mode': 'FIXE', 'anchorLocal': (self.now + timedelta(days=1)).replace(hour=6, minute=0, second=0, microsecond=0).replace(tzinfo=None).isoformat(),
            'zoneId': 'Africa/Tunis', 'active': True, 'requiredSkillId': skill['id']}, {'name': PREFIX + 'Entretien mensuel POMPE-001'})
        self.act('preventive-plans', plan, 'generate')
        preventive = sorted((w for w in self.all('work-orders', True) if w.get('planId') == plan['id']), key=lambda w: w['createdAt'])[0]
        self.execute(preventive, 'ali', 30, 3, checklist=True)
        print('Patrimoine et exemple preventif valide disponibles.', flush=True)

        item = self.ensure('inventory/items', {'code': 'JOINT-001', 'name': PREFIX + 'Joint de pompe',
            'unit': 'piece', 'minimum': 2, 'unitCost': 12.5}, {'code': 'JOINT-001'}, self.tokens['achats'])
        failure = self.ensure('failure-reports', {'assetId': asset['id'], 'description': PREFIX + 'Fuite au niveau du joint',
            'priority': 'P2'}, {'description': PREFIX + 'Fuite au niveau du joint'}, self.tokens['ali'])
        failure = self.get('failure-reports', failure)
        if not failure.get('workOrderId'):
            failure = self.act('failure-reports', failure, 'qualify', {'title': PREFIX + 'Remplacement du joint POMPE-001'}, self.tokens['responsable'])
        corrective = {'id': failure['workOrderId']}

        def procure(work):
            work = self.get('work-orders', work)
            if work['status'] not in ('EN_COURS', 'EN_ATTENTE'):
                return
            existing_request = next((p for p in self.all('purchase-requests', True) if p.get('workOrderId') == work['id']), None)
            if not existing_request:
                if not any(r['workOrderId'] == work['id'] and r['itemId'] == item['id'] for r in self.all('inventory/reservations', True)):
                    self.act('work-orders', work, 'reserve', {'itemId': item['id'], 'quantity': 5}, self.tokens['ali'])
                if work['status'] == 'EN_COURS':
                    self.act('work-orders', work, 'wait', {'reason': 'PIECE', 'note': PREFIX + 'Attente de cinq joints'}, self.tokens['ali'])
            purchase = self.ensure('purchase-requests', {'workOrderId': work['id'], 'itemId': item['id'], 'quantity': 5,
                'justification': PREFIX + 'Reparer la fuite'}, {'workOrderId': work['id']}, self.tokens['ali'])
            purchase = self.get('purchase-requests', purchase)
            if purchase['status'] == 'BROUILLON':
                purchase = self.act('purchase-requests', purchase, 'submit', token=self.tokens['ali'])
            if purchase['status'] == 'SOUMISE':
                purchase = self.act('purchase-requests', purchase, 'approve', token=self.tokens['responsable'])
            if purchase['status'] == 'APPROUVEE_TECHNIQUE':
                purchase = self.act('purchase-requests', purchase, 'send-admin', token=self.tokens['responsable'])
            order = self.ensure('purchase-orders', {'requestId': purchase['id'], 'supplierId': supplier['id'],
                'quantity': 5, 'unitPrice': 12.5, 'expectedOn': self.now.date().isoformat()}, {'requestId': purchase['id']}, self.tokens['achats'])
            if self.get('purchase-orders', order)['status'] == 'BROUILLON':
                self.act('purchase-orders', order, 'place', token=self.tokens['achats'])
            for reference, quantity in [('DEMO-BL-001', 3), ('DEMO-BL-002', 2)]:
                receipt = self.ensure('goods-receipts', {'orderId': order['id'], 'deliveryReference': reference, 'quantity': quantity},
                    {'deliveryReference': reference}, self.tokens['achats'])
                if self.get('goods-receipts', receipt)['status'] == 'QUARANTAINE':
                    self.act('goods-receipts', receipt, 'quality', {'accepted': quantity, 'rejected': 0,
                        'note': PREFIX + 'Reception conforme'}, self.tokens['achats'])
            if self.get('work-orders', work)['status'] == 'EN_ATTENTE':
                self.act('work-orders', work, 'resume', token=self.tokens['ali'])
            reservations = [r for r in self.all('inventory/reservations', True) if r['workOrderId'] == work['id']]
            for reservation in reservations:
                remaining = reservation['quantity'] - reservation['consumed']
                if remaining > 0:
                    self.act('work-orders', work, 'consume', {'reservationId': reservation['id'], 'quantity': remaining}, self.tokens['ali'])
            anomaly = self.ensure('anomalies', {'workOrderId': work['id'], 'description': PREFIX + 'Fixation desserree',
                'severity': 'MOYENNE'}, {'workOrderId': work['id'], 'description': PREFIX + 'Fixation desserree'}, self.tokens['ali'])
            if not self.get('anomalies', anomaly).get('resolvedAt'):
                self.act('anomalies', anomaly, 'resolve', {'reason': PREFIX + 'Fixation resserree'}, self.tokens['responsable'])

        self.execute(corrective, 'ali', 60, 2, before_finish=procure)
        self.ensure('downtimes', {'assetId': asset['id'], 'workOrderId': corrective['id'],
            'startsAt': (self.now - timedelta(hours=2)).isoformat(), 'endsAt': (self.now - timedelta(hours=1)).isoformat(),
            'reason': PREFIX + 'Remplacement du joint'}, {'workOrderId': corrective['id'], 'reason': PREFIX + 'Remplacement du joint'})
        print('Correctif, anomalie, achats, deux receptions et consommation termines.', flush=True)

        contract_start = self.now.date().replace(day=1)
        contract_end = contract_start.replace(year=contract_start.year + 1) - timedelta(days=1)
        contract = self.ensure('contracts', {'contractorId': contractor['id'], 'reference': 'CT-2026-001',
            'startsOn': contract_start.isoformat(), 'endsOn': contract_end.isoformat(), 'scope': PREFIX + 'Maintenance des pompes techniques',
            'amount': 1500}, {'reference': 'CT-2026-001'})
        external_work = self.ensure('work-orders', {'title': PREFIX + 'Visite specialisee de la pompe',
            'description': 'Exemple fictif de prestation externe', 'assetId': asset['id'], 'priority': 'P3', 'type': 'CORRECTIVE'},
            {'title': PREFIX + 'Visite specialisee de la pompe'})
        if self.get('work-orders', external_work)['status'] == 'NOUVELLE':
            self.act('work-orders', external_work, 'assign', {'contractorId': contractor['id']}, self.tokens['responsable'])
        mission = self.ensure('external-interventions', {'contractId': contract['id'], 'workOrderId': external_work['id'],
            'plannedAt': self.now.isoformat(), 'cost': 120}, {'contractId': contract['id'], 'workOrderId': external_work['id']})
        material = self.ensure('material-requests', {'interventionId': mission['id'], 'designation': PREFIX + 'Kit de joints',
            'quantity': 1, 'justification': PREFIX + 'Materiel de la visite'}, {'interventionId': mission['id'], 'designation': PREFIX + 'Kit de joints'}, self.tokens['externe'])
        if self.get('material-requests', material)['status'] == 'SOUMISE':
            self.act('material-requests', material, 'approve', token=self.tokens['responsable'])
        if self.get('external-interventions', mission)['status'] == 'PLANIFIEE':
            self.act('external-interventions', mission, 'submit-report', {'report': PREFIX + 'Visite fictive : controles documentes, aucune prestation reelle.'}, self.tokens['externe'])
        if self.get('external-interventions', mission)['status'] == 'SOUMISE':
            self.act('external-interventions', mission, 'approve', token=self.tokens['responsable'])

        practice = self.ensure('work-orders', {'title': PREFIX + 'Intervention a realiser - surveillance pompe',
            'description': 'Exercice utilisateur : demarrer, saisir le temps et soumettre pour validation.',
            'assetId': asset['id'], 'checklistId': checklist['id'], 'requiredSkillId': skill['id'],
            'priority': 'P2', 'type': 'CORRECTIVE', 'dueAt': first.isoformat()},
            {'title': PREFIX + 'Intervention a realiser - surveillance pompe'})
        if self.get('work-orders', practice)['status'] == 'NOUVELLE':
            self.act('work-orders', practice, 'assign', {'technicianId': self.users['ali']['id'], 'teamId': teams['A']['id']}, self.tokens['responsable'])
        practice = self.get('work-orders', practice)
        if practice['status'] in ('AFFECTEE', 'EN_COURS', 'EN_ATTENTE'):
            practice_item = self.ensure('inventory/items', {'code': 'FILTRE-001', 'name': PREFIX + 'Filtre de rechange',
                'unit': 'piece', 'minimum': 2, 'unitCost': 8.5}, {'code': 'FILTRE-001'}, self.tokens['achats'])
            if not any(m['itemId'] == practice_item['id'] and m['reason'] == PREFIX + 'Inventaire initial' for m in self.all('inventory/movements', True)):
                self.act('inventory/items', practice_item, 'adjust', {'type': 'ADJUST_IN', 'quantity': 10,
                    'reason': PREFIX + 'Inventaire initial'}, self.tokens['achats'])
            if not any(r['workOrderId'] == practice['id'] and r['itemId'] == practice_item['id'] for r in self.all('inventory/reservations', True)):
                self.act('work-orders', practice, 'reserve', {'itemId': practice_item['id'], 'quantity': 2}, self.tokens['ali'])
        pdf = self.request('GET', 'reports/export?format=pdf', token=self.admin, raw=True)
        self.upload('work-orders', corrective, 'rapport-demonstration.pdf', pdf, 'application/pdf')
        qr = self.request('GET', f'assets/{asset["id"]}/qr', token=self.admin, raw=True)
        self.upload('assets', asset, 'qr-pompe-demonstration.png', qr, 'image/png')
        self.state['practiceWorkOrder'] = practice['id']
        self.state['assetId'] = asset['id']
        self.state['completed'] = True
        self.state['summary'] = {route: len(self.all(route, True)) for route in (
            'users', 'teams', 'locations', 'assets', 'shifts', 'shift-assignments', 'work-orders',
            'purchase-requests', 'purchase-orders', 'goods-receipts', 'inventory/movements',
            'external-interventions', 'material-requests', 'attachments', 'notifications', 'audit-events')}
        self.state['dashboard'] = self.request('GET', 'reports/dashboard', token=self.tokens['direction'])
        self.save()
        print('Demonstration complete. Resultat : .cache/demo-seed.json', flush=True)


if __name__ == '__main__':
    Demo().run()
