import React, { useEffect } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { Button, Col, Row } from 'reactstrap';
import { Translate, ValidatedField, ValidatedForm, translate } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { convertDateTimeFromServer, convertDateTimeToServer, displayDefaultDateTime } from 'app/shared/util/date-utils';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntities as getRooms } from 'app/entities/room/room.reducer';
import { getEntities as getAppUsers } from 'app/entities/app-user/app-user.reducer';
import { RoomMemberRole } from 'app/shared/model/enumerations/room-member-role.model';
import { createEntity, getEntity, reset, updateEntity } from './room-member.reducer';

export const RoomMemberUpdate = () => {
  const dispatch = useAppDispatch();

  const navigate = useNavigate();

  const { id } = useParams<'id'>();
  const isNew = id === undefined;

  const rooms = useAppSelector(state => state.room.entities);
  const appUsers = useAppSelector(state => state.appUser.entities);
  const roomMemberEntity = useAppSelector(state => state.roomMember.entity);
  const loading = useAppSelector(state => state.roomMember.loading);
  const updating = useAppSelector(state => state.roomMember.updating);
  const updateSuccess = useAppSelector(state => state.roomMember.updateSuccess);
  const roomMemberRoleValues = Object.keys(RoomMemberRole);

  const handleClose = () => {
    navigate(`/room-member${location.search}`);
  };

  useEffect(() => {
    if (isNew) {
      dispatch(reset());
    } else {
      dispatch(getEntity(id));
    }

    dispatch(getRooms({}));
    dispatch(getAppUsers({}));
  }, []);

  useEffect(() => {
    if (updateSuccess) {
      handleClose();
    }
  }, [updateSuccess]);

  const saveEntity = values => {
    values.joinedAt = convertDateTimeToServer(values.joinedAt);
    values.lastActiveAt = convertDateTimeToServer(values.lastActiveAt);

    const entity = {
      ...roomMemberEntity,
      ...values,
      room: rooms.find(it => it.id.toString() === values.room?.toString()),
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
          joinedAt: displayDefaultDateTime(),
          lastActiveAt: displayDefaultDateTime(),
        }
      : {
          role: 'OWNER',
          ...roomMemberEntity,
          joinedAt: convertDateTimeFromServer(roomMemberEntity.joinedAt),
          lastActiveAt: convertDateTimeFromServer(roomMemberEntity.lastActiveAt),
          room: roomMemberEntity?.room?.id,
          appUser: roomMemberEntity?.appUser?.id,
        };

  return (
    <div>
      <Row className="justify-content-center">
        <Col md="8">
          <h2 id="partywaveApp.roomMember.home.createOrEditLabel" data-cy="RoomMemberCreateUpdateHeading">
            <Translate contentKey="partywaveApp.roomMember.home.createOrEditLabel">Create or edit a RoomMember</Translate>
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
                  id="room-member-id"
                  label={translate('partywaveApp.roomMember.id')}
                  validate={{ required: true }}
                />
              ) : null}
              <ValidatedField
                label={translate('partywaveApp.roomMember.joinedAt')}
                id="room-member-joinedAt"
                name="joinedAt"
                data-cy="joinedAt"
                type="datetime-local"
                placeholder="YYYY-MM-DD HH:mm"
                validate={{
                  required: { value: true, message: translate('entity.validation.required') },
                }}
              />
              <ValidatedField
                label={translate('partywaveApp.roomMember.lastActiveAt')}
                id="room-member-lastActiveAt"
                name="lastActiveAt"
                data-cy="lastActiveAt"
                type="datetime-local"
                placeholder="YYYY-MM-DD HH:mm"
              />
              <ValidatedField
                label={translate('partywaveApp.roomMember.role')}
                id="room-member-role"
                name="role"
                data-cy="role"
                type="select"
              >
                {roomMemberRoleValues.map(roomMemberRole => (
                  <option value={roomMemberRole} key={roomMemberRole}>
                    {translate(`partywaveApp.RoomMemberRole.${roomMemberRole}`)}
                  </option>
                ))}
              </ValidatedField>
              <ValidatedField
                id="room-member-room"
                name="room"
                data-cy="room"
                label={translate('partywaveApp.roomMember.room')}
                type="select"
              >
                <option value="" key="0" />
                {rooms
                  ? rooms.map(otherEntity => (
                      <option value={otherEntity.id} key={otherEntity.id}>
                        {otherEntity.name}
                      </option>
                    ))
                  : null}
              </ValidatedField>
              <ValidatedField
                id="room-member-appUser"
                name="appUser"
                data-cy="appUser"
                label={translate('partywaveApp.roomMember.appUser')}
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
              <Button tag={Link} id="cancel-save" data-cy="entityCreateCancelButton" to="/room-member" replace color="info">
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

export default RoomMemberUpdate;
