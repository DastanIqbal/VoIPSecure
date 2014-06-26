package local.media;


import local.net.RtpPacket;
import local.net.RtpSocket;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.DatagramSocket;


/** RtpStreamSender è un generico stream di invio,
 * che riceve pacchetti da un InputStream li invia tramite RTP.
 */
public class RtpStreamSender extends Thread
{
   /** Debug mode. */
   //private static final boolean DEBUG=true;
   public static boolean DEBUG=false;
   
   /** InputStream */
   InputStream input_stream=null;
   
   /** RtpSocket */
   RtpSocket rtp_socket=null;
   
   /** Payload type */
   int p_type;
   
   /** Numero di frame per secondo. */
   long frame_rate;  

   /** Numero di bytes per frame. */
   int frame_size;

   /** Flag che indica se il socket è stato creato. */
   boolean socket_is_local=false;   

   /** Flag che indica se si lavoro sincronizzati con un orologio locale,
    * o se si agisce subordinatamente all'InputStream. */
   boolean do_sync=true;

   /** Valore di correzione di sincronizazzione, in millisecondi.
    * Viene acceletara la velocità di trasmissione rispetto al valore niminale,
    * in modo da compensare il ritardo dovuto alla latenza. */
   int sync_adj=0;

   /** Flag che indica se il Thread è in esecuzione.*/
   boolean running=false;   


   /** Costruisce un RtpStreamSender.
     * @param input_stream
     * @param do_sync Flag che indica se si lavoro sincronizzati con un orologio locale,
     * 				  o se si agisce subordinatamente all'InputStream.
     * @param payload_type
     * @param frame_rate Numero di frame per secondo.
     * @param frame_size Numero di bytes per frame.
     * @param src_socket socket usato per inviare i pacchetti
     * @param dest_addr indirizzo di destinazione
     * @param dest_port porta di destinazione */
   public RtpStreamSender(InputStream input_stream, boolean do_sync, int payload_type, long frame_rate, int frame_size, DatagramSocket src_socket, String dest_addr, int dest_port)
   {  init(input_stream,do_sync,payload_type,frame_rate,frame_size,src_socket,dest_addr,dest_port);
   }                

   /** Inizializza RTPStreamSender*/
   private void init(InputStream input_stream, boolean do_sync, int payload_type, long frame_rate, int frame_size, DatagramSocket src_socket, /*int src_port,*/ String dest_addr, int dest_port)
   {
      this.input_stream=input_stream;
      this.p_type=payload_type;
      this.frame_rate=frame_rate;
      this.frame_size=frame_size;
      this.do_sync=do_sync;
      try
      {  if (src_socket==null)
         {  //if (src_port>0) src_socket=new DatagramSocket(src_port); else
            src_socket=new DatagramSocket();
            socket_is_local=true;
         }
         rtp_socket=new RtpSocket(src_socket,InetAddress.getByName(dest_addr),dest_port);
      }
      catch (Exception e) {  e.printStackTrace();  }    
   }          


   /** Imposta il <i>valore di correzione di sincronizazzione</i> per compensare il ritardo di latenza */
   public void setSyncAdj(int millisecs)
   {  sync_adj=millisecs;
   }

   /** Verifica se il Thread è in esecuzione. */
   public boolean isRunning()
   {  return running;
   }

   /** Stop esecuzione*/
   public void halt()
   {  running=false;
   }

   /** Esecuzione in un nuovo Thread. */
   public void run()
   {
      if (rtp_socket==null || input_stream==null) return;
      //else
      
      byte[] buffer=new byte[frame_size+12];
      RtpPacket rtp_packet=new RtpPacket(buffer,0);
      rtp_packet.setPayloadType(p_type);      
      int seqn=0;
      long time=0;
      //long start_time=System.currentTimeMillis();
      long byte_rate=frame_rate*frame_size;
      
      running=true;
            
      if (DEBUG) println("Reading blocks of "+(buffer.length-12)+" bytes");

      try
      {  while (running)
         {
            //if (DEBUG) System.out.print("o");
            int num=input_stream.read(buffer,12,buffer.length-12);
      		//if (DEBUG) System.out.print("*");
            if (num>0)
            {  rtp_packet.setSequenceNumber(seqn++);
               rtp_packet.setTimestamp(time);
               rtp_packet.setPayloadLength(num);
               rtp_socket.send(rtp_packet);
               // update rtp timestamp (in milliseconds)
               long frame_time=(num*1000)/byte_rate;
               time+=frame_time;
               // wait fo next departure
               if (do_sync)
               {  // wait before next departure..
                  //long frame_time=start_time+time-System.currentTimeMillis();
                  // accellerate in order to compensate possible program latency.. ;)
                  frame_time-=sync_adj;
                  try {  Thread.sleep(frame_time);  } catch (Exception e) {}
               }
            }
            else
            if (num<0)
            {  running=false;
               if (DEBUG) println("Error reading from InputStream");
            }
         }
      }
      catch (Exception e) {  running=false;  e.printStackTrace();  }     

      //if (DEBUG) println("rtp time:  "+time);
      //if (DEBUG) println("real time: "+(System.currentTimeMillis()-start_time));

      // close RtpSocket and local DatagramSocket
      DatagramSocket socket=rtp_socket.getDatagramSocket();
      rtp_socket.close();
      if (socket_is_local && socket!=null) socket.close();

      // free all
      input_stream=null;
      rtp_socket=null;

      if (DEBUG) println("rtp sender terminated");
   }
   

   /** Debug output */
   private static void println(String str)
   {  System.out.println("RtpStreamSender: "+str);
   }

}