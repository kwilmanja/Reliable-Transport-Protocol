import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;

//Class for sending/receiving JSON messages over a DatagramChannel
class NetUtil {

  //Receive the next packet on the DatagramChannel
  public static JSONObject receiveMessage(DatagramChannel dc) throws IOException, JSONException {
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    buffer.clear();

    SocketAddress senderAddress = dc.receive(buffer);

    buffer.flip();
    byte[] data = new byte[buffer.limit()];
    buffer.get(data);

    String messageStr = new String(data, StandardCharsets.UTF_8);
    System.out.println("Received from " + senderAddress + ": " + messageStr);
    return new JSONObject(messageStr);
  }

  //Send the JSONObject over the DatagramChannel
  public static void sendMessage(JSONObject msg, DatagramChannel dc) throws IOException {
    byte[] messageBytes = msg.toString().getBytes(StandardCharsets.UTF_8);
    ByteBuffer buffer = ByteBuffer.wrap(messageBytes);
    dc.write(buffer);
  }

}