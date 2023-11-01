import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class SenderMain {

  //Main method to run the router simulator
  public static void main(String[] args) throws Exception {

    String host = args[0];
    int port = Integer.parseInt(args[1]);
    SocketAddress address = new InetSocketAddress(host, port);

    DatagramChannel dc = DatagramChannel.open();
    dc.connect(address);

    Sender s = new Sender(dc);
    s.run();
  }


}


class Sender{

  public DatagramChannel dc;
  public final Queue<Packet> packets = new ArrayDeque<>();
  public final List<Packet> activePackets = new ArrayList<>();
  //  public final List<Packet> nonAck = new ArrayList<>();
  public final ArrayList<Packet> latestAckLack = new ArrayList<>();
  public int windowSize;
  public int rtt;
  public int dataLength;

  public Sender(DatagramChannel dc){
    this.dc = dc;
    this.windowSize = 2;
    this.rtt = 1000;
    this.dataLength = 1000;
    this.latestAckLack.add(new Packet(0));
  }

  private int index(){
    int index = Integer.MAX_VALUE;
    if(!this.activePackets.isEmpty()){
      for(Packet p: this.activePackets){
        if(p.seq < index){
          index = p.seq;
        }
      }
    } else if(!this.packets.isEmpty()){
      index = this.packets.peek().seq;
    }

    if(index == Integer.MAX_VALUE){
      System.exit(0);
    }

    return index;
  }

  private byte[] scanInput(){
    //Scan Input:
    Scanner sc = new Scanner(System.in);
    StringBuilder input = new StringBuilder();
    while (sc.hasNextLine()) {
      String line = sc.nextLine();
      if (line.isEmpty()) {
        break;
      }
      input.append(line);
    }
    sc.close();
    return input.toString().getBytes(StandardCharsets.UTF_8);
  }

  private void buildPackets(){
    byte[] data = this.scanInput();
    int seq = 0;

    for(int i=0; i<data.length; i+=this.dataLength){
      int end = Math.min(data.length, i+this.dataLength);
      byte[] dataChunk = Arrays.copyOfRange(data, i, end);
      Packet p = new Packet(dataChunk, seq, this.dataLength);
      this.packets.add(p);
      seq++;
    }
  }

  public void fillWindowAndSend() throws IOException {
    //While there are still packets to be sent and the window is not full, add packets
    while(!packets.isEmpty() && this.activePackets.size() < this.windowSize){
      Packet p = this.packets.poll();
      this.sendPacket(p);
      activePackets.add(p);
    }
  }


  public void run() throws IOException {

    //Build Packets:
    this.buildPackets();

//    System.out.println(this.packets.size() + " packets built!");

    this.fillWindowAndSend();


    //Transfer Packets (Reliably!):
    while (!packets.isEmpty() || !activePackets.isEmpty()) {

      this.fillWindowAndSend();

      ExecutorService executor = Executors.newSingleThreadExecutor();
      Callable<Integer> callableTask = () -> {

        while(true){
          int i = this.index();
          Packet ackPacket = this.waitForNewAck();

          //Remove packet that has been acknowledged
          this.activePackets.remove(ackPacket);
          //Remove packets that are not lacking and between index...ackPacket
          for(int n=i; n<ackPacket.seq; n++){
            Packet p = new Packet(n);
            if(!this.latestAckLack.contains(p)){
              this.activePackets.remove(p);
            }
          }


//          //Testing:
//
//          for(Packet p : this.activePackets){
//            if(this.latestAckLack.contains(p)){
//              this.sendPacket(p);
//            }
//          }
//
//          //Testing:


          this.windowSize++;

          if(!this.activePackets.contains(new Packet(i))){
            return ackPacket.seq;
          }

        }

      };

      Future<Integer> future = executor.submit(callableTask);

      try {
        future.get(this.rtt * 2L, TimeUnit.MILLISECONDS);
      } catch (TimeoutException e) {
        System.out.println("Timeout! Did not receive Ack for " + this.index());
        this.sendLackingPackets();
        this.windowSize = Math.max(this.windowSize / 2, 3);
      } catch (InterruptedException | ExecutionException e) {
        e.printStackTrace();
      } finally {
        executor.shutdown();
      }

    }

  }

  public void sendLackingPackets() throws IOException {
    for(Packet p: this.activePackets){
      if(this.latestAckLack.contains(p) || p.seq == this.index()){
        this.sendPacket(p);
      }
    }
  }

  public void sendPacket(Packet p) throws IOException {
    System.out.println("Send: " + p.toString());
    ByteBuffer buffer = ByteBuffer.wrap(p.toBytes());
    this.dc.write(buffer);
  }

  public Packet waitForNewAck() throws IOException {

    //Read Data
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    buffer.clear();
    this.dc.receive(buffer);
    buffer.flip();
    byte[] incomingData = new byte[buffer.limit()];
    buffer.get(incomingData);

    //Handle String -> Packet
    String dataString = new String(incomingData, StandardCharsets.UTF_8);
    String[] dataSplit = dataString.split("\\.");
    int seqAck = Integer.parseInt(dataSplit[0]);
    Packet ackPacket = new Packet(seqAck);
    System.out.println("Received Ack for " + seqAck);
    if(this.activePackets.contains(ackPacket)){

      this.latestAckLack.clear();
      for(int i=1; i<dataSplit.length; i++){
        this.latestAckLack.add(new Packet(Integer.parseInt(dataSplit[i])));
      }

//      Tetsing:
      System.out.println("Latest Ack Lack: ");
      for(Packet p: this.latestAckLack){
        System.out.print(p.seq + " ");
      }

      return ackPacket;
    } else{
      return this.waitForNewAck();
    }
  }

}





class Packet{

  public byte[] data;
  public int seq;
  public int count;

  public Packet(byte[] data, int seq, int count){
    this.data = data;
    this.seq = seq;
    this.count = count;
  }

  public Packet(int seq){
    this.seq = seq;
  }


  public byte[] toBytes(){
    byte[] rawBytes = this.toString().getBytes(StandardCharsets.UTF_8);

    //ToDo: Error Checking! add something to the data to make it robust

    byte[] toSend = rawBytes;
    return toSend;
  }


  public String toString(){
    String text = new String(this.data, StandardCharsets.UTF_8);
    String seq = String.format("%4s", this.seq).replace(" ", "0");
    String length = String.format("%4s", this.count).replace(" ", "0");
//    int maskedSeq = this.seq & 0xFF;
//    String binarySeq = String.format("%8s", Integer.toBinaryString(maskedSeq)).replace(' ', '0');
//    int maskedCount = this.count & 0xFF;
//    String length = String.format("%8s", Integer.toBinaryString(maskedCount)).replace(' ', '0');
//    return "Packet: -"+binary+"-"+length+"-"+text+"-";
    return seq+"."+ length +"."+text;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    Packet other = (Packet) obj;
    return this.seq == other.seq;
  }

  @Override
  public int hashCode(){
    return Objects.hash(this.seq);
  }



}

