import React from 'react';
import { Route } from 'react-router';

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';

import RefreshToken from './refresh-token';
import RefreshTokenDetail from './refresh-token-detail';
import RefreshTokenUpdate from './refresh-token-update';
import RefreshTokenDeleteDialog from './refresh-token-delete-dialog';

const RefreshTokenRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route index element={<RefreshToken />} />
    <Route path="new" element={<RefreshTokenUpdate />} />
    <Route path=":id">
      <Route index element={<RefreshTokenDetail />} />
      <Route path="edit" element={<RefreshTokenUpdate />} />
      <Route path="delete" element={<RefreshTokenDeleteDialog />} />
    </Route>
  </ErrorBoundaryRoutes>
);

export default RefreshTokenRoutes;
