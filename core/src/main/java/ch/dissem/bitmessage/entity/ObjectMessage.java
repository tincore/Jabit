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

import ch.dissem.bitmessage.entity.payload.ObjectPayload;
import ch.dissem.bitmessage.entity.payload.ObjectType;
import ch.dissem.bitmessage.entity.payload.Pubkey;
import ch.dissem.bitmessage.entity.valueobject.InventoryVector;
import ch.dissem.bitmessage.entity.valueobject.PrivateKey;
import ch.dissem.bitmessage.exception.ApplicationException;
import ch.dissem.bitmessage.exception.DecryptionFailedException;
import ch.dissem.bitmessage.utils.Bytes;
import ch.dissem.bitmessage.utils.Encode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

import static ch.dissem.bitmessage.utils.Singleton.cryptography;

/**
 * The 'object' command sends an object that is shared throughout the network.
 */
public class ObjectMessage implements MessagePayload {
    private static final long serialVersionUID = 2495752480120659139L;

    private byte[] nonce;
    private long expiresTime;
    private long objectType;
    /**
     * The object's version
     */
    private long version;
    private long stream;

    private ObjectPayload payload;
    private byte[] payloadBytes;

    private ObjectMessage(Builder builder) {
        nonce = builder.nonce;
        expiresTime = builder.expiresTime;
        objectType = builder.objectType;
        version = builder.payload.getVersion();
        stream = builder.streamNumber > 0 ? builder.streamNumber : builder.payload.getStream();
        payload = builder.payload;
    }

    @Override
    public Command getCommand() {
        return Command.OBJECT;
    }

    public byte[] getNonce() {
        return nonce;
    }

    public void setNonce(byte[] nonce) {
        this.nonce = nonce;
    }

    public long getExpiresTime() {
        return expiresTime;
    }

    public long getType() {
        return objectType;
    }

    public ObjectPayload getPayload() {
        return payload;
    }

    public long getVersion() {
        return version;
    }

    public long getStream() {
        return stream;
    }

    public InventoryVector getInventoryVector() {
        return new InventoryVector(
                Bytes.truncate(cryptography().doubleSha512(nonce, getPayloadBytesWithoutNonce()), 32)
        );
    }

    private boolean isEncrypted() {
        return payload instanceof Encrypted && !((Encrypted) payload).isDecrypted();
    }

    public boolean isSigned() {
        return payload.isSigned();
    }

    private byte[] getBytesToSign() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            writeHeaderWithoutNonce(out);
            payload.writeBytesToSign(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ApplicationException(e);
        }
    }

    public void sign(PrivateKey key) {
        if (payload.isSigned()) {
            payload.setSignature(cryptography().getSignature(getBytesToSign(), key));
        }
    }

    public void decrypt(PrivateKey key) throws IOException, DecryptionFailedException {
        if (payload instanceof Encrypted) {
            ((Encrypted) payload).decrypt(key.getPrivateEncryptionKey());
        }
    }

    public void decrypt(byte[] privateEncryptionKey) throws IOException, DecryptionFailedException {
        if (payload instanceof Encrypted) {
            ((Encrypted) payload).decrypt(privateEncryptionKey);
        }
    }

    public void encrypt(byte[] publicEncryptionKey) throws IOException {
        if (payload instanceof Encrypted) {
            ((Encrypted) payload).encrypt(publicEncryptionKey);
        }
    }

    public void encrypt(Pubkey publicKey) {
        try {
            if (payload instanceof Encrypted) {
                ((Encrypted) payload).encrypt(publicKey.getEncryptionKey());
            }
        } catch (IOException e) {
            throw new ApplicationException(e);
        }
    }

    public boolean isSignatureValid(Pubkey pubkey) throws IOException {
        if (isEncrypted()) throw new IllegalStateException("Payload must be decrypted first");
        return cryptography().isSignatureValid(getBytesToSign(), payload.getSignature(), pubkey);
    }

    @Override
    public void write(OutputStream out) throws IOException {
        if (nonce == null) {
            out.write(new byte[8]);
        } else {
            out.write(nonce);
        }
        out.write(getPayloadBytesWithoutNonce());
    }

    @Override
    public void write(ByteBuffer buffer) {
        if (nonce == null) {
            buffer.put(new byte[8]);
        } else {
            buffer.put(nonce);
        }
        buffer.put(getPayloadBytesWithoutNonce());
    }

    private void writeHeaderWithoutNonce(OutputStream out) throws IOException {
        Encode.int64(expiresTime, out);
        Encode.int32(objectType, out);
        Encode.varInt(version, out);
        Encode.varInt(stream, out);
    }

    public byte[] getPayloadBytesWithoutNonce() {
        try {
            if (payloadBytes == null) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                writeHeaderWithoutNonce(out);
                payload.write(out);
                payloadBytes = out.toByteArray();
            }
            return payloadBytes;
        } catch (IOException e) {
            throw new ApplicationException(e);
        }
    }

    public static final class Builder {
        private byte[] nonce;
        private long expiresTime;
        private long objectType = -1;
        private long streamNumber;
        private ObjectPayload payload;

        public Builder nonce(byte[] nonce) {
            this.nonce = nonce;
            return this;
        }

        public Builder expiresTime(long expiresTime) {
            this.expiresTime = expiresTime;
            return this;
        }

        public Builder objectType(long objectType) {
            this.objectType = objectType;
            return this;
        }

        public Builder objectType(ObjectType objectType) {
            this.objectType = objectType.getNumber();
            return this;
        }

        public Builder stream(long streamNumber) {
            this.streamNumber = streamNumber;
            return this;
        }

        public Builder payload(ObjectPayload payload) {
            this.payload = payload;
            if (this.objectType == -1)
                this.objectType = payload.getType().getNumber();
            return this;
        }

        public ObjectMessage build() {
            return new ObjectMessage(this);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ObjectMessage that = (ObjectMessage) o;

        return expiresTime == that.expiresTime &&
                objectType == that.objectType &&
                version == that.version &&
                stream == that.stream &&
                Objects.equals(payload, that.payload);
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(nonce);
        result = 31 * result + (int) (expiresTime ^ (expiresTime >>> 32));
        result = 31 * result + (int) (objectType ^ (objectType >>> 32));
        result = 31 * result + (int) (version ^ (version >>> 32));
        result = 31 * result + (int) (stream ^ (stream >>> 32));
        result = 31 * result + (payload != null ? payload.hashCode() : 0);
        return result;
    }
}
