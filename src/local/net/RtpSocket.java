package local.net;


import java.net.InetAddress;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.io.IOException;


/** RtpSocket implementa un socket RTP per ricevere ed inviare pacchetti RTP. 
 * <p> Ad un RtpSocket è associato un DatagramSocket il quale 
 * è usato per inviare e ricevere RtpPacket.
 */
public class RtpSocket
{
   /** UDP socket */
   DatagramSocket socket;
        
   /** Indirizzo remoto */
   InetAddress r_addr;

   /** Porta remota */
   int r_port;
 
   /** Crea un nuovo RTP socket solo in ricezione. */
   public RtpSocket(DatagramSocket datagram_socket)
   {  socket=datagram_socket;
      r_addr=null;
      r_port=0;
   }

   /** Crea un nuovo RTP socket bidirezionale. */
   public RtpSocket(DatagramSocket datagram_socket, InetAddress remote_address, int remote_port)
   {  socket=datagram_socket;
      r_addr=remote_address;
      r_port=remote_port;
   }

   /** Restituisce il socket UDP*/
   public DatagramSocket getDatagramSocket()
   {  return socket;
   }

   /** Riceve un pacchetto RTP da questo socket. */
   public void receive(RtpPacket rtpp) throws IOException
   {  DatagramPacket datagram=new DatagramPacket(rtpp.packet,rtpp.packet.length);
      socket.receive(datagram);
      rtpp.packet_len=datagram.getLength();     
   }
   
   /** Invia un pacchetto RTP da questo socket*/
   public void send(RtpPacket rtpp) throws IOException
   {  DatagramPacket datagram=new DatagramPacket(rtpp.packet,rtpp.packet_len);
      datagram.setAddress(r_addr);
      datagram.setPort(r_port);
      socket.send(datagram);
   }

   /** Chiude questo socket. */
   public void close()
   {  //socket.close();
   }

}
