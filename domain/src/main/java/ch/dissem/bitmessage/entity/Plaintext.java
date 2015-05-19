/*
 * Copyright 2015 Christian Basler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ch.dissem.bitmessage.entity;

import ch.dissem.bitmessage.entity.payload.Pubkey;
import ch.dissem.bitmessage.entity.valueobject.Label;
import ch.dissem.bitmessage.factory.Factory;
import ch.dissem.bitmessage.utils.Decode;
import ch.dissem.bitmessage.utils.Encode;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;

/**
 * The unencrypted message to be sent by 'msg' or 'broadcast'.
 */
public class Plaintext implements Streamable {
    private final BitmessageAddress from;
    private final long encoding;
    private final byte[] message;
    private final byte[] ack;
    private Object id;
    private BitmessageAddress to;
    private byte[] signature;
    private Status status;
    private Long sent;
    private Long received;

    private Set<Label> labels;

    private Plaintext(Builder builder) {
        id = builder.id;
        from = builder.from;
        to = builder.to;
        encoding = builder.encoding;
        message = builder.message;
        ack = builder.ack;
        signature = builder.signature;
        status = builder.status;
        sent = builder.sent;
        received = builder.received;
        labels = builder.labels;
    }

    public static Plaintext read(InputStream in) throws IOException {
        return readWithoutSignature(in)
                .signature(Decode.varBytes(in))
                .build();
    }

    public static Plaintext.Builder readWithoutSignature(InputStream in) throws IOException {
        return new Builder()
                .addressVersion(Decode.varInt(in))
                .stream(Decode.varInt(in))
                .behaviorBitfield(Decode.int32(in))
                .publicSigningKey(Decode.bytes(in, 64))
                .publicEncryptionKey(Decode.bytes(in, 64))
                .nonceTrialsPerByte(Decode.varInt(in))
                .extraBytes(Decode.varInt(in))
                .destinationRipe(Decode.bytes(in, 20))
                .encoding(Decode.varInt(in))
                .message(Decode.varBytes(in))
                .ack(Decode.varBytes(in));
    }

    public byte[] getMessage() {
        return message;
    }

    public BitmessageAddress getFrom() {
        return from;
    }

    public BitmessageAddress getTo() {
        return to;
    }

    public void setTo(BitmessageAddress to) {
        if (this.to.getVersion() != 0)
            throw new RuntimeException("Correct address already set");
        if (Arrays.equals(this.to.getRipe(), to.getRipe())) {
            throw new RuntimeException("RIPEs don't match");
        }
        this.to = to;
    }

    public Set<Label> getLabels() {
        return labels;
    }

    public long getStream() {
        return from.getStream();
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public void write(OutputStream out, boolean includeSignature) throws IOException {
        Encode.varInt(from.getVersion(), out);
        Encode.varInt(from.getStream(), out);
        Encode.int32(from.getPubkey().getBehaviorBitfield(), out);
        out.write(from.getPubkey().getSigningKey());
        out.write(from.getPubkey().getEncryptionKey());
        Encode.varInt(from.getPubkey().getNonceTrialsPerByte(), out);
        Encode.varInt(from.getPubkey().getExtraBytes(), out);
        out.write(to.getRipe());
        Encode.varInt(encoding, out);
        Encode.varInt(message.length, out);
        out.write(message);
        Encode.varInt(ack.length, out);
        out.write(ack);
        if (includeSignature) {
            Encode.varInt(signature.length, out);
            out.write(signature);
        }
    }

    @Override
    public void write(OutputStream out) throws IOException {
        write(out, true);
    }

    public Object getId() {
        return id;
    }

    public void setId(long id) {
        if (this.id != null) throw new IllegalStateException("ID already set");
        this.id = id;
    }

    public Long getSent() {
        return sent;
    }

    public Long getReceived() {
        return received;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public enum Encoding {
        IGNORE(0), TRIVIAL(1), SIMPLE(2);

        long code;

        Encoding(long code) {
            this.code = code;
        }

        public static Encoding fromCode(long code) {
            for (Encoding e : values()) {
                if (e.getCode() == code) return e;
            }
            return null;
        }

        public long getCode() {
            return code;
        }
    }

    public enum Status {
        PUBKEY_REQUESTED,
        DOING_PROOF_OF_WORK,
        SENT,
        ACKNOWLEDGED
    }

    public static final class Builder {
        private Object id;
        private BitmessageAddress from;
        private BitmessageAddress to;
        private long addressVersion;
        private long stream;
        private int behaviorBitfield;
        private byte[] publicSigningKey;
        private byte[] publicEncryptionKey;
        private long nonceTrialsPerByte;
        private long extraBytes;
        private byte[] destinationRipe;
        private long encoding;
        private byte[] message = new byte[0];
        private byte[] ack = new byte[0];
        private byte[] signature;
        private long sent;
        private long received;
        private Status status;
        private Set<Label> labels = new TreeSet<>();

        public Builder() {
        }

        public Builder id(Object id) {
            this.id = id;
            return this;
        }

        public Builder from(BitmessageAddress address) {
            from = address;
            return this;
        }

        public Builder to(BitmessageAddress address) {
            to = address;
            return this;
        }

        private Builder addressVersion(long addressVersion) {
            this.addressVersion = addressVersion;
            return this;
        }

        private Builder stream(long stream) {
            this.stream = stream;
            return this;
        }

        private Builder behaviorBitfield(int behaviorBitfield) {
            this.behaviorBitfield = behaviorBitfield;
            return this;
        }

        private Builder publicSigningKey(byte[] publicSigningKey) {
            this.publicSigningKey = publicSigningKey;
            return this;
        }

        private Builder publicEncryptionKey(byte[] publicEncryptionKey) {
            this.publicEncryptionKey = publicEncryptionKey;
            return this;
        }

        private Builder nonceTrialsPerByte(long nonceTrialsPerByte) {
            this.nonceTrialsPerByte = nonceTrialsPerByte;
            return this;
        }

        private Builder extraBytes(long extraBytes) {
            this.extraBytes = extraBytes;
            return this;
        }

        private Builder destinationRipe(byte[] ripe) {
            this.destinationRipe = ripe;
            return this;
        }

        public Builder encoding(Encoding encoding) {
            this.encoding = encoding.getCode();
            return this;
        }

        private Builder encoding(long encoding) {
            this.encoding = encoding;
            return this;
        }

        public Builder message(String subject, String message) {
            try {
                this.message = ("Subject:" + subject + '\n' + "Body:" + message).getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
            return this;
        }

        public Builder message(byte[] message) {
            this.message = message;
            return this;
        }

        public Builder ack(byte[] ack) {
            this.ack = ack;
            return this;
        }

        public Builder signature(byte[] signature) {
            this.signature = signature;
            return this;
        }

        public Builder sent(long sent) {
            this.sent = sent;
            return this;
        }

        public Builder received(long received) {
            this.received = received;
            return this;
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Builder labels(Collection<Label> labels) {
            this.labels.addAll(labels);
            return this;
        }

        public Plaintext build() {
            if (id == null) {
                id = UUID.randomUUID();
            }
            if (from == null) {
                from = new BitmessageAddress(Factory.createPubkey(
                        addressVersion,
                        stream,
                        publicSigningKey,
                        publicEncryptionKey,
                        nonceTrialsPerByte,
                        extraBytes,
                        Pubkey.Feature.features(behaviorBitfield)
                ));
            }
            if (to == null) {
                to = new BitmessageAddress(0, 0, destinationRipe);
            }
            return new Plaintext(this);
        }
    }
}