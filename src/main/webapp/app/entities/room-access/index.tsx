import React from 'react';
import { Route } from 'react-router';

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';

import RoomAccess from './room-access';
import RoomAccessDetail from './room-access-detail';
import RoomAccessUpdate from './room-access-update';
import RoomAccessDeleteDialog from './room-access-delete-dialog';

const RoomAccessRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route index element={<RoomAccess />} />
    <Route path="new" element={<RoomAccessUpdate />} />
    <Route path=":id">
      <Route index element={<RoomAccessDetail />} />
      <Route path="edit" element={<RoomAccessUpdate />} />
      <Route path="delete" element={<RoomAccessDeleteDialog />} />
    </Route>
  </ErrorBoundaryRoutes>
);

export default RoomAccessRoutes;
