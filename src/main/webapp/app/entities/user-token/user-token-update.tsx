import React, { useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Button, Col, FormText, Row } from 'reactstrap';
import { Translate, ValidatedField, ValidatedForm, translate } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { convertDateTimeFromServer, convertDateTimeToServer, displayDefaultDateTime } from 'app/shared/util/date-utils';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntities as getAppUsers } from 'app/entities/app-user/app-user.reducer';
import { createEntity, getEntity, reset, updateEntity } from './user-token.reducer';

export const UserTokenUpdate = () => {
  const dispatch = useAppDispatch();

  const navigate = useNavigate();

  const { id } = useParams<'id'>();
  const isNew = id === undefined;

  const appUsers = useAppSelector(state => state.appUser.entities);
  const userTokenEntity = useAppSelector(state => state.userToken.entity);
  const loading = useAppSelector(state => state.userToken.loading);
  const updating = useAppSelector(state => state.userToken.updating);
  const updateSuccess = useAppSelector(state => state.userToken.updateSuccess);

  const handleClose = () => {
    navigate('/user-token');
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
    values.expiresAt = convertDateTimeToServer(values.expiresAt);

    const entity = {
      ...userTokenEntity,
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
        }
      : {
          ...userTokenEntity,
          expiresAt: convertDateTimeFromServer(userTokenEntity.expiresAt),
          appUser: userTokenEntity?.appUser?.id,
        };

  return (
    <div>
      <Row className="justify-content-center">
        <Col md="8">
          <h2 id="partywaveApp.userToken.home.createOrEditLabel" data-cy="UserTokenCreateUpdateHeading">
            <Translate contentKey="partywaveApp.userToken.home.createOrEditLabel">Create or edit a UserToken</Translate>
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
                  id="user-token-id"
                  label={translate('partywaveApp.userToken.id')}
                  validate={{ required: true }}
                />
              ) : null}
              <ValidatedField
                label={translate('partywaveApp.userToken.accessToken')}
                id="user-token-accessToken"
                name="accessToken"
                data-cy="accessToken"
                type="text"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                }}
              />
              <ValidatedField
                label={translate('partywaveApp.userToken.refreshToken')}
                id="user-token-refreshToken"
                name="refreshToken"
                data-cy="refreshToken"
                type="text"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                }}
              />
              <ValidatedField
                label={translate('partywaveApp.userToken.tokenType')}
                id="user-token-tokenType"
                name="tokenType"
                data-cy="tokenType"
                type="text"
              />
              <ValidatedField
                label={translate('partywaveApp.userToken.expiresAt')}
                id="user-token-expiresAt"
                name="expiresAt"
                data-cy="expiresAt"
                type="datetime-local"
                placeholder="YYYY-MM-DD HH:mm"
              />
              <ValidatedField
                label={translate('partywaveApp.userToken.scope')}
                id="user-token-scope"
                name="scope"
                data-cy="scope"
                type="text"
              />
              <ValidatedField
                id="user-token-appUser"
                name="appUser"
                data-cy="appUser"
                label={translate('partywaveApp.userToken.appUser')}
                type="select"
                required
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
              <FormText>
                <Translate contentKey="entity.validation.required">This field is required.</Translate>
              </FormText>
              <Button tag={Link} id="cancel-save" data-cy="entityCreateCancelButton" to="/user-token" replace color="info">
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

export default UserTokenUpdate;
