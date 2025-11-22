import React, { useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Button, Col, Row } from 'reactstrap';
import { Translate, ValidatedField, ValidatedForm, translate } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { convertDateTimeFromServer, convertDateTimeToServer, displayDefaultDateTime } from 'app/shared/util/date-utils';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntities as getAppUsers } from 'app/entities/app-user/app-user.reducer';
import { createEntity, getEntity, reset, updateEntity } from './refresh-token.reducer';

export const RefreshTokenUpdate = () => {
  const dispatch = useAppDispatch();

  const navigate = useNavigate();

  const { id } = useParams<'id'>();
  const isNew = id === undefined;

  const appUsers = useAppSelector(state => state.appUser.entities);
  const refreshTokenEntity = useAppSelector(state => state.refreshToken.entity);
  const loading = useAppSelector(state => state.refreshToken.loading);
  const updating = useAppSelector(state => state.refreshToken.updating);
  const updateSuccess = useAppSelector(state => state.refreshToken.updateSuccess);

  const handleClose = () => {
    navigate('/refresh-token');
  };

  useEffect(() => {
    if (isNew) {
      dispatch(reset());
    } else {
      dispatch(getEntity(id));
    }

    dispatch(getAppUsers({}));
  }, []);

  useEffect(() => {
    if (updateSuccess) {
      handleClose();
    }
  }, [updateSuccess]);

  const saveEntity = values => {
    if (values.id !== undefined && typeof values.id !== 'number') {
      values.id = Number(values.id);
    }
    values.expiresAt = convertDateTimeToServer(values.expiresAt);
    values.createdAt = convertDateTimeToServer(values.createdAt);
    values.revokedAt = convertDateTimeToServer(values.revokedAt);

    const entity = {
      ...refreshTokenEntity,
      ...values,
      appUser: appUsers.find(it => it.id.toString() === values.appUser?.toString()),
    };

    if (isNew) {
      dispatch(createEntity(entity));
    } else {
      dispatch(updateEntity(entity));
    }
  };

  const defaultValues = () =>
    isNew
      ? {
          expiresAt: displayDefaultDateTime(),
          createdAt: displayDefaultDateTime(),
          revokedAt: displayDefaultDateTime(),
        }
      : {
          ...refreshTokenEntity,
          expiresAt: convertDateTimeFromServer(refreshTokenEntity.expiresAt),
          createdAt: convertDateTimeFromServer(refreshTokenEntity.createdAt),
          revokedAt: convertDateTimeFromServer(refreshTokenEntity.revokedAt),
          appUser: refreshTokenEntity?.appUser?.id,
        };

  return (
    <div>
      <Row className="justify-content-center">
        <Col md="8">
          <h2 id="partywaveApp.refreshToken.home.createOrEditLabel" data-cy="RefreshTokenCreateUpdateHeading">
            <Translate contentKey="partywaveApp.refreshToken.home.createOrEditLabel">Create or edit a RefreshToken</Translate>
          </h2>
        </Col>
      </Row>
      <Row className="justify-content-center">
        <Col md="8">
          {loading ? (
            <p>Loading...</p>
          ) : (
            <ValidatedForm defaultValues={defaultValues()} onSubmit={saveEntity}>
              {!isNew ? (
                <ValidatedField
                  name="id"
                  required
                  readOnly
                  id="refresh-token-id"
                  label={translate('global.field.id')}
                  validate={{ required: true }}
                />
              ) : null}
              <ValidatedField
                label={translate('partywaveApp.refreshToken.tokenHash')}
                id="refresh-token-tokenHash"
                name="tokenHash"
                data-cy="tokenHash"
                type="text"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                }}
              />
              <ValidatedField
                label={translate('partywaveApp.refreshToken.expiresAt')}
                id="refresh-token-expiresAt"
                name="expiresAt"
                data-cy="expiresAt"
                type="datetime-local"
                placeholder="YYYY-MM-DD HH:mm"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                }}
              />
              <ValidatedField
                label={translate('partywaveApp.refreshToken.createdAt')}
                id="refresh-token-createdAt"
                name="createdAt"
                data-cy="createdAt"
                type="datetime-local"
                placeholder="YYYY-MM-DD HH:mm"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                }}
              />
              <ValidatedField
                label={translate('partywaveApp.refreshToken.revokedAt')}
                id="refresh-token-revokedAt"
                name="revokedAt"
                data-cy="revokedAt"
                type="datetime-local"
                placeholder="YYYY-MM-DD HH:mm"
              />
              <ValidatedField
                label={translate('partywaveApp.refreshToken.deviceInfo')}
                id="refresh-token-deviceInfo"
                name="deviceInfo"
                data-cy="deviceInfo"
                type="text"
              />
              <ValidatedField
                label={translate('partywaveApp.refreshToken.ipAddress')}
                id="refresh-token-ipAddress"
                name="ipAddress"
                data-cy="ipAddress"
                type="text"
              />
              <ValidatedField
                id="refresh-token-appUser"
                name="appUser"
                data-cy="appUser"
                label={translate('partywaveApp.refreshToken.appUser')}
                type="select"
              >
                <option value="" key="0" />
                {appUsers
                  ? appUsers.map(otherEntity => (
                      <option value={otherEntity.id} key={otherEntity.id}>
                        {otherEntity.displayName}
                      </option>
                    ))
                  : null}
              </ValidatedField>
              <Button tag={Link} id="cancel-save" data-cy="entityCreateCancelButton" to="/refresh-token" replace color="info">
                <FontAwesomeIcon icon="arrow-left" />
                &nbsp;
                <span className="d-none d-md-inline">
                  <Translate contentKey="entity.action.back">Back</Translate>
                </span>
              </Button>
              &nbsp;
              <Button color="primary" id="save-entity" data-cy="entityCreateSaveButton" type="submit" disabled={updating}>
                <FontAwesomeIcon icon="save" />
                &nbsp;
                <Translate contentKey="entity.action.save">Save</Translate>
              </Button>
            </ValidatedForm>
          )}
        </Col>
      </Row>
    </div>
  );
};

export default RefreshTokenUpdate;
