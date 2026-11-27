import { Entity } from '../services/api';
import { Field } from './domain';
export type Action = { key: string; label: string; fields: Field[] };
const f = (name: string, type = 's', required = true): Field => ({ name, type, required });
export function actions(route: string, e: Entity, role: string): Action[] {
  const list: Action[] = [];
  const sup = ['ADMIN', 'RESPONSABLE_TECHNIQUE'].includes(role);
  const buy = role === 'ADMIN';
  const execute = sup || ['TECHNICIEN', 'SOUS_TRAITANT'].includes(role);
  const add = (key: string, label: string, fields: Field[] = []) =>
    list.push({ key, label, fields });
  if (route === 'work-orders') {
    if (sup && ['NOUVELLE', 'AFFECTEE', 'EN_ATTENTE'].includes(e.status))
      add('assign', 'Affecter', [
        f('technicianIds', 'u', false),
        f('teamId', 'u', false),
        f('contractorId', 'u', false),
      ]);
    if (e.status === 'AFFECTEE' && execute) add('start', 'Démarrer', [f('safetyConfirmed', 'b')]);
    if (e.status === 'EN_COURS' && execute) {
      add('wait', 'Mettre en attente', [f('reason'), f('note', 'x')]);
      add('time', 'Enregistrer du temps', [f('startsAt', 't'), f('endsAt', 't')]);
      add('complete', 'Soumettre pour validation', [
        f('diagnostic', 'x'),
        f('cause', 'x'),
        f('solution', 'x'),
        f('report', 'x'),
      ]);
    }
    if (e.status === 'EN_ATTENTE' && execute) add('resume', 'Reprendre');
    if (sup && e.status === 'TERMINEE') {
      add('validate', 'Valider l’intervention');
      add('reject-validation', 'Refuser la validation', [f('reason', 'x')]);
    }
    if (
      !['VALIDEE', 'ANNULEE'].includes(e.status) &&
      ['ADMIN', 'RESPONSABLE_TECHNIQUE', 'TECHNICIEN'].includes(role)
    ) {
      add('reserve', 'Réserver une pièce', [f('itemId', 'u'), f('quantity', 'n')]);
      if (e.status === 'EN_COURS')
        add('consume', 'Consommer une pièce', [f('reservationId', 'u'), f('quantity', 'n')]);
    }
    if (sup && !['VALIDEE', 'ANNULEE'].includes(e.status))
      add('cancel', 'Annuler l’intervention', [f('reason', 'x')]);
  }
  if (route === 'checklists' && sup && !e.published) add('publish', 'Publier la gamme');
  if (route === 'preventive-plans' && sup) {
    add('generate', 'Générer les échéances');
    if (e.active) add('disable', 'Désactiver le plan', [f('reason', 'x')]);
  }
  if (route === 'failure-reports' && sup && !e.workOrderId)
    add('qualify', 'Créer l’intervention', [f('title')]);
  if (route === 'purchase-requests') {
    if (e.status === 'BROUILLON') add('submit', 'Soumettre la demande');
    if (sup && e.status === 'SOUMISE') {
      add('approve', 'Approuver');
      add('reject', 'Renvoyer au demandeur', [f('reason', 'x')]);
    }
    if (sup && e.status === 'APPROUVEE_TECHNIQUE') add('send-admin', 'Transmettre aux achats');
  }
  if (route === 'purchase-orders' && buy && e.status === 'BROUILLON') {
    add('place', 'Confirmer la commande');
    add('cancel', 'Annuler la commande', [f('reason', 'x')]);
  }
  if (route === 'goods-receipts' && (sup || buy) && e.status === 'QUARANTAINE')
    add('quality', 'Contrôler la réception', [
      f('accepted', 'n'),
      f('rejected', 'n'),
      f('note', 'x'),
    ]);
  if (route === 'inventory/items' && buy)
    add('adjust', 'Ajuster le stock', [f('type'), f('quantity', 'n'), f('reason', 'x')]);
  if (route === 'inventory/reservations' && (sup || buy))
    add('release', 'Libérer la réservation', [f('reason', 'x')]);
  if (route === 'shifts' && sup && e.status === 'BROUILLON') add('publish', 'Publier le poste');
  if (route === 'shift-assignments' && sup)
    add('withdraw', 'Retirer l’affectation', [f('reason', 'x')]);
  if (route === 'absences' && sup && e.status === 'SOUMISE') {
    add('approve', 'Approuver l’absence');
    add('reject', 'Refuser', [f('reason', 'x')]);
  }
  if (route === 'handovers' && !e.acknowledgedAt && (sup || role === 'TECHNICIEN'))
    add('acknowledge', 'Confirmer la réception');
  if (route === 'anomalies' && sup && !e.resolvedAt) {
    add('resolve', 'Enregistrer la disposition', [f('reason', 'x')]);
    if (!e.correctiveReportId) add('corrective-request', 'Créer une demande corrective');
  }
  if (route === 'external-interventions') {
    if ((sup || role === 'SOUS_TRAITANT') && e.status === 'PLANIFIEE')
      add('submit-report', 'Remettre le rapport', [f('report', 'x')]);
    if (sup && e.status === 'SOUMISE') {
      add('approve', 'Accepter la prestation');
      add('reject', 'Demander une correction', [f('reason', 'x')]);
    }
  }
  if (route === 'material-requests' && sup && e.status === 'SOUMISE') {
    add('approve', 'Approuver le besoin');
    add('reject', 'Refuser le besoin', [f('reason', 'x')]);
  }
  if (route === 'notifications' && !e.readAt) add('read', 'Marquer comme lu');
  return list;
}
