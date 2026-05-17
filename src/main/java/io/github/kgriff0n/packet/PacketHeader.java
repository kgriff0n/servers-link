package io.github.kgriff0n.packet;

import java.io.Serializable;

public abstract class PacketHeader implements Serializable {

    protected final String sender;
    protected final String recipient;


    public PacketHeader(String sender, String recipient) {
        this.sender = sender;
        this.recipient = recipient;
    }

    public String getSender() {
        return sender;
    }

    public String getRecipient() {
        return recipient;
    }
}
