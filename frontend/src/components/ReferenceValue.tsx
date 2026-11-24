import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../app/Auth';
import { fetchCached } from '../services/offline';
import { Resource, referenceRoutes, shiftAssignmentLabel, userReferenceFields } from './domain';
import type { Entity } from '../services/api';
import { display } from './EntityDisplay';
export default function ReferenceValue({
  field,
  value,
  resource,
  entity,
}: {
  field: string;
  value: any;
  resource: Resource;
  entity?: Entity;
}) {
  const { user } = useAuth();
  const isUser = userReferenceFields.has(field);
  const target =
    field === 'parentId' && resource.route === 'assets' ? 'assets' : referenceRoutes[field];

  const { data } = useQuery({
    queryKey: ['reference', target, value, user?.id],
    queryFn: () => fetchCached(user!.id, `${target}/${value}`, `/${target}/${value}`),
    enabled: !!user && !!target && !!value && !isUser,
    retry: false,
  });

  if (isUser) {
    if (!value) return <>{display(value)}</>;
    return (
      <>
        {entity?.userNames?.[field] ||
          (user && value === user.id ? user.name : 'Utilisateur indisponible')}
      </>
    );
  }
  if (field === 'replacementForId') {
    if (!value) return <>{display(value)}</>;
    return <>{data ? shiftAssignmentLabel(data) : 'Affectation indisponible'}</>;
  }
  return <>{data?.number || data?.code || data?.name || data?.title || display(value)}</>;
}
