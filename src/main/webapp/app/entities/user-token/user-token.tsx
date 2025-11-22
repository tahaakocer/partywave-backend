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

import { getEntities } from './user-token.reducer';

export const UserToken = () => {
  const dispatch = useAppDispatch();

  const pageLocation = useLocation();
  const navigate = useNavigate();

  const [sortState, setSortState] = useState(overrideSortStateWithQueryParams(getSortState(pageLocation, 'id'), pageLocation.search));

  const userTokenList = useAppSelector(state => state.userToken.entities);
  const loading = useAppSelector(state => state.userToken.loading);

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
      <h2 id="user-token-heading" data-cy="UserTokenHeading">
        <Translate contentKey="partywaveApp.userToken.home.title">User Tokens</Translate>
        <div className="d-flex justify-content-end">
          <Button className="me-2" color="info" onClick={handleSyncList} disabled={loading}>
            <FontAwesomeIcon icon="sync" spin={loading} />{' '}
            <Translate contentKey="partywaveApp.userToken.home.refreshListLabel">Refresh List</Translate>
          </Button>
          <Link to="/user-token/new" className="btn btn-primary jh-create-entity" id="jh-create-entity" data-cy="entityCreateButton">
            <FontAwesomeIcon icon="plus" />
            &nbsp;
            <Translate contentKey="partywaveApp.userToken.home.createLabel">Create new User Token</Translate>
          </Link>
        </div>
      </h2>
      <div className="table-responsive">
        {userTokenList && userTokenList.length > 0 ? (
          <Table responsive>
            <thead>
              <tr>
                <th className="hand" onClick={sort('id')}>
                  <Translate contentKey="partywaveApp.userToken.id">ID</Translate> <FontAwesomeIcon icon={getSortIconByFieldName('id')} />
                </th>
                <th className="hand" onClick={sort('accessToken')}>
                  <Translate contentKey="partywaveApp.userToken.accessToken">Access Token</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('accessToken')} />
                </th>
                <th className="hand" onClick={sort('refreshToken')}>
                  <Translate contentKey="partywaveApp.userToken.refreshToken">Refresh Token</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('refreshToken')} />
                </th>
                <th className="hand" onClick={sort('tokenType')}>
                  <Translate contentKey="partywaveApp.userToken.tokenType">Token Type</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('tokenType')} />
                </th>
                <th className="hand" onClick={sort('expiresAt')}>
                  <Translate contentKey="partywaveApp.userToken.expiresAt">Expires At</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('expiresAt')} />
                </th>
                <th className="hand" onClick={sort('scope')}>
                  <Translate contentKey="partywaveApp.userToken.scope">Scope</Translate>{' '}
                  <FontAwesomeIcon icon={getSortIconByFieldName('scope')} />
                </th>
                <th>
                  <Translate contentKey="partywaveApp.userToken.appUser">App User</Translate> <FontAwesomeIcon icon="sort" />
                </th>
                <th />
              </tr>
            </thead>
            <tbody>
              {userTokenList.map((userToken, i) => (
                <tr key={`entity-${i}`} data-cy="entityTable">
                  <td>
                    <Button tag={Link} to={`/user-token/${userToken.id}`} color="link" size="sm">
                      {userToken.id}
                    </Button>
                  </td>
                  <td>{userToken.accessToken}</td>
                  <td>{userToken.refreshToken}</td>
                  <td>{userToken.tokenType}</td>
                  <td>{userToken.expiresAt ? <TextFormat type="date" value={userToken.expiresAt} format={APP_DATE_FORMAT} /> : null}</td>
                  <td>{userToken.scope}</td>
                  <td>{userToken.appUser ? <Link to={`/app-user/${userToken.appUser.id}`}>{userToken.appUser.displayName}</Link> : ''}</td>
                  <td className="text-end">
                    <div className="btn-group flex-btn-group-container">
                      <Button tag={Link} to={`/user-token/${userToken.id}`} color="info" size="sm" data-cy="entityDetailsButton">
                        <FontAwesomeIcon icon="eye" />{' '}
                        <span className="d-none d-md-inline">
                          <Translate contentKey="entity.action.view">View</Translate>
                        </span>
                      </Button>
                      <Button tag={Link} to={`/user-token/${userToken.id}/edit`} color="primary" size="sm" data-cy="entityEditButton">
                        <FontAwesomeIcon icon="pencil-alt" />{' '}
                        <span className="d-none d-md-inline">
                          <Translate contentKey="entity.action.edit">Edit</Translate>
                        </span>
                      </Button>
                      <Button
                        onClick={() => (window.location.href = `/user-token/${userToken.id}/delete`)}
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
              <Translate contentKey="partywaveApp.userToken.home.notFound">No User Tokens found</Translate>
            </div>
          )
        )}
      </div>
    </div>
  );
};

export default UserToken;
