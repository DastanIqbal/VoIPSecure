package local.media;


import java.io.*;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;


/** Riproduzione di un file audio.
  */
public class AudioClipPlayer implements LineListener 
{
   /** The sound clip */
   Clip clip=null;

   /**Player Listener. */
   AudioClipPlayerListener p_listener=null; 


   /**Crea il SoundPlayer
 * @param String filename
 * @param AudioClipPlayerListener listener
 */
public AudioClipPlayer(String filename, AudioClipPlayerListener listener)
   {  try
      {  FileInputStream inputstream=new FileInputStream(new File(filename));
         AudioInputStream audio_inputstream=AudioSystem.getAudioInputStream(inputstream);
         init(audio_inputstream,listener);
      }
      catch (Exception e) { e.printStackTrace(); }
   }


   /** Crea il SoundPlayer
    * @param File file
    * @param AudioClipPlayerListener listener
    */
public AudioClipPlayer(File file, AudioClipPlayerListener listener)
   {  try
      {  AudioInputStream audio_inputstream=AudioSystem.getAudioInputStream(new FileInputStream(file));
         init(audio_inputstream,listener);
      }
      catch (Exception e) { e.printStackTrace(); }
   }


   /** Crea il SoundPlayer
 * @param InputStream inputstream
 * @param AudioClipPlayerListener listener
 */
public AudioClipPlayer(InputStream inputstream, AudioClipPlayerListener listener)
   {  try
      {  AudioInputStream audio_inputstream=AudioSystem.getAudioInputStream(inputstream);
         init(audio_inputstream,listener);
      }
      catch (Exception e) { e.printStackTrace(); }
   }


   /**Crea il SoundPlayer
 * @param AudioInputStream audio_inputstream
 * @param AudioClipPlayerListener listener
 */
public AudioClipPlayer(AudioInputStream audio_inputstream, AudioClipPlayerListener listener)
   {  
      init(audio_inputstream,listener);
   }


	/** Inizializza il SoundPlayer. */
   void init(AudioInputStream audio_inputstream, AudioClipPlayerListener listener)
   {  p_listener=listener;
      if (audio_inputstream!=null)
      try
      {  AudioFormat format=audio_inputstream.getFormat();
         DataLine.Info info=new DataLine.Info(Clip.class,format);
         clip=(Clip)AudioSystem.getLine(info);
         clip.addLineListener(this);
         clip.open(audio_inputstream);
      }
      catch (Exception e) { e.printStackTrace(); }
   }

   /** Loop del suono finoallo stop. */
   public void loop()
   {  
      loop(0);
   }


   /** Loopa il suono n volte, se <i>n</i>=0 il loop termina. */
   public void loop(int n)
   {  
      rewind(); 
      if (clip!=null)
      {  if (n<=0) clip.loop(Clip.LOOP_CONTINUOUSLY);
         else clip.loop(n-1);
      }
   }

   /** Play. */
   public void play()
   {  
      if (clip!=null) clip.start();
   }


   /** Stop e Rewind. */
   public void stop()
   {  
      if (clip!=null) clip.stop();
   }


   /** Rewind*/
   public void rewind()
   {  
      if (clip!=null) clip.setMicrosecondPosition(0);
   }


   /** Va alla posizione indicata da <i>millisec</i> */
   public void goTo(long millisec)
   {  
      if (clip!=null) clip.setMicrosecondPosition(millisec);
   }


   /** Riproduce il suono dall'inizio */
   public void replay()
   {  
      if (clip!=null) { rewind(); clip.start(); }
   }


   /**Aggiorna lo stato di linea. */
   public void update(LineEvent event)
   {
      if (event.getType().equals(LineEvent.Type.STOP))
      {  //System.out.println("DEBUG: clip stop");
         //clip.close();
         if (p_listener!=null) p_listener.onAudioClipStop(this);
      }
   }


}


