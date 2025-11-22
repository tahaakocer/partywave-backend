import React from 'react';
import { Route } from 'react-router';

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';

import UserToken from './user-token';
import UserTokenDetail from './user-token-detail';
import UserTokenUpdate from './user-token-update';
import UserTokenDeleteDialog from './user-token-delete-dialog';

const UserTokenRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route index element={<UserToken />} />
    <Route path="new" element={<UserTokenUpdate />} />
    <Route path=":id">
      <Route index element={<UserTokenDetail />} />
      <Route path="edit" element={<UserTokenUpdate />} />
      <Route path="delete" element={<UserTokenDeleteDialog />} />
    </Route>
  </ErrorBoundaryRoutes>
);

export default UserTokenRoutes;
