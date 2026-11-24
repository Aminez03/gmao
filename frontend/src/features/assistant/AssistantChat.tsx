import AutoAwesomeOutlined from '@mui/icons-material/AutoAwesomeOutlined';
import CloseOutlined from '@mui/icons-material/CloseOutlined';
import SendOutlined from '@mui/icons-material/SendOutlined';
import {
  Box,
  Button,
  Chip,
  Dialog,
  DialogContent,
  DialogTitle,
  Divider,
  Fab,
  IconButton,
  Stack,
  TextField,
  Typography,
} from '@mui/material';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import { api, errorText } from '../../services/api';

type Reference = { label: string; route: string; id: string };
type Message = {
  role: 'assistant' | 'user';
  content: string;
  suggestions?: string[];
  references?: Reference[];
};

const welcome: Message = {
  role: 'assistant',
  content:
    'Bonjour. Je peux vous aider à suivre les OT, les pannes et les stocks de votre périmètre.',
  suggestions: [
    'Quels OT sont urgents ?',
    'Quels articles sont sous le stock minimum ?',
    'Montre les pannes ouvertes.',
  ],
};

export default function AssistantChat({ variant = 'fab' }: { variant?: 'fab' | 'header' }) {
  const [open, setOpen] = useState(false);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const [messages, setMessages] = useState<Message[]>([welcome]);

  async function send(question = input) {
    const message = question.trim();
    if (!message || loading) return;
    const history = messages.slice(-12).map(({ role, content }) => ({ role, content }));
    setMessages((items) => [...items, { role: 'user', content: message }]);
    setInput('');
    setLoading(true);
    try {
      const { data } = await api.post('/assistant/chat', { message, history }, { timeout: 60000 });
      setMessages((items) => [
        ...items,
        {
          role: 'assistant',
          content: data.answer,
          references: data.references || [],
        },
      ]);
    } catch (error) {
      setMessages((items) => [...items, { role: 'assistant', content: errorText(error) }]);
    } finally {
      setLoading(false);
    }
  }

  return (
    <>
      {variant === 'header' ? (
        <IconButton
          aria-label="Ouvrir l'assistant GMAO"
          color="primary"
          onClick={() => setOpen(true)}
        >
          <AutoAwesomeOutlined />
        </IconButton>
      ) : (
        <Fab
          color="primary"
          aria-label="Ouvrir l'assistant GMAO"
          onClick={() => setOpen(true)}
          sx={{
            position: 'fixed',
            right: { xs: 20, md: 32 },
            bottom: { xs: 20, md: 32 },
            zIndex: 1200,
          }}
        >
          <AutoAwesomeOutlined />
        </Fab>
      )}
      <Dialog
        open={open}
        onClose={() => setOpen(false)}
        fullWidth
        maxWidth="sm"
        aria-labelledby="assistant-title"
      >
        <DialogTitle id="assistant-title" sx={{ pr: 7 }}>
          <Stack direction="row" alignItems="center" gap={1}>
            <AutoAwesomeOutlined color="primary" />
            <Box>
              <Typography fontWeight={700}>Assistant GMAO</Typography>
              <Typography variant="caption" color="text.secondary">
                Réponses limitées à vos données autorisées
              </Typography>
            </Box>
          </Stack>
          <IconButton
            aria-label="Fermer"
            onClick={() => setOpen(false)}
            sx={{ position: 'absolute', right: 12, top: 12 }}
          >
            <CloseOutlined />
          </IconButton>
        </DialogTitle>
        <Divider />
        <DialogContent sx={{ p: 0 }}>
          <Stack spacing={1.5} sx={{ height: 430, overflowY: 'auto', p: 2.5 }}>
            {messages.map((message, index) => (
              <Box
                key={`${message.role}-${index}`}
                sx={{
                  alignSelf: message.role === 'user' ? 'flex-end' : 'flex-start',
                  maxWidth: '88%',
                }}
              >
                <Box
                  sx={{
                    bgcolor: message.role === 'user' ? 'primary.main' : 'action.hover',
                    color: message.role === 'user' ? 'primary.contrastText' : 'text.primary',
                    borderRadius: 2,
                    px: 1.5,
                    py: 1,
                  }}
                >
                  <Typography variant="body2" sx={{ whiteSpace: 'pre-wrap' }}>
                    {message.content}
                  </Typography>
                </Box>
                {message.references?.map((reference) => (
                  <Button
                    key={`${reference.route}:${reference.id}`}
                    component={Link}
                    to={`/m/${reference.route}?id=${encodeURIComponent(reference.id)}`}
                    size="small"
                    sx={{ mt: 0.5, textTransform: 'none' }}
                    onClick={() => setOpen(false)}
                  >
                    Voir {reference.label}
                  </Button>
                ))}
                {message.suggestions && message.role === 'assistant' && (
                  <Stack direction="row" flexWrap="wrap" gap={0.75} sx={{ mt: 0.75 }}>
                    {message.suggestions.map((suggestion) => (
                      <Chip
                        key={suggestion}
                        label={suggestion}
                        size="small"
                        variant="outlined"
                        disabled={loading}
                        onClick={() => void send(suggestion)}
                      />
                    ))}
                  </Stack>
                )}
              </Box>
            ))}
            {loading && (
              <Typography variant="caption" color="text.secondary">
                Analyse en cours…
              </Typography>
            )}
          </Stack>
          <Divider />
          <Stack direction="row" gap={1} p={2}>
            <TextField
              fullWidth
              size="small"
              placeholder="Ex. Quels OT sont urgents ?"
              value={input}
              onChange={(event) => setInput(event.target.value)}
              onKeyDown={(event) => {
                if (event.key === 'Enter' && !event.shiftKey) {
                  event.preventDefault();
                  void send();
                }
              }}
            />
            <IconButton
              color="primary"
              aria-label="Envoyer"
              disabled={!input.trim() || loading}
              onClick={() => void send()}
            >
              <SendOutlined />
            </IconButton>
          </Stack>
        </DialogContent>
      </Dialog>
    </>
  );
}
