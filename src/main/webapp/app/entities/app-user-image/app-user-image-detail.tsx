import React, { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Button, Col, Row } from 'reactstrap';
import { Translate } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntity } from './app-user-image.reducer';

export const AppUserImageDetail = () => {
  const dispatch = useAppDispatch();

  const { id } = useParams<'id'>();

  useEffect(() => {
    dispatch(getEntity(id));
  }, []);

  const appUserImageEntity = useAppSelector(state => state.appUserImage.entity);
  return (
    <Row>
      <Col md="8">
        <h2 data-cy="appUserImageDetailsHeading">
          <Translate contentKey="partywaveApp.appUserImage.detail.title">AppUserImage</Translate>
        </h2>
        <dl className="jh-entity-details">
          <dt>
            <span id="id">
              <Translate contentKey="global.field.id">ID</Translate>
            </span>
          </dt>
          <dd>{appUserImageEntity.id}</dd>
          <dt>
            <span id="url">
              <Translate contentKey="partywaveApp.appUserImage.url">Url</Translate>
            </span>
          </dt>
          <dd>{appUserImageEntity.url}</dd>
          <dt>
            <span id="height">
              <Translate contentKey="partywaveApp.appUserImage.height">Height</Translate>
            </span>
          </dt>
          <dd>{appUserImageEntity.height}</dd>
          <dt>
            <span id="width">
              <Translate contentKey="partywaveApp.appUserImage.width">Width</Translate>
            </span>
          </dt>
          <dd>{appUserImageEntity.width}</dd>
          <dt>
            <Translate contentKey="partywaveApp.appUserImage.appUser">App User</Translate>
          </dt>
          <dd>{appUserImageEntity.appUser ? appUserImageEntity.appUser.displayName : ''}</dd>
        </dl>
        <Button tag={Link} to="/app-user-image" replace color="info" data-cy="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.back">Back</Translate>
          </span>
        </Button>
        &nbsp;
        <Button tag={Link} to={`/app-user-image/${appUserImageEntity.id}/edit`} replace color="primary">
          <FontAwesomeIcon icon="pencil-alt" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.edit">Edit</Translate>
          </span>
        </Button>
      </Col>
    </Row>
  );
};

export default AppUserImageDetail;
