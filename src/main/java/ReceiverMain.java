import java.util.Arrays;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.HashMap;
import java.util.Map;

public class ReceiverMain {

  //Main method to run the router simulator
  public static void main(String[] args) throws Exception {
    Receiver receiver = new Receiver();
    receiver.receiveAndPrintData();
  }

}

class Receiver{
  private DatagramChannel datagramChannel;
  private Map<Integer, byte[]> receivedData;
  private int expectedSeq;
  private int windowSize;

  public Receiver() throws IOException {
    datagramChannel = DatagramChannel.open();
    datagramChannel.bind(new InetSocketAddress(0));
    receivedData = new HashMap<>();
    expectedSeq = 0;
    windowSize = 2; // Set desired window size
  }

  public void receiveAndPrintData() {
    ByteBuffer buffer = ByteBuffer.allocate(1500);
    while (true) {
      try {
        datagramChannel.receive(buffer);
        buffer.flip();

        byte[] packetData = new byte[buffer.remaining()];
        buffer.get(packetData);

        ReceiverPacket packet = ReceiverPacket.parse(packetData);

        if (packet != null) {
          int packetSeq = packet.getSeq();

          // Check if the received packet is within the expected sequence number
          if (packetSeq >= expectedSeq) {
            // Check if the packet is not a duplicate
            if (packetSeq == expectedSeq && !receivedData.containsKey(packetSeq)) {
              // Print the data to stdout
              System.out.print(packet.getDataString());

              // Store the received data
              receivedData.put(expectedSeq, packet.getData());

              // Update the eggspected sequence number
              expectedSeq++;

              // Check for addition consecutive packets in the buffer
              while (receivedData.containsKey(expectedSeq)) {
                byte[] nextPacketData = receivedData.get(expectedSeq);
                System.out.print(new String(nextPacketData));

                // Remove processed data from storage
                receivedData.remove(expectedSeq);
                expectedSeq++;
              }
            }
          }
        }
        buffer.clear();
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

}

class ReceiverPacket {

  private byte[] data;
  private int seq;

  public ReceiverPacket(byte[] data, int seq) {
    this.data = data;
    this.seq = seq;
  }

  public int getSeq() {
    return seq;
  }

  public byte[] getData() {
    return data;
  }

  public String getDataString() {
    return new String(data);
  }

  public static ReceiverPacket parse(byte[] packetData) {
    if (packetData.length < 9) {
      return null; // Invalid packet
    }

    byte[] seqBytes = Arrays.copyOfRange(packetData, 0, 4);
    int seq = ByteBuffer.wrap(seqBytes).getInt();
    byte[] data = Arrays.copyOfRange(packetData, 4, packetData.length);

    return new ReceiverPacket(data, seq);
  }
}