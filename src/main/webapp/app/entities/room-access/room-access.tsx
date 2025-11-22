import React, { useEffect, useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { Button, Table } from 'reactstrap';
import { TextFormat, Translate, getSortState } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';
import { faSort, faSortDown, faSortUp } from '@fortawesome/free-solid-svg-icons';
import { APP_DATE_FORMAT } from 'app/config/constants';
import { ASC, DESC } from 'app/shared/util/pagination.constants';
import { overrideSortStateWithQueryParams } from 'app/shared/util/entity-utils';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntities } from './room-access.reducer';

export const RoomAccess = () => {
  const dispatch = useAppDispatch();

  const pageLocation = useLocation();
  const navigate = useNavigate();

  const [sortState, setSortState] = useState(overrideSortStateWithQueryParams(getSortState(pageLocation, 'id'), pageLocation.search));

  const roomAccessList = useAppSelector(state => state.roomAccess.entities);
  const loading = useAppSelector(state => state.roomAccess.loading);

  const getAllEntities = () => {
    dispatch(
      getEntities({
        sort: `${sortState.sort},${sortState.order}`,
      }),
    );
  };

  const sortEntities = () => {
    getAllEntities();
    const endURL = `?sort=${sortState.sort},${sortState.order}`;
    if (pageLocation.search !== endURL) {
      navigate(`${pageLocation.pathname}${endURL}`);
    }
  };

  useEffect(() => {
    sortEntities();
  }, [sortState.order, sortState.sort]);

  const sort = p => () => {
    setSortState({
      ...sortState,
      order: sortState.order === ASC ? DESC : ASC,
      sort: p,
    });
  };

  const handleSyncList = () => {
    sortEntities();
  };

  const getSortIconByFieldName = (fieldName: string) => {
    const sortFieldName = sortState.sort;
    const order = sortState.order;
    if (sortFieldName !== fieldName) {
      return faSort;
    }
    return order === ASC ? faSortUp : faSortDown;
  };

  return (
    <div>
      <h2 id="room-access-heading" data-cy="RoomAccessHeading">
        <Translate contentKey="partywaveApp.roomAccess.home.title">Room Accesses</Translate>
        <div className="d-flex justify-content-end">
          <Button className="me-2" color="info" onClick={handleSyncList} disabled={loading}>
            <FontAwesomeIcon icon="sync" spin={loading} />{' '}
            <Translate contentKey="partywaveApp.roomAccess.home.refreshListLabel">Refresh List</Translate>
          </Button>
          <Link to="/room-access/new" className="btn btn-primary jh-create-entity" id="jh-create-entity" data-cy="entityCreateButton">
            <FontAwesomeIcon icon="plus" />
            &nbsp;
            <Translate contentKey="partywaveApp.roomAccess.home.createLabel">Create new Room Access</Translate>
          </Link>
        </div>
      </h2>
      <div className="table-responsive">
        {roomAccessList && roomAccessList.length > 0 ? (
          <Table responsive>
            <thead>
              <tr>
                <th className="hand" onClick={sort('id')}>
                  <Translate contentKey="partywaveApp.roomAccess.id">ID</Translate> <FontAwesomeIcon icon={getSortIconByFieldName('id')} />
                </th>
                <th className="hand" onClick={sort('grantedAt')}>
                  <Translate contentKey="partywaveApp.roomAccess.grantedAt">Granted At</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('grantedAt')} />
                </th>
                <th>
                  <Translate contentKey="partywaveApp.roomAccess.room">Room</Translate> <FontAwesomeIcon icon="sort" />
                </th>
                <th>
                  <Translate contentKey="partywaveApp.roomAccess.appUser">App User</Translate> <FontAwesomeIcon icon="sort" />
                </th>
                <th>
                  <Translate contentKey="partywaveApp.roomAccess.grantedBy">Granted By</Translate> <FontAwesomeIcon icon="sort" />
                </th>
                <th />
              </tr>
            </thead>
            <tbody>
              {roomAccessList.map((roomAccess, i) => (
                <tr key={`entity-${i}`} data-cy="entityTable">
                  <td>
                    <Button tag={Link} to={`/room-access/${roomAccess.id}`} color="link" size="sm">
                      {roomAccess.id}
                    </Button>
                  </td>
                  <td>{roomAccess.grantedAt ? <TextFormat type="date" value={roomAccess.grantedAt} format={APP_DATE_FORMAT} /> : null}</td>
                  <td>{roomAccess.room ? <Link to={`/room/${roomAccess.room.id}`}>{roomAccess.room.name}</Link> : ''}</td>
                  <td>
                    {roomAccess.appUser ? <Link to={`/app-user/${roomAccess.appUser.id}`}>{roomAccess.appUser.displayName}</Link> : ''}
                  </td>
                  <td>
                    {roomAccess.grantedBy ? (
                      <Link to={`/app-user/${roomAccess.grantedBy.id}`}>{roomAccess.grantedBy.displayName}</Link>
                    ) : (
                      ''
                    )}
                  </td>
                  <td className="text-end">
                    <div className="btn-group flex-btn-group-container">
                      <Button tag={Link} to={`/room-access/${roomAccess.id}`} color="info" size="sm" data-cy="entityDetailsButton">
                        <FontAwesomeIcon icon="eye" />{' '}
                        <span className="d-none d-md-inline">
                          <Translate contentKey="entity.action.view">View</Translate>
                        </span>
                      </Button>
                      <Button tag={Link} to={`/room-access/${roomAccess.id}/edit`} color="primary" size="sm" data-cy="entityEditButton">
                        <FontAwesomeIcon icon="pencil-alt" />{' '}
                        <span className="d-none d-md-inline">
                          <Translate contentKey="entity.action.edit">Edit</Translate>
                        </span>
                      </Button>
                      <Button
                        onClick={() => (window.location.href = `/room-access/${roomAccess.id}/delete`)}
                        color="danger"
                        size="sm"
                        data-cy="entityDeleteButton"
                      >
                        <FontAwesomeIcon icon="trash" />{' '}
                        <span className="d-none d-md-inline">
                          <Translate contentKey="entity.action.delete">Delete</Translate>
                        </span>
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </Table>
        ) : (
          !loading && (
            <div className="alert alert-warning">
              <Translate contentKey="partywaveApp.roomAccess.home.notFound">No Room Accesses found</Translate>
            </div>
          )
        )}
      </div>
    </div>
  );
};

export default RoomAccess;
