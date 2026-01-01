package io.github.seal90.serviceclient.rsocket.core;// RSocketExchange.java

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.CompositeByteBuf;
import io.rsocket.Payload;
import io.rsocket.metadata.CompositeMetadata;
import io.rsocket.metadata.CompositeMetadataCodec;
import io.rsocket.util.DefaultPayload;
import lombok.Getter;
import org.springframework.core.ResolvableType;
import org.springframework.core.codec.Decoder;
import org.springframework.core.codec.Encoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.NettyDataBuffer;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.MimeType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class RSocketExchange {

    private final Payload payload;

    private List<StashEntry> entries;

    private RSocketStrategies strategies;

    private NettyDataBufferFactory nettyDataBufferFactory;

    private final Map<String, Object> attributes = new ConcurrentHashMap<>();

    private RSocketExchange(Payload payload, RSocketStrategies strategies, NettyDataBufferFactory nettyDataBufferFactory) {
        this.payload = Objects.requireNonNull(payload);
        this.strategies = strategies;
        this.nettyDataBufferFactory = nettyDataBufferFactory;
        this.entries = new ArrayList<>();

        if(payload.hasMetadata()) {
            CompositeMetadata composite = new CompositeMetadata(payload.sliceMetadata(), true);
            entries = new ArrayList<>(4);

            composite.forEach(entry -> {
                StashEntry stashEntry = new StashEntry(entry.getContent().retainedSlice(), entry.getMimeType());
                entries.add(stashEntry);
            });
        }
    }

    public static RSocketExchange from(Payload payload, RSocketStrategies strategies, NettyDataBufferFactory nettyDataBufferFactory) {
        return new RSocketExchange(payload, strategies, nettyDataBufferFactory);
    }

    public <T> Optional<T> findFirstMetadata(String mimeType, Class<T> type) {
        Optional<StashEntry> entryOpt = entries.stream()
            .filter(e -> mimeType.equals(e.mimeType))
            .findFirst();

        if (entryOpt.isEmpty()) {
            return Optional.empty();
        }

        StashEntry entry = entryOpt.get();

        // Return cached parsed object if type matches
        if (type.isInstance(entry.parsedObj)) {
            return Optional.of((T) entry.parsedObj);
        }
        ResolvableType dataType = ResolvableType.forClass(type);
        MimeType dataMimeType = MimeType.valueOf(mimeType);
        Decoder<Object> decoder = strategies.decoder(dataType, dataMimeType);
        NettyDataBuffer nettyDataBuffer = nettyDataBufferFactory.wrap(entry.getContent().retainedSlice());
        T decoded = (T) decoder.decode(nettyDataBuffer, dataType, dataMimeType, null);
        entry.parsedObj = decoded;
        return Optional.of(decoded);
    }

    public void addMetadata(String mimeType, ByteBuf data) {
        StashEntry stashEntry = new StashEntry(data, mimeType);
        this.entries.add(stashEntry);
    }

    public void addMetadata(String mimeType, Object data) {
        Objects.requireNonNull(mimeType, "mimeType must not be null");
        Objects.requireNonNull(data, "data must not be null");

        StashEntry stashEntry = new StashEntry(data, mimeType);
        this.entries.add(stashEntry);
    }

    public static ByteBuf dataBufferToRetainedByteBuf(DataBuffer dataBuffer) {
        if (dataBuffer instanceof NettyDataBuffer) {
            ByteBuf byteBuf = ((NettyDataBuffer) dataBuffer).getNativeBuffer();
            return byteBuf.retain();
        } else {
            throw new IllegalArgumentException(
                "DataBuffer is not backed by Netty ByteBuf: " + dataBuffer.getClass()
            );
        }
    }

    public Payload buildNewPayload() {
        ByteBuf newMetadata = buildCompositeMetadata();
        ByteBuf newData = payload.sliceData().retain();
        return DefaultPayload.create(newData, newMetadata);
    }

    private ByteBuf buildCompositeMetadata() {
        CompositeByteBuf newCompositeMetadata = ByteBufAllocator.DEFAULT.compositeBuffer();
        for (StashEntry entry : entries) {
            ByteBuf byteBuf = null;
            if(entry.getUpdated()) {
                ResolvableType dataType = ResolvableType.forClass(entry.getParsedObj().getClass());
                MimeType dataMimeType = MimeType.valueOf(entry.getMimeType());
                Encoder<Object> encoder = strategies.encoder(dataType, dataMimeType);
                DataBuffer dataBuffer = encoder.encodeValue(entry.getParsedObj(), strategies.dataBufferFactory(), dataType, dataMimeType, null);
                byteBuf = dataBufferToRetainedByteBuf(dataBuffer);
            } else {
                byteBuf = entry.getContent().retain();
            }
            CompositeMetadataCodec.encodeAndAddMetadata(
                newCompositeMetadata,
                ByteBufAllocator.DEFAULT,
                entry.getMimeType(),
                byteBuf
            );
        }
        return newCompositeMetadata;
    }

    public <T> T getAttribute(String name) {
        return (T) attributes.get(name);
    }

    public void setAttribute(String name, Object value) {
        attributes.put(name, value);
    }

    @Getter
    public static class StashEntry {

        private ByteBuf content;

        private String mimeType;

        private Object parsedObj;

        private Boolean updated;

        public StashEntry(ByteBuf content, String mimeType) {
            this.content = content;
            this.mimeType = mimeType;
            this.parsedObj = null;
            this.updated = false;
        }

        public StashEntry(Object parsedObj, String mimeType) {
            this.content = null;
            this.mimeType = mimeType;
            this.parsedObj = parsedObj;
            this.updated = true;
        }

        public void setParsedObj(Object parsedObj) {
            this.parsedObj = parsedObj;
            this.updated = true;
        }
    }

}