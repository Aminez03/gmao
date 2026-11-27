import { Chip } from '@mui/material';
import { human } from './domain';
export function Status({ value }: { value: string }) {
  const color = [
    'VALIDEE',
    'DISPONIBLE',
    'EN_SERVICE',
    'CONTROLEE',
    'PUBLIE',
    'APPROUVEE',
  ].includes(value)
    ? 'success'
    : ['EN_ATTENTE', 'QUARANTAINE', 'SOUMISE', 'P1', 'INDISPONIBLE'].includes(value)
      ? 'warning'
      : value === 'ANNULEE' || value === 'HORS_SERVICE'
        ? 'default'
        : 'info';
  return <Chip size="small" color={color} variant="outlined" label={human(value)} />;
}

export function display(value: unknown) {
  if (value === null || value === undefined || value === '') return '—';
  if (typeof value === 'boolean') return value ? 'Oui' : 'Non';
  if (typeof value === 'object') return JSON.stringify(value);
  if (typeof value === 'string' && /^\d{4}-\d{2}-\d{2}T/.test(value))
    return new Date(value).toLocaleString('fr-TN');
  if (typeof value === 'string' && /^[0-9a-f]{8}-[0-9a-f-]{27}$/i.test(value))
    return 'Référence indisponible';
  return String(value);
}
