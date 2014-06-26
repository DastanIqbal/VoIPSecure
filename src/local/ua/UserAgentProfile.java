package local.ua;

import org.zoolu.sip.address.*;
import org.zoolu.sip.provider.SipStack;
import org.zoolu.sip.provider.SipProvider;
import org.zoolu.tools.Configure;
import org.zoolu.tools.Parser;


/** UserAgentProfile contiene la configurazione dello UA
  */
public class UserAgentProfile extends Configure
{

   public String from_url=null;
   /** Contact URL. */
   public String contact_url=null;
   /** Username. */
   public String username=null;
   /** Dominio. */
   public String realm=null;
   /** Password. */
   public String passwd=null;
   /** Flag che indica se è richiesta la registrazione con il registar server*/
   public boolean do_register=false;
   /** Flag che indica se è richiesta la DEregistrazione con il registar server del contatto*/
   public boolean do_unregister=false;
   /** Flag che indica se è richiesta la DEregistrazione con il registar server di tutti i contatti*/
   public boolean do_unregister_all=false;
   /** Tempo di scadenza. */
   public int expires=3600;

   /** Frequenza di pacchetti keep-alive da inviare al registar server[millisecondi].
    * <p> Se impostato a zero viene disabilitato l'invio di keep-alive. */
   public long keepalive_time=0;

   /** Flag che indica l'uso o meno dell'audio. */
   public boolean audio=false;
   /** Audio port */
   public int audio_port=21000;
   /** Audio avp */
   public int audio_avp=0;
   /** Codec Audio*/
   public String audio_codec="PCMU";
   /** Frequenza di ccampionamento Audio. */
   public int audio_sample_rate=8000;
   /** Dimensione campionamento . */
   public int audio_sample_size=1;
   /** Dimensione del frame audio. */
   public int audio_frame_size=160;
   
   
   /** Crea uno UserAgentProfile. */
   public UserAgentProfile()
   {  init();
   }

   /** Crea uno UserAgentProfile. */
   public UserAgentProfile(String file)
   {  // load configuration
      loadFile(file);
      // post-load manipulation     
      init();
   }

   /** Inizializza uno UserAgentProfile. */
   private void init()
   {  if (realm==null && contact_url!=null) realm=new NameAddress(contact_url).getAddress().getHost();
      if (username==null) username=(contact_url!=null)? new NameAddress(contact_url).getAddress().getUserName() : "user";
   }  

   
   /** Setting di contact_url e from_url con le informazione di trasporto. */
   public void initContactAddress(SipProvider sip_provider)
   {  // contact_url
      if (contact_url==null)
      {  contact_url="sip:"+username+"@"+sip_provider.getViaAddress();
         if (sip_provider.getPort()!=SipStack.default_port) contact_url+=":"+sip_provider.getPort();
         if (!sip_provider.getDefaultTransport().equals(SipProvider.PROTO_UDP)) contact_url+=";transport="+sip_provider.getDefaultTransport();
      }
      // from_url
      if (from_url==null) from_url=contact_url;
   }


    /** Analizza ogni singola linea del file di configurazione caricato. */
   protected void parseLine(String line)
   {  String attribute;
      Parser par;
      int index=line.indexOf("=");
      if (index>0) {  attribute=line.substring(0,index).trim(); par=new Parser(line,index+1);  }
      else {  attribute=line; par=new Parser("");  }
              
      if (attribute.equals("from_url"))       { from_url=par.getRemainingString().trim(); return; }
      if (attribute.equals("contact_url"))    { contact_url=par.getRemainingString().trim(); return; }
      if (attribute.equals("username"))       { username=par.getString(); return; } 
      if (attribute.equals("realm"))          { realm=par.getRemainingString().trim(); return; }
      if (attribute.equals("passwd"))         { passwd=par.getRemainingString().trim(); return; }
      
      if (attribute.equals("do_register"))    { do_register=(par.getString().toLowerCase().startsWith("y")); return; }
      if (attribute.equals("do_unregister"))  { do_unregister=(par.getString().toLowerCase().startsWith("y")); return; }
      if (attribute.equals("do_unregister_all")) { do_unregister_all=(par.getString().toLowerCase().startsWith("y")); return; }
      if (attribute.equals("expires"))        { expires=par.getInt(); return; } 
      if (attribute.equals("keepalive_time")) { keepalive_time=par.getInt(); return; } 

      if (attribute.equals("audio"))          { audio=(par.getString().toLowerCase().startsWith("y")); return; }
      if (attribute.equals("audio_port"))     { audio_port=par.getInt(); return; } 
      if (attribute.equals("audio_avp"))      { audio_avp=par.getInt(); return; } 
      if (attribute.equals("audio_codec"))    { audio_codec=par.getString(); return; } 
      if (attribute.equals("audio_sample_rate"))     { audio_sample_rate=par.getInt(); return; } 
      if (attribute.equals("audio_sample_size"))     { audio_sample_size=par.getInt(); return; } 
      if (attribute.equals("audio_frame_size"))      { audio_frame_size=par.getInt(); return; } 
   }
}
