package android.net;

import java.io.Closeable;
import java.io.IOException;
import java.net.StandardProtocolFamily;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;

/* Follows AOSP LocalServerSocket.java; the listening fd is a JDK AF_UNIX ServerSocketChannel
 * (see LocalSocket for the differences; the FileDescriptor constructor and getFileDescriptor are not available). */
public class LocalServerSocket implements Closeable {

	private final ServerSocketChannel channel;
	private final LocalSocketAddress localAddress;
	private final Path abstractFile;

	public LocalServerSocket(String name) throws IOException {
		localAddress = new LocalSocketAddress(name);
		Path p = LocalSocket.pathFor(localAddress, true);
		channel = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
		try {
			channel.bind(UnixDomainSocketAddress.of(p), 50);
		} catch (IOException e) {
			channel.close();
			throw e;
		}
		abstractFile = p;
	}

	public LocalSocketAddress getLocalSocketAddress() {
		return localAddress;
	}

	public LocalSocket accept() throws IOException {
		SocketChannel c = channel.accept();
		return new LocalSocket(c);
	}

	@Override
	public void close() throws IOException {
		channel.close();
		Files.deleteIfExists(abstractFile);
	}
}
