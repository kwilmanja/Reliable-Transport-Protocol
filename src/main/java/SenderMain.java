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

//    String host = args[0];
//    int port = Integer.parseInt(args[1]);
//    SocketAddress address = new InetSocketAddress(InetAddress.getByName(host), port);
//
//    DatagramChannel dc = DatagramChannel.open();
//    dc.connect(address);

    Scanner sc = new Scanner(System.in);
    StringBuilder sb = new StringBuilder();
    ArrayList<byte[]> packets = new ArrayList<>();
    int packetLength = 1024;

    while(true){

      byte[] packet = new byte[packetLength];
      int bytesRead;

      while((bytesRead = System.in.read(packet)) != -1){
//        offset += bytesRead;
        System.out.println("pakcket: " + new String(packet, java.nio.charset.StandardCharsets.UTF_8));
        packet = new byte[packetLength];
      }
//
//      for(byte[] data : packets){
//        System.out.println(new String(data, java.nio.charset.StandardCharsets.UTF_8));
//      }

//      while(sc.hasNext()) {
//        sb.append(sc.next());
//        byte[] data = sb.toString().getBytes(StandardCharsets.UTF_8);
//
//        int offset = 0;
//        while (offset < data.length) {
//          int packetLength = Math.min(packetSize, data.length - offset);
//          byte[] packet = new byte[packetLength];
//          System.arraycopy(data, offset, packet, 0, packetLength);
//          packets.add(packet);
//          offset += packetLength;
//        }
//
//
//        for(byte[] packet : packets){
//          System.out.println(new String(packet, java.nio.charset.StandardCharsets.UTF_8));
//        }
//
//      }
    }

//    while(true) {
//      List<byte[]> packets = convertStdinToPackets(1024);
//
//      // Process or store the packets as needed
//      for (byte[] packet : packets) {
//        System.out.println(packet + "::" + new String(packet, java.nio.charset.StandardCharsets.UTF_8));
//      }
//    }
  }

//    public static List<byte[]> convertStdinToPackets(int packetSize) {
//      List<byte[]> packets = new ArrayList<>();
//      byte[] buffer = new byte[packetSize];
//      int bytesRead;
//
//      try {
//        while ((bytesRead = System.in.read(buffer)) != -1) {
//          byte[] packet = new byte[bytesRead];
//          System.arraycopy(buffer, 0, packet, 0, bytesRead);
//          packets.add(packet);
//        }
//      } catch (IOException e) {
//        e.printStackTrace();
//      }
//
//      return packets;
//    }
  }

