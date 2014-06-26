package local.net;


import org.zoolu.net.*;
import org.zoolu.sip.provider.SipProvider;
import org.zoolu.sip.message.Message;


/** KeepAliveUdp è un Thread, che serve a mantenere la connessione verso un nodo SIP.
 * che può essere un server proxy o uno UA remoto.
 * <p> Periodicamente vengono inviati dei keep-alive tokens per aggiornare la sessione.
 */
public class KeepAliveSip extends KeepAliveUdp
{
   /** SipProvider */
   SipProvider sip_provider;
   
   /** Sip message */
   Message message=null;


   /** Crea un nuovo demone SIP KeepAliveSip*/
   public KeepAliveSip(SipProvider sip_provider, SocketAddress target, long delta_time)
   {  super(target,delta_time);
      init(sip_provider,null);
      start();
   }
   
   /** Crea un nuovo demone SIP KeepAliveSip*/
   public KeepAliveSip(SipProvider sip_provider, SocketAddress target, Message message, long delta_time)
   {  super(target,delta_time);
      init(sip_provider,message);
      start();
   }
   

   /** Inizializza il KeepAliveSip in SIP mode*/
   private void init(SipProvider sip_provider, Message message)
   {  this.sip_provider=sip_provider;
      if (message==null)
      {  message=new Message("\r\n");
      }
      this.message=message;
   }


   /** Invia un pacchetto keep-alive. */
   public void sendToken() throws java.io.IOException
   {  // do send?
      if (!stop && target!=null && sip_provider!=null)
      {  sip_provider.sendMessage(message,sip_provider.getDefaultTransport(),target.getAddress().toString(),target.getPort(),127);
      }
   }


   /** Main thread. */
   public void run()
   {  super.run();
      sip_provider=null;
   }
   

   /** Restituisce una rappresentazione in stringa dell'ogetto. */
   public String toString()
   {  String str=null;
      if (sip_provider!=null)
      {  str="sip:"+sip_provider.getViaAddress()+":"+sip_provider.getPort()+"-->"+target.toString();
      }
      return str+" ("+delta_time+"ms)"; 
   }
    
}