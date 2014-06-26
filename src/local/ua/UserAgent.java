package local.ua;

import local.media.AudioClipPlayer;

import org.bouncycastle.openpgp.PGPException;
import org.zoolu.sip.call.*;
import org.zoolu.sip.address.*;
import org.zoolu.sip.provider.SipStack;
import org.zoolu.sip.provider.SipProvider;
import org.zoolu.sip.message.*;
import org.zoolu.sdp.*;
import org.zoolu.tools.Log;
import org.zoolu.tools.LogLevel;
import org.zoolu.tools.Parser;
import org.zoolu.tools.Archive;

import local.security.*;

import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import java.io.*;


/** User Agent (UA).
 */
public class UserAgent extends CallListenerAdapter
{
		/** Event logger. */
	   Log log;

	   /** UserAgentProfile */
	   protected UserAgentProfile user_profile;

	   /** SipProvider */
	   protected SipProvider sip_provider;

	   /** Call */
	   protected ExtendedCall call;
	   
	   /** Public Key */
	   private Keys KEYS;
	   
	   /** Audio application */
	   protected MediaLauncher audio_app=null;
	   
	   /** Local sdp */
	   protected String local_session=null;

	   /** UserAgent listener */
	   protected UserAgentListener listener=null;

	   /** Media file path */
	   final String MEDIA_PATH="media";

	   /** On wav file */
	   final String CLIP_ON=MEDIA_PATH+"on.wav";
	   /** Off wav file */
	   final String CLIP_OFF=MEDIA_PATH+"off.wav";
	   /** Ring wav file */
	   final String CLIP_RING=MEDIA_PATH+"ring.wav";

	   /** Ring sound */
	   AudioClipPlayer clip_ring;
	   /** On sound */
	   AudioClipPlayer clip_on;
	   /** Off sound */
	   AudioClipPlayer clip_off;
	   
	   
	   // *********************** Startup Configuration *********************** 
	   
	   /** UA_IDLE=0 */
	   static final String UA_IDLE="IDLE";
	   /** UA_INCOMING_CALL=1 */
	   static final String UA_INCOMING_CALL="INCOMING_CALL";
	   /** UA_OUTGOING_CALL=2 */
	   static final String UA_OUTGOING_CALL="OUTGOING_CALL";
	   /** UA_ONCALL=3 */
	   static final String UA_ONCALL="ONCALL";
	   
	   /** Call state mantiene informazioni sullo stato della chiamata.
	     * <P>UA_IDLE=0, <BR>UA_INCOMING_CALL=1, <BR>UA_OUTGOING_CALL=2, <BR>UA_ONCALL=3 */
	   String call_state=UA_IDLE;
	   


   

	   /** Cambia lo stato della chiamata */
	   protected void changeStatus(String state)
	   {  call_state=state;
//	      printLog("state: "+call_state,LogLevel.MEDIUM); 
	   }
	   
	   /** Verifica lo stato della chiamata. */
	   protected boolean statusIs(String state)
	   {  return call_state.equals(state); 
	   }

	   /** Ritorna lo stato della chiamata */
	   protected String getStatus()
	   {  return call_state; 
	   }
	   
	   /** Abilita Audio */
	   public void setAudio(boolean enable)
	   {  user_profile.audio=enable;
	   }
	   
	   /** Ritorna il SDP locale. */
	   public String getSessionDescriptor()
	   {  return local_session;
	   }   

	   /** Modifica il SDP locale */
	   public void setSessionDescriptor(String sdp)
	   {  local_session=sdp;
	   }
	   
	   /** Genera Chiave Simmetrica */
	   public String genSK(String called) {
		   printLog("GEN Simmetric Key...");
		   return KEYS.genSK(called);
		}
	   /** firma il file passato come argomento e lo restituisce sottoforma di stringa
	 * @throws Exception */
	   public String sign(String mode, String file, String key) throws Exception {
			return KEYS.sign(mode,file,key);
		}
	   
	   /** Inizializza il SDP locale. */
	   public void initSessionDescriptor() {
		   SessionDescriptor sdp=new SessionDescriptor(user_profile.from_url,sip_provider.getViaAddress());
	      local_session=sdp.toString();	      
	   }
	   
	   /** Aggiunge media nel SDP con relativi attributi. */
	   public void addMediaDescriptor(String media, int port, int avp, String codec, int rate)
	   {  if (local_session==null) initSessionDescriptor();
	      SessionDescriptor sdp=new SessionDescriptor(local_session);
	      String attr_param=String.valueOf(avp);
	      if (codec!=null) attr_param+=" "+codec+"/"+rate;
	      sdp.addMedia(new MediaField(media,port,0,"RTP/AVP",String.valueOf(avp)),new AttributeField("rtpmap",attr_param));
	      
	      local_session=sdp.toString();
	   }
	   
	   /** Aggiunge il campo key-mgmt nel SDP con la chiave simmetrica <b> cifrata </b> come attributo*/
	   public void addKeyAttr(String KKs,int len) {
		   local_session=null;
		   initSessionDescriptor();
		   addMediaDescriptor("audio",user_profile.audio_port,user_profile.audio_avp,user_profile.audio_codec,user_profile.audio_sample_rate);
		   setSessionDescriptor(local_session+"a=key-mgmt:SK "+len+" [ "+KKs+" ] "+"\n");
			
		}


	   /** Costruisce uno UA */
	   public UserAgent(SipProvider sip_provider, UserAgentProfile user_profile, UserAgentListener listener)
	   {  this.sip_provider=sip_provider;
	      log=sip_provider.getLog();
	      this.listener=listener;
	      this.user_profile=user_profile;

	      user_profile.initContactAddress(sip_provider);
	      
	      // load sounds  
	          try {
	            this.clip_on = new AudioClipPlayer(Archive.getAudioInputStream(Archive.getFileURL("media/on.wav")), null);
	            this.clip_off = new AudioClipPlayer(Archive.getAudioInputStream(Archive.getFileURL("media/off.wav")), null);
	            this.clip_ring = new AudioClipPlayer(Archive.getAudioInputStream(Archive.getFileURL("media/ring.wav")), null);
	          }
	          catch (Exception localException) {
	            printException(localException, 1);
	          }     
	      if (user_profile.audio) {
	    	  local.media.AudioInput.initAudioLine();
	    	  local.media.AudioOutput.initAudioLine();
	      }
	      try {
	    	  File f = new File("pub.asc");
		      
		      if(!(f.exists())) {
		    	  KEYS = new Keys(user_profile.username+"@"+user_profile.realm,user_profile.passwd,false);
		    	  printLog("GEN Keys Pair...");
		      }
		      /* Se pub.asc esiste e l'ID non corrisponde con pub.asc
		       * fai prima un backup di pub.asc e poi crea una nuova chiave pubblica 
		       */
		      else if(f.exists() && !(ToolsKeys.matching(user_profile.username+"@"+user_profile.realm, new FileInputStream("pub.asc")))) {
		    	  //backup
		    	  Date d = new Date();
		    	  File renPub = new File("pub_"+d.getTime()+".asc");
		    	  if(!(f.renameTo(renPub))) {
		    		  throw new IOException("Makes a backup of keys.");
		    	  }
		    	  File secr =  new File("secret.asc");
		    	  if(secr.exists()) {
		    		  File renSecr = new File("secret_"+d.getTime()+".asc");
		    		  if(!(secr.renameTo(renSecr))) {
			    		  throw new IOException("Makes a backup of keys.");
			    	  }
		    	  }
		    	  KEYS = new Keys(user_profile.username+"@"+user_profile.realm,user_profile.passwd,false);
		    	  printLog("GEN Keys Pair...");
		      }
		      else KEYS = new Keys(user_profile.username+"@"+user_profile.realm,user_profile.passwd,true); //ricordarsi di gestire il cambio id , quindi il cambio di chiave pub.asc

		      // set local sdp
		      initSessionDescriptor();
		      if (user_profile.audio) addMediaDescriptor("audio",user_profile.audio_port,user_profile.audio_avp,user_profile.audio_codec,user_profile.audio_sample_rate); 
	      }
	      catch(IOException e) {
	    	  System.err.println(e);
	    	  e.printStackTrace();
	    	  System.exit(1);
	      }
	      catch(PGPException p) {
	    	  System.err.println(p+"\n Exception in matching ID and public key");
	    	  p.printStackTrace();
	    	  System.exit(1);
	      }
	          
	   }
	   
	  
	   /** Crea una nuova chiamata. */
	   public void call(String target_url)
	   {  changeStatus(UA_OUTGOING_CALL);
	      call=new ExtendedCall(sip_provider,user_profile.from_url,user_profile.contact_url,user_profile.username,user_profile.realm,user_profile.passwd,this);      
	      target_url=sip_provider.completeNameAddress(target_url).toString();
	      call.call(target_url,local_session);
	   }   

	   /** In attesa di una chiamata in entrata. */
	   public void listen()
	   {  changeStatus(UA_IDLE);
	      call=new ExtendedCall(sip_provider,user_profile.from_url,user_profile.contact_url,user_profile.username,user_profile.realm,user_profile.passwd,this);      
	      call.listen();  
	   } 

	   /** Termina una chiamata. */
	   public void hangup()
	   {  if (clip_ring!=null) clip_ring.stop();      
	      closeMediaApplication();
	      if (call!=null) call.hangup();
	      changeStatus(UA_IDLE);
	   } 

	   /** Accetta una chiamata in entrata. */
	   public void accept()
	   {  if (clip_ring!=null) clip_ring.stop();
	      if (call!=null) {
	    	  call.accept(local_session);
	      }
	   }
	   
	   /** Redirects an incoming call */
	   public void redirect(String redirection)
	   {  if (clip_ring!=null) clip_ring.stop();
	      if (call!=null) call.redirect(redirection);
	   }
	   
	   /** Lancia l'applicazione che gestisce l'audio. */
	   protected void launchMediaApplication()
	   {
	      // exit se Media Application è già running
	      if (audio_app!=null)
	      {  printLog("DEBUG: media application is already running",LogLevel.HIGH);
	         return;
	      }
	      SessionDescriptor local_sdp=new SessionDescriptor(call.getLocalSessionDescriptor());
//	      String local_media_address=(new Parser(local_sdp.getConnection().toString())).skipString().skipString().getString();
	      int local_audio_port=0;
	      // parse local sdp
	      for (Enumeration e=local_sdp.getMediaDescriptors().elements(); e.hasMoreElements(); )
	      {  MediaField media=((MediaDescriptor)e.nextElement()).getMedia();
	         if (media.getMedia().equals("audio")) 
	            local_audio_port=media.getPort();
	      }
	      // parse remote sdp
	      SessionDescriptor remote_sdp=new SessionDescriptor(call.getRemoteSessionDescriptor());
	      String remote_media_address=(new Parser(remote_sdp.getConnection().toString())).skipString().skipString().getString();
	      int remote_audio_port=0;                      
	      for (Enumeration e=remote_sdp.getMediaDescriptors().elements(); e.hasMoreElements(); )
	      {  MediaField media=((MediaDescriptor)e.nextElement()).getMedia();
	         if (media.getMedia().equals("audio")) 
	            remote_audio_port=media.getPort();
	      }
	      
	      if (user_profile.audio && local_audio_port!=0 && remote_audio_port!=0)
	      {
	    	  if (audio_app==null)
	          {  
	             String audio_in=null;
	             String audio_out=null;
	             audio_app=new JAudioLauncher(local_audio_port,remote_media_address,remote_audio_port,audio_in,audio_out,KEYS.getSecretKeySpec(),user_profile.audio_sample_rate,user_profile.audio_sample_size,user_profile.audio_frame_size,log);
	          }
	          audio_app.startMedia();
	      }
	   }
	   
	   /** Termina la Media Application  */
	   protected void closeMediaApplication()
	   {  if (audio_app!=null)
	      {  audio_app.stopMedia();
	         audio_app=null;
	      }
	   }
	   
	// ********************** Call callback functions **********************
	   
	   /** Fuzione di ritorno chiamata quando arriva un INVITE (chiamata in entrata). */
	   public void onCallIncoming(Call call, NameAddress callee, NameAddress caller, String sdp, Message invite) {
		   try {
			   printLog("onCallIncoming()",LogLevel.LOW);
			      if (call!=this.call) {  printLog("NOT the current call",LogLevel.LOW);  return;  }
			      printLog("INCOMING",LogLevel.HIGH);
			      //estrapolo la chiave pubblica nell'sdp e la salvo come byte
			      
			      if(KEYS.pubKeyGET(caller.getAddress().toString().substring(4))) {
			    	  
			    	  byte[] KKKs = ToolsKeys.sdp2SK(sdp);
				      //rigenero lachiave simmetrica comune
				      if(KKKs==null) {
				    	  printLog("Not secure call incoming...");
				    	  printLog("Refuse...");
				    	  call.refuse();
				    	  call.listen();
				    	  System.out.println("digit the callee's URL to make a call or press 'enter' to exit");
				      }
				      else {
				    	  printLog("reGEN Simmetric Key...");
					      KEYS.setSK(KKKs);
					      			      
					      changeStatus(UA_INCOMING_CALL);
					      call.ring();

					      // play "ring"
					      if (clip_ring!=null) clip_ring.loop();
					      if (listener!=null) listener.onUaCallIncoming(this,callee,caller);
				      }
			      }
			      else {
			    	  printLog("Not exist Public Key of caller");
			    	  call.refuse();
			    	  call.listen();
			    	  System.out.println("digit the callee's URL to make a call or press 'enter' to exit");
			    	  
			      }   
			      
		   }
		   catch (Exception e) {
			   System.err.println(e);
			   	e.printStackTrace();
			   	System.exit(1);
		   }
		  
	   }  

	   /** Funzione di ritorno chiamata quando arriva un Re-INNVITE. */
	   public void onCallModifying(Call call, String sdp, Message invite)
	   {  printLog("onCallModifying()",LogLevel.LOW);
	      if (call!=this.call) {  printLog("NOT the current call",LogLevel.LOW);  return;  }
	      printLog("RE-INVITE/MODIFY",LogLevel.HIGH);
	   }

	   /** Funzione di ritorno chiamata quando arriva uno squillo (180 RINGING). */
	   public void onCallRinging(Call call, Message resp)
	   {  printLog("onCallRinging()",LogLevel.LOW);
	   	  if (call!=this.call) {  printLog("NOT the current call",LogLevel.LOW);  return;  }
	      printLog("RINGING",LogLevel.HIGH);
	      // play "on"
	      if (clip_on!=null) clip_on.replay();
	      if (listener!=null) listener.onUaCallRinging(this);
	   }

	   /** Funzione di ritorno chiamata quando arriva un messaggio di accettazione chiamata (2xx call accepted). */
	   public void onCallAccepted(Call call, String sdp, Message resp)
	   {  printLog("onCallAccepted()",LogLevel.LOW);
	   	  if (call!=this.call ) {  printLog("NOT the current call",LogLevel.LOW);  return;  }
	      printLog("ACCEPTED/CALL",LogLevel.HIGH);
	      changeStatus(UA_ONCALL);
	      
	      // play "on"
	      if (clip_on!=null) clip_on.replay();
	      if (listener!=null) listener.onUaCallAccepted(this);

	      launchMediaApplication();
	   }

	   /** Funzione di ritorno chiamata quando arriva un ACK (call confirmed). */
	   public void onCallConfirmed(Call call, String sdp, Message ack)
	   {  printLog("onCallConfirmed()",LogLevel.LOW);
	      if (call!=this.call) {  printLog("NOT the current call",LogLevel.LOW);  return;  }
	      printLog("CONFIRMED/CALL",LogLevel.HIGH);
	      changeStatus(UA_ONCALL);
	      // play "on" 
	      if (clip_on!=null) clip_on.replay();
	      launchMediaApplication(); 
	   }

	   /** Funzione di ritorno chiamato quando arriva un messaggio di accettazione(2xx) di chiamata tramite RE-INVITE. */
	   public void onCallReInviteAccepted(Call call, String sdp, Message resp)
	   {  printLog("onCallReInviteAccepted()",LogLevel.LOW);
	      if (call!=this.call) {  printLog("NOT the current call",LogLevel.LOW);  return;  }
	      printLog("RE-INVITE-ACCEPTED/CALL",LogLevel.HIGH);
	   }

	   /** Funzione di ritorno chiamata quando arriva un messaggio di chiamata fallita(4xx) tramite RE-INVITE.*/
	   public void onCallReInviteRefused(Call call, String reason, Message resp)
	   {  printLog("onCallReInviteRefused()",LogLevel.LOW);
	      if (call!=this.call) {  printLog("NOT the current call",LogLevel.LOW);  return;  }
	      printLog("RE-INVITE-REFUSED ("+reason+")/CALL",LogLevel.HIGH);
	      if (listener!=null) listener.onUaCallFailed(this);
	   }


	   /** Funzione di ritorno chiamata quando arriva un messaggio di chiamata fallita (4xx call failure).*/
	   public void onCallRefused(Call call, String reason, Message resp)
	   {  printLog("onCallRefused()",LogLevel.LOW);
	      if (call!=this.call) {  printLog("NOT the current call",LogLevel.LOW);  return;  }
	      printLog("REFUSED ("+reason+")",LogLevel.HIGH);
	      changeStatus(UA_IDLE);
	      // play "off"
	      if (clip_off!=null) clip_off.replay();
	      if (listener!=null) listener.onUaCallFailed(this);
	   }

	   /** Funzione di ritorno chiamata quando arriva un 3xx(chimata reindirizzata).*/
	   public void onCallRedirection(Call call, String reason, Vector contact_list, Message resp)
	   {  printLog("onCallRedirection()",LogLevel.LOW);
	      if (call!=this.call) {  printLog("NOT the current call",LogLevel.LOW);  return;  }
	      printLog("REDIRECTION ("+reason+")",LogLevel.HIGH);
	      call.call(((String)contact_list.elementAt(0))); 
	   }

	   /** Funzione di ritorno chiamata quando arriva una richiesta di CANCEL. */
	   public void onCallCanceling(Call call, Message cancel)
	   {  printLog("onCallCanceling()",LogLevel.LOW);
	      if (call!=this.call) {  printLog("NOT the current call",LogLevel.LOW);  return;  }
	      printLog("CANCEL",LogLevel.HIGH);
	      changeStatus(UA_IDLE);
	      // stop ringing
	      if (clip_ring!=null) clip_ring.stop();
	      // play "off"
	      if (clip_off!=null) clip_off.replay();
	      if (listener!=null) listener.onUaCallCancelled(this);
	   }

	   /** Funzione di ritorno chiamata quando arriva una richiesta di BYE.*/
	   public void onCallClosing(Call call, Message bye)
	   {  printLog("onCallClosing()",LogLevel.LOW);
	   	  if (call!=this.call) {  printLog("NOT the current call",LogLevel.LOW);  return;  }

	   	  printLog("CLOSE",LogLevel.HIGH);
	      closeMediaApplication();
	      // play "off"
	      if (clip_off!=null) clip_off.replay();
	      if (listener!=null) listener.onUaCallClosed(this);
	      changeStatus(UA_IDLE);
	   }

	   /** Funzione di ritorno chiamata quando arriva una risposta dopo una richiesta di BYE.*/
	   public void onCallClosed(Call call, Message resp)
	   {  printLog("onCallClosed()",LogLevel.LOW);
	      if (call!=this.call) {  printLog("NOT the current call",LogLevel.LOW);  return;  }
	      printLog("CLOSE/OK",LogLevel.HIGH);
	      if (listener!=null) listener.onUaCallClosed(this);
	      changeStatus(UA_IDLE);
	   }

	   /** Funzione di ritorno chiamata quando un invite è scaduto. */
	   public void onCallTimeout(Call call)
	   {  printLog("onCallTimeout()",LogLevel.LOW);
	      if (call!=this.call) {  printLog("NOT the current call",LogLevel.LOW);  return;  }
	      printLog("NOT FOUND/TIMEOUT",LogLevel.HIGH);
	      changeStatus(UA_IDLE);
	      // play "off" 
	      if (clip_off!=null) clip_off.replay();
	      if (listener!=null) listener.onUaCallFailed(this);
	   }
	   
	   
	// ****************************** Logs *****************************

	   /** Aggiunge una nuova stringa al Log. */
	   void printLog(String str)
	   {  printLog(str,LogLevel.HIGH);
	   }

	   /** Aggiunge una nuova stringa al Log. */
	   void printLog(String str, int level)
	   {  if (log!=null) log.println("UA: "+str,level+SipStack.LOG_LEVEL_UA);  
	      if (level<=LogLevel.HIGH) System.out.println("UA: "+str);
	   }

	   /** Aggiunge una messaggio di Eccezione al Log */
	   void printException(Exception e,int level)
	   {  if (log!=null) log.printException(e,level+SipStack.LOG_LEVEL_UA);
	   }

}
