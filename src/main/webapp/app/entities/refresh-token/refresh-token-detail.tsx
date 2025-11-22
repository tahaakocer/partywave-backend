import React, { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Button, Col, Row } from 'reactstrap';
import { TextFormat, Translate } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { APP_DATE_FORMAT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntity } from './refresh-token.reducer';

export const RefreshTokenDetail = () => {
  const dispatch = useAppDispatch();

  const { id } = useParams<'id'>();

  useEffect(() => {
    dispatch(getEntity(id));
  }, []);

  const refreshTokenEntity = useAppSelector(state => state.refreshToken.entity);
  return (
    <Row>
      <Col md="8">
        <h2 data-cy="refreshTokenDetailsHeading">
          <Translate contentKey="partywaveApp.refreshToken.detail.title">RefreshToken</Translate>
        </h2>
        <dl className="jh-entity-details">
          <dt>
            <span id="id">
              <Translate contentKey="global.field.id">ID</Translate>
            </span>
          </dt>
          <dd>{refreshTokenEntity.id}</dd>
          <dt>
            <span id="tokenHash">
              <Translate contentKey="partywaveApp.refreshToken.tokenHash">Token Hash</Translate>
            </span>
          </dt>
          <dd>{refreshTokenEntity.tokenHash}</dd>
          <dt>
            <span id="expiresAt">
              <Translate contentKey="partywaveApp.refreshToken.expiresAt">Expires At</Translate>
            </span>
          </dt>
          <dd>
            {refreshTokenEntity.expiresAt ? <TextFormat value={refreshTokenEntity.expiresAt} type="date" format={APP_DATE_FORMAT} /> : null}
          </dd>
          <dt>
            <span id="createdAt">
              <Translate contentKey="partywaveApp.refreshToken.createdAt">Created At</Translate>
            </span>
          </dt>
          <dd>
            {refreshTokenEntity.createdAt ? <TextFormat value={refreshTokenEntity.createdAt} type="date" format={APP_DATE_FORMAT} /> : null}
          </dd>
          <dt>
            <span id="revokedAt">
              <Translate contentKey="partywaveApp.refreshToken.revokedAt">Revoked At</Translate>
            </span>
          </dt>
          <dd>
            {refreshTokenEntity.revokedAt ? <TextFormat value={refreshTokenEntity.revokedAt} type="date" format={APP_DATE_FORMAT} /> : null}
          </dd>
          <dt>
            <span id="deviceInfo">
              <Translate contentKey="partywaveApp.refreshToken.deviceInfo">Device Info</Translate>
            </span>
          </dt>
          <dd>{refreshTokenEntity.deviceInfo}</dd>
          <dt>
            <span id="ipAddress">
              <Translate contentKey="partywaveApp.refreshToken.ipAddress">Ip Address</Translate>
            </span>
          </dt>
          <dd>{refreshTokenEntity.ipAddress}</dd>
          <dt>
            <Translate contentKey="partywaveApp.refreshToken.appUser">App User</Translate>
          </dt>
          <dd>{refreshTokenEntity.appUser ? refreshTokenEntity.appUser.displayName : ''}</dd>
        </dl>
        <Button tag={Link} to="/refresh-token" replace color="info" data-cy="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.back">Back</Translate>
          </span>
        </Button>
        &nbsp;
        <Button tag={Link} to={`/refresh-token/${refreshTokenEntity.id}/edit`} replace color="primary">
          <FontAwesomeIcon icon="pencil-alt" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.edit">Edit</Translate>
          </span>
        </Button>
      </Col>
    </Row>
  );
};

export default RefreshTokenDetail;
