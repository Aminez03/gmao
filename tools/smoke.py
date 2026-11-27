"""End-to-end HTTP verification. Runs only against the isolated Docker test port."""
import json, os, secrets, uuid, urllib.request, urllib.error
from datetime import datetime, timezone, timedelta
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1]
BASE='http://localhost:13000'
settings={}
for line in (ROOT/'.env').read_text(encoding='utf-8-sig').splitlines():
    if line and not line.startswith('#') and '=' in line:
        key,value=line.split('=',1);settings[key]=value
checks=[]
def check(name,condition):
    if not condition: raise AssertionError(name)
    checks.append(name)
    print('OK:',name,flush=True)

def request(method,path,body=None,token=None,key=None,version=None,expected=200,raw=False,content_type=None):
    headers={'Origin':BASE}
    if token:headers['Authorization']='Bearer '+token
    if key:headers['Idempotency-Key']=str(key)
    if version is not None:headers['If-Match']='"'+str(version)+'"'
    if body is not None:
        if not isinstance(body,bytes):body=json.dumps(body).encode('utf-8')
        headers['Content-Type']=content_type or 'application/json'
    req=urllib.request.Request(BASE+path,data=body,headers=headers,method=method)
    try:
        with urllib.request.urlopen(req,timeout=40) as response:code=response.status;payload=response.read()
    except urllib.error.HTTPError as error:
        code=error.code;payload=error.read()
    if code!=expected:
        # Do not dump request bodies, credentials or access tokens.
        problem=json.loads(payload) if payload.startswith(b'{') else {}
        raise AssertionError(f'{method} {path}: {code}, expected {expected}; {problem.get("code", "")} {problem.get("detail", "")}')
    if raw:return payload
    return json.loads(payload) if payload else None

def login(email,password):return request('POST','/api/v1/auth/login',{'email':email,'password':password})['accessToken']
def create(route,fields,token):return request('POST','/api/v1/'+route,{'fields':fields},token,uuid.uuid4(),expected=201)
def get(route,id,token):return request('GET','/api/v1/'+route+'/'+id,token=token)
def act(route,entity,action,body,token,expected=200):
    latest=get(route,entity['id'],token)
    return request('POST',f'/api/v1/{route}/{entity["id"]}/{action}',body,token,uuid.uuid4(),latest['version'],expected)

def run():
    check('Frontend HTTP',b'root' in request('GET','/',raw=True))
    check('Manifest PWA',request('GET','/manifest.webmanifest')['display']=='standalone')
    check('Service worker',len(request('GET','/sw.js',raw=True))>100)
    admin=login(settings['BOOTSTRAP_EMAIL'],settings['BOOTSTRAP_PASSWORD'])
    runid=uuid.uuid4().hex[:8]
    def user(role,contractor=None):
        email=f'{role.lower()}-{runid}-{uuid.uuid4().hex[:6]}@test.invalid';password=secrets.token_urlsafe(24)
        account=request('POST','/api/v1/users',{'email':email,'name':'Vérification '+role,'role':role,'password':password,'active':True,'contractorId':contractor},admin,uuid.uuid4())
        return account,login(email,password)
    tech_user,tech=user('TECHNICIEN');lead_user,lead=user('RESPONSABLE_TECHNIQUE');other_user,other=user('TECHNICIEN');buyer_user,buyer=user('ADMIN')
    check('Comptes des quatre rôles administrables',len(request('GET','/api/v1/users',token=admin)['items'])>=4)
    existing_teams={t['code']:t for t in request('GET','/api/v1/teams?size=100',token=admin)['items']}
    teams=[existing_teams.get(code) or create('teams',{'code':code,'name':'Équipe '+code},admin) for code in ['A','B','C']]
    start_day=datetime.now().date()+timedelta(days=10+secrets.randbelow(10000))
    generated=request('POST','/api/v1/planning/generate',{'startDate':start_day.isoformat(),'days':2,'cycleDays':1},admin,uuid.uuid4())
    check('Rotation A/B/C',generated['created']==6)
    shifts=sorted(request('GET','/api/v1/shifts?size=100',token=admin)['items'],key=lambda s:s['startsAt'])
    shifts=[s for s in shifts if start_day.isoformat()<=s['startsAt'][:10]<=(start_day+timedelta(days=1)).isoformat()]
    create('shift-assignments',{'shiftId':shifts[0]['id'],'userId':tech_user['id']},admin)
    create('shift-assignments',{'shiftId':shifts[1]['id'],'userId':other_user['id']},admin)
    act('shifts',shifts[0],'publish',{},admin)
    handover=create('handovers',{'fromShiftId':shifts[0]['id'],'toShiftId':shifts[1]['id'],'priority':'P1','comment':'Transmission de vérification'},tech)
    check('Accusé de réception par équipe entrante',act('handovers',handover,'acknowledge',{},other)['acknowledgedBy']==other_user['id'])
    parent=None
    for kind in ['BATIMENT','ETAGE','ZONE','LOCAL']:
        fields={'code':kind+'-'+runid,'name':kind+' de test','type':kind}
        if parent:fields['parentId']=parent['id']
        parent=create('locations',fields,admin)
    asset=create('assets',{'code':'POMPE-'+runid,'name':'Pompe de vérification','category':'Hydraulique','locationId':parent['id'],'criticality':'C1'},admin)
    check('QR code réel',request('GET',f'/api/v1/assets/{asset["id"]}/qr',token=tech,raw=True).startswith(b'\x89PNG'))
    check('Résolution QR autorisée',request('GET',f'/api/v1/assets/by-qr/{asset["qrToken"]}',token=tech)['id']==asset['id'])
    checklist=create('checklists',{'name':'Gamme de vérification','steps':[{'key':'visual','label':'Inspection visuelle','type':'CHECKBOX','required':True},{'key':'voltage','label':'Tension','type':'MEASURE','min':210,'max':240,'unit':'V','required':True}]},admin)
    act('checklists',checklist,'publish',{},admin)
    anchor=(datetime.now()+timedelta(days=1)).replace(hour=6,minute=0,second=0,microsecond=0).isoformat()
    plan=create('preventive-plans',{'name':'Préventif de test','assetId':asset['id'],'checklistId':checklist['id'],'teamId':teams[0]['id'],'frequency':'DAILY','intervalCount':1,'mode':'FIXE','anchorLocal':anchor,'zoneId':'Africa/Tunis','active':True},admin)
    act('preventive-plans',plan,'generate',{},admin)
    orders=request('GET','/api/v1/work-orders?size=100',token=admin)['items']
    w=next(o for o in orders if o.get('planId')==plan['id'])
    act('work-orders',w,'assign',{'technicianId':tech_user['id'],'teamId':teams[0]['id']},lead)
    act('work-orders',w,'start',{'safetyConfirmed':True},tech)
    act('work-orders',w,'answers',{'answers':{'visual':True,'voltage':230}},tech)
    act('work-orders',w,'time',{'startsAt':'2025-02-01T10:00:00Z','endsAt':'2025-02-01T10:30:00Z'},tech)
    act('work-orders',w,'complete',{'diagnostic':'Contrôle nominal','cause':'Préventif','solution':'Contrôles effectués','report':'Aucune anomalie'},tech)
    check('Validation indépendante',act('work-orders',w,'validate',{},lead)['status']=='VALIDEE')
    orders=request('GET','/api/v1/work-orders?size=100',token=admin)['items']
    check('Un seul prochain OT préventif',len([o for o in orders if o.get('planId')==plan['id'] and o['status'] not in ['VALIDEE','ANNULEE']])==1)
    failure=create('failure-reports',{'assetId':asset['id'],'description':'Fuite de vérification','priority':'P1'},tech)
    failure=act('failure-reports',failure,'qualify',{'title':'Réparation de vérification'},lead)
    corrective=get('work-orders',failure['workOrderId'],lead)
    act('work-orders',corrective,'assign',{'technicianId':tech_user['id']},lead)
    act('work-orders',corrective,'start',{'safetyConfirmed':True},tech)
    # Attachment round-trip through PostgreSQL.
    boundary='gmao-'+uuid.uuid4().hex;pdf=request('GET','/api/v1/reports/export?format=pdf',token=admin,raw=True)
    body=b''
    for name,value in [('targetType','work-orders'),('targetId',corrective['id'])]:
        body+=f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"\r\n\r\n{value}\r\n'.encode()
    body+=f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="verification.pdf"\r\nContent-Type: application/pdf\r\n\r\n'.encode()+pdf+f'\r\n--{boundary}--\r\n'.encode()
    attachment=request('POST','/api/v1/files',body,tech,content_type='multipart/form-data; boundary='+boundary)
    check('Fichier PostgreSQL aller-retour',request('GET',f'/api/v1/files/{attachment["id"]}/download',token=tech,raw=True)==pdf)
    item=create('inventory/items',{'code':'JOINT-'+runid,'name':'Joint','unit':'unité','minimum':2,'unitCost':3.5},admin)
    check('Couts masques pour le technicien','unitCost' not in get('inventory/items',item['id'],tech))
    act('work-orders',corrective,'reserve',{'itemId':item['id'],'quantity':10},tech)
    act('work-orders',corrective,'wait',{'reason':'PIECE','note':'Pièces indisponibles'},tech)
    purchase=create('purchase-requests',{'workOrderId':corrective['id'],'itemId':item['id'],'quantity':10,'justification':'Réparer la fuite'},tech)
    act('purchase-requests',purchase,'submit',{},tech);act('purchase-requests',purchase,'approve',{},lead);act('purchase-requests',purchase,'send-admin',{},lead)
    supplier=create('suppliers',{'code':'FOUR-'+runid,'name':'Fournisseur de test'},admin)
    order=create('purchase-orders',{'requestId':purchase['id'],'supplierId':supplier['id'],'quantity':10,'unitPrice':3.5},admin)
    act('purchase-orders',order,'place',{},admin)
    receipt=create('goods-receipts',{'orderId':order['id'],'deliveryReference':'BL-1-'+runid,'quantity':4},admin)
    act('goods-receipts',receipt,'quality',{'accepted':4,'rejected':0,'note':'Conforme'},buyer)
    check('Demande partiellement recue',get('purchase-requests',purchase['id'],tech)['status']=='PARTIELLEMENT_RECUE')
    check('Reprise bloquée après réception partielle',act('work-orders',corrective,'resume',{},tech,expected=422)['code']=='MISSING_PARTS')
    receipt=create('goods-receipts',{'orderId':order['id'],'deliveryReference':'BL-2-'+runid,'quantity':6},admin)
    act('goods-receipts',receipt,'quality',{'accepted':6,'rejected':0,'note':'Conforme'},buyer)
    check('Demande disponible apres controle',get('purchase-requests',purchase['id'],tech)['status']=='DISPONIBLE')
    check('Reprise après couverture complète',act('work-orders',corrective,'resume',{},tech)['status']=='EN_COURS')
    reservation=next(r for r in request('GET','/api/v1/inventory/reservations?size=100',token=admin)['items'] if r['workOrderId']==corrective['id'])
    act('work-orders',corrective,'consume',{'reservationId':reservation['id'],'quantity':10},tech)
    check('Consommation de stock réelle',get('inventory/items',item['id'],admin)['stock']==0)
    contractor=create('subcontractors',{'code':'PREST-'+runid,'name':'Prestataire de vérification'},admin)
    contract=create('contracts',{'contractorId':contractor['id'],'reference':'CT-'+runid,'startsOn':(datetime.now().date()-timedelta(days=1)).isoformat(),'endsOn':(datetime.now().date()+timedelta(days=365)).isoformat(),'scope':'Pompes'},admin)
    external=create('external-interventions',{'contractId':contract['id'],'workOrderId':corrective['id']},lead)
    ext_user,ext=user('SOUS_TRAITANT',contractor['id'])
    check('Mission externe filtrée',len(request('GET','/api/v1/external-interventions',token=ext)['items'])==1)
    act('external-interventions',external,'submit-report',{'report':'Rapport de vérification'},ext)
    check('Réception interne de prestation',act('external-interventions',external,'approve',{},lead)['status']=='VALIDEE')
    report_admin_user,report_admin=user('ADMIN')
    report=request('GET','/api/v1/reports/dashboard',token=report_admin)
    check('Indicateurs réels',report['active']>=1 and len(report['series'])==7)
    check('Export PDF',request('GET','/api/v1/reports/export?format=pdf',token=report_admin,raw=True).startswith(b'%PDF'))
    check('Export XLSX',request('GET','/api/v1/reports/export?format=xlsx',token=report_admin,raw=True).startswith(b'PK'))
    check('Journal d’audit',request('GET','/api/v1/audit-events?size=100',token=admin)['totalElements']>10)
    request('POST','/api/v1/auth/logout',{},tech)
    request('GET','/api/v1/auth/me',token=tech,expected=401)
    check('Session révoquée',True)
    result={'checks':checks,'count':len(checks),'environment':'gmao-verification','baseUrl':BASE,'timestamp':datetime.now(timezone.utc).isoformat()}
    (ROOT/'.cache/smoke-result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2),encoding='utf-8')
    print(f'{len(checks)} vérifications HTTP réussies ; aucune donnée ajoutée à la base de l’application principale.')

if __name__=='__main__':run()
