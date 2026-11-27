import { useEffect, useRef, useState } from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useQuery } from '@tanstack/react-query';
import {
  Alert,
  AlertTitle,
  Button,
  Stack,
  TextField,
  MenuItem,
  Checkbox,
  FormControlLabel,
  FormHelperText,
  Typography,
  IconButton,
  Paper,
} from '@mui/material';
import DeleteOutline from '@mui/icons-material/DeleteOutline';
import {
  Field,
  Resource,
  editableFields,
  fieldOptions,
  referenceRoutes,
  shiftAssignmentLabel,
  labels,
  human,
} from './domain';
import { api, Entity, errorText } from '../services/api';
import { businessReference } from './businessReference';
export function StepsEditor({
  value,
  onChange,
}: {
  value: Entity[];
  onChange: (steps: Entity[]) => void;
}) {
  return (
    <Stack spacing={2}>
      <Typography variant="subtitle2">Étapes de la gamme opératoire</Typography>
      {value.map((s, i) => (
        <Paper key={s.key} variant="outlined" sx={{ p: 2 }}>
          <Stack spacing={1.5}>
            <Stack direction="row" gap={1}>
              <TextField
                label="Intitulé"
                value={s.label}
                onChange={(e) =>
                  onChange(value.map((a, n) => (n === i ? { ...a, label: e.target.value } : a)))
                }
                fullWidth
              />
              <IconButton
                aria-label="Retirer cette étape"
                onClick={() => onChange(value.filter((_, n) => n !== i))}
              >
                <DeleteOutline />
              </IconButton>
            </Stack>
            <TextField
              select
              label="Type de contrôle"
              value={s.type}
              onChange={(e) =>
                onChange(value.map((a, n) => (n === i ? { ...a, type: e.target.value } : a)))
              }
            >
              {['CHECKBOX', 'CONFORMITY', 'NUMBER', 'TEXT', 'MEASURE', 'PHOTO', 'ATTESTATION'].map(
                (t) => (
                  <MenuItem key={t} value={t}>
                    {human(t)}
                  </MenuItem>
                ),
              )}
            </TextField>
            {['NUMBER', 'MEASURE'].includes(s.type) && (
              <Stack direction="row" gap={1}>
                {['min', 'max', 'unit'].map((key) => (
                  <TextField
                    key={key}
                    label={{ min: 'Minimum', max: 'Maximum', unit: 'Unité' }[key]}
                    type={key === 'unit' ? 'text' : 'number'}
                    value={s[key] ?? ''}
                    onChange={(e) =>
                      onChange(
                        value.map((a, n) =>
                          n === i
                            ? {
                                ...a,
                                [key]:
                                  e.target.value === ''
                                    ? null
                                    : key === 'unit'
                                      ? e.target.value
                                      : Number(e.target.value),
                              }
                            : a,
                        ),
                      )
                    }
                  />
                ))}
              </Stack>
            )}
            <FormControlLabel
              control={
                <Checkbox
                  checked={s.required}
                  onChange={(e) =>
                    onChange(
                      value.map((a, n) => (n === i ? { ...a, required: e.target.checked } : a)),
                    )
                  }
                />
              }
              label="Étape obligatoire"
            />
          </Stack>
        </Paper>
      ))}
      <Button
        variant="outlined"
        onClick={() =>
          onChange([
            ...value,
            { key: crypto.randomUUID(), label: '', type: 'CHECKBOX', required: true },
          ])
        }
      >
        Ajouter une étape
      </Button>
    </Stack>
  );
}
function ReferenceField({
  field,
  value,
  onChange,
  resource,
}: {
  field: Field;
  value: any;
  onChange: (value: any) => void;
  resource?: Resource;
}) {
  let route = referenceRoutes[field.name];
  if (field.name === 'parentId' && resource?.route === 'assets') route = 'assets';
  const { data } = useQuery({
    queryKey: ['lookup', route],
    queryFn: async () => (await api.get(`/${route}`, { params: { size: 100 } })).data,
    enabled: !!route,
  });
  return (
    <TextField
      select
      label={labels[field.name] || field.name}
      value={value || ''}
      onChange={(e) => onChange(e.target.value)}
      fullWidth
      required={field.required}
      helperText={!data ? 'Chargement ou référence non accessible' : ''}
    >
      <MenuItem value="">Aucun</MenuItem>
      {(data?.items || [])
        .filter((item: Entity) => field.name !== 'checklistId' || item.published)
        .map((item: Entity) => (
          <MenuItem value={item.id} key={item.id}>
            {field.name === 'replacementForId'
              ? shiftAssignmentLabel(item)
              : route === 'users'
                ? item.name
                : businessReference(item)}
            {route !== 'users' && field.name !== 'replacementForId' && (item.name || item.title)
              ? ` · ${item.name || item.title}`
              : ''}
          </MenuItem>
        ))}
    </TextField>
  );
}
export default function ResourceForm({
  resource,
  fields: customFields,
  initial = {},
  onSave,
  onCancel,
  submitLabel = 'Enregistrer',
}: {
  resource?: Resource;
  fields?: Field[];
  initial?: Entity;
  onSave: (values: Entity) => Promise<void>;
  onCancel: () => void;
  submitLabel?: string;
}) {
  const fields = customFields || editableFields(resource!);
  const [error, setError] = useState('');
  const errorRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    if (!error) return;
    errorRef.current?.scrollIntoView?.({ block: 'nearest', behavior: 'smooth' });
    errorRef.current?.focus({ preventScroll: true });
  }, [error]);
  const schema = z.record(z.string(), z.any()).superRefine((values, ctx) => {
    for (const f of fields) {
      if (
        f.name === 'steps' &&
        (!Array.isArray(values.steps) || values.steps.length < 1 || values.steps.length > 100)
      )
        ctx.addIssue({
          code: 'custom',
          path: [f.name],
          message: 'Ajoutez entre 1 et 100 étapes à la gamme.',
        });
      if (
        f.required &&
        (values[f.name] === undefined || values[f.name] === null || values[f.name] === '')
      )
        ctx.addIssue({ code: 'custom', path: [f.name], message: 'Ce champ est obligatoire.' });
    }
  });
  const defaults: Entity = {};
  for (const f of fields) {
    let v = initial[f.name];
    if (v === undefined)
      v =
        f.type === 'b'
          ? f.name === 'active'
          : f.name === 'zoneId'
            ? 'Africa/Tunis'
            : f.name === 'intervalCount'
              ? 1
              : f.name === 'priority'
                ? 'P2'
                : f.name === 'criticality'
                  ? 'C2'
                  : f.name === 'frequency'
                    ? 'MONTHLY'
                    : f.name === 'mode'
                      ? 'FIXE'
                      : f.name === 'minimum' || f.name === 'unitCost'
                        ? 0
                        : f.name === 'steps'
                          ? []
                          : '';
    if (f.type === 't' && v)
      v = new Date(new Date(v).getTime() - new Date().getTimezoneOffset() * 60000)
        .toISOString()
        .slice(0, 16);
    if (f.type === 'j' && f.name !== 'steps' && typeof v !== 'string') v = JSON.stringify(v ?? []);
    defaults[f.name] = v;
  }
  const {
    control,
    handleSubmit,
    formState: { isSubmitting, errors },
  } = useForm<Entity>({ defaultValues: defaults, resolver: zodResolver(schema) });
  async function submit(values: Entity) {
    setError('');
    try {
      const body: Entity = {};
      for (const f of fields) {
        let v = values[f.name];
        if (v === '' || v === undefined) {
          if (!f.required) continue;
        } else if (f.type === 't') v = new Date(v).toISOString();
        else if (['i', 'l', 'n'].includes(f.type)) v = Number(v);
        else if (f.type === 'j' && typeof v === 'string') v = JSON.parse(v);
        body[f.name] = v;
      }
      await onSave(body);
    } catch (e) {
      setError(errorText(e));
    }
  }
  return (
    <form
      onSubmit={handleSubmit(submit, () =>
        setError('Vérifiez les champs signalés avant d’enregistrer.'),
      )}
    >
      <Stack spacing={2.5}>
        {error && (
          <Alert severity="error" ref={errorRef} tabIndex={-1} aria-live="assertive">
            <AlertTitle>Enregistrement impossible</AlertTitle>
            {error}
          </Alert>
        )}
        {fields.map((f) => (
          <Controller
            name={f.name}
            control={control}
            key={f.name}
            render={({ field }) =>
              f.name === 'steps' ? (
                <Stack spacing={1}>
                  <StepsEditor
                    value={Array.isArray(field.value) ? field.value : []}
                    onChange={field.onChange}
                  />
                  {errors[f.name] && (
                    <FormHelperText error>{errors[f.name]?.message?.toString()}</FormHelperText>
                  )}
                </Stack>
              ) : f.type === 'b' ? (
                <FormControlLabel
                  label={labels[f.name] || f.name}
                  control={<Checkbox checked={!!field.value} onChange={field.onChange} />}
                />
              ) : referenceRoutes[f.name] ? (
                <ReferenceField
                  field={f}
                  value={field.value}
                  onChange={field.onChange}
                  resource={resource}
                />
              ) : (
                <TextField
                  {...field}
                  value={field.value ?? ''}
                  label={labels[f.name] || f.name}
                  required={f.required}
                  error={!!errors[f.name]}
                  helperText={errors[f.name]?.message?.toString()}
                  fullWidth
                  select={!!fieldOptions(f, resource)}
                  multiline={f.type === 'x' || f.type === 'j'}
                  minRows={f.type === 'x' || f.type === 'j' ? 3 : undefined}
                  type={
                    f.name.toLowerCase().includes('password')
                      ? 'password'
                      : f.type === 't' || f.name === 'anchorLocal'
                        ? 'datetime-local'
                        : f.type === 'd'
                          ? 'date'
                          : ['n', 'i', 'l'].includes(f.type)
                            ? 'number'
                            : 'text'
                  }
                  slotProps={{
                    inputLabel: { shrink: true },
                    htmlInput: {
                      step: f.type === 'n' ? '0.001' : undefined,
                      maxLength: f.type === 'x' ? 10000 : 255,
                    },
                  }}
                >
                  {fieldOptions(f, resource)?.map((o) => (
                    <MenuItem key={o} value={o}>
                      {human(o)}
                    </MenuItem>
                  ))}
                </TextField>
              )
            }
          />
        ))}
        <Stack direction="row" justifyContent="flex-end" gap={1}>
          <Button onClick={onCancel}>Annuler</Button>
          <Button type="submit" variant="contained" disabled={isSubmitting}>
            {isSubmitting ? 'Enregistrement…' : submitLabel}
          </Button>
        </Stack>
      </Stack>
    </form>
  );
}
