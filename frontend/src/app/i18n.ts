import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
void i18n.use(initReactI18next).init({
  lng: 'fr',
  fallbackLng: 'fr',
  resources: {
    fr: {
      translation: {
        dashboard: 'Dashboard',
        search: 'Rechercher',
        save: 'Enregistrer',
        cancel: 'Annuler',
        maintenance: 'Maintenance',
        offline: 'Hors connexion',
        online: 'Connecté',
        new: 'Ajouter',
        empty: 'Aucun élément pour le moment',
      },
    },
  },
  interpolation: { escapeValue: false },
});
export default i18n;
