import { useEffect, useRef, useState } from 'react';
import { useLocation } from 'react-router-dom';
import { Alert, Box, Button, Chip, Paper, Stack, TextField, Typography } from '@mui/material';
import { useAuth } from '../../../app/Auth';
import { api, errorText } from '../../../services/api';

type TurnstileApi = {
  render: (
    container: HTMLElement,
    options: {
      sitekey: string;
      callback: (token: string) => void;
      'expired-callback': () => void;
      'error-callback': () => void;
      theme: 'auto';
    },
  ) => string;
  reset: (widgetId?: string) => void;
};

declare global {
  interface Window {
    turnstile?: TurnstileApi;
  }
}

const turnstileSiteKey = import.meta.env.VITE_TURNSTILE_SITE_KEY?.trim() ?? '';

export default function Login() {
  const { login } = useAuth();
  const path = useLocation().pathname;
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [forgot, setForgot] = useState(false);
  const [notice, setNotice] = useState('');
  const [turnstileToken, setTurnstileToken] = useState('');
  const [turnstileReady, setTurnstileReady] = useState(false);
  const turnstileRef = useRef<HTMLDivElement>(null);
  const widgetId = useRef<string | undefined>(undefined);
  const reset = path === '/reset-password';

  useEffect(() => {
    if (!turnstileSiteKey || reset || forgot || !turnstileRef.current) return;

    let disposed = false;
    const renderWidget = () => {
      if (disposed || !turnstileRef.current || !window.turnstile || widgetId.current) return;
      widgetId.current = window.turnstile.render(turnstileRef.current, {
        sitekey: turnstileSiteKey,
        theme: 'auto',
        callback: (token) => {
          setTurnstileToken(token);
          setTurnstileReady(true);
        },
        'expired-callback': () => {
          setTurnstileToken('');
          setTurnstileReady(false);
        },
        'error-callback': () => {
          setTurnstileToken('');
          setTurnstileReady(false);
          setError('La vérification Cloudflare a échoué. Réessayez.');
        },
      });
    };

    if (window.turnstile) {
      renderWidget();
    } else {
      let script = document.querySelector<HTMLScriptElement>('#cloudflare-turnstile-script');
      if (!script) {
        script = document.createElement('script');
        script.id = 'cloudflare-turnstile-script';
        script.src = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit';
        script.async = true;
        script.defer = true;
        document.head.appendChild(script);
      }
      script.addEventListener('load', renderWidget);
      const timer = window.setInterval(renderWidget, 100);
      return () => {
        disposed = true;
        window.clearInterval(timer);
        script?.removeEventListener('load', renderWidget);
        if (widgetId.current) window.turnstile?.reset(widgetId.current);
        widgetId.current = undefined;
      };
    }

    return () => {
      disposed = true;
      if (widgetId.current) window.turnstile?.reset(widgetId.current);
      widgetId.current = undefined;
    };
  }, [forgot, reset]);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    setError('');
    try {
      if (reset) {
        const token = new URLSearchParams(location.search).get('token');
        await api.post('/auth/reset-password', { token, password });
        setNotice('Mot de passe modifié. Vous pouvez vous connecter.');
      } else if (forgot) {
        await api.post('/auth/forgot-password', { email });
        setNotice(
          'Si votre compte est éligible et la messagerie configurée, un lien vous sera envoyé.',
        );
      } else {
        if (turnstileSiteKey && !turnstileToken) {
          setError('Veuillez terminer la vérification Cloudflare.');
          return;
        }
        await login(email, password, turnstileToken || undefined);
        setTurnstileToken('');
        setTurnstileReady(false);
      }
    } catch (ex) {
      setError(errorText(ex));
      if (!reset && !forgot && turnstileSiteKey) {
        setTurnstileToken('');
        setTurnstileReady(false);
        if (widgetId.current) window.turnstile?.reset(widgetId.current);
      }
    } finally {
      setBusy(false);
    }
  }
  return (
    <Box className="login-layout">
      <Box className="login-story">
        <Box sx={{ width: { xs: 240, sm: 300 }, maxWidth: '100%', mb: 2, borderRadius: 2, overflow: 'hidden', boxShadow: '0 10px 30px rgba(0, 0, 0, 0.12)' }}>
          <img
            src="/lmx-gmao-logo.jpeg"
            alt="LMX GMAO"
            style={{ display: 'block', width: '100%', height: 'auto' }}
          />
        </Box>
        <Typography
          variant="h3"
          sx={{ maxWidth: 550, mt: 2, fontSize: { xs: '2rem', lg: '3.2rem' } }}
        >
          La continuité technique,
          <br />à chaque instant.
        </Typography>
        <Typography sx={{ maxWidth: 430, color: '#c8dde9', mt: 3, lineHeight: 1.9 }}>
          Un espace partagé pour coordonner vos équipes, entretenir votre patrimoine et assurer la
          transmission entre les postes.
        </Typography>
        <Stack direction="row" gap={1.5} mt={5}>
          {['3 équipes', '24 h/24', '7 j/7'].map((v) => (
            <Chip key={v} label={v} sx={{ color: 'white', bgcolor: '#ffffff15' }} />
          ))}
        </Stack>
      </Box>
      <Box sx={{ display: 'grid', placeItems: 'center', p: 3 }}>
        <Paper
          elevation={0}
          sx={{ width: '100%', maxWidth: 420, p: { xs: 2, sm: 4 }, bgcolor: 'transparent' }}
        >
          <Typography variant="overline" color="primary">
            ESPACE SÉCURISÉ
          </Typography>
          <Typography variant="h4" mb={1}>
            {reset ? 'Nouveau mot de passe' : forgot ? 'Récupérer votre accès' : 'Bienvenue'}
          </Typography>
          <Typography color="text.secondary" mb={4}>
            {reset
              ? 'Choisissez un mot de passe de 12 caractères minimum.'
              : forgot
                ? 'Saisissez votre adresse professionnelle.'
                : 'Connectez-vous avec votre compte professionnel.'}
          </Typography>
          <form onSubmit={submit}>
            <Stack spacing={2.5}>
              {error && <Alert severity="error">{error}</Alert>}
              {notice && <Alert>{notice}</Alert>}
              {!reset && (
                <TextField
                  label="Adresse e-mail"
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  required
                  autoComplete="username"
                />
              )}
              {!forgot && (
                <TextField
                  label="Mot de passe"
                  type="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  autoComplete={reset ? 'new-password' : 'current-password'}
                  slotProps={{ htmlInput: { minLength: reset ? 12 : 1, maxLength: 72 } }}
                />
              )}
              {!reset && !forgot && turnstileSiteKey && (
                <Box sx={{ minHeight: 66, display: 'flex', justifyContent: 'center' }}>
                  <Box ref={turnstileRef} />
                </Box>
              )}
              <Button
                type="submit"
                variant="contained"
                size="large"
                disabled={
                  busy || (!reset && !forgot && Boolean(turnstileSiteKey) && !turnstileReady)
                }
              >
                {busy
                  ? 'Traitement…'
                  : reset
                    ? 'Modifier le mot de passe'
                    : forgot
                      ? 'Envoyer le lien'
                      : 'Se connecter'}
              </Button>
              {!reset && (
                <Button
                  onClick={() => {
                    setForgot(!forgot);
                    setError('');
                    setNotice('');
                  }}
                >
                  {forgot ? 'Revenir à la connexion' : 'Mot de passe oublié ?'}
                </Button>
              )}
            </Stack>
          </form>
          <Typography variant="caption" color="text.secondary" display="block" mt={4}>
            Accès réservé au personnel et aux partenaires habilités de la polyclinique.
          </Typography>
        </Paper>
      </Box>
    </Box>
  );
}
