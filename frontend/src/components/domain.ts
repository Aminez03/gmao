import catalogue from '../catalog.json';

export type Field = { name: string; type: string; required: boolean };

export type Resource = {
  class: string;
  route: string;
  label: string;
  group: string;
  fields: Field[];
};

export const resources = catalogue as Resource[];

export function getResource(route: string): Resource {
  const resource = resources.find((item) => item.route === route);
  if (!resource) throw new Error(`Ressource inconnue : ${route}`);
  return resource;
}

export const labels: Record<string, string> = {
  id: 'ID',
  displayNumber: 'Numéro',
  code: 'Code',
  name: 'Nom',
  currentPassword: 'Mot de passe actuel',
  newPassword: 'Nouveau mot de passe (12 caractères minimum)',
  startDate: 'Premier jour',
  days: 'Nombre de jours',
  cycleDays: 'Jours avant rotation',
  type: 'Type',
  parentId: 'Parent',
  category: 'Catégorie',
  locationId: 'Localisation',
  brand: 'Marque',
  model: 'Modèle',
  serialNumber: 'Numéro de série',
  commissionedOn: 'Mise en service',
  supplierId: 'Fournisseur',
  warrantyUntil: 'Fin de garantie',
  criticality: 'Criticité',
  status: 'État',
  teamId: 'Équipe',
  userId: 'Utilisateur',
  createdBy: 'Créé par',
  approvedBy: 'Approuvé par',
  validatedBy: 'Validé par',
  acknowledgedBy: 'Accusé de réception par',
  acknowledgedAt: 'Date d’accusé de réception',
  resolvedBy: 'Résolu par',
  recipientId: 'Destinataire',
  actorId: 'Auteur',
  skillId: 'Compétence',
  validUntil: 'Valide jusqu’au',
  validFrom: 'Valide à partir du',
  startsAt: 'Début',
  endsAt: 'Fin',
  shiftId: 'Poste',
  replacementForId: 'Affectation remplacée',
  reason: 'Motif',
  fromShiftId: 'Poste sortant',
  toShiftId: 'Poste entrant',
  priority: 'Priorité',
  comment: 'Consignes',
  workOrderIds: 'Interventions à transmettre',
  assetIds: 'Équipements à surveiller',
  assetId: 'Équipement',
  steps: 'Étapes de la gamme',
  revision: 'Version',
  checklistId: 'Gamme publiée',
  frequency: 'Fréquence',
  intervalCount: 'Intervalle',
  mode: 'Calcul de l’échéance',
  anchorLocal: 'Date et heure de référence',
  zoneId: 'Fuseau horaire',
  active: 'Actif',
  planId: 'Plan préventif',
  requiredSkillId: 'Habilitation requise',
  title: 'Intitulé',
  description: 'Description',
  technicianId: 'Technicien',
  contractorId: 'Prestataire',
  dueAt: 'Échéance',
  diagnostic: 'Diagnostic',
  cause: 'Cause identifiée',
  solution: 'Solution appliquée',
  report: 'Rapport final',
  safetyConfirmed: 'Autorisations et contrôles de sécurité vérifiés',
  actualMinutes: 'Temps réel (min)',
  workOrderId: 'Intervention',
  stepKey: 'Clé de l’étape',
  severity: 'Gravité',
  disposition: 'Disposition prise',
  email: 'Adresse e-mail',
  phone: 'Téléphone',
  address: 'Adresse',
  unit: 'Unité',
  stock: 'Stock physique',
  reserved: 'Réservé',
  minimum: 'Seuil minimum',
  unitCost: 'Coût unitaire (TND)',
  itemId: 'Pièce',
  quantity: 'Quantité',
  justification: 'Justification',
  requestId: 'Demande d’achat',
  unitPrice: 'Prix unitaire (TND)',
  expectedOn: 'Livraison prévue',
  orderId: 'Commande',
  deliveryReference: 'Référence du bon de livraison',
  accepted: 'Quantité acceptée',
  rejected: 'Quantité refusée',
  qualityNote: 'Note du contrôle',
  skills: 'Domaines de compétence',
  reference: 'Référence',
  startsOn: 'Date de début',
  endsOn: 'Date de fin',
  scope: 'Périmètre du contrat',
  amount: 'Montant (TND)',
  contractId: 'Contrat',
  plannedAt: 'Visite prévue',
  visitedAt: 'Visite effectuée',
  cost: 'Coût de prestation',
  interventionId: 'Mission externe',
  designation: 'Désignation',
  targetType: 'Type de document',
  targetId: 'Ressource associée',
  contentType: 'Format',
  size: 'Taille',
  createdAt: 'Création',
  updatedAt: 'Dernière mise à jour',
  number: 'Numéro',
  role: 'Rôle',
  password: 'Mot de passe initial',
  note: 'Commentaire',
  reservationId: 'Réservation',
  minutes: 'Minutes',
  nextDueAt: 'Prochaine échéance',
};

export const options: Record<string, string[]> = {
  criticality: ['C1', 'C2', 'C3', 'C4'],
  priority: ['P1', 'P2', 'P3', 'P4'],
  frequency: ['DAILY', 'WEEKLY', 'MONTHLY', 'QUARTERLY', 'SEMESTRAL', 'ANNUAL'],
  mode: ['FIXE', 'GLISSANT'],
  severity: ['FAIBLE', 'MOYENNE', 'CRITIQUE'],
  role: ['ADMIN', 'RESPONSABLE_TECHNIQUE', 'TECHNICIEN', 'SOUS_TRAITANT'],
  zoneId: ['Africa/Tunis'],
  targetType: ['assets', 'work-orders', 'failure-reports', 'external-interventions', 'contracts'],
};

export const userReferenceFields = new Set([
  'userId',
  'technicianId',
  'createdBy',
  'approvedBy',
  'validatedBy',
  'acknowledgedBy',
  'resolvedBy',
  'recipientId',
  'actorId',
]);

export const referenceRoutes: Record<string, string> = {
  ...Object.fromEntries([...userReferenceFields].map((field) => [field, 'users'])),
  parentId: 'locations',
  locationId: 'locations',
  supplierId: 'suppliers',
  assetId: 'assets',
  teamId: 'teams',
  skillId: 'skills',
  requiredSkillId: 'skills',
  shiftId: 'shifts',
  fromShiftId: 'shifts',
  toShiftId: 'shifts',
  replacementForId: 'shift-assignments',
  checklistId: 'checklists',
  planId: 'preventive-plans',
  workOrderId: 'work-orders',
  itemId: 'inventory/items',
  requestId: 'purchase-requests',
  orderId: 'purchase-orders',
  contractorId: 'subcontractors',
  contractId: 'contracts',
  interventionId: 'external-interventions',
  reservationId: 'inventory/reservations',
};

export function isReferenceField(name: string) {
  return !!referenceRoutes[name] || ['resourceId', 'targetId'].includes(name);
}

export function shiftAssignmentLabel(assignment: { userNames?: Record<string, string> }) {
  return assignment.userNames?.userId || 'Utilisateur indisponible';
}

export const generated = new Set([
  'id',
  'displayNumber',
  'version',
  'createdAt',
  'updatedAt',
  'createdBy',
  'archived',
  'number',
  'qrToken',
  'stock',
  'reserved',
  'allocated',
  'consumed',
  'accepted',
  'rejected',
  'approvedAt',
  'approvedBy',
  'acknowledgedAt',
  'acknowledgedBy',
  'validatedAt',
  'validatedBy',
  'startedAt',
  'completedAt',
  'published',
  'revision',
  'actualMinutes',
  'answers',
  'checklistSnapshot',
  'occurrenceId',
  'planId',
  'resolvedAt',
  'resolvedBy',
  'correctiveReportId',
  'status',
  'nextDueAt',
  'recipientId',
  'resourceType',
  'resourceId',
  'contentType',
  'size',
]);

export function editableFields(r: Resource) {
  return r.fields.filter(
    (f) =>
      (!generated.has(f.name) || (f.name === 'status' && r.route === 'assets')) &&
      !(f.name === 'workOrderId' && r.route === 'failure-reports') &&
      !(r.route === 'external-interventions' && f.name === 'contractorId'),
  );
}

export function fieldOptions(f: Field, r?: Resource) {
  if (f.name === 'type' && r?.route === 'locations') return ['BATIMENT', 'ETAGE', 'ZONE', 'LOCAL'];
  if (f.name === 'type' && r?.route === 'work-orders') return ['CORRECTIVE', 'PREVENTIVE'];
  if (f.name === 'status' && r?.route === 'assets')
    return ['EN_SERVICE', 'INDISPONIBLE', 'SURVEILLANCE', 'HORS_SERVICE'];
  return options[f.name];
}

export const human = (s: string) =>
  ({
    DAILY: 'Quotidienne',
    WEEKLY: 'Hebdomadaire',
    MONTHLY: 'Mensuelle',
    QUARTERLY: 'Trimestrielle',
    SEMESTRAL: 'Semestrielle',
    ANNUAL: 'Annuelle',
    FIXE: 'Calendrier fixe',
    GLISSANT: 'Depuis la validation',
    RESPONSABLE_TECHNIQUE: 'Responsable technique',
    TECHNICIEN: 'Technicien',
    SOUS_TRAITANT: 'Sous-traitant',
    ADMIN: 'Administrateur',
  })[s] ||
  s
    ?.replaceAll('_', ' ')
    .toLowerCase()
    .replace(/^./, (v) => v.toUpperCase());

export function canCreate(r: Resource, role: string) {
  if (
    [
      'notifications',
      'audit-events',
      'attachments',
      'preventive-occurrences',
      'inventory/movements',
      'inventory/reservations',
      'time-entries',
    ].includes(r.route)
  )
    return false;
  if (['ADMIN', 'RESPONSABLE_TECHNIQUE'].includes(role)) return true;
  if (role === 'TECHNICIEN')
    return ['failure-reports', 'purchase-requests', 'absences', 'handovers'].includes(r.route);
  return role === 'SOUS_TRAITANT' && r.route === 'material-requests';
}

export function visible(r: Resource, role: string) {
  if (['ADMIN', 'RESPONSABLE_TECHNIQUE'].includes(role)) return true;
  if (role === 'SOUS_TRAITANT')
    return [
      'assets',
      'work-orders',
      'anomalies',
      'subcontractors',
      'contracts',
      'external-interventions',
      'material-requests',
      'notifications',
      'attachments',
    ].includes(r.route);
  if (role === 'TECHNICIEN')
    return ![
      'audit-events',
      'users',
      'suppliers',
      'purchase-orders',
      'goods-receipts',
      'inventory/movements',
      'subcontractors',
      'contracts',
      'external-interventions',
      'material-requests',
    ].includes(r.route);
  return false;
}
