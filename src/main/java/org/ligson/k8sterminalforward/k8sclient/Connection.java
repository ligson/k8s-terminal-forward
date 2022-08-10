package org.ligson.k8sterminalforward.k8sclient;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.ligson.k8sterminalforward.component.WebSocketSessionManager;

import javax.net.ssl.SSLEngine;
import java.net.URI;

@Data
@Slf4j
public class Connection {
    private String url;
    private String token;
    private EventLoopGroup group;
    private Bootstrap boot;
    private ChannelFuture cf;
    private String sid;
    private WebSocketSessionManager webSocketSessionManager;

    public Connection(String url, String token, WebSocketSessionManager webSocketSessionManager, String sid) {
        this.url = url;
        this.token = token;
        group = new NioEventLoopGroup();
        boot = new Bootstrap();
        this.webSocketSessionManager = webSocketSessionManager;
        this.sid = sid;
    }

    private void bindChannel(SocketChannel socketChannel, K8SClientHandler handler) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline();

        SslContext sslContext = SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        //InetSocketAddress address = (InetSocketAddress) socketChannel.remoteAddress();

        SSLEngine sslEngine = sslContext.newEngine(socketChannel.alloc());
        //SSLEngine sslEngine = SSLContext.getDefault().createSSLEngine();
        //sslEngine.setUseClientMode(true);

        //JdkSslClientContext.newClientContext()..sessionContext().


        pipeline.addLast(new SslHandler(sslEngine));
        //pipeline.addLast(new HttpContentDecompressor());
        pipeline.addLast(new HttpClientCodec());
        pipeline.addLast(new WebSocket13FrameEncoder(true));
        pipeline.addLast(new HttpObjectAggregator(8192));
        pipeline.addLast(WebSocketClientCompressionHandler.INSTANCE);
        //pipeline.addLast("decoder", new HttpResponseDecoder());
        //pipeline.addLast("encoder", new HttpRequestEncoder());
        //pipeline.addLast(new WebSocketClientProtocolHandler(handshaker));
        pipeline.addLast(handler);
    }

    public void connect() throws Exception {
        URI websocketURI = new URI(url);
        HttpHeaders httpHeaders = new DefaultHttpHeaders();
        httpHeaders.add("Authorization", "Bearer " + token);
        //进行握手
        WebSocketClientHandshaker handshaker = WebSocketClientHandshakerFactory.newHandshaker(websocketURI, WebSocketVersion.V13, null, true, httpHeaders);
        K8SClientHandler handler = new K8SClientHandler(handshaker, this);
        boot.option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .group(group)
                .handler(new LoggingHandler(LogLevel.DEBUG))
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        bindChannel(socketChannel, handler);
                    }
                });
            cf = boot.connect(websocketURI.getHost(), websocketURI.getPort() == -1 ? 443:websocketURI.getPort());
        //阻塞等待是否握手成功
        //cf = handshaker.handshake(channel);
        cf.addListener((ChannelFutureListener) channelFuture -> {
            if (channelFuture.isSuccess()) {
                log.debug("连接：{}成功", url);
            } else {
                log.debug("连接：{}失败", url);
            }
        });
        cf.sync();

        ChannelFuture hf = handler.handshakeFuture();
        hf.addListener((ChannelFutureListener) channelFuture -> {
            if (channelFuture.isSuccess()) {
                log.debug("WS连接握手：{}成功", url);
            } else {
                log.debug("WS连接握手：{}失败", url);
            }
        });
        hf.sync();
    }

    public void close() {
        if (!group.isShutdown()) {
            group.shutdownGracefully();
        }
    }

    public void reply(String msg) {
        webSocketSessionManager.reply(msg, sid);
    }

    public void receiveMsg(String text) {
        log.debug("接受到web发过来信息:{}", text);
        byte[] cmd = Base64.decodeBase64(text.substring(1));
        byte type = Byte.parseByte(text.substring(0, 1));
        byte[] buffer = new byte[1 + cmd.length];
        buffer[0] = type;
        System.arraycopy(cmd, 0, buffer, 1, cmd.length);
        //buffer[1] = 0x6c;
        //boolean a = (0x6c==108);
        //System.out.println(a);
        log.debug("给服务器发送信息:{}", buffer);
        BinaryWebSocketFrame frame = new BinaryWebSocketFrame(Unpooled.wrappedBuffer(buffer));
        cf.channel().writeAndFlush(frame);
    }
}
