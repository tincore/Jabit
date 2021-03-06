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

package ch.dissem.bitmessage.entity.payload;

import ch.dissem.bitmessage.entity.BitmessageAddress;
import ch.dissem.bitmessage.entity.Encrypted;
import ch.dissem.bitmessage.entity.Plaintext;
import ch.dissem.bitmessage.entity.PlaintextHolder;
import ch.dissem.bitmessage.exception.DecryptionFailedException;

import java.io.IOException;
import java.util.Objects;

import static ch.dissem.bitmessage.entity.Plaintext.Type.BROADCAST;
import static ch.dissem.bitmessage.utils.Singleton.cryptography;

/**
 * Users who are subscribed to the sending address will see the message appear in their inbox.
 * Broadcasts are version 4 or 5.
 */
public abstract class Broadcast extends ObjectPayload implements Encrypted, PlaintextHolder {
    private static final long serialVersionUID = 4064521827582239069L;

    protected final long stream;
    protected CryptoBox encrypted;
    protected Plaintext plaintext;

    protected Broadcast(long version, long stream, CryptoBox encrypted, Plaintext plaintext) {
        super(version);
        this.stream = stream;
        this.encrypted = encrypted;
        this.plaintext = plaintext;
    }

    public static long getVersion(BitmessageAddress address) {
        return address.getVersion() < 4 ? 4 : 5;
    }

    @Override
    public boolean isSigned() {
        return true;
    }

    @Override
    public byte[] getSignature() {
        return plaintext.getSignature();
    }

    @Override
    public void setSignature(byte[] signature) {
        plaintext.setSignature(signature);
    }

    @Override
    public long getStream() {
        return stream;
    }

    @Override
    public Plaintext getPlaintext() {
        return plaintext;
    }

    @Override
    public void encrypt(byte[] publicKey) throws IOException {
        this.encrypted = new CryptoBox(plaintext, publicKey);
    }

    public void encrypt() throws IOException {
        encrypt(cryptography().createPublicKey(plaintext.getFrom().getPublicDecryptionKey()));
    }

    @Override
    public void decrypt(byte[] privateKey) throws IOException, DecryptionFailedException {
        plaintext = Plaintext.read(BROADCAST, encrypted.decrypt(privateKey));
    }

    public void decrypt(BitmessageAddress address) throws IOException, DecryptionFailedException {
        decrypt(address.getPublicDecryptionKey());
    }

    @Override
    public boolean isDecrypted() {
        return plaintext != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Broadcast broadcast = (Broadcast) o;
        return stream == broadcast.stream &&
                (Objects.equals(encrypted, broadcast.encrypted) || Objects.equals(plaintext, broadcast.plaintext));
    }

    @Override
    public int hashCode() {
        return Objects.hash(stream);
    }
}
