package local.ua;


import local.net.KeepAliveSip;
import org.zoolu.net.SocketAddress;
import org.zoolu.sip.address.*;
import org.zoolu.sip.provider.SipStack;
import org.zoolu.sip.provider.SipProvider;
import org.zoolu.sip.header.*;
import org.zoolu.sip.message.*;
import org.zoolu.sip.transaction.TransactionClient;
import org.zoolu.sip.transaction.TransactionClientListener;
import org.zoolu.sip.authentication.DigestAuthentication;
import org.zoolu.tools.Log;
import org.zoolu.tools.LogLevel;

import java.util.Vector;


/** Register User Agent.
 * Si registra un contatto con un registrar server.
 */
public class RegisterAgent implements Runnable, TransactionClientListener
{
	/** Massimo numero di registrazioni attese. */
   static final int MAX_ATTEMPTS=3;

   /** RegisterAgent listener */
   RegisterAgentListener listener;
   
   /** SipProvider */
   SipProvider sip_provider;

   /** URI dello user completo */
   NameAddress target;

   /** User name. */
   String username;

   /** Dominio. */
   String realm;

   /** Passwordd. */
   String passwd;

   /** Nonce per la prossima autenticazione. */
   String next_nonce;

   /** Qop per la prossima autenticazione. */
   String qop;

   /** Indirizzo dello user. */
   NameAddress contact; 

   /** Scadenza. */
   int expire_time;

   /** Rinnovo scadenza. */
   int renew_time;

   /** Flag per la continuazione della registrazione. */
   boolean loop;

   /** Flag esecuzione del thread. */
   boolean is_running;

   /** Event logger. */
   Log log;

   /** Numero di registrazioni attese */
   int attempts;
   
   /** KeepAliveSip daemon. */
   KeepAliveSip keep_alive;

   
   /** Crea un nuovo RegisterAgent */
   public RegisterAgent(SipProvider sip_provider, String target_url, String contact_url, RegisterAgentListener listener)
   {  init(sip_provider,target_url,contact_url,listener);
   }
   
   /** Crea un nuovo RegistreAgent con credenziali di autenticazione. */
   public RegisterAgent(SipProvider sip_provider, String target_url, String contact_url, String username, String realm, String passwd, RegisterAgentListener listener)
   {  init(sip_provider,target_url,contact_url,listener);
      // authentication
      this.username=username;
      this.realm=realm;
      this.passwd=passwd;
   }

   
   /** Inizializza un RegisterAgent. */
   private void init(SipProvider sip_provider, String target_url, String contact_url, RegisterAgentListener listener)
   {  this.listener=listener;
      this.sip_provider=sip_provider;
      this.log=sip_provider.getLog();
      this.target=new NameAddress(target_url);
      this.contact=new NameAddress(contact_url);
      this.expire_time=SipStack.default_expires;
      this.renew_time=0;
      this.is_running=false;
      this.keep_alive=null;
      // authentication
      this.username=null;
      this.realm=null;
      this.passwd=null;
      this.next_nonce=null;
      this.qop=null;
      this.attempts=0;
   }

   
   /** Verifica se la registrazione periodica è in esecuzione. */
   public boolean isRegistering()
   {  return is_running;
   }

   /** Registrazione con il registar server. */
   public void register()
   {  register(expire_time);
   }

   /** Registrazione con il registar server per <i>expire_time</i> secondi. */
   public void register(int expire_time)
   {  attempts=0;
      if (expire_time>0) this.expire_time=expire_time;
      Message req=MessageFactory.createRegisterRequest(sip_provider,target,target,contact);
      req.setExpiresHeader(new ExpiresHeader(String.valueOf(expire_time)));
      if (next_nonce!=null)
      {  AuthorizationHeader ah=new AuthorizationHeader("Digest");
//         SipURL target_url=target.getAddress();
         ah.addUsernameParam(username);
         ah.addRealmParam(realm);
         ah.addNonceParam(next_nonce);
         ah.addUriParam(req.getRequestLine().getAddress().toString());
         ah.addQopParam(qop);
         String response=(new DigestAuthentication(SipMethods.REGISTER,ah,null,passwd)).getResponse();
         ah.addResponseParam(response);
         req.setAuthorizationHeader(ah);
      }
      if (expire_time>0) printLog("Registering contact "+contact+" (it expires in "+expire_time+" secs)",LogLevel.HIGH);
      else printLog("Unregistering contact "+contact,LogLevel.HIGH);
      TransactionClient t=new TransactionClient(sip_provider,req,this);
      t.request(); 
   }

   /** deRegistrazione con il registar server.*/
   public void unregister()
   {  register(0);
   } 

   /** deRegistrazione di tutti i contatti con il registar server. */
   public void unregisterall()
   {  attempts=0;
//      NameAddress user=new NameAddress(target);
      Message req=MessageFactory.createRegisterRequest(sip_provider,target,target,null);
      req.setExpiresHeader(new ExpiresHeader(String.valueOf(0)));
      printLog("Unregistering all contacts",LogLevel.HIGH);
      TransactionClient t=new TransactionClient(sip_provider,req,this); 
      t.request(); 
   }

   /** Periodica registrazione con il registrar server.
    * @param expire_time tempo di scadenza in secondi
    * @param renew_time rinnovo tempo in secondi */
   public void loopRegister(int expire_time, int renew_time)
   {  this.expire_time=expire_time;
      this.renew_time=renew_time;
      loop=true;
      if (!is_running) (new Thread(this)).start();
   }
   
   /** Periodica registrazione con il registrar server.
    * @param expire_time tempo di scadenza in secondi.
    * @param renew_time rinnovo tempo in secondi.
    * @param keepalive_time keep-alive packet rate in millisecondi. */
   public void loopRegister(int expire_time, int renew_time, long keepalive_time)
   {  loopRegister(expire_time,renew_time);
      // keep-alive
      if (keepalive_time>0)
      {  SipURL target_url=target.getAddress();
         String target_host=target_url.getHost();
         int targe_port=target_url.getPort();
         if (targe_port<0) targe_port=SipStack.default_port;
         new KeepAliveSip(sip_provider,new SocketAddress(target_host,targe_port),null,keepalive_time);
      }
   }

   /** Ferma la registrazione periodica. */
   public void halt()
   {  if (is_running) loop=false;
      if (keep_alive!=null) keep_alive.halt();
   }


   /** RUN. */
   public void run()
   {  
      is_running=true;
      try
      {  while (loop)
         {  register();
            Thread.sleep(renew_time*1000);
         }
      }
      catch (Exception e) {  printException(e,LogLevel.HIGH);  }
      is_running=false;
   }

   
   // **************** Transaction callback functions *****************

   /** Funzione di ritorno chiamata quando unclient invia una risposta provvisoria(1xx). */
   public void onTransProvisionalResponse(TransactionClient transaction, Message resp)
   {  // solo implementazione..
   }

   /** Funzione di ritorno chiamata quando un client invia un risposta di success(2xx). */
   public void onTransSuccessResponse(TransactionClient transaction, Message resp)
   {  if (transaction.getTransactionMethod().equals(SipMethods.REGISTER))
      {  if (resp.hasAuthenticationInfoHeader())
         {  next_nonce=resp.getAuthenticationInfoHeader().getNextnonceParam();
         }
         StatusLine status=resp.getStatusLine();
         String result=status.getCode()+" "+status.getReason();
         
         // update the renew_time
         int expires=0;
         if (resp.hasExpiresHeader())
         {  expires=resp.getExpiresHeader().getDeltaSeconds();
         }
         else
         if (resp.hasContactHeader())
         {  Vector contacts=resp.getContacts().getHeaders();
            for (int i=0; i<contacts.size(); i++)
            {  int exp_i=(new ContactHeader((Header)contacts.elementAt(i))).getExpires();
               if (exp_i>0 && (expires==0 || exp_i<expires)) expires=exp_i;
            }    
         }
         if (expires>0 && expires<renew_time) renew_time=expires;
         
         printLog("Registration success: "+result,LogLevel.HIGH);
         if (listener!=null) listener.onUaRegistrationSuccess(this,target,contact,result);
      }
   }

   /** Funzione di ritorno chiamata quando un client invia un failure response. */
   public void onTransFailureResponse(TransactionClient transaction, Message resp)
   {  if (transaction.getTransactionMethod().equals(SipMethods.REGISTER))
      {  StatusLine status=resp.getStatusLine();
         int code=status.getCode();
         if (code==401 && attempts<MAX_ATTEMPTS && resp.hasWwwAuthenticateHeader() && resp.getWwwAuthenticateHeader().getRealmParam().equalsIgnoreCase(realm))
         {  attempts++;
            Message req=transaction.getRequestMessage();
            req.setCSeqHeader(req.getCSeqHeader().incSequenceNumber());
            WwwAuthenticateHeader wah=resp.getWwwAuthenticateHeader();
            String qop_options=wah.getQopOptionsParam();
            //printLog("DEBUG: qop-options: "+qop_options,LogLevel.MEDIUM);
            qop=(qop_options!=null)? "auth" : null;
            AuthorizationHeader ah=(new DigestAuthentication(SipMethods.REGISTER,req.getRequestLine().getAddress().toString(),wah,qop,null,username,passwd)).getAuthorizationHeader();
            req.setAuthorizationHeader(ah);
            TransactionClient t=new TransactionClient(sip_provider,req,this);
            t.request();
         }
         else
         {  String result=code+" "+status.getReason();
            printLog("Registration failure: "+result,LogLevel.HIGH);
            if (listener!=null) listener.onUaRegistrationFailure(this,target,contact,result);
         }
      }
   }

   /** Funzione di ritorno chiamata quando un scade il timeout. */
   public void onTransTimeout(TransactionClient transaction)
   {  if (transaction.getTransactionMethod().equals(SipMethods.REGISTER))
      {  printLog("Registration failure: No response from server.",LogLevel.HIGH);
         if (listener!=null) listener.onUaRegistrationFailure(this,target,contact,"Timeout");
      }
   }

   
   // ****************************** Logs *****************************

   /** Aggiunge una nuova stringa al Log. */
   void printLog(String str, int level)
   {  if (log!=null) log.println("RegisterAgent: "+str,level+SipStack.LOG_LEVEL_UA);  
   }

   /** Adds the Exception message to the default Log */
   void printException(Exception e,int level)
   {  if (log!=null) log.printException(e,level+SipStack.LOG_LEVEL_UA);
   }

}
