import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App';
import { AuthProvider } from './app/Auth';
import './app/i18n';
import './style.css';
const client = new QueryClient({
  defaultOptions: { queries: { retry: 1, refetchOnWindowFocus: false, staleTime: 15000 } },
});
ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <BrowserRouter>
      <QueryClientProvider client={client}>
        <AuthProvider>
          <App />
        </AuthProvider>
      </QueryClientProvider>
    </BrowserRouter>
  </React.StrictMode>,
);
