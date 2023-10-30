import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.sql.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;

public class SenderMain {

  //Main method to run the router simulator
  public static void main(String[] args) throws Exception {

    String host = args[0];
    int port = Integer.parseInt(args[1]);
    SocketAddress address = new InetSocketAddress(InetAddress.getByName(host), port);

    DatagramChannel dc = DatagramChannel.open();
    dc.connect(address);

    Sender s = new Sender(dc);
    s.run();
  }


}


class Sender{

  public DatagramChannel dc;
  public final ArrayList<Packet> packets = new ArrayList<>();
  public int packetLength;
  public int seq;

  public Sender(DatagramChannel dc){
    this.dc = dc;
    this.seq = 0;
    this.packetLength = 1024;
  }

  public void run(){

    Thread inputThread = new Thread(() -> {
      try {
        //Initialization:
        byte[] data = new byte[packetLength];


        //Create packets from the input
        while (true) {
          int count;
          while ((count = System.in.read(data)) != -1) {
            Packet p = new Packet(data, seq, count);
            packets.add(p);
            data = new byte[packetLength];
          }
        }
      } catch (Exception e) {
        e.printStackTrace();
      }

    });

    Thread outputThread = new Thread(() -> {
      while (true) {

        if (packets.isEmpty()) {
          try {
            Thread.sleep(100); // Wait if there's no data
          } catch (InterruptedException e) {
            e.printStackTrace();
          }
        }

        synchronized (packets) {
          for(Packet p : packets){
            System.out.println("Sent: " + p.toString());
          }
          packets.clear();
        }
      }
    });


    inputThread.start();
    outputThread.start();

    try {
      inputThread.join();
      outputThread.join();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

}

class SendPacketThread extends Thread{

  public SendPacketThread(){

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


  public byte[] toBytes(){
    byte[] result = new byte[data.length+2];
    result[0] = (byte) this.seq;
    result[1] = (byte) this.count;
    for(int i=0; i<this.data.length; i++){
      result[2+i] = this.data[i];
    }
    return result;
  }


  public String toString(){
    String text = new String(this.data, java.nio.charset.StandardCharsets.UTF_8);
//    int maskedSeq = this.seq & 0xFF;
//    String binary = String.format("%8s", Integer.toBinaryString(maskedSeq)).replace(' ', '0');
//    int maskedCount = this.count & 0xFF;
//    String length = String.format("%8s", Integer.toBinaryString(maskedCount)).replace(' ', '0');
//    return "Packet: -"+binary+"-"+length+"-"+text+"-";
      return "Packet: -"+ (byte) this.seq+"-"+ (byte) this.count +"-"+text+"-";

  }

}

