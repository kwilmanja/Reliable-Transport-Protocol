import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        //Read data and store packet
        Optional<ReceiverPacket> packetOpt = this.readDataPacket();
        if(packetOpt.isEmpty()){
          continue;
        }

        ReceiverPacket packet = packetOpt.get();

        //If we don't already have the packet:
        if(!receivedData.containsKey(packet.getSeq())){
          //Store the packet
          this.receivedData.put(packet.getSeq(), packet);
          //Print any packets we can:
          while(receivedData.containsKey(this.printIndex)){
            ReceiverPacket toPrint = receivedData.get(printIndex);
            System.out.print(toPrint.getData());
            this.printIndex++;
          }
          //Send Ack
        }
        this.sendAck(packet);

      } catch (IOException e) {
        e.printStackTrace();
      }
    }
  }

  private int findMaxSequenceNumber(){
    int max = Integer.MIN_VALUE; // Initialize max to the smallest possible integer

    for (int n : this.receivedData.keySet()) {
      if (n > max) {
        max = n;
      }
    }
    return max;
  }

  private Optional<ReceiverPacket> readDataPacket() throws IOException {
    ByteBuffer buffer = ByteBuffer.allocate(2000);
    if(dc.isConnected()){
      dc.receive(buffer);
    } else{
      dc.connect(dc.receive(buffer));
    }
    buffer.flip();
    byte[] packetData = new byte[buffer.remaining()];
    buffer.get(packetData);

    return ReceiverPacket.parse(packetData);
  }

  private void sendAck(ReceiverPacket packet) throws IOException {
    // this.toString().getBytes(StandardCharsets.UTF_8);
    StringBuilder ack = new StringBuilder();

    //Start with sequence number
    String seq = String.format("%4s", packet.getSeq()).replace(" ", "0");
    ack.append(seq).append(".");

    //Append missing Acks
    for(int i=this.printIndex; i<this.findMaxSequenceNumber(); i++){
      if(!this.receivedData.containsKey(i)){
        ack.append(String.format("%4s", i).replace(" ", "0")).append(".");
      }
    }

    //Send data
    ByteBuffer buffer = ByteBuffer.wrap(ack.toString().getBytes(StandardCharsets.UTF_8));
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

  public static Optional<ReceiverPacket> parse(byte[] packetData) {
    // Split the packet data
    String dataString = new String(packetData, StandardCharsets.UTF_8);
    String[] dataSplit = dataString.split("\\.");

    if(dataSplit.length < 3) {
      // Not enough parts in the packet, thus invalid
      return Optional.empty();
    }

    int seq = Integer.parseInt(dataSplit[0]);
    int length = Integer.parseInt(dataSplit[1]);
    String data = dataSplit[2];

    // Extract the received checksum from the packet
    byte[] receivedChecksumBytes = Arrays.copyOf(packetData, 4);
    int receivedChecksum = bytesToInt(receivedChecksumBytes);

    // Calculate the checksum of the data part
    byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
    int calculatedChecksum = calculateChecksum(dataBytes);

    // Compare the received checksum to the calculated checksum
    if(receivedChecksum == calculatedChecksum) {
      return Optional.of(new ReceiverPacket(data, seq));
    } else {
      // Checksums do not match, thus invalid
      return Optional.empty();
    }
  }

  private static int bytesToInt(byte[] bytes) {
    ByteBuffer buffer = ByteBuffer.wrap(bytes);
    return buffer.getInt();
  }

  private static int calculateChecksum(byte[] data) {
    int checksum = 0;
    for (byte b : data) {
      checksum += (int) b;
    }
    return checksum;
  }
}