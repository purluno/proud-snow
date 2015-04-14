package app.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

import java.nio.ByteBuffer;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;

public class ChannelActor extends UntypedActor {
	private InnerHandler innerHandler = new InnerHandler();

	private ChannelHandlerContext channelCtx;

	private ActorRef handler;

	public ChannelActor(ActorRef handler) {
		this.handler = handler;
	}

	@Override
	public void postStop() throws Exception {
		if (channelCtx != null) {
			channelCtx.close();
			channelCtx = null;
		}
	}

	@Override
	public void onReceive(Object message) throws Exception {
		if (message instanceof ChannelCommand.Write) {
			ByteBuffer data = ((ChannelCommand.Write) message).getData();
			channelCtx.writeAndFlush(Unpooled.wrappedBuffer(data));
		} else if (message instanceof ChannelCommand.Close) {
			getContext().stop(getSelf());
		} else {
			unhandled(message);
		}
	}

	public InnerHandler getInnerHandler() {
		return innerHandler;
	}

	final class InnerHandler extends ChannelInboundHandlerAdapter {
		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			channelCtx = ctx;
			handler.tell(ChannelEvent.connected(), getSelf());
		}

		@Override
		public void channelInactive(ChannelHandlerContext ctx) throws Exception {
			handler.tell(ChannelEvent.disconnected(), getSelf());
			getContext().stop(getSelf());
		}

		@Override
		public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
			try {
				ByteBuf bb = (ByteBuf) msg;
				ByteBuffer data = ByteBuffer.allocate(bb.readableBytes());
				bb.readBytes(data);
				handler.tell(ChannelEvent.received(data), getSelf());
			} finally {
				ReferenceCountUtil.release(msg);
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
			handler.tell(ChannelEvent.exceptionCaught(cause), getSelf());
		}
	}
}
