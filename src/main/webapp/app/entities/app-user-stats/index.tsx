import React from 'react';
import { Route } from 'react-router';

import ErrorBoundaryRoutes from 'app/shared/error/error-boundary-routes';

import AppUserStats from './app-user-stats';
import AppUserStatsDetail from './app-user-stats-detail';
import AppUserStatsUpdate from './app-user-stats-update';
import AppUserStatsDeleteDialog from './app-user-stats-delete-dialog';

const AppUserStatsRoutes = () => (
  <ErrorBoundaryRoutes>
    <Route index element={<AppUserStats />} />
    <Route path="new" element={<AppUserStatsUpdate />} />
    <Route path=":id">
      <Route index element={<AppUserStatsDetail />} />
      <Route path="edit" element={<AppUserStatsUpdate />} />
      <Route path="delete" element={<AppUserStatsDeleteDialog />} />
    </Route>
  </ErrorBoundaryRoutes>
);

export default AppUserStatsRoutes;
