import React, { useEffect } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Button, Col, Row } from 'reactstrap';
import { TextFormat, Translate } from 'react-jhipster';
import { FontAwesomeIcon } from '@fortawesome/react-fontawesome';

import { APP_DATE_FORMAT } from 'app/config/constants';
import { useAppDispatch, useAppSelector } from 'app/config/store';

import { getEntity } from './chat-message.reducer';

export const ChatMessageDetail = () => {
  const dispatch = useAppDispatch();

  const { id } = useParams<'id'>();

  useEffect(() => {
    dispatch(getEntity(id));
  }, []);

  const chatMessageEntity = useAppSelector(state => state.chatMessage.entity);
  return (
    <Row>
      <Col md="8">
        <h2 data-cy="chatMessageDetailsHeading">
          <Translate contentKey="partywaveApp.chatMessage.detail.title">ChatMessage</Translate>
        </h2>
        <dl className="jh-entity-details">
          <dt>
            <span id="id">
              <Translate contentKey="partywaveApp.chatMessage.id">Id</Translate>
            </span>
          </dt>
          <dd>{chatMessageEntity.id}</dd>
          <dt>
            <span id="content">
              <Translate contentKey="partywaveApp.chatMessage.content">Content</Translate>
            </span>
          </dt>
          <dd>{chatMessageEntity.content}</dd>
          <dt>
            <span id="sentAt">
              <Translate contentKey="partywaveApp.chatMessage.sentAt">Sent At</Translate>
            </span>
          </dt>
          <dd>{chatMessageEntity.sentAt ? <TextFormat value={chatMessageEntity.sentAt} type="date" format={APP_DATE_FORMAT} /> : null}</dd>
          <dt>
            <Translate contentKey="partywaveApp.chatMessage.room">Room</Translate>
          </dt>
          <dd>{chatMessageEntity.room ? chatMessageEntity.room.name : ''}</dd>
          <dt>
            <Translate contentKey="partywaveApp.chatMessage.sender">Sender</Translate>
          </dt>
          <dd>{chatMessageEntity.sender ? chatMessageEntity.sender.displayName : ''}</dd>
        </dl>
        <Button tag={Link} to="/chat-message" replace color="info" data-cy="entityDetailsBackButton">
          <FontAwesomeIcon icon="arrow-left" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.back">Back</Translate>
          </span>
        </Button>
        &nbsp;
        <Button tag={Link} to={`/chat-message/${chatMessageEntity.id}/edit`} replace color="primary">
          <FontAwesomeIcon icon="pencil-alt" />{' '}
          <span className="d-none d-md-inline">
            <Translate contentKey="entity.action.edit">Edit</Translate>
          </span>
        </Button>
      </Col>
    </Row>
  );
};

export default ChatMessageDetail;
