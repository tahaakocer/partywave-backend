import React, { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Button, Col, Row } from 'reactstrap';
import { TextFormat, Translate } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { APP_DATE_FORMAT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntity } from './user-token.reducer';

export const UserTokenDetail = () => {
  const dispatch = useAppDispatch();

  const { id } = useParams<'id'>();

  useEffect(() => {
    dispatch(getEntity(id));
  }, []);

  const userTokenEntity = useAppSelector(state => state.userToken.entity);
  return (
    <Row>
      <Col md="8">
        <h2 data-cy="userTokenDetailsHeading">
          <Translate contentKey="partywaveApp.userToken.detail.title">UserToken</Translate>
        </h2>
        <dl className="jh-entity-details">
          <dt>
            <span id="id">
              <Translate contentKey="global.field.id">ID</Translate>
            </span>
          </dt>
          <dd>{userTokenEntity.id}</dd>
          <dt>
            <span id="accessToken">
              <Translate contentKey="partywaveApp.userToken.accessToken">Access Token</Translate>
            </span>
          </dt>
          <dd>{userTokenEntity.accessToken}</dd>
          <dt>
            <span id="refreshToken">
              <Translate contentKey="partywaveApp.userToken.refreshToken">Refresh Token</Translate>
            </span>
          </dt>
          <dd>{userTokenEntity.refreshToken}</dd>
          <dt>
            <span id="tokenType">
              <Translate contentKey="partywaveApp.userToken.tokenType">Token Type</Translate>
            </span>
          </dt>
          <dd>{userTokenEntity.tokenType}</dd>
          <dt>
            <span id="expiresAt">
              <Translate contentKey="partywaveApp.userToken.expiresAt">Expires At</Translate>
            </span>
          </dt>
          <dd>
            {userTokenEntity.expiresAt ? <TextFormat value={userTokenEntity.expiresAt} type="date" format={APP_DATE_FORMAT} /> : null}
          </dd>
          <dt>
            <span id="scope">
              <Translate contentKey="partywaveApp.userToken.scope">Scope</Translate>
            </span>
          </dt>
          <dd>{userTokenEntity.scope}</dd>
          <dt>
            <Translate contentKey="partywaveApp.userToken.appUser">App User</Translate>
          </dt>
          <dd>{userTokenEntity.appUser ? userTokenEntity.appUser.displayName : ''}</dd>
        </dl>
        <Button tag={Link} to="/user-token" replace color="info" data-cy="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.back">Back</Translate>
          </span>
        </Button>
        &nbsp;
        <Button tag={Link} to={`/user-token/${userTokenEntity.id}/edit`} replace color="primary">
          <FontAwesomeIcon icon="pencil-alt" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.edit">Edit</Translate>
          </span>
        </Button>
      </Col>
    </Row>
  );
};

export default UserTokenDetail;
