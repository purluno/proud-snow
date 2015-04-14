package app.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import akka.actor.ActorRef;

public class TcpServer {
	private Args args;

	public TcpServer(Args args) {
		this.args = args;
	}

	public void run() throws Exception {
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
					.childHandler(new ChannelInitializer<SocketChannel>() {
						@Override
						protected void initChannel(SocketChannel ch) throws Exception {
							// ch.pipeline().addLast(new
							// TcpServerHandler(args.getHandler()));
						}
					}).option(ChannelOption.SO_BACKLOG, args.getBacklog())
					.childOption(ChannelOption.SO_KEEPALIVE, args.isKeepalive())
					.childOption(ChannelOption.TCP_NODELAY, args.isTcpNodelay());
			ChannelFuture f = b.bind(args.getHost(), args.getPort()).sync();
			f.channel().closeFuture().sync();
		} finally {
			workerGroup.shutdownGracefully();
			bossGroup.shutdownGracefully();
		}
	}

	public static final class Args {
		private String host = "0.0.0.0";

		private int port;

		private int backlog = 10;

		private boolean keepalive = false;

		private boolean tcpNodelay = false;

		private ActorRef handler;

		public String getHost() {
			return host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public int getBacklog() {
			return backlog;
		}

		public void setBacklog(int backlog) {
			this.backlog = backlog;
		}

		public boolean isKeepalive() {
			return keepalive;
		}

		public void setKeepalive(boolean keepalive) {
			this.keepalive = keepalive;
		}

		public boolean isTcpNodelay() {
			return tcpNodelay;
		}

		public void setTcpNodelay(boolean tcpNodelay) {
			this.tcpNodelay = tcpNodelay;
		}

		public ActorRef getHandler() {
			return handler;
		}

		public void setHandler(ActorRef handler) {
			this.handler = handler;
		}
	}
}
