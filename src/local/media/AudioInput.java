package local.media;


import javax.sound.sampled.*;
import java.io.InputStream;


/** AudioInput consente l'accesso al sistema dell'audio in input, utilizzando javax.sound.
*/
public class AudioInput
{



   static boolean DEBUG=false;
   
   static final int INTERNAL_BUFFER_SIZE=40960;
   
   static TargetDataLine target_line=null;
   
   
   /** Inizializza il sistema dell'audio in input. */
   public static void initAudioLine()
   {
      float fFrameRate=8000.0F;
      AudioFormat format=new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, fFrameRate, 16, 1, 2, fFrameRate, false);

      DataLine.Info lineInfo=new DataLine.Info(TargetDataLine.class,format,INTERNAL_BUFFER_SIZE);
      
      if (!AudioSystem.isLineSupported(lineInfo))
      {  System.err.println("ERROR: AudioLine not supported by this System.");
      }
      try
      {  target_line=(TargetDataLine)AudioSystem.getLine(lineInfo);
         if (DEBUG) println("TargetDataLine: "+target_line);
         target_line.open(format,INTERNAL_BUFFER_SIZE);
      }
      catch (LineUnavailableException e)
      {  System.err.println("ERROR: LineUnavailableException at AudioSender()");
         e.printStackTrace();
      }
   }

   /** Chiude il sistema audio in input*/
   static public void closeAudioLine()
   {  target_line.close();
   }





   AudioInputStream audio_input_stream=null;


   /** Costruttore di un AudioInput con audio_format=[8000 Hz, ULAW, 8bit, Mono]*/
   public AudioInput()
   {  init(null);
   }          

   /** Costruisce un AudioInput. */
   public AudioInput(AudioFormat audio_format)
   {  init(audio_format);
   }          
      
   /** Inizializza l' AudioInput. */
   private void init(AudioFormat format)
   {
      if (target_line==null) initAudioLine(); 
      
      if (format==null) 
      {  // by default use 8000 Hz, ULAW, 8bit, Mono
         float fFrameRate=8000.0F;
         format=new AudioFormat(AudioFormat.Encoding.ULAW, fFrameRate, 8, 1, 1, fFrameRate, false);
      }
      
      if (target_line.isOpen())
      {  audio_input_stream=new AudioInputStream(target_line);
         // convert the audio stream to the selected format
         audio_input_stream=AudioSystem.getAudioInputStream(format,audio_input_stream);
      }
      else
      {  System.err.print("WARNING: Audio init error: target line is not open.");
      }
   }          


   /** Restituisce l'audio come InputStream. */
   public InputStream getInputStream()
   {  return audio_input_stream;
   }



   /** Starts playing */
   public void play()
   {  if (target_line.isOpen()) target_line.start();
      else
      {  System.err.print("WARNING: Audio play error: target line is not open.");
      }
   }


   /** Stops playing */
   public void stop()
   {  if (target_line.isOpen()) target_line.stop();
      else
      {  System.err.print("WARNING: Audio stop error: target line is not open.");
      }
      //target_line.close();
   }


   /** Debug output */
   private static void println(String str)
   {  System.out.println("AudioInput: "+str);
   }


}