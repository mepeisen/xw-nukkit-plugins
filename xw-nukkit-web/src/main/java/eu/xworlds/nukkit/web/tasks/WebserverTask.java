/*
    This file is part of "nukkit xWorlds plugin".

    "nukkit xWorlds plugin" is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    "nukkit xWorlds plugin" is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with "nukkit xWorlds plugin". If not, see <http://www.gnu.org/licenses/>.

 */
package eu.xworlds.nukkit.web.tasks;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import cn.nukkit.scheduler.PluginTask;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.TextFormat;
import eu.xworlds.nukkit.web.WebserverPlugin;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpHeaders.Names;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.ssl.SslContext;
import io.netty.util.concurrent.GenericFutureListener;

/**
 * A task to manage the built in web server.
 * 
 * @author mepeisen
 */
public class WebserverTask extends PluginTask<WebserverPlugin> {

	public static final String CONFIG_KEY_ENABLED = "webserver-enabled";
	public static final String CONFIG_KEY_PORT = "webserver-port";
	public static final String CONFIG_KEY_MAINTENANCE = "webserver-maintenance";
	
	private boolean startupIssued;
	
	private boolean stopIssued;
	
	private boolean maintenance;
	
	private int port;
	
	public enum ServerState {
		STOPPED,
		STOPPING,
		STARTING,
		RUNNING,
		MAINTENANCE
	}
	
	private ServerState currentState = ServerState.STOPPED;
	
	private int currentPort = -1;
	
	private Map<String, WebpageHandlerFactory> factories = new ConcurrentHashMap<>();
	
	public WebserverTask(WebserverPlugin owner, Config config) { 
		super(owner);
		if (config.getBoolean(CONFIG_KEY_MAINTENANCE)) {
			this.maintenance = true;
		}
		if (config.getBoolean(CONFIG_KEY_ENABLED)) {
			this.startupIssued = true;
		}
		this.port = config.getInt(CONFIG_KEY_PORT);
	}

	/**
	 * @param name
	 * @param factory
	 */
	public void registerFactory(String name, WebpageHandlerFactory factory) {
		this.factories.put(name, factory);
	}

	/**
	 * @param name
	 */
	public void unregisterFactory(String name) {
		this.factories.remove(name);
	}

	@Override
	public void onRun(int currentTick) {
		if (this.startupIssued) {
			if (this.stopIssued) {
				// issued both commands since last run
				this.startupIssued = false;
				this.stopIssued = false;
				this.owner.getLogger().warning(TextFormat.RED + "Webserver got start and stop command. Ignoring start/stop commands.");
			}
			else {
				// start the server asynchronous
				switch (this.currentState) {
					case STOPPED:
						this.startupIssued = false;
						this.currentState = ServerState.STARTING;
						this.owner.getLogger().info(TextFormat.DARK_GREEN + "Perform web server start.");
						this.startAsync();
						break;
					case MAINTENANCE:
						this.startupIssued = false;
						this.owner.getLogger().warning(TextFormat.RED + "Webserver already running but in maintenance mode. Ignoring start command.");
						break;
					case RUNNING:
						this.startupIssued = false;
						this.owner.getLogger().warning(TextFormat.RED + "Webserver already running. Ignoring start command.");
						break;
					case STARTING:
						this.startupIssued = false;
						this.owner.getLogger().warning(TextFormat.RED + "Webserver already starting. Ignoring start command.");
						break;
					case STOPPING:
						this.owner.getLogger().warning(TextFormat.RED + "Webserver is currently stopping. Will try to start again later.");
						break;
				}
			}
		}
		else if (this.stopIssued) {
			// stop the server asynchronous
			switch (this.currentState) {
				case STOPPED:
					this.stopIssued = false;
					this.owner.getLogger().warning(TextFormat.RED + "Webserver already stopped. Ignoring stop command.");
					this.currentState = ServerState.STARTING;
					break;
				case MAINTENANCE:
				case RUNNING:
					this.stopIssued = false;
					this.currentState = ServerState.STOPPING;
					this.owner.getLogger().info(TextFormat.DARK_GREEN + "Perform web server stop.");
					this.stopAsync();
					break;
				case STOPPING:
					this.stopIssued = false;
					this.owner.getLogger().warning(TextFormat.RED + "Webserver already stopping. Ignoring stop command.");
					break;
				case STARTING:
					this.owner.getLogger().warning(TextFormat.RED + "Webserver is currently starting. Will try to start again later.");
					break;
			}
		}
	}

	/**
	 * @return the currentState
	 */
	public ServerState getCurrentState() {
		return this.currentState;
	}

	/**
	 * @param startupIssued the startupIssued to set
	 */
	public void setStartupIssued(boolean startupIssued) {
		this.startupIssued = startupIssued;
	}

	/**
	 * @param stopIssued the stopIssued to set
	 */
	public void setStopIssued(boolean stopIssued) {
		this.stopIssued = stopIssued;
	}

	/**
	 * @param maintenance the maintenance to set
	 */
	public void setMaintenance(boolean maintenance) {
		this.maintenance = maintenance;
	}
	
	/**
	 * @return the port
	 */
	public int getPort() {
		return this.port;
	}

	/**
	 * @param port the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * @return the maintenance
	 */
	public boolean isMaintenance() {
		return this.maintenance;
	}

	/**
	 * Forces web server shutdown due to shutting down the whole server
	 */
	public void shutdown() {
		final Channel ch = serverChannel;
		if (ch != null) {
			boolean success = false;
			try {
				success = ch.close().await(5000);
			} catch (InterruptedException e) {
				// silently ignore
			}
			if (!success) {
				getOwner().getLogger().error("Failed to stop webserver within time limit.");
			}
		}
	}

	/**
	 * @return
	 */
	public int getCurrentPort() {
		return this.currentPort;
	}

	/**
	 * stops the web server async
	 */
	private void stopAsync() {
		serverChannel.close();
	}
	
	private Channel serverChannel;
	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;

	/**
	 * starts the web server async
	 */
	private void startAsync() {
		bossGroup = new NioEventLoopGroup(1);
		workerGroup = new NioEventLoopGroup();
		final ServerBootstrap b = new ServerBootstrap();
		b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).childHandler(new NukkitNettyInitializer(null));
		this.currentPort = this.port;
		b.bind(this.port).addListener(new StartupListener());
	}
	
	private final class StartupListener implements GenericFutureListener<ChannelFuture> {

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			if (future.isSuccess()) {
				serverChannel = future.channel();
				serverChannel.closeFuture().addListener(new ShutdownListener());
				currentState = maintenance ? ServerState.MAINTENANCE : ServerState.RUNNING;
			}
			else {
				WebserverTask.this.getOwner().getLogger().error("Unable to bind webserver. Starting failed.");
				currentState = ServerState.STOPPED;
			}
		}
		
	}
	
	private final class ShutdownListener implements GenericFutureListener<ChannelFuture> {

		@Override
		public void operationComplete(ChannelFuture future) throws Exception {
			bossGroup.shutdownGracefully();
			workerGroup.shutdownGracefully();
			bossGroup = null;
			workerGroup = null;
			serverChannel = null;
			currentState = ServerState.STOPPED;
		}
		
	}
	
	private final class RequestContext implements WebRequestContext {
		
		protected HttpHeaders headers;
		protected HttpHeaders trailingHeaders;
		protected HttpVersion protocolVersion;
		protected String uri;
		protected QueryStringDecoder queryStringDecoder;
		protected ByteBuf content;
		protected HttpMethod method;

		@Override
		public HttpHeaders getRequestHeaders() {
			return this.headers;
		}

		@Override
		public HttpHeaders getRequestTrailingHeaders() {
			return this.trailingHeaders;
		}

		@Override
		public HttpVersion getProtocolVersion() {
			return this.protocolVersion;
		}

		@Override
		public String getRequestUri() {
			return this.uri;
		}

		@Override
		public QueryStringDecoder getQueryString() {
			return this.queryStringDecoder;
		}

		@Override
		public ByteBuf getRequestContent() {
			return this.content;
		}

		@Override
		public HttpMethod getMethod() {
			return this.method;
		}
		
	}
	
	private final class NukkitNettyHandler extends SimpleChannelInboundHandler<Object> {
		
		private HttpRequest request;
		
		private final RequestContext rContext = new RequestContext();

		/**
		 * @see io.netty.channel.SimpleChannelInboundHandler#channelRead0(io.netty.channel.ChannelHandlerContext, java.lang.Object)
		 */
		@Override
		protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
			if (msg instanceof HttpRequest) {
				final HttpRequest request = this.request = (HttpRequest) msg;
				if (HttpHeaders.is100ContinueExpected(request)) {
					send100Continue(ctx);
				}
				
				this.rContext.protocolVersion = request.getProtocolVersion();
				this.rContext.headers = request.headers();
				this.rContext.uri = request.getUri();
				this.rContext.headers = request.headers();
				this.rContext.queryStringDecoder = new QueryStringDecoder(request.getUri());
				this.rContext.method = request.getMethod();
			}
			
			if (msg instanceof HttpContent) {
				HttpContent httpContent = (HttpContent) msg;
				
				this.rContext.content = httpContent.content();
				
				if (msg instanceof LastHttpContent) {
					LastHttpContent trailer = (LastHttpContent) msg;
					this.rContext.trailingHeaders = trailer.trailingHeaders();
					
					if (!writeResponse(trailer, ctx)) {
						// If keep-alive is off, close the connection once the content is fully written.
						ctx.writeAndFlush(Unpooled.EMPTY_BUFFER).addListener(ChannelFutureListener.CLOSE);
					}
				}
			}

		}
		
		private WebpageHandler getHandler() {
			if (currentState != ServerState.RUNNING) {
				return WebserverPlugin.MAINTENANCE;
			}
			final String path = rContext.getQueryString().path();
			final String[] splitted = path.split("/");
			if (splitted == null || splitted.length == 0) {
				return WebserverPlugin.INDEX;
			}
			final String plugin = splitted[1];
			final WebpageHandlerFactory factory = factories.get(plugin);
			if (factory != null) {
				final WebpageHandler result = factory.requestHandler(rContext, getOwner().getServer(), splitted);
				if (result != null) {
					return result;
				}
			}
			return WebserverPlugin._404;
		}
		
		private boolean writeResponse(HttpObject currentObj, ChannelHandlerContext ctx) {
			// Decide whether to close the connection or not.
			boolean keepAlive = HttpHeaders.isKeepAlive(request);
			
			// TODO set keepalive to false because browsers seem not to like it ?!?!?
			//      there maybe some bug elsewhere
			keepAlive = false;
			
			// Build the response object.
			// TODO For better load management perform handleRequest in a separate worker group
			//      Dont do that in http encoding/decoding worker group
			FullHttpResponse response = null;
			try {
				response = getHandler().handleRequest(this.rContext, getOwner().getServer());
			}
			catch (RuntimeException ex) {
				response = WebserverPlugin._500.handleRequest(this.rContext, getOwner().getServer());
			}
			
			if (keepAlive) {
				// Add 'Content-Length' header only for a keep-alive connection.
				response.headers().set(Names.CONTENT_LENGTH, response.content().readableBytes());
				// Add keep alive header as per:
				// - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
				response.headers().set(Names.CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
			}
			else {
				response.headers().set(Names.CONNECTION, HttpHeaders.Values.CLOSE);
			}
			
			ctx.write(response);
			return keepAlive;
		}
		
		private void send100Continue(ChannelHandlerContext ctx) {
			FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.CONTINUE);
			ctx.write(response);
		}

		/**
		 * @see io.netty.channel.ChannelInboundHandlerAdapter#channelReadComplete(io.netty.channel.ChannelHandlerContext)
		 */
		@Override
		public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
			ctx.flush();
		}

		/**
		 * @see io.netty.channel.ChannelInboundHandlerAdapter#exceptionCaught(io.netty.channel.ChannelHandlerContext, java.lang.Throwable)
		 */
		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			final StringWriter writer = new StringWriter();
			cause.printStackTrace(new PrintWriter(writer));
			WebserverTask.this.owner.getLogger().error("Exception within webserver: " + cause.getMessage() + "\n" + writer.toString());
			ctx.close();
		}

	}
	
	private final class NukkitNettyInitializer extends ChannelInitializer<SocketChannel> {
		
		private final SslContext sslCtx;
		
		public NukkitNettyInitializer(SslContext sslCtx) {
			this.sslCtx = sslCtx;
		}

		/**
		 * @see io.netty.channel.ChannelInitializer#initChannel(io.netty.channel.Channel)
		 */
		@Override
		protected void initChannel(SocketChannel ch) throws Exception {
			final ChannelPipeline p = ch.pipeline();
			
			if (this.sslCtx != null) p.addLast(sslCtx.newHandler(ch.alloc()));
			
			p.addLast(new HttpRequestDecoder());
			p.addLast(new HttpObjectAggregator(1048576)); // http chunks
			p.addLast(new HttpResponseEncoder());
			p.addLast(new HttpContentCompressor()); // automatic compression
			p.addLast(new NukkitNettyHandler());
		}
		
	}
	
}
