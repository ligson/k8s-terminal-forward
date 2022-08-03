package org.ligson.k8sterminalforward.k8sclient;

import io.netty.channel.*;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;

@Slf4j
public class K8SClientHandler extends ChannelDuplexHandler {

    private final WebSocketClientHandshaker handshaker;
    private ChannelPromise handshakeFuture;
    private ChannelHandlerContext context;
    private Connection connection;

    public K8SClientHandler(WebSocketClientHandshaker handshaker, Connection connection) {
        this.handshaker = handshaker;
        this.connection = connection;
    }

    public ChannelFuture handshakeFuture() {
        return handshakeFuture;
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        handshaker.handshake(ctx.channel());
        context = ctx;
        log.debug("1ctx-hashcode:{}", ctx.hashCode());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.debug("WebSocket Client disconnected!");
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("通道错误:{}", cause.getMessage(), cause);
        if (!handshakeFuture.isDone()) {
            handshakeFuture.setFailure(cause);
        }
        ctx.close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelReadComplete();
        ctx.flush();
    }


    protected void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
        Channel ch = ctx.channel();
        if (!handshaker.isHandshakeComplete()) {
            handshaker.finishHandshake(ch, (FullHttpResponse) msg);
            log.debug("WebSocket Client connected!");
            handshakeFuture.setSuccess();
            return;
        }

        if (msg instanceof FullHttpResponse) {
            FullHttpResponse response = (FullHttpResponse) msg;
            throw new IllegalStateException(
                    "Unexpected FullHttpResponse (getStatus=" + response.status().code() +
                            ", content=" + response.content().toString(CharsetUtil.UTF_8) + ')');
        }

        WebSocketFrame frame = (WebSocketFrame) msg;
        if (frame instanceof TextWebSocketFrame) {
            log.debug("WebSocket Client received message: " + frame);
        } else if (frame instanceof PongWebSocketFrame) {
            log.debug("WebSocket Client received pong");
        } else if (frame instanceof CloseWebSocketFrame) {
            log.debug("WebSocket Client received closing");
            ch.close();
        } else if (frame instanceof BinaryWebSocketFrame binaryWebSocketFrame) {
            byte[] bytes = new byte[frame.content().readableBytes()];
            binaryWebSocketFrame.content().readBytes(bytes);
            log.debug("收到服务端响应字节 :{}", bytes);
            log.debug("2ctx-hashcode:{}", ctx.hashCode());
            byte type = bytes[0];
            byte[] buffer = new byte[bytes.length - 1];
            System.arraycopy(bytes, 1, buffer, 0, buffer.length);
            String cmd = Base64.encodeBase64String(buffer);
            String result = type + cmd;
            log.debug("向前端回复字符串:{}", result);
            connection.reply(result);
        }
    }

   /* @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, Object o) throws Exception {
        messageReceived(channelHandlerContext, o);
    }*/

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        log.debug("收到服务端消息类型:{}", msg.getClass().getName());
        messageReceived(ctx, msg);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        log.debug("给服务端发送消息，类型是 :{}", msg.getClass().getName());
        ctx.write(msg);
    }
}
