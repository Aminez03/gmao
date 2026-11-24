import { useMemo, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import {
  Alert,
  Box,
  Button,
  Chip,
  Dialog,
  DialogContent,
  DialogTitle,
  Divider,
  IconButton,
  LinearProgress,
  Paper,
  Stack,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import AddTaskOutlined from '@mui/icons-material/AddTaskOutlined';
import CalendarMonthOutlined from '@mui/icons-material/CalendarMonthOutlined';
import ChevronLeftOutlined from '@mui/icons-material/ChevronLeftOutlined';
import ChevronRightOutlined from '@mui/icons-material/ChevronRightOutlined';
import GroupsOutlined from '@mui/icons-material/GroupsOutlined';
import PersonOffOutlined from '@mui/icons-material/PersonOffOutlined';
import TodayOutlined from '@mui/icons-material/TodayOutlined';
import { api, Entity, errorText, headers } from '../../../services/api';
import ResourceForm from '../../../components/ResourceForm';
import { Status } from '../../../components/EntityDisplay';
import { useAuth } from '../../../app/Auth';

const tunisDate = new Intl.DateTimeFormat('fr-TN', {
  weekday: 'long',
  day: 'numeric',
  month: 'long',
});
const shortDate = new Intl.DateTimeFormat('fr-TN', { weekday: 'short', day: 'numeric' });

function dateAtNoon(value: string) {
  return new Date(`${value}T12:00:00`);
}

function addDays(value: string, count: number) {
  const day = dateAtNoon(value);
  day.setDate(day.getDate() + count);
  return day.toISOString().slice(0, 10);
}

function weekLabel(start: string) {
  return `${tunisDate.format(dateAtNoon(start))} — ${tunisDate.format(dateAtNoon(addDays(start, 6)))}`;
}

export default function PlanningPage() {
  const { user } = useAuth();
  const today = new Date().toISOString().slice(0, 10);
  const [date, setDate] = useState(today);
  const [generate, setGenerate] = useState(false);
  const { data, error, isLoading, refetch } = useQuery({
    queryKey: ['planning', date, user?.id],
    queryFn: async () => (await api.get(`/planning?from=${date}`)).data as Entity[],
    enabled: !!user,
  });
  const days = useMemo(() => Array.from({ length: 7 }, (_, index) => addDays(date, index)), [date]);
  const shifts = data || [];
  const summary = useMemo(() => {
    const published = shifts.filter((shift) => shift.status === 'PUBLIE').length;
    const staffed = shifts.filter((shift) => shift.technicians?.length).length;
    return { shifts: shifts.length, published, staffed, unstaffed: shifts.length - staffed };
  }, [shifts]);
  const canGenerate = ['ADMIN', 'RESPONSABLE_TECHNIQUE'].includes(user?.role || '');
  const isCurrentWeek = date === today;

  return (
    <Stack spacing={3}>
      <Stack direction={{ xs: 'column', lg: 'row' }} justifyContent="space-between" gap={2.5}>
        <Box>
          <Stack direction="row" alignItems="center" gap={1} mb={0.75}>
            <CalendarMonthOutlined color="primary" fontSize="small" />
            <Typography variant="overline" color="text.secondary" fontWeight={700}>
              Organisation 3 × 8
            </Typography>
          </Stack>
          <Typography variant="h4">Planning des équipes</Typography>
          <Typography color="text.secondary" mt={0.75}>
            Visualisez les postes, leur publication et la couverture des équipes sur la semaine.
          </Typography>
        </Box>
        <Stack direction={{ xs: 'column', sm: 'row' }} gap={1} alignSelf={{ lg: 'center' }}>
          <Button component={Link} to="/m/shift-assignments" startIcon={<GroupsOutlined />}>
            Affecter les techniciens
          </Button>
          {canGenerate && (
            <Button
              variant="contained"
              onClick={() => setGenerate(true)}
              startIcon={<AddTaskOutlined />}
            >
              Générer les postes
            </Button>
          )}
        </Stack>
      </Stack>

      <Paper variant="outlined" sx={{ overflow: 'hidden' }}>
        <Stack
          direction={{ xs: 'column', md: 'row' }}
          alignItems={{ md: 'center' }}
          justifyContent="space-between"
          gap={2}
          p={2}
          sx={{ bgcolor: 'action.hover' }}
        >
          <Stack direction="row" alignItems="center" gap={0.5}>
            <Tooltip title="Semaine précédente">
              <IconButton
                aria-label="Semaine précédente"
                onClick={() => setDate(addDays(date, -7))}
              >
                <ChevronLeftOutlined />
              </IconButton>
            </Tooltip>
            <Box minWidth={{ sm: 280 }} textAlign="center">
              <Typography fontWeight={700} textTransform="capitalize">
                {weekLabel(date)}
              </Typography>
              <Typography variant="caption" color="text.secondary">
                7 jours · {summary.shifts} poste{summary.shifts > 1 ? 's' : ''} planifié
                {summary.shifts > 1 ? 's' : ''}
              </Typography>
            </Box>
            <Tooltip title="Semaine suivante">
              <IconButton aria-label="Semaine suivante" onClick={() => setDate(addDays(date, 7))}>
                <ChevronRightOutlined />
              </IconButton>
            </Tooltip>
          </Stack>
          <Stack direction={{ xs: 'column', sm: 'row' }} gap={1} alignItems={{ sm: 'center' }}>
            <TextField
              label="Début de semaine"
              type="date"
              value={date}
              onChange={(event) => setDate(event.target.value)}
              slotProps={{ inputLabel: { shrink: true } }}
              size="small"
            />
            <Button
              variant={isCurrentWeek ? 'text' : 'outlined'}
              onClick={() => setDate(today)}
              startIcon={<TodayOutlined />}
              disabled={isCurrentWeek}
            >
              Aujourd’hui
            </Button>
          </Stack>
        </Stack>
        <Divider />
        <Box className="planning-summary">
          {[
            ['Postes planifiés', summary.shifts, '#087f8c'],
            ['Postes publiés', summary.published, '#36866c'],
            ['Postes couverts', summary.staffed, '#366a9a'],
            ['À affecter', summary.unstaffed, summary.unstaffed ? '#c47712' : '#36866c'],
          ].map(([label, value, color]) => (
            <Box key={String(label)} className="planning-stat">
              <Typography variant="body2" color="text.secondary">
                {label}
              </Typography>
              <Typography variant="h5" sx={{ color, mt: 0.25 }}>
                {value}
              </Typography>
            </Box>
          ))}
        </Box>
      </Paper>

      <Alert severity="info" variant="outlined">
        Les postes générés restent en brouillon jusqu’à la vérification de leur couverture et leur
        publication. Vérifiez les repos et les remplacements avant de publier.
      </Alert>
      {error && <Alert severity="error">{errorText(error)}</Alert>}
      {isLoading && <LinearProgress />}

      <Box className="planning-grid" aria-label="Planning hebdomadaire">
        {days.map((day) => {
          const dayShifts = shifts.filter((shift) => shift.localDate === day);
          const isToday = day === today;
          return (
            <Paper
              key={day}
              variant="outlined"
              className="planning-day"
              sx={{ borderColor: isToday ? 'primary.main' : undefined }}
            >
              <Box className="planning-day-heading">
                <Typography fontWeight={700} textTransform="capitalize">
                  {shortDate.format(dateAtNoon(day))}
                </Typography>
                {isToday && <Chip label="Aujourd’hui" color="primary" size="small" />}
              </Box>
              <Typography variant="caption" color="text.secondary" display="block" mb={1.5}>
                {dayShifts.length
                  ? `${dayShifts.length} poste${dayShifts.length > 1 ? 's' : ''}`
                  : 'Aucun poste'}
              </Typography>
              <Stack spacing={1.25}>
                {dayShifts.map((shift: Entity) => {
                  const technicians = shift.technicians || [];
                  return (
                    <Box key={shift.id} className="planning-shift-card">
                      <Stack
                        direction="row"
                        justifyContent="space-between"
                        gap={1}
                        alignItems="flex-start"
                      >
                        <Box minWidth={0}>
                          <Typography fontWeight={700} noWrap title={shift.name}>
                            {shift.name}
                          </Typography>
                          <Typography variant="caption" color="text.secondary" display="block">
                            {shift.teamName} · {shift.hours}
                          </Typography>
                        </Box>
                        <Status value={shift.status} />
                      </Stack>
                      <Divider sx={{ my: 1.25 }} />
                      {technicians.length ? (
                        <Stack direction="row" flexWrap="wrap" gap={0.5}>
                          {technicians.map((technician: string) => (
                            <Chip
                              key={technician}
                              label={technician}
                              size="small"
                              variant="outlined"
                            />
                          ))}
                        </Stack>
                      ) : (
                        <Stack direction="row" gap={0.75} alignItems="center" color="warning.main">
                          <PersonOffOutlined fontSize="small" />
                          <Typography variant="caption" fontWeight={600}>
                            Aucun technicien affecté
                          </Typography>
                        </Stack>
                      )}
                    </Box>
                  );
                })}
                {!dayShifts.length && !isLoading && (
                  <Box className="planning-empty-day">
                    <CalendarMonthOutlined fontSize="small" />
                    <Typography variant="body2">Aucun poste planifié</Typography>
                  </Box>
                )}
              </Stack>
            </Paper>
          );
        })}
      </Box>

      <Dialog open={generate} onClose={() => setGenerate(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Préparer les postes en rotation</DialogTitle>
        <DialogContent sx={{ pt: '16px !important' }}>
          <ResourceForm
            fields={[
              { name: 'startDate', type: 'd', required: true },
              { name: 'days', type: 'i', required: true },
              { name: 'cycleDays', type: 'i', required: true },
            ]}
            initial={{ startDate: date, days: 7, cycleDays: 7 }}
            onCancel={() => setGenerate(false)}
            onSave={async (values) => {
              await api.post('/planning/generate', values, { headers: headers() });
              setGenerate(false);
              await refetch();
            }}
          />
        </DialogContent>
      </Dialog>
    </Stack>
  );
}
