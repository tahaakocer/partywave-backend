package com.partywave.backend.domain;

import java.util.UUID;

public class ChatMessageTestSamples {

    public static ChatMessage getChatMessageSample1() {
        return new ChatMessage().id(UUID.fromString("23d8dc04-a48b-45d9-a01d-4b728f0ad4aa")).content("content1");
    }

    public static ChatMessage getChatMessageSample2() {
        return new ChatMessage().id(UUID.fromString("ad79f240-3727-46c3-b89f-2cf6ebd74367")).content("content2");
    }

    public static ChatMessage getChatMessageRandomSampleGenerator() {
        return new ChatMessage().id(UUID.randomUUID()).content(UUID.randomUUID().toString());
    }
}
