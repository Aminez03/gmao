import { useQuery } from '@tanstack/react-query';
import { useAuth } from '../app/Auth';
import { fetchCached } from '../services/offline';
import {
  Resource,
  resources,
  referenceRoutes,
  shiftAssignmentLabel,
  userReferenceFields,
} from './domain';
import { businessReference } from './businessReference';
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
  const linkedRoute =
    field === 'resourceId'
      ? entity?.resourceType
      : field === 'targetId'
        ? entity?.targetType
        : undefined;
  const target = linkedRoute
    ? linkedRoute === 'users'
      ? 'users'
      : resources.find((item) => item.route === linkedRoute)?.route
    : field === 'parentId' && resource.route === 'assets'
      ? 'assets'
      : referenceRoutes[field];

  const { data } = useQuery({
    queryKey: ['reference', target, target === 'users' ? null : value, user?.id],
    queryFn: () =>
      target === 'users'
        ? fetchCached(user!.id, 'users', '/users')
        : fetchCached(user!.id, `${target}/${value}`, `/${target}/${value}`),
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
  if (!value) return <>{display(value)}</>;
  const record = target === 'users' ? data?.items?.find((item: Entity) => item.id === value) : data;
  return (
    <>
      {record
        ? record.displayNumber ||
          record.number ||
          record.code ||
          record.name ||
          record.title ||
          businessReference(record)
        : 'Référence indisponible'}
    </>
  );
}
