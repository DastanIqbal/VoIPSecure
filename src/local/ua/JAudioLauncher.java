package local.ua;


import local.media.AudioInput;
import local.media.AudioOutput;
import local.media.RtpStreamSender;
import local.media.RtpStreamReceiver;
import org.zoolu.sip.provider.SipStack;
import org.zoolu.tools.Log;
import org.zoolu.tools.LogLevel;

import java.net.DatagramSocket;

import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;
import javax.sound.sampled.AudioFormat;


/** Audio launcher basato su javax.sound. */
public class JAudioLauncher implements MediaLauncher
{  
   /** Event logger. */
   Log log=null;

   /** Payload type, <i>0</i> è associato alla codifica PCMU */
   int payload_type=0;
   /** Sample rate [bytes] */
   int sample_rate=8000;
   /** Sample size [bytes] */
   int sample_size=1;
   /** Frame size [bytes] */
   int frame_size=500;
   /** Frame rate [frames per second] */
   int frame_rate=16; //=sample_rate/(frame_size/sample_size);
   AudioFormat.Encoding codec=AudioFormat.Encoding.ULAW;
   boolean signed=false; 
   boolean big_endian=false;

   

   DatagramSocket socket=null;
   RtpStreamSender sender=null;
   RtpStreamReceiver receiver=null;
   AudioInput audio_input=null;
   AudioOutput audio_output=null;
   

   /** Costruisce un audio launcher. */
   public JAudioLauncher(int local_port, String remote_addr, int remote_port, String audiofile_in, String audiofile_out, SecretKeySpec secretKey, int sample_rate, int sample_size, int frame_size, Log logger)
   {  log=logger;
      frame_rate=sample_rate/frame_size;
      try
      {  
    	  socket=new DatagramSocket(local_port);
        	 printLog("new audio sender to "+remote_addr+":"+remote_port,LogLevel.MEDIUM);
            //audio_input=new AudioInput();
            AudioFormat format=new AudioFormat(codec,sample_rate,8*sample_size,1,sample_size,sample_rate,big_endian);
            audio_input=new AudioInput(format);
            audio_output=new AudioOutput(format);

            CipherInputStream audio_inputC= local.security.CipherAudio.send(audio_input.getInputStream(),secretKey);
                        
            sender=new RtpStreamSender(audio_inputC,true,payload_type,frame_rate,frame_size,socket,remote_addr,remote_port);
            
            //viene accelerato di 2 millisecondi l'invio di pacchetti per far fronte alla latenza
            sender.setSyncAdj(2);
            
            CipherOutputStream audio_outputC = local.security.CipherAudio.receive(audio_output.getOuputStream(),secretKey);
            
            receiver=new RtpStreamReceiver(audio_outputC,socket);
      }
      catch (Exception e) {  printException(e,LogLevel.HIGH);  }
   }


   /** Starts media application */
   public boolean startMedia()
   {  printLog("starting java audio..",LogLevel.HIGH);

      if (sender!=null)
      {  printLog("start sending",LogLevel.LOW);
         sender.start();
         if (audio_input!=null) audio_input.play();
      }
      if (receiver!=null)
      {  printLog("start receiving",LogLevel.LOW);
         receiver.start();
         if (audio_output!=null) audio_output.play();
      }
      
      return true;      
   }


   /** Stops media application */
   public boolean stopMedia()
   {  printLog("halting java audio..",LogLevel.HIGH);    
      if (sender!=null)
      {  sender.halt(); sender=null;
         printLog("sender halted",LogLevel.LOW);
      }      
      if (audio_input!=null)
      {  audio_input.stop(); audio_output=null;
      }      
      if (receiver!=null)
      {  receiver.halt(); receiver=null;
         printLog("receiver halted",LogLevel.LOW);
      }      
      if (audio_output!=null)
      {  audio_output.stop(); audio_output=null;
      }
      try { Thread.sleep(RtpStreamReceiver.SO_TIMEOUT); } catch (Exception e) {}
      socket.close();
      return true;
   }



   // ****************************** Logs *****************************

   /** Aggiunge una nuova stringa al Log. */
   private void printLog(String str)
   {  printLog(str,LogLevel.HIGH);
   }

   /** Aggiunge una nuova stringa al Log. */
   private void printLog(String str, int level)
   {  if (log!=null) log.println("AudioLauncher: "+str,level+SipStack.LOG_LEVEL_UA);  
      if (level<=LogLevel.HIGH) System.out.println("AudioLauncher: "+str);
   }

   /** Aggiunge una messaggio di Eccezione al Log */
   void printException(Exception e,int level)
   {  if (log!=null) log.printException(e,level+SipStack.LOG_LEVEL_UA);
      if (level<=LogLevel.HIGH) e.printStackTrace();
   }

}