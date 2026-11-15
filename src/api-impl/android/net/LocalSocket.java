package android.net;

import java.io.Closeable;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.net.SocketOptions;
import java.net.StandardSocketOptions;
import java.net.UnixDomainSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/* Public surface and state handling follow AOSP frameworks/base core/java/android/net/LocalSocket.java
 * (including its UnsupportedOperationException members). AOSP's LocalSocketImpl is a JNI wrapper around
 * an AF_UNIX fd; here the JDK's AF_UNIX SocketChannel does the work, so no fd is exposed. Differences:
 * only SOCKET_STREAM, no abstract namespace (emulated by files in a private directory), no fd passing,
 * no SO_PEERCRED, SO_TIMEOUT is stored only. */
public class LocalSocket implements Closeable {

	public static final int SOCKET_DGRAM = 1;
	public static final int SOCKET_STREAM = 2;
	public static final int SOCKET_SEQPACKET = 3;

	private final int sockType;
	private SocketChannel channel;
	private LocalSocketAddress localAddress;
	private boolean isBound;
	private boolean isConnected;
	private boolean closed;
	private int soTimeout;
	private int rcvBuf = -1;
	private int sndBuf = -1;
	private Path abstractFile;

	public LocalSocket() {
		this(SOCKET_STREAM);
	}

	public LocalSocket(int sockType) {
		this.sockType = sockType;
	}

	LocalSocket(SocketChannel accepted) {
		this.sockType = SOCKET_STREAM;
		this.channel = accepted;
		this.isBound = true;
		this.isConnected = true;
	}

	/* Maps an address to a filesystem path: RESERVED is /dev/socket/<name> as in AOSP's
	 * socket_local_client; ABSTRACT has no JDK equivalent. */
	static Path pathFor(LocalSocketAddress a, boolean create) throws IOException {
		switch (a.getNamespace()) {
		case FILESYSTEM:
			return Paths.get(a.getName());
		case RESERVED:
			return Paths.get("/dev/socket", a.getName());
		default:
			String base = System.getenv("XDG_RUNTIME_DIR");
			Path dir = Paths.get(base != null ? base : "/tmp", "atl-local-abstract-" + System.getProperty("user.name"));
			if (create)
				Files.createDirectories(dir);
			return dir.resolve(java.net.URLEncoder.encode(a.getName(), "UTF-8"));
		}
	}

	private synchronized void implCreateIfNeeded() throws IOException {
		if (channel == null) {
			if (sockType != SOCKET_STREAM)
				throw new IOException("Protocol not supported");
			channel = SocketChannel.open(java.net.StandardProtocolFamily.UNIX);
			if (rcvBuf > 0)
				channel.setOption(StandardSocketOptions.SO_RCVBUF, rcvBuf);
			if (sndBuf > 0)
				channel.setOption(StandardSocketOptions.SO_SNDBUF, sndBuf);
		}
	}

	@Override
	public String toString() {
		return super.toString() + " channel:" + channel;
	}

	public void connect(LocalSocketAddress endpoint) throws IOException {
		synchronized (this) {
			if (isConnected) {
				throw new IOException("already connected");
			}
			implCreateIfNeeded();
			try {
				channel.connect(UnixDomainSocketAddress.of(pathFor(endpoint, false)));
			} catch (java.net.SocketException e) {
				throw new java.net.ConnectException(e.getMessage());
			}
			isConnected = true;
			isBound = true;
		}
	}

	public void bind(LocalSocketAddress bindpoint) throws IOException {
		implCreateIfNeeded();
		synchronized (this) {
			if (isBound) {
				throw new IOException("already bound");
			}
			localAddress = bindpoint;
			Path p = pathFor(bindpoint, true);
			channel.bind(UnixDomainSocketAddress.of(p));
			if (bindpoint.getNamespace() == LocalSocketAddress.Namespace.ABSTRACT)
				abstractFile = p;
			isBound = true;
		}
	}

	public LocalSocketAddress getLocalSocketAddress() {
		return localAddress;
	}

	public InputStream getInputStream() throws IOException {
		implCreateIfNeeded();
		final SocketChannel ch = channel;
		return new InputStream() {
			@Override
			public int read() throws IOException {
				byte[] b = new byte[1];
				int n = read(b, 0, 1);
				return n < 0 ? -1 : b[0] & 0xff;
			}

			@Override
			public int read(byte[] b, int off, int len) throws IOException {
				if (len == 0)
					return 0;
				return ch.read(ByteBuffer.wrap(b, off, len));
			}

			@Override
			public void close() throws IOException {
				LocalSocket.this.close();
			}
		};
	}

	public OutputStream getOutputStream() throws IOException {
		implCreateIfNeeded();
		final SocketChannel ch = channel;
		return new OutputStream() {
			@Override
			public void write(int b) throws IOException {
				write(new byte[] {(byte) b}, 0, 1);
			}

			@Override
			public void write(byte[] b, int off, int len) throws IOException {
				ByteBuffer bb = ByteBuffer.wrap(b, off, len);
				while (bb.hasRemaining())
					ch.write(bb);
			}

			@Override
			public void close() throws IOException {
				LocalSocket.this.close();
			}
		};
	}

	@Override
	public void close() throws IOException {
		implCreateIfNeeded();
		synchronized (this) {
			closed = true;
			channel.close();
			if (abstractFile != null)
				Files.deleteIfExists(abstractFile);
		}
	}

	public void shutdownInput() throws IOException {
		implCreateIfNeeded();
		channel.shutdownInput();
	}

	public void shutdownOutput() throws IOException {
		implCreateIfNeeded();
		channel.shutdownOutput();
	}

	public void setReceiveBufferSize(int size) throws IOException {
		rcvBuf = size;
		if (channel != null)
			channel.setOption(StandardSocketOptions.SO_RCVBUF, size);
	}

	public int getReceiveBufferSize() throws IOException {
		implCreateIfNeeded();
		return channel.getOption(StandardSocketOptions.SO_RCVBUF);
	}

	public void setSoTimeout(int n) throws IOException {
		soTimeout = n;
	}

	public int getSoTimeout() throws IOException {
		return soTimeout;
	}

	public void setSendBufferSize(int n) throws IOException {
		sndBuf = n;
		if (channel != null)
			channel.setOption(StandardSocketOptions.SO_SNDBUF, n);
	}

	public int getSendBufferSize() throws IOException {
		implCreateIfNeeded();
		return channel.getOption(StandardSocketOptions.SO_SNDBUF);
	}

	public LocalSocketAddress getRemoteSocketAddress() {
		throw new UnsupportedOperationException();
	}

	public synchronized boolean isConnected() {
		return isConnected;
	}

	public boolean isClosed() {
		throw new UnsupportedOperationException();
	}

	public synchronized boolean isBound() {
		return isBound;
	}

	public boolean isOutputShutdown() {
		throw new UnsupportedOperationException();
	}

	public boolean isInputShutdown() {
		throw new UnsupportedOperationException();
	}

	public void connect(LocalSocketAddress endpoint, int timeout) throws IOException {
		throw new UnsupportedOperationException();
	}

	/* File descriptors cannot be passed through the JDK channel API. */
	public void setFileDescriptorsForSend(FileDescriptor[] fds) {
	}

	public FileDescriptor[] getAncillaryFileDescriptors() throws IOException {
		return null;
	}

	public FileDescriptor getFileDescriptor() {
		return null;
	}
}
