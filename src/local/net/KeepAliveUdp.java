package local.net;


import org.zoolu.net.*;


/** KeepAliveUdp è un Thread, che serve a mantenere la connessione verso un nodo target.
 * che può essere un server proxy o uno UA remoto.
 * <p> Periodicamente vengono inviati dei keep-alive tokens per aggiornare la sessione.
 */

public class KeepAliveUdp extends Thread
{
	/** Indirizzo del socket di destinazione. */
   protected SocketAddress target;

   /** Tempo tra 2 keep-alive tokens [millisecondi]. */
   protected long delta_time;

   /** UdpSocket */
   UdpSocket udp_socket;

   /** Udp packet */
   UdpPacket udp_packet=null;

   /** Scadenza [millisecondi]. */
   long expire=0; 

   /** Flag che indica se il Thread è in esecuzione. */
   boolean stop=false;


   /** Crea un nuovo demone KeepAliveUdp. */
   protected KeepAliveUdp(SocketAddress target, long delta_time)
   {  this.target=target;
      this.delta_time=delta_time;
   }

   /** Verifica se il Thread è in esecuzione. */
   public boolean isRunning()
   {  return !stop;
   }

   /** Imposta il tempo [millisecondi] tra 2 keep-alive tokens. */
   public void setDeltaTime(long delta_time)
   {  this.delta_time=delta_time;
   }

   /** Restituisce il tempo [millisecondi] tra 2 keep-alive tokens. */
   public long getDeltaTime()
   {  return delta_time;
   }

   /** Imposta il SocketAddress di destinazione. */
   public void setDestSoAddress(SocketAddress soaddr)
   {  target=soaddr;
      if (udp_packet!=null && target!=null)
      {  udp_packet.setIpAddress(target.getAddress());
         udp_packet.setPort(target.getPort());
      }
         
   }

   /** Restituisce il SocketAddress di destinazione. */
   public SocketAddress getDestSoAddress()
   {  return target;
   }

   
   /** Imposta il tempo di scadenza [millisecondi]. */
   public void setExpirationTime(long time)
   {  if (time==0) expire=0;
      else expire=System.currentTimeMillis()+time;
   }

   /** Stop invio di keep-alive tokens. */
   public void halt()
   {  stop=true;
   }

   /** Invia un pacchetto con il keep-alive token [null] */
   public void sendToken() throws java.io.IOException
   {  // do send?
      if (!stop && target!=null && udp_socket!=null)
      {  udp_socket.send(udp_packet);
      }
   }


   /** Main thread. */
   public void run()
   {  try   
      {  while(!stop)
         {  sendToken();
            //System.out.print(".");
            Thread.sleep(delta_time);
            if (expire>0 && System.currentTimeMillis()>expire) halt(); 
         }
      }
      catch (Exception e) { e.printStackTrace(); }
      //System.out.println("o");
      udp_socket=null;
   }
   

   /** Restituisce una rappresentazione in stringa dell'ogetto. */
   public String toString()
   {  String str=null;
      if (udp_socket!=null)
      {  str="udp:"+udp_socket.getLocalAddress()+":"+udp_socket.getLocalPort()+"-->"+target.toString();
      }
      return str+" ("+delta_time+"ms)"; 
   }
    
}