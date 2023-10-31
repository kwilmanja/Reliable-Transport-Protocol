import java.nio.charset.StandardCharsets;
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
    receiver.run();
  }

}

class Receiver{
  private DatagramChannel dc;
  private Map<Integer, ReceiverPacket> receivedData;
  private int printIndex;
  private int windowSize;

  public Receiver() throws IOException {
    dc = DatagramChannel.open();
    dc.bind(new InetSocketAddress(3000));
    System.err.println("Bound to port 3000");
    receivedData = new HashMap<>();
    printIndex = 0;
    windowSize = 2; // Set desired window size
  }

  public void run() {
    while (true) {
      try {

        //Read data and create packet
        ByteBuffer buffer = ByteBuffer.allocate(2000);
        if(dc.isConnected()){
          dc.receive(buffer);
        } else{
          dc.connect(dc.receive(buffer));
        }
        buffer.flip();
        byte[] packetData = new byte[buffer.remaining()];
        buffer.get(packetData);
        ReceiverPacket packet = ReceiverPacket.parse(packetData);


        if (packet != null) {
          this.receivedData.put(packet.getSeq(), packet);
          this.sendAck(packet);

          //Print any packets we can:
          while(receivedData.containsKey(this.printIndex)){
            ReceiverPacket toPrint = receivedData.get(printIndex);
            System.out.print(toPrint.getData());
            this.printIndex++;
          }
        }

      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private void sendAck(ReceiverPacket packet) throws IOException {
    this.toString().getBytes(StandardCharsets.UTF_8);
    String seq = String.format("%4s", packet.getSeq()).replace(" ", "0");
    String ack = seq + ".";
    ByteBuffer buffer = ByteBuffer.wrap(ack.getBytes(StandardCharsets.UTF_8));
    this.dc.write(buffer);
  }

}

class ReceiverPacket {

  private String data;
  private int seq;

  public ReceiverPacket(String data, int seq) {
    this.data = data;
    this.seq = seq;
  }

  public int getSeq() {
    return seq;
  }

  public String getData() {
    return data;
  }

  public static ReceiverPacket parse(byte[] packetData) {
//    if (packetData.length < 9) {
//      return null; // Invalid packet
//    }

    String dataString = new String(packetData, StandardCharsets.UTF_8);
    String[] dataSplit = dataString.split("\\.");

    int seq = Integer.parseInt(dataSplit[0]);
    int length = Integer.parseInt(dataSplit[1]);
    String data = dataSplit[2];

//    System.out.println(data);
//    byte dataLength = packetData[1];
//    byte[] data = Arrays.copyOfRange(packetData, 2, dataLength);
//    byte[] seqBytes = Arrays.copyOfRange(packetData, 0, 4);
//    int seq = ByteBuffer.wrap(seqBytes).getInt();
//    byte[] data = Arrays.copyOfRange(packetData, 4, packetData.length);

    return new ReceiverPacket(data, seq);
  }
}