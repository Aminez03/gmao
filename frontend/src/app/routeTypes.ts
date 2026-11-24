import type { ComponentType, LazyExoticComponent } from 'react';

export interface ModuleRoute {
  resource: string;
  Component: LazyExoticComponent<ComponentType>;
}
