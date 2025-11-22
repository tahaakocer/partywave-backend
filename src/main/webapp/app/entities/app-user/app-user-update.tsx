import React, { useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Button, Col, Row } from 'reactstrap';
import { Translate, ValidatedField, ValidatedForm, translate } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { convertDateTimeFromServer, convertDateTimeToServer, displayDefaultDateTime } from 'app/shared/util/date-utils';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntities as getAppUserStats } from 'app/entities/app-user-stats/app-user-stats.reducer';
import { AppUserStatus } from 'app/shared/model/enumerations/app-user-status.model';
import { createEntity, getEntity, reset, updateEntity } from './app-user.reducer';

export const AppUserUpdate = () => {
  const dispatch = useAppDispatch();

  const navigate = useNavigate();

  const { id } = useParams<'id'>();
  const isNew = id === undefined;

  const appUserStats = useAppSelector(state => state.appUserStats.entities);
  const appUserEntity = useAppSelector(state => state.appUser.entity);
  const loading = useAppSelector(state => state.appUser.loading);
  const updating = useAppSelector(state => state.appUser.updating);
  const updateSuccess = useAppSelector(state => state.appUser.updateSuccess);
  const appUserStatusValues = Object.keys(AppUserStatus);

  const handleClose = () => {
    navigate(`/app-user${location.search}`);
  };

  useEffect(() => {
    if (isNew) {
      dispatch(reset());
    } else {
      dispatch(getEntity(id));
    }

    dispatch(getAppUserStats({}));
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
    values.lastActiveAt = convertDateTimeToServer(values.lastActiveAt);

    const entity = {
      ...appUserEntity,
      ...values,
      appUserStats: appUserStats.find(it => it.id.toString() === values.appUserStats?.toString()),
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
          lastActiveAt: displayDefaultDateTime(),
        }
      : {
          status: 'ONLINE',
          ...appUserEntity,
          lastActiveAt: convertDateTimeFromServer(appUserEntity.lastActiveAt),
          appUserStats: appUserEntity?.appUserStats?.id,
        };

  return (
    <div>
      <Row className="justify-content-center">
        <Col md="8">
          <h2 id="partywaveApp.appUser.home.createOrEditLabel" data-cy="AppUserCreateUpdateHeading">
            <Translate contentKey="partywaveApp.appUser.home.createOrEditLabel">Create or edit a AppUser</Translate>
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
                  id="app-user-id"
                  label={translate('global.field.id')}
                  validate={{ required: true }}
                />
              ) : null}
              <ValidatedField
                label={translate('partywaveApp.appUser.spotifyUserId')}
                id="app-user-spotifyUserId"
                name="spotifyUserId"
                data-cy="spotifyUserId"
                type="text"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                }}
              />
              <ValidatedField
                label={translate('partywaveApp.appUser.displayName')}
                id="app-user-displayName"
                name="displayName"
                data-cy="displayName"
                type="text"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                }}
              />
              <ValidatedField
                label={translate('partywaveApp.appUser.email')}
                id="app-user-email"
                name="email"
                data-cy="email"
                type="text"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                }}
              />
              <ValidatedField
                label={translate('partywaveApp.appUser.country')}
                id="app-user-country"
                name="country"
                data-cy="country"
                type="text"
              />
              <ValidatedField label={translate('partywaveApp.appUser.href')} id="app-user-href" name="href" data-cy="href" type="text" />
              <ValidatedField label={translate('partywaveApp.appUser.url')} id="app-user-url" name="url" data-cy="url" type="text" />
              <ValidatedField label={translate('partywaveApp.appUser.type')} id="app-user-type" name="type" data-cy="type" type="text" />
              <ValidatedField
                label={translate('partywaveApp.appUser.ipAddress')}
                id="app-user-ipAddress"
                name="ipAddress"
                data-cy="ipAddress"
                type="text"
              />
              <ValidatedField
                label={translate('partywaveApp.appUser.lastActiveAt')}
                id="app-user-lastActiveAt"
                name="lastActiveAt"
                data-cy="lastActiveAt"
                type="datetime-local"
                placeholder="YYYY-MM-DD HH:mm"
              />
              <ValidatedField
                label={translate('partywaveApp.appUser.status')}
                id="app-user-status"
                name="status"
                data-cy="status"
                type="select"
              >
                {appUserStatusValues.map(appUserStatus => (
                  <option value={appUserStatus} key={appUserStatus}>
                    {translate(`partywaveApp.AppUserStatus.${appUserStatus}`)}
                  </option>
                ))}
              </ValidatedField>
              <ValidatedField
                id="app-user-appUserStats"
                name="appUserStats"
                data-cy="appUserStats"
                label={translate('partywaveApp.appUser.appUserStats')}
                type="select"
              >
                <option value="" key="0" />
                {appUserStats
                  ? appUserStats.map(otherEntity => (
                      <option value={otherEntity.id} key={otherEntity.id}>
                        {otherEntity.id}
                      </option>
                    ))
                  : null}
              </ValidatedField>
              <Button tag={Link} id="cancel-save" data-cy="entityCreateCancelButton" to="/app-user" replace color="info">
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

export default AppUserUpdate;
