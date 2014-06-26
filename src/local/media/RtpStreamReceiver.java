package local.media;


import local.net.RtpPacket;
import local.net.RtpSocket;

import java.io.*;
import java.net.DatagramSocket;


/** RtpStreamReceiver è un generico stream ricevitore, 
 * che riceve pacchetti da RTP e scrive in un OutputStream
 */
public class RtpStreamReceiver extends Thread
{

   /** Debug mode. */
   //private static final boolean DEBUG=true;
   public static boolean DEBUG=false;

   /** Dimensione del buffer di lettura. */
   public static final int BUFFER_SIZE=32768;

   /** Tempo massimo, di blocco, passato in attesa per leggere nuovi bytes [millisecondi]*/
   public static final int SO_TIMEOUT=200;

   /** OutputStream */
   OutputStream output_stream=null;

   /** RtpSocket */
   RtpSocket rtp_socket=null;

   /** Flag che indica se il socket è stato creato. */
   boolean socket_is_local=false;

   /** Flag che indica se il Thread è run*/
   boolean running=false;


   /** Costruisce un RtpStreamReceiver.
    * @param output_stream 
    * @param local_port the porta locale di ricezione */
   public RtpStreamReceiver(OutputStream output_stream, int local_port)
   {  try
      {  DatagramSocket socket=new DatagramSocket(local_port);
         socket_is_local=true;
         init(output_stream,socket);
      }
      catch (Exception e) {  e.printStackTrace();  }
   }

   /** Costruisce un RtpStreamReceiver.
    * @param output_stream 
    * @param socket DatagramSocket locale ricevente */
   public RtpStreamReceiver(OutputStream output_stream, DatagramSocket socket)
   {  init(output_stream,socket);
   }

   /** Inizializa RtpStreamReceiver. */
   private void init(OutputStream output_stream, DatagramSocket socket)
   {  this.output_stream=output_stream;
      if (socket!=null) rtp_socket=new RtpSocket(socket);
   }


   /** Verifica se è in esecuzione. */
   public boolean isRunning()
   {  return running;
   }

   /** Stop esecuzione */
   public void halt()
   {  running=false;
   }

   /** Esecuzione in un nuovo Thread. */
   public void run()
   {
      if (rtp_socket==null)
      {  if (DEBUG) println("ERROR: RTP socket is null");
         return;
      }
      //else

      byte[] buffer=new byte[BUFFER_SIZE];
      RtpPacket rtp_packet=new RtpPacket(buffer,0);

      if (DEBUG) println("Reading blocks of max "+buffer.length+" bytes");

      //byte[] aux=new byte[BUFFER_SIZE];

      running=true;
      try
      {  rtp_socket.getDatagramSocket().setSoTimeout(SO_TIMEOUT);
         while (running)
         {  try
            {  // read a block of data from the rtp socket
               rtp_socket.receive(rtp_packet);
               //if (DEBUG) System.out.print(".");
               
               // write this block to the output_stream (only if still running..)
               if (running) output_stream.write(rtp_packet.getPacket(), rtp_packet.getHeaderLength(), rtp_packet.getPayloadLength());
            }
            catch (java.io.InterruptedIOException e) { }
         }
      }
      catch (Exception e) {  running=false;  e.printStackTrace();  }

      // close RtpSocket and local DatagramSocket
      DatagramSocket socket=rtp_socket.getDatagramSocket();
      rtp_socket.close();
      if (socket_is_local && socket!=null) socket.close();
      
      // free all
      output_stream=null;
      rtp_socket=null;
      
      if (DEBUG) println("rtp receiver terminated");
   }


   /** Debug output */
   private static void println(String str)
   {  System.out.println("RtpStreamReceiver: "+str);
   }
   

//   public static int byte2int(byte b)
//   {  //return (b>=0)? b : -((b^0xFF)+1);
//      //return (b>=0)? b : b+0x100; 
//      return (b+0x100)%0x100;
//   }
//
//   public static int byte2int(byte b1, byte b2)
//   {  return (((b1+0x100)%0x100)<<8)+(b2+0x100)%0x100; 
//   }
}


