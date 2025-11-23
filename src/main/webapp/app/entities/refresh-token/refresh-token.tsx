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

import { getEntities } from './refresh-token.reducer';

export const RefreshToken = () => {
  const dispatch = useAppDispatch();

  const pageLocation = useLocation();
  const navigate = useNavigate();

  const [sortState, setSortState] = useState(overrideSortStateWithQueryParams(getSortState(pageLocation, 'id'), pageLocation.search));

  const refreshTokenList = useAppSelector(state => state.refreshToken.entities);
  const loading = useAppSelector(state => state.refreshToken.loading);

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
      <h2 id="refresh-token-heading" data-cy="RefreshTokenHeading">
        <Translate contentKey="partywaveApp.refreshToken.home.title">Refresh Tokens</Translate>
        <div className="d-flex justify-content-end">
          <Button className="me-2" color="info" onClick={handleSyncList} disabled={loading}>
            <FontAwesomeIcon icon="sync" spin={loading} />{' '}
            <Translate contentKey="partywaveApp.refreshToken.home.refreshListLabel">Refresh List</Translate>
          </Button>
          <Link to="/refresh-token/new" className="btn btn-primary jh-create-entity" id="jh-create-entity" data-cy="entityCreateButton">
            <FontAwesomeIcon icon="plus" />
            &nbsp;
            <Translate contentKey="partywaveApp.refreshToken.home.createLabel">Create new Refresh Token</Translate>
          </Link>
        </div>
      </h2>
      <div className="table-responsive">
        {refreshTokenList && refreshTokenList.length > 0 ? (
          <Table responsive>
            <thead>
              <tr>
                <th className="hand" onClick={sort('id')}>
                  <Translate contentKey="partywaveApp.refreshToken.id">Id</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('id')} />
                </th>
                <th className="hand" onClick={sort('tokenHash')}>
                  <Translate contentKey="partywaveApp.refreshToken.tokenHash">Token Hash</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('tokenHash')} />
                </th>
                <th className="hand" onClick={sort('expiresAt')}>
                  <Translate contentKey="partywaveApp.refreshToken.expiresAt">Expires At</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('expiresAt')} />
                </th>
                <th className="hand" onClick={sort('createdAt')}>
                  <Translate contentKey="partywaveApp.refreshToken.createdAt">Created At</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('createdAt')} />
                </th>
                <th className="hand" onClick={sort('revokedAt')}>
                  <Translate contentKey="partywaveApp.refreshToken.revokedAt">Revoked At</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('revokedAt')} />
                </th>
                <th className="hand" onClick={sort('deviceInfo')}>
                  <Translate contentKey="partywaveApp.refreshToken.deviceInfo">Device Info</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('deviceInfo')} />
                </th>
                <th className="hand" onClick={sort('ipAddress')}>
                  <Translate contentKey="partywaveApp.refreshToken.ipAddress">Ip Address</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('ipAddress')} />
                </th>
                <th>
                  <Translate contentKey="partywaveApp.refreshToken.appUser">App User</Translate> <FontAwesomeIcon icon="sort" />
                </th>
                <th />
              </tr>
            </thead>
            <tbody>
              {refreshTokenList.map((refreshToken, i) => (
                <tr key={`entity-${i}`} data-cy="entityTable">
                  <td>
                    <Button tag={Link} to={`/refresh-token/${refreshToken.id}`} color="link" size="sm">
                      {refreshToken.id}
                    </Button>
                  </td>
                  <td>{refreshToken.tokenHash}</td>
                  <td>
                    {refreshToken.expiresAt ? <TextFormat type="date" value={refreshToken.expiresAt} format={APP_DATE_FORMAT} /> : null}
                  </td>
                  <td>
                    {refreshToken.createdAt ? <TextFormat type="date" value={refreshToken.createdAt} format={APP_DATE_FORMAT} /> : null}
                  </td>
                  <td>
                    {refreshToken.revokedAt ? <TextFormat type="date" value={refreshToken.revokedAt} format={APP_DATE_FORMAT} /> : null}
                  </td>
                  <td>{refreshToken.deviceInfo}</td>
                  <td>{refreshToken.ipAddress}</td>
                  <td>
                    {refreshToken.appUser ? (
                      <Link to={`/app-user/${refreshToken.appUser.id}`}>{refreshToken.appUser.displayName}</Link>
                    ) : (
                      ''
                    )}
                  </td>
                  <td className="text-end">
                    <div className="btn-group flex-btn-group-container">
                      <Button tag={Link} to={`/refresh-token/${refreshToken.id}`} color="info" size="sm" data-cy="entityDetailsButton">
                        <FontAwesomeIcon icon="eye" />{' '}
                        <span className="d-none d-md-inline">
                          <Translate contentKey="entity.action.view">View</Translate>
                        </span>
                      </Button>
                      <Button tag={Link} to={`/refresh-token/${refreshToken.id}/edit`} color="primary" size="sm" data-cy="entityEditButton">
                        <FontAwesomeIcon icon="pencil-alt" />{' '}
                        <span className="d-none d-md-inline">
                          <Translate contentKey="entity.action.edit">Edit</Translate>
                        </span>
                      </Button>
                      <Button
                        onClick={() => (window.location.href = `/refresh-token/${refreshToken.id}/delete`)}
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
              <Translate contentKey="partywaveApp.refreshToken.home.notFound">No Refresh Tokens found</Translate>
            </div>
          )
        )}
      </div>
    </div>
  );
};

export default RefreshToken;
