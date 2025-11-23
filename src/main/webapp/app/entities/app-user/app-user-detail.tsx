import React, { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Button, Col, Row } from 'reactstrap';
import { TextFormat, Translate } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { APP_DATE_FORMAT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntity } from './app-user.reducer';

export const AppUserDetail = () => {
  const dispatch = useAppDispatch();

  const { id } = useParams<'id'>();

  useEffect(() => {
    dispatch(getEntity(id));
  }, []);

  const appUserEntity = useAppSelector(state => state.appUser.entity);
  return (
    <Row>
      <Col md="8">
        <h2 data-cy="appUserDetailsHeading">
          <Translate contentKey="partywaveApp.appUser.detail.title">AppUser</Translate>
        </h2>
        <dl className="jh-entity-details">
          <dt>
            <span id="id">
              <Translate contentKey="partywaveApp.appUser.id">Id</Translate>
            </span>
          </dt>
          <dd>{appUserEntity.id}</dd>
          <dt>
            <span id="spotifyUserId">
              <Translate contentKey="partywaveApp.appUser.spotifyUserId">Spotify User Id</Translate>
            </span>
          </dt>
          <dd>{appUserEntity.spotifyUserId}</dd>
          <dt>
            <span id="displayName">
              <Translate contentKey="partywaveApp.appUser.displayName">Display Name</Translate>
            </span>
          </dt>
          <dd>{appUserEntity.displayName}</dd>
          <dt>
            <span id="email">
              <Translate contentKey="partywaveApp.appUser.email">Email</Translate>
            </span>
          </dt>
          <dd>{appUserEntity.email}</dd>
          <dt>
            <span id="country">
              <Translate contentKey="partywaveApp.appUser.country">Country</Translate>
            </span>
          </dt>
          <dd>{appUserEntity.country}</dd>
          <dt>
            <span id="href">
              <Translate contentKey="partywaveApp.appUser.href">Href</Translate>
            </span>
          </dt>
          <dd>{appUserEntity.href}</dd>
          <dt>
            <span id="url">
              <Translate contentKey="partywaveApp.appUser.url">Url</Translate>
            </span>
          </dt>
          <dd>{appUserEntity.url}</dd>
          <dt>
            <span id="type">
              <Translate contentKey="partywaveApp.appUser.type">Type</Translate>
            </span>
          </dt>
          <dd>{appUserEntity.type}</dd>
          <dt>
            <span id="ipAddress">
              <Translate contentKey="partywaveApp.appUser.ipAddress">Ip Address</Translate>
            </span>
          </dt>
          <dd>{appUserEntity.ipAddress}</dd>
          <dt>
            <span id="lastActiveAt">
              <Translate contentKey="partywaveApp.appUser.lastActiveAt">Last Active At</Translate>
            </span>
          </dt>
          <dd>
            {appUserEntity.lastActiveAt ? <TextFormat value={appUserEntity.lastActiveAt} type="date" format={APP_DATE_FORMAT} /> : null}
          </dd>
          <dt>
            <span id="status">
              <Translate contentKey="partywaveApp.appUser.status">Status</Translate>
            </span>
          </dt>
          <dd>{appUserEntity.status}</dd>
          <dt>
            <Translate contentKey="partywaveApp.appUser.stats">Stats</Translate>
          </dt>
          <dd>{appUserEntity.stats ? appUserEntity.stats.id : ''}</dd>
        </dl>
        <Button tag={Link} to="/app-user" replace color="info" data-cy="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.back">Back</Translate>
          </span>
        </Button>
        &nbsp;
        <Button tag={Link} to={`/app-user/${appUserEntity.id}/edit`} replace color="primary">
          <FontAwesomeIcon icon="pencil-alt" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.edit">Edit</Translate>
          </span>
        </Button>
      </Col>
    </Row>
  );
};

export default AppUserDetail;
