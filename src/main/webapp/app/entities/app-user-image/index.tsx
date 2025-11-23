import React from 'react';
import { Route } from 'react-router';

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';

import AppUserImage from './app-user-image';
import AppUserImageDetail from './app-user-image-detail';
import AppUserImageUpdate from './app-user-image-update';
import AppUserImageDeleteDialog from './app-user-image-delete-dialog';

const AppUserImageRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route index element={<AppUserImage />} />
    <Route path="new" element={<AppUserImageUpdate />} />
    <Route path=":id">
      <Route index element={<AppUserImageDetail />} />
      <Route path="edit" element={<AppUserImageUpdate />} />
      <Route path="delete" element={<AppUserImageDeleteDialog />} />
    </Route>
  </ErrorBoundaryRoutes>
);

export default AppUserImageRoutes;
