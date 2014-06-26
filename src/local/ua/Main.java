package local.ua;


import org.zoolu.sip.address.*;
import org.zoolu.sip.provider.SipStack;
import org.zoolu.sip.provider.SipProvider;
import org.zoolu.net.SocketAddress;
import org.zoolu.tools.Log;
import org.zoolu.tools.LogLevel;

import java.io.*;

import local.security.SignFile;
import local.security.ToolsKeys;


/** Classe che gestisce da riga di comando lo User Agent(UA) basato su protocollo SIP.
 * 
 */
public class Main implements UserAgentListener, RegisterAgentListener
{           

   /** Event logger. */
   Log log;
   
   /** UserAgent */
   UserAgent ua;

   /** RegisterAgent */
   RegisterAgent ra;
   
   /** UserAgentProfile */
   UserAgentProfile user_profile;
         
   /** Standard input */
   BufferedReader stdin=null; 
         
   /** Standard output */
   PrintStream stdout=null; 

        
   /** Costruisce uno UserAgent e un RegisterAgent */
   public Main(SipProvider sip_provider, UserAgentProfile user_profile)
   {  log=sip_provider.getLog();
      this.user_profile=user_profile;

      ua=new UserAgent(sip_provider,user_profile,this);      
      ra=new RegisterAgent(sip_provider,user_profile.from_url,user_profile.contact_url,user_profile.username,user_profile.realm,user_profile.passwd,this);
 
    	  stdin=new BufferedReader(new InputStreamReader(System.in));  
    	  stdout=System.out;
      
      run();
   }

   /** Registrazione con il registar server.
    * @param expire_time tempo di scadenza in secondi */
   public void register(int expire_time)
   {  if (ra.isRegistering()) ra.halt();
      ra.register(expire_time);
   }

   /** Registrazione periodica con il registrar server.
    * @param expire_time tempo di scadenza in secondi
    * @param renew_time tempo di rinnovo in secondi
    * @param keepalive_time keep-alive packet rate in millisecondi */
   public void loopRegister(int expire_time, int renew_time, long keepalive_time)
   {  if (ra.isRegistering()) ra.halt();
      ra.loopRegister(expire_time,renew_time,keepalive_time);
   }

   /** Deregistrazione con il registrar server */
   public void unregister()
   {  if (ra.isRegistering()) ra.halt();
      ra.unregister();
   }

   /** Deregistrazione di tutti i contatti registrar server */
   public void unregisterall()
   {  if (ra.isRegistering()) ra.halt();
      ra.unregisterall();
   }

   /** Crea una nuova chiamata */
   public void call(String target_url)
   {  ua.hangup();
      ua.printLog("UAC: CALLING "+target_url);
//      if (!ua.user_profile.audio && !ua.user_profile.video) ua.printLog("ONLY SIGNALING, NO MEDIA");       
      ua.call(target_url);       
   } 
         
   /** Abilita la ricezione chiamate in entrata */
   public void listen()
   {  ua.printLog("UAS: WAITING FOR INCOMING CALL");
//      if (!ua.user_profile.audio && !ua.user_profile.video) ua.printLog("ONLY SIGNALING, NO MEDIA");       
      ua.listen(); 
      printOut("digit the callee's URL to make a call or press 'enter' to exit");
   } 


   /** Start UA */
   void run()
   {
      try
      {  
         if (user_profile.do_unregister_all)
         {  ua.printLog("UNREGISTER ALL contact URLs");
            unregisterall();
         } 

         if (user_profile.do_unregister)
         {  ua.printLog("UNREGISTER the contact URL");
            unregister();
         } 

         if (user_profile.do_register)
         {  ua.printLog("REGISTRATION");
            loopRegister(user_profile.expires,user_profile.expires/2,user_profile.keepalive_time);
         }
         listen();
         while (stdin!=null)
         {  String line=readLine();
            if (ua.statusIs(UserAgent.UA_INCOMING_CALL))
            {  if (line.toLowerCase().startsWith("n"))
               {  
         	   ua.hangup();
         	   printOut("digit the callee's URL to make a call or press 'enter' to exit");
               }
               else
               {  ua.accept();
               }
            }
            else
            if (ua.statusIs(UserAgent.UA_IDLE))
            {  if (line!=null && line.length()>0)
               {
         	   String Ks=ua.genSK(line);
         	   
         	   if(Ks!=null) {
             	   String KKs =  ToolsKeys.chiper("e","SK","pubRemote.asc",null);
             	   String KKKs = ua.sign("s", "SK.bpg", "secret.asc");
             	   //serve per contare i byte di SK.bpg per incapsularli nell'sdp 
             	   FileInputStream    in = new FileInputStream("SK.bpg.bpg");
             	   ua.addKeyAttr(KKKs,in.available());
             	   call(line);
         	   }
         	   else exit();
         	   
               }
               else
               {  exit();
               }
            }
            else
            if (ua.statusIs(UserAgent.UA_ONCALL))
            {  ua.hangup();
            }
         }
      }
      catch (Exception e)  {  e.printStackTrace(); System.exit(0);  }
   }


   /** Exit */
   public void exit()
   {  try {  Thread.sleep(1000);  } catch (Exception e) {}
      System.exit(0);
   }


   // ******************* UserAgent callback functions ******************

   /** Quando una nuova chiamate è in arrivo */
   public void onUaCallIncoming(UserAgent ua, NameAddress callee, NameAddress caller)
   {
	   printOut("incoming call from "+caller.toString());
       printOut("accept? [yes/no]");
   }
   /** Quando una chiamata in uscita sta squillando in remoto */
   public void onUaCallRinging(UserAgent ua)
   {  
   }
   /** Quando una chiamata in uscita è stata accettata */
   public void onUaCallAccepted(UserAgent ua)
   {
   }
   /** When a call has been trasferred */
   public void onUaCallTrasferred(UserAgent ua)
   {  
   }
   /** Quando una chiamata in arrivo è stata cancellata */
   public void onUaCallCancelled(UserAgent ua)
   {  listen();
   }
   /** Quando una chiamata in uscita è stata rifiutata o è scaduto il timeuot*/
   public void onUaCallFailed(UserAgent ua)
   {  listen();
   }
   /** Quando una chiamata è stata chiusa in locale o in remoto */
   public void onUaCallClosed(UserAgent ua)
   {  listen();     
   }


   // **************** RegisterAgent callback functions *****************

   /** Quando uno UA è stato registrato con successo. */
   public void onUaRegistrationSuccess(RegisterAgent ra, NameAddress target, NameAddress contact, String result)
   {  ua.printLog("Registration success: "+result,LogLevel.HIGH);
   }
   /** Quando uno UA ha fallito la registrazione. */
   public void onUaRegistrationFailure(RegisterAgent ra, NameAddress target, NameAddress contact, String result)
   {  ua.printLog("Registration failure: "+result,LogLevel.HIGH);
   }
   

   // ***************************** MAIN *****************************


   /** The main method. */
   public static void main(String[] args)
   {  System.out.println("RUN");
	  if(args.length==0){
		  System.out.println("No param in input \n");
          System.out.println("usage:\n   java Main [options]");
          System.out.println("   options:");
          System.out.println("   -h              this help");
          System.out.println("   -f <file>       specifies a configuration file");
//          System.out.println("   --username <name>   user name used for authentication(i.e user@siprovider)");
//          System.out.println("   --realm <realm>     realm used for authentication");
//          System.out.println("   --passwd <passwd>   passwd used for authentication");
          System.exit(0);
	  }
      String file=null;
      boolean opt_regist=false;
      boolean opt_unregist=false;
      boolean opt_unregist_all=false;
      int     opt_expires=-1;
      long    opt_keepalive_time=-1;
      boolean opt_audio=false;

      String opt_from_url=null;
      String opt_contact_url=null;
      String opt_username=null;
      String opt_realm=null;
      String opt_passwd=null;

      String opt_log_path=null;
      String opt_outbound_proxy=null;
      String opt_via_addr=SipProvider.AUTO_CONFIGURATION;
      int opt_host_port=SipStack.default_port;
      
      
      try
      {  
    	  
    	  
         for (int i=0; i<args.length; i++)
         {
            if (args[i].equals("-f") && args.length>(i+1))
            {  file=args[++i];
               continue;
            }
            if (args[i].equals("-g") && args.length>(i+1)) // registrate the contact url
            {  opt_regist=true;
               String time=args[++i];
               if (time.charAt(time.length()-1)=='h') opt_expires=Integer.parseInt(time.substring(0,time.length()-1))*3600;
               else opt_expires=Integer.parseInt(time);
               continue;
            }
            if (args[i].equals("-u")) // unregistrate the contact url
            {  opt_unregist=true;
               continue;
            }
            if (args[i].equals("--username") && args.length>(i+1)) // username
            {  opt_username=args[++i];
               continue;
            }
            if (args[i].equals("--realm") && args.length>(i+1)) // realm
            {  opt_realm=args[++i];
               continue;
            }
            if (args[i].equals("--passwd") && args.length>(i+1)) // passwd
            {  opt_passwd=args[++i];
               continue;
            }

            
            if (!args[i].equals("-h"))
               System.out.println("unrecognized param '"+args[i]+"'\n");
            
            System.out.println("usage:\n   java Main [options]");
            System.out.println("   options:");
            System.out.println("   -h              this help");
            System.out.println("   -f <file>       specifies a configuration file");
//            System.out.println("   --username <name>   user name used for authentication(i.e user@siprovider)");
//            System.out.println("   --realm <realm>     realm used for authentication");
//            System.out.println("   --passwd <passwd>   passwd used for authentication");
            System.exit(0);
         }
                     
         
         SipStack.init(file);
         if (opt_log_path!=null) SipStack.log_path=opt_log_path;
         SipProvider sip_provider;
         if (file!=null) sip_provider=new SipProvider(file); else sip_provider=new SipProvider(opt_via_addr,opt_host_port);
         if (opt_outbound_proxy!=null) sip_provider.setOutboundProxy(new SocketAddress(opt_outbound_proxy));
         UserAgentProfile user_profile=new UserAgentProfile(file);
         
         if (opt_regist) user_profile.do_register=true;
         if (opt_unregist) user_profile.do_unregister=true;
         if (opt_unregist_all) user_profile.do_unregister_all=true;
         if (opt_expires>0) user_profile.expires=opt_expires;
         if (opt_keepalive_time>=0) user_profile.keepalive_time=opt_keepalive_time;
         if (opt_audio) user_profile.audio=true;
         if (opt_from_url!=null) user_profile.from_url=opt_from_url;
         if (opt_contact_url!=null) user_profile.contact_url=opt_contact_url;
         if (opt_username!=null) user_profile.username=opt_username;
         if (opt_realm!=null) user_profile.realm=opt_realm;
         if (opt_passwd!=null) user_profile.passwd=opt_passwd;

         new Main(sip_provider,user_profile);
      }
      catch (Exception e)  {  e.printStackTrace(); System.exit(0);  }
   }    
   

   // ****************************** Logs *****************************

   /** Legge una nuova linea dallo standard di input. */
   protected String readLine()
   {  try { if (stdin!=null) return stdin.readLine(); } catch (IOException e) {}
      return null;
   }

   /** Stampa a standard output. */
   protected void printOut(String str)
   {  if (stdout!=null) System.out.println(str);
   }

   /** Aggiunge una nuova stringa al Log. */
   void printLog(String str)
   {  printLog(str,LogLevel.HIGH);
   }

   /** Aggiunge una nuova stringa al Log. */
   void printLog(String str, int level)
   {  if (log!=null) log.println("CommandLineUA: "+str,level+SipStack.LOG_LEVEL_UA);  
   }

   /** Aggiunge una messaggio di Eccezione al Log */
   void printException(Exception e,int level)
   {  if (log!=null) log.printException(e,level+SipStack.LOG_LEVEL_UA);
   }

}
