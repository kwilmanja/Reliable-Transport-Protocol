import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Objects;
import java.util.Queue;
import java.util.Scanner;
import java.util.Set;

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
  public final Set<Packet> activePackets = new HashSet<>();
  public int index;
  public int window = 3;
  public int dataLength = 1022;

  public Sender(DatagramChannel dc){
    this.dc = dc;
    this.index = 0;
  }

  public void run(){

//    Thread buildPackets = new Thread(() -> {
//      try {
//        //Initialization:
//        byte[] data = new byte[dataLength];
//        int seq = 0;
//
//        //Create packets from the input
//        while (true) {
//          int count;
//          while ((count = System.in.read(data)) != -1) {
//            System.out.println(count);
//            Packet p = new Packet(data, seq, count);
//            packets.add(p);
//            seq++;
//            data = new byte[dataLength];
//          }
//        }
//      } catch (Exception e) {
//        e.printStackTrace();
//      }
//
//    });

    Scanner sc = new Scanner(System.in);
    StringBuilder input = new StringBuilder();

    while (true) {
      int count;
      while ((count = System.in.read(data)) != -1) {
        System.out.println(count);
        Packet p = new Packet(data, seq, count);
        packets.add(p);
        seq++;
        data = new byte[dataLength];
      }
    }

    Thread outputThread = new Thread(() -> {

      while (true) {
        try{

        if (packets.isEmpty()) {
          try {
            Thread.sleep(100); // Wait if there's no data
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }

        if(!packets.isEmpty()){
          synchronized (packets) {

            //Add packets to sending set
            while(activePackets.size() < this.window
            && !packets.isEmpty()){
              activePackets.add(this.packets.poll());
            }


            //Send all packets in sending set
            for(Packet p: this.activePackets){
              System.out.println("Send: " + p.toString());
              ByteBuffer buffer = ByteBuffer.wrap(p.toBytes());
              this.dc.write(buffer);
            }

            //Wait for all packets to be received:
            while(!this.activePackets.isEmpty()){
              ByteBuffer buffer = ByteBuffer.allocate(1024);
              buffer.clear();
              this.dc.receive(buffer);
              buffer.flip();
              byte[] data = new byte[buffer.limit()];
              buffer.get(data);

              String dataString = new String(data, StandardCharsets.UTF_8);
              String[] dataSplit = dataString.split("-");
              int seqAck = Integer.parseInt(dataSplit[0]);
              Packet toRemove = new Packet(seqAck);
              System.out.println("Received Ack for " + seqAck);
              this.activePackets.remove(toRemove);
            }
          }
        }

        } catch(Exception e){
          e.printStackTrace();
        }

      }
    });

//    buildPackets.start();
    outputThread.start();

    try {
//      buildPackets.join();
      outputThread.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
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
//    byte[] result = new byte[data.length+2];
//    result[0] = (byte) this.seq;
//    result[1] = (byte) this.count;
//    for(int i=0; i<this.data.length; i++){
//      result[2+i] = this.data[i];
//    }
//    return result;
    return this.toString().getBytes(StandardCharsets.UTF_8);
  }


  public String toString(){
    String text = new String(this.data, java.nio.charset.StandardCharsets.UTF_8);
    String binarySeq = String.format("%4s", this.seq).replace(" ", "0");
    String binaryLength = String.format("%4s", this.count).replace(" ", "0");
//    int maskedSeq = this.seq & 0xFF;
//    String binarySeq = String.format("%8s", Integer.toBinaryString(maskedSeq)).replace(' ', '0');
//    int maskedCount = this.count & 0xFF;
//    String binaryLength = String.format("%8s", Integer.toBinaryString(maskedCount)).replace(' ', '0');
//    return "Packet: -"+binary+"-"+binaryLength+"-"+text+"-";
    return binarySeq+"-"+ binaryLength +"-"+text;
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

